package co.medellin.transit.routing;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareConfig;
import co.medellin.transit.fares.FareResult;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.geocoding.ParsedAddress;
import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.Trip;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reference test that validates the Java implementation against
 * the Python prototype results stored in test_cases.csv.
 * 
 * Each test case contains:
 * - origin: address string
 * - destination: address string
 * - minutes: expected trip duration
 * - cost_cop: expected fare in Colombian Pesos
 * - transfers: expected number of transfers
 * - vehicles: sequence of vehicles used (e.g., "bus:191", "metro:A > bus:90")
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReferenceTest {
    
    private static final String DATA_DIR = "C:\\Users\\marti\\Downloads\\transporte-medellin\\kiro\\data";
    private static final String TEST_CASES_FILE = DATA_DIR + "\\test_cases.csv";
    
    private static TransitGraph graph;
    private static Router router;
    private static FareCalculator fareCalculator;
    private static AddressParser addressParser;
    private static AddressLocator addressLocator;
    private static StepBuilder stepBuilder;
    private static Parameters params;
    
    private static List<TestCase> testCases = new ArrayList<>();
    
    @BeforeAll
    static void setUpAll() throws IOException {
        System.out.println("=== Reference Test ===");
        System.out.println("Loading graph and test cases...\n");
        
        // Load graph
        long startTime = System.currentTimeMillis();
        graph = new GraphBuilder().build(Paths.get(DATA_DIR));
        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("Graph loaded in " + loadTime + "ms");
        System.out.println("States: " + graph.getStateCount());
        System.out.println("Edges: " + graph.getEdgeCount());
        System.out.println();
        
        // Initialize components
        params = new Parameters(500.0, 120, 1.5);
        router = new Router(graph, params);
        stepBuilder = new StepBuilder(graph);
        fareCalculator = new FareCalculator(FareConfig.defaults(), graph);
        addressParser = new AddressParser();
        addressLocator = new AddressLocator();
        
        // Load test cases
        loadTestCases();
        System.out.println("Loaded " + testCases.size() + " test cases\n");
    }
    
    @Test
    @Order(1)
    @DisplayName("Validate all reference test cases")
    void runAllTestCases() {
        System.out.println("Running " + testCases.size() + " test cases...\n");
        
        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            runTestCase(i + 1, testCase);
        }
        
        System.out.println("\nReference test completed.");
    }
    
    private void runTestCase(int testNumber, TestCase testCase) {
        try {
            // Geocode origin
            Optional<Coordinate> originCoord = geocodeAddress(testCase.origin);
            if (originCoord.isEmpty()) {
                fail("Test #" + testNumber + ": Failed to geocode origin: " + testCase.origin);
            }
            
            // Geocode destination
            Optional<Coordinate> destCoord = geocodeAddress(testCase.destination);
            if (destCoord.isEmpty()) {
                fail("Test #" + testNumber + ": Failed to geocode destination: " + testCase.destination);
            }
            
            Place origin = new Place("Origin", originCoord.get());
            Place destination = new Place("Destination", destCoord.get());
            
            // Find route
            Optional<DijkstraResult> dijkstraResult = router.findRoute(origin, destination);
            
            // Handle walk-only case (no route needed)
            if (testCase.vehicles.equals("walk")) {
                if (dijkstraResult.isEmpty()) {
                    System.out.println("Test #" + testNumber + " PASSED (walk-only)");
                    return;
                }
            }
            
            if (dijkstraResult.isEmpty()) {
                fail("Test #" + testNumber + ": No route found for " + testCase.origin + " -> " + testCase.destination);
            }
            
            // Build trip
            Trip trip = stepBuilder.buildTrip(dijkstraResult.get());
            
            // Assert route matches expectations
            assertRouteMatches(trip, testCase, testNumber);
            
            System.out.println("Test #" + testNumber + " PASSED");
            
        } catch (AssertionError e) {
            throw e; // Re-throw assertion failures
        } catch (Exception e) {
            fail("Test #" + testNumber + ": Exception - " + e.getMessage(), e);
        }
    }
    
    /**
     * Asserts that the computed trip matches the expected test case results.
     * 
     * @param trip The computed trip
     * @param testCase The expected test case
     * @param testNumber The test case number for error messages
     */
    private void assertRouteMatches(Trip trip, TestCase testCase, int testNumber) {
        // Calculate actual values
        int actualMinutes = (int) Math.ceil(trip.getTotalDurationSec() / 60.0);
        FareResult fareResult = fareCalculator.calculate(trip);
        int actualCost = fareResult.getTotalCOP();
        int actualTransfers = countTransfers(trip);
        String actualVehicles = extractVehicleSequence(trip);
        
        // Assert travel time with ±2 minutes tolerance
        int timeDiff = Math.abs(actualMinutes - testCase.expectedMinutes);
        assertTrue(timeDiff <= 2,
                String.format("Test #%d: Travel time mismatch - expected %d min, got %d min (diff: %d min, tolerance: ±2 min)",
                        testNumber, testCase.expectedMinutes, actualMinutes, timeDiff));
        
        // Assert cost with ±100 COP tolerance
        int costDiff = Math.abs(actualCost - testCase.expectedCost);
        assertTrue(costDiff <= 100,
                String.format("Test #%d: Cost mismatch - expected $%d COP, got $%d COP (diff: $%d COP, tolerance: ±100 COP)",
                        testNumber, testCase.expectedCost, actualCost, costDiff));
        
        // Assert transfers (exact match)
        assertEquals(testCase.expectedTransfers, actualTransfers,
                String.format("Test #%d: Transfer count mismatch - expected %d, got %d",
                        testNumber, testCase.expectedTransfers, actualTransfers));
        
        // Assert vehicle sequence (exact match - comma-separated route names)
        assertEquals(testCase.vehicles, actualVehicles,
                String.format("Test #%d: Vehicle sequence mismatch - expected '%s', got '%s'",
                        testNumber, testCase.vehicles, actualVehicles));
    }
    
    private Optional<Coordinate> geocodeAddress(String addressStr) {
        Optional<ParsedAddress> parsed = addressParser.parse(addressStr);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        
        return addressLocator.locate(parsed.get());
    }
    
    private String extractVehicleSequence(Trip trip) {
        List<String> vehicles = new ArrayList<>();
        String currentVehicle = null;
        
        for (Step step : trip.getSteps()) {
            if (step.getType() == co.medellin.transit.model.StepType.BOARD) {
                // Extract route from description (e.g., "Abordar Ruta 191 en...")
                String desc = step.getDescription();
                if (desc.contains("Ruta ")) {
                    int startIdx = desc.indexOf("Ruta ") + 5;
                    int endIdx = desc.indexOf(" ", startIdx);
                    if (endIdx == -1) endIdx = desc.indexOf(" en", startIdx);
                    if (endIdx != -1) {
                        currentVehicle = desc.substring(startIdx, endIdx).trim();
                    }
                } else if (desc.contains("Línea ")) {
                    int startIdx = desc.indexOf("Línea ") + 6;
                    int endIdx = desc.indexOf(" ", startIdx);
                    if (endIdx == -1) endIdx = desc.indexOf(" en", startIdx);
                    if (endIdx != -1) {
                        currentVehicle = "metro:" + desc.substring(startIdx, endIdx).trim();
                    }
                }
            } else if (step.getType() == co.medellin.transit.model.StepType.ALIGHT && currentVehicle != null) {
                vehicles.add(currentVehicle);
                currentVehicle = null;
            }
        }
        
        if (vehicles.isEmpty()) {
            return "walk";
        }
        
        return String.join(" > ", vehicles);
    }
    
    private int countTransfers(Trip trip) {
        int boards = 0;
        for (Step step : trip.getSteps()) {
            if (step.getType() == co.medellin.transit.model.StepType.BOARD) {
                boards++;
            }
        }
        return Math.max(0, boards - 1); // transfers = boards - 1
    }
    
    private static void loadTestCases() throws IOException {
        Path testCasesPath = Paths.get(TEST_CASES_FILE);
        
        if (!Files.exists(testCasesPath)) {
            System.err.println("WARNING: test_cases.csv not found at: " + TEST_CASES_FILE);
            System.err.println("Skipping reference test.");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_CASES_FILE))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 6) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
                
                try {
                    TestCase testCase = new TestCase();
                    testCase.origin = parts[0].trim();
                    testCase.destination = parts[1].trim();
                    testCase.expectedMinutes = Integer.parseInt(parts[2].trim());
                    testCase.expectedCost = Integer.parseInt(parts[3].trim());
                    testCase.expectedTransfers = Integer.parseInt(parts[4].trim());
                    testCase.vehicles = parts[5].trim();
                    
                    testCases.add(testCase);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line with invalid numbers: " + line);
                }
            }
        }
    }
    
    static class TestCase {
        String origin;
        String destination;
        int expectedMinutes;
        int expectedCost;
        int expectedTransfers;
        String vehicles;
    }
}
