package co.medellin.transit.routing;

import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.geocoding.ParsedAddress;
import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Analyzes perceived costs for specific test cases to understand routing differences.
 */
class CostAnalysisTest {
    
    private static final String DATA_DIR = "data";
    
    private static TransitGraph graph;
    private static Router router;
    private static StepBuilder stepBuilder;
    private static Parameters params;
    private static AddressParser addressParser;
    private static AddressLocator addressLocator;
    
    @BeforeAll
    static void setUp() throws IOException {
        graph = new GraphBuilder().build(Paths.get(DATA_DIR));
        params = new Parameters(500.0, 120, 1.5);
        router = new Router(graph, params);
        Path dataPath = Paths.get(DATA_DIR);
        VehicleLabelFormatter vehicleLabelFormatter = new VehicleLabelFormatter(graph, dataPath);
        stepBuilder = new StepBuilder(graph, vehicleLabelFormatter);
        addressParser = new AddressParser();
        addressLocator = new AddressLocator();
    }
    
    @Test
    @DisplayName("Case 2: Metro A - analyze 2-minute shortfall")
    void analyzeCase2() {
        System.out.println("\n=== CASE 2 ANALYSIS ===");
        System.out.println("Expected: 34 min (2015s), metro:A");
        System.out.println("Actual: 32 min (1920s), metro:A");
        System.out.println("Gap: 2 min (95s)");
        System.out.println("\nReference steps:");
        System.out.println("  1. walk 662m / 689s");
        System.out.println("  2. metro:A wait 240s + ride 747s = 987s");
        System.out.println("  3. walk 326m / 339s");
        System.out.println("  Total: 689 + 987 + 339 = 2015s");
        
        Optional<Coordinate> origin = geocode("CL 10 43A-20");
        Optional<Coordinate> dest = geocode("CL 77 65-30");
        
        if (origin.isPresent() && dest.isPresent()) {
            Optional<DijkstraResult> result = router.findRoute(
                new Place("Origin", origin.get()),
                new Place("Destination", dest.get())
            );
            
            if (result.isPresent()) {
                Trip trip = stepBuilder.buildTrip(result.get());
                System.out.println("\nJava implementation:");
                System.out.println("  Access walk: " + result.get().getAccessWalkTimeSec() + "s (" + 
                    String.format("%.0f", result.get().getAccessWalkMeters()) + "m)");
                
                for (Step step : trip.getSteps()) {
                    if (step.getType() == StepType.BOARD || step.getType() == StepType.RIDE) {
                        System.out.println("  " + step.getType() + ": " + step.getDurationSec() + "s - " + step.getDescription());
                    }
                }
                
                System.out.println("  Egress walk: " + result.get().getEgressWalkTimeSec() + "s (" + 
                    String.format("%.0f", result.get().getEgressWalkMeters()) + "m)");
                System.out.println("  Total real duration: " + result.get().getRealDurationSec() + "s");
            }
        }
    }
    
    @Test
    @DisplayName("Case 6: Compute perceived costs for both routes")
    void analyzeCase6() {
        System.out.println("\n=== CASE 6 ANALYSIS ===");
        System.out.println("Expected: bus:243 > metro:B > bus:91 (46 min, 2 transfers)");
        System.out.println("Actual: bus:240 > bus:105 (46 min, 1 transfer)");
        System.out.println("\nReference route perceived cost:");
        System.out.println("  Access walk 211m/219s: (219*1.5)+30 = 359");
        System.out.println("  Board bus:243: 300+120 = 420");
        System.out.println("  Ride bus:243: 227s (527-300 wait)");
        System.out.println("  Transfer walk 67m/70s: (70*1.5)+30 = 135");
        System.out.println("  Board metro:B: 240+120 = 360");
        System.out.println("  Ride metro:B: 331s (571-240 wait)");
        System.out.println("  Transfer walk 168m/175s: (175*1.5)+30 = 293");
        System.out.println("  Board bus:91: 300+120 = 420");
        System.out.println("  Ride bus:91: 876s (1176-300 wait)");
        System.out.println("  Egress walk 1m/1s: (1*1.5)+30 = 32");
        System.out.println("  TOTAL: 359+420+227+135+360+331+293+420+876+32 = 3453");
        
        Optional<Coordinate> origin = geocode("CR 82 x CL 51B");
        Optional<Coordinate> dest = geocode("CL 57A 30-87");
        
        if (origin.isPresent() && dest.isPresent()) {
            Optional<DijkstraResult> result = router.findRoute(
                new Place("Origin", origin.get()),
                new Place("Destination", dest.get())
            );
            
            if (result.isPresent()) {
                Trip trip = stepBuilder.buildTrip(result.get());
                System.out.println("\nJava route perceived cost:");
                
                int perceivedCost = 0;
                
                // Access walk
                int accessWalk = result.get().getAccessWalkTimeSec();
                int accessCost = (int)((accessWalk * 1.5) + 30);
                perceivedCost += accessCost;
                System.out.println("  Access walk " + accessWalk + "s: " + accessCost);
                
                // Steps
                for (Step step : trip.getSteps()) {
                    if (step.getType() == StepType.BOARD) {
                        // Boarding includes wait (in duration) + penalty (120)
                        int boardCost = step.getDurationSec() + 120;
                        perceivedCost += boardCost;
                        System.out.println("  Board " + step.getRouteInfo().orElse("?") + ": " + step.getDurationSec() + "+120 = " + boardCost);
                    } else if (step.getType() == StepType.RIDE) {
                        perceivedCost += step.getDurationSec();
                        System.out.println("  Ride " + step.getRouteInfo().orElse("?") + ": " + step.getDurationSec());
                    } else if (step.getType() == StepType.WALK) {
                        int walkCost = (int)((step.getDurationSec() * 1.5) + 30);
                        perceivedCost += walkCost;
                        System.out.println("  Transfer walk " + step.getDurationSec() + "s: " + walkCost);
                    }
                }
                
                // Egress walk
                int egressWalk = result.get().getEgressWalkTimeSec();
                int egressCost = (int)((egressWalk * 1.5) + 30);
                perceivedCost += egressCost;
                System.out.println("  Egress walk " + egressWalk + "s: " + egressCost);
                
                System.out.println("  TOTAL: " + perceivedCost);
                System.out.println("  Dijkstra reported: " + result.get().getPerceivedCost());
            }
        }
    }
    
    @Test
    @DisplayName("Case 9: Compare bus:255P vs bus:255D routes")
    void analyzeCase9() {
        System.out.println("\n=== CASE 9 ANALYSIS ===");
        System.out.println("Expected: bus:255P > cable_car:J > metro:B > bus:90");
        System.out.println("Actual: bus:255D > cable_car:J > metro:B > bus:90");
        System.out.println("\nNote: 255P and 255D are different route variants");
        
        Optional<Coordinate> origin = geocode("CR 143A x CL 60C");
        Optional<Coordinate> dest = geocode("CL 59A x CR 35");
        
        if (origin.isPresent() && dest.isPresent()) {
            Optional<DijkstraResult> result = router.findRoute(
                new Place("Origin", origin.get()),
                new Place("Destination", dest.get())
            );
            
            if (result.isPresent()) {
                Trip trip = stepBuilder.buildTrip(result.get());
                System.out.println("\nJava found route:");
                for (Step step : trip.getSteps()) {
                    if (step.getType() == StepType.BOARD || step.getType() == StepType.RIDE) {
                        System.out.println("  " + step.getType() + ": " + step.getDescription());
                    }
                }
            }
        }
    }
    
    @Test
    @DisplayName("Case 13: bus:C23 vs bus:C23_Alterna")
    void analyzeCase13() {
        System.out.println("\n=== CASE 13 ANALYSIS ===");
        System.out.println("Expected: bus:301 > bus:C23_Alterna (2 boards, NO transfer walk between them)");
        System.out.println("Actual: bus:301 > bus:C23");
        System.out.println("\nReference shows buses share a stop at 'CL 44 79B-46' (no walk between)");
        System.out.println("This is a turnaround edge connecting last outbound to first inbound");
        
        Optional<Coordinate> origin = geocode("CL 30A x CR 82A");
        Optional<Coordinate> dest = geocode("CL 43 x CR 115A");
        
        if (origin.isPresent() && dest.isPresent()) {
            Optional<DijkstraResult> result = router.findRoute(
                new Place("Origin", origin.get()),
                new Place("Destination", dest.get())
            );
            
            if (result.isPresent()) {
                Trip trip = stepBuilder.buildTrip(result.get());
                System.out.println("\nJava found route:");
                for (Step step : trip.getSteps()) {
                    System.out.println("  " + step.getType() + ": " + step.getDescription());
                }
            }
        }
    }
    
    private Optional<Coordinate> geocode(String address) {
        Optional<ParsedAddress> parsed = addressParser.parse(address);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        return addressLocator.locate(parsed.get());
    }
}
