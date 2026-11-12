package co.medellin.transit.ui;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareConfig;
import co.medellin.transit.fares.FareResult;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.geocoding.ParsedAddress;
import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;
import co.medellin.transit.routing.DijkstraResult;
import co.medellin.transit.routing.Parameters;
import co.medellin.transit.routing.Router;
import co.medellin.transit.routing.StepBuilder;
import co.medellin.transit.routing.VehicleLabelFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that trip times are consistent:
 * - Sum of step minutes equals displayed total (±1 min for rounding)
 * - Total matches REF| minutes from test_cases.csv
 */
class TimeConsistencyTest {
    
    private static final String DATA_DIR = "data";
    private static final Path DATA_PATH = Paths.get(DATA_DIR);
    private static final String TEST_CASES_FILE = DATA_DIR + "\\test_cases.csv";
    
    private static TransitGraph graph;
    private static Router router;
    private static FareCalculator fareCalculator;
    private static AddressParser addressParser;
    private static AddressLocator addressLocator;
    private static StepBuilder stepBuilder;
    
    private static class TestCase {
        String origin;
        String destination;
        int expectedMinutes;
        int expectedCost;
        int expectedTransfers;
        String expectedVehicles;
    }
    
    @BeforeAll
    static void setUpAll() throws IOException {
        System.out.println("Loading graph for time consistency test...");
        graph = new GraphBuilder().build(DATA_PATH);
        
        Parameters params = new Parameters(500.0, 120, 1.5);
        router = new Router(graph, params);
        VehicleLabelFormatter vehicleLabelFormatter = new VehicleLabelFormatter(graph, DATA_PATH);
        stepBuilder = new StepBuilder(graph, vehicleLabelFormatter);
        fareCalculator = new FareCalculator(FareConfig.defaults(), graph);
        addressParser = new AddressParser();
        addressLocator = new AddressLocator();
    }
    
    @Test
    void testTimeConsistencyForAll15ReferenceCases() throws IOException {
        List<TestCase> testCases = loadTestCases();
        assertEquals(15, testCases.size(), "Should have 15 reference cases");
        
        int passed = 0;
        List<String> failures = new ArrayList<>();
        
        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            int caseNum = i + 1;
            
            // Parse addresses
            ParsedAddress originAddr = addressParser.parse(tc.origin).orElse(null);
            ParsedAddress destAddr = addressParser.parse(tc.destination).orElse(null);
            
            if (originAddr == null || destAddr == null) {
                failures.add(String.format("Case %d: Could not parse addresses", caseNum));
                continue;
            }
            
            // Locate coordinates
            Coordinate originCoord = addressLocator.locate(originAddr).orElse(null);
            Coordinate destCoord = addressLocator.locate(destAddr).orElse(null);
            
            if (originCoord == null || destCoord == null) {
                failures.add(String.format("Case %d: Could not locate addresses", caseNum));
                continue;
            }
            
            // Route
            Optional<DijkstraResult> resultOpt = router.findRoute(
                    new co.medellin.transit.model.Place("origin", originCoord),
                    new co.medellin.transit.model.Place("destination", destCoord)
            );
            
            if (resultOpt.isEmpty()) {
                // Walk-only trip - check if expected is 0 cost
                if (tc.expectedCost == 0) {
                    passed++;
                    System.out.printf("Case %d OK: walk-only trip%n", caseNum);
                } else {
                    failures.add(String.format("Case %d: Routing failed", caseNum));
                }
                continue;
            }
            
            DijkstraResult result = resultOpt.get();
            if (result.getPath().isEmpty()) {
                failures.add(String.format("Case %d: Empty path", caseNum));
                continue;
            }
            
            // Build trip and calculate fare
            Trip trip = stepBuilder.buildTrip(result);
            FareResult fare = fareCalculator.calculate(trip);
            
            // Calculate displayed total (what the user sees in the header)
            int displayedTotal = (int) Math.round(trip.getTotalDurationSec() / 60.0);
            
            // Calculate sum of step minutes (what the user sees in the instructions)
            // Use largest-remainder method to match TripPrinter
            int stepSum = 0;
            int[] allocatedMinutes = allocateMinutesLargestRemainder(trip, displayedTotal);
            for (int j = 0; j < trip.getSteps().size(); j++) {
                Step step = trip.getSteps().get(j);
                if (step.getDurationSec() > 0) {
                    stepSum += allocatedMinutes[j];
                }
            }
            
            // Check 1: Sum of step minutes should equal displayed total (±1 for rounding)
            int tolerance = 1;
            int diff = Math.abs(stepSum - displayedTotal);
            if (diff > tolerance) {
                failures.add(String.format("Case %d (%s -> %s): Step sum %d min != displayed total %d min (diff %d)",
                        caseNum, tc.origin, tc.destination, stepSum, displayedTotal, diff));
                continue;
            }
            
            // Check 2: Displayed total should match REF minutes (±1)
            int refDiff = Math.abs(displayedTotal - tc.expectedMinutes);
            if (refDiff > 1) {
                failures.add(String.format("Case %d (%s -> %s): Displayed total %d min != REF %d min (diff %d)",
                        caseNum, tc.origin, tc.destination, displayedTotal, tc.expectedMinutes, refDiff));
                continue;
            }
            
            passed++;
            System.out.printf("Case %d OK: displayed=%d, steps=%d, REF=%d%n", 
                    caseNum, displayedTotal, stepSum, tc.expectedMinutes);
        }
        
        // Report results
        System.out.println();
        System.out.printf("Time consistency: %d/%d cases passed%n", passed, testCases.size());
        
        if (!failures.isEmpty()) {
            System.out.println("\nFailures:");
            failures.forEach(System.out::println);
            fail(String.format("%d cases failed time consistency check", failures.size()));
        }
    }
    
    /**
     * Allocates minutes to steps using the largest-remainder method.
     * This ensures that the sum of displayed step minutes equals the total.
     * Matches the logic in TripPrinter.
     */
    private static int[] allocateMinutesLargestRemainder(Trip trip, int totalMinutes) {
        List<Step> steps = trip.getSteps();
        int[] minutes = new int[steps.size()];
        double[] fractional = new double[steps.size()];
        int allocatedSum = 0;
        
        // First pass: allocate floor values and track remainders
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (step.getDurationSec() > 0) {
                double exactMinutes = step.getDurationSec() / 60.0;
                int floor = (int) exactMinutes;
                minutes[i] = floor;
                fractional[i] = exactMinutes - floor;
                allocatedSum += floor;
            } else {
                minutes[i] = 0;
                fractional[i] = 0.0;
            }
        }
        
        // Second pass: distribute remaining minutes to steps with largest remainders
        int remaining = totalMinutes - allocatedSum;
        while (remaining > 0) {
            int maxIndex = -1;
            double maxFractional = -1.0;
            for (int i = 0; i < steps.size(); i++) {
                if (fractional[i] > maxFractional && steps.get(i).getDurationSec() > 0) {
                    maxFractional = fractional[i];
                    maxIndex = i;
                }
            }
            
            if (maxIndex == -1) {
                break;
            }
            
            minutes[maxIndex]++;
            fractional[maxIndex] = -1.0;
            allocatedSum++;
            remaining--;
        }
        
        // Ensure minimum of 1 minute for visible steps
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (step.getDurationSec() > 0 && 
                step.getType() != StepType.BOARD && 
                step.getType() != StepType.ALIGHT && 
                minutes[i] == 0) {
                minutes[i] = 1;
            }
        }
        
        return minutes;
    }
    
    private static List<TestCase> loadTestCases() throws IOException {
        List<TestCase> cases = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_CASES_FILE))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 5) {
                    TestCase tc = new TestCase();
                    tc.origin = parts[0];
                    tc.destination = parts[1];
                    tc.expectedMinutes = Integer.parseInt(parts[2]);
                    tc.expectedCost = Integer.parseInt(parts[3]);
                    tc.expectedTransfers = Integer.parseInt(parts[4]);
                    tc.expectedVehicles = parts.length > 5 ? parts[5] : "";
                    cases.add(tc);
                }
            }
        }
        
        return cases;
    }
}
