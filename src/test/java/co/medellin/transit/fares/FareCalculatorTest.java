package co.medellin.transit.fares;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FareCalculator using hand-crafted trips.
 * Tests each fare rule from docs/fares.md.
 */
class FareCalculatorTest {
    
    private FareCalculator calculator;
    private TransitGraph graph;
    private FareConfig config;
    
    @BeforeEach
    void setUp() {
        // Create a mock graph with route types
        graph = new TransitGraph(100);
        
        // Register known route types
        graph.setRouteType("90001", "Integrada");      // Mock Integrada bus
        graph.setRouteType("90002", "Directa Centro"); // Mock Directa Centro bus
        graph.setLineMode("A", "metro");
        graph.setLineMode("B", "metro");
        graph.setLineMode("J", "cable_car");
        graph.setLineMode("1", "brt");
        
        config = FareConfig.defaults();
        calculator = new FareCalculator(config, graph);
    }
    
    @Test
    @DisplayName("Walk-only trip costs 0 COP")
    void testWalkOnlyTrip() {
        Trip trip = createTrip(
            walk(100, 104)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(0, result.getTotalCOP(), "Walk-only trip should cost 0 COP");
        assertTrue(result.getSegments().isEmpty(), "Walk-only trip should have no fare segments");
        assertFalse(result.isIntegrationApplied(), "Walk-only trip has no integration");
    }
    
    @Test
    @DisplayName("Single bus boarding costs 3800 COP")
    void testSingleBusBoarding() {
        Trip trip = createTrip(
            walk(50, 52),
            board("90002", 300, 52),  // t0 = 52
            ride("90002", 600, 352),
            alight(952)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(3800, result.getTotalCOP(), "Single bus should cost 3800 COP");
        assertEquals(1, result.getSegments().size(), "Should have 1 fare segment");
    }
    
    @Test
    @DisplayName("Single metro boarding costs 3820 COP")
    void testSingleMetroBoarding() {
        Trip trip = createTrip(
            walk(100, 104),
            board("A", 240, 104),  // t0 = 104
            ride("A", 500, 344),
            alight(844)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(3820, result.getTotalCOP(), "Single metro should cost 3820 COP");
        assertEquals(1, result.getSegments().size(), "Should have 1 fare segment");
    }
    
    @Test
    @DisplayName("Metro -> cable car within 90 min costs 3820 COP (integration, no surcharge)")
    void testMetroCableCarIntegration() {
        Trip trip = createTrip(
            walk(100, 104),
            board("A", 240, 104),   // t0 = 104
            ride("A", 500, 344),
            alight(844),
            walk(50, 52),
            board("J", 120, 896),   // t0 = 896 (792 seconds = 13.2 min since first transit)
            ride("J", 300, 416),
            alight(1312)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(3820, result.getTotalCOP(), 
            "Metro -> cable car within 90 min should cost 3820 COP (no surcharge for 2 integrations)");
        assertFalse(result.getSegments().isEmpty(), "Should have fare segments");
    }
    
    @Test
    @DisplayName("Integrada bus -> metro costs 4715 COP (bus+transit integration)")
    void testIntegradaBusToMetroIntegration() {
        Trip trip = createTrip(
            walk(50, 52),
            board("90001", 300, 52),  // Integrada bus, t0 = 52
            ride("90001", 600, 352),
            alight(952),
            walk(100, 104),
            board("A", 240, 1056),    // Metro, t0 = 1056 (1004 sec = 16.7 min since bus)
            ride("A", 500, 344),
            alight(1400)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(4715, result.getTotalCOP(), 
            "Integrada bus -> metro should cost 4715 COP");
        assertFalse(result.getSegments().isEmpty(), "Should have fare segments");
        assertTrue(result.isIntegrationApplied(), "Integration should be applied");
    }
    
    @Test
    @DisplayName("Directa Centro bus -> metro costs 7620 COP (3800 + 3820, no integration)")
    void testDirectaCentroBusToMetroNoIntegration() {
        Trip trip = createTrip(
            walk(50, 52),
            board("90002", 300, 52),  // Directa Centro bus, t0 = 52
            ride("90002", 600, 352),
            alight(952),
            walk(100, 104),
            board("A", 240, 1056),    // Metro, t0 = 1056
            ride("A", 500, 344),
            alight(1400)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(7620, result.getTotalCOP(), 
            "Directa Centro bus -> metro should cost 7620 COP (no integration)");
        assertFalse(result.getSegments().isEmpty(), "Should have fare segments");
        assertFalse(result.isIntegrationApplied(), "No integration for Directa Centro");
    }
    
    @Test
    @DisplayName("Bus -> bus costs 7600 COP (3800 + 3800, no integration)")
    void testBusToBusNoIntegration() {
        Trip trip = createTrip(
            walk(50, 52),
            board("90002", 300, 52),  // First bus, t0 = 52
            ride("90002", 600, 352),
            alight(952),
            walk(50, 52),
            board("90001", 300, 1004), // Second bus, t0 = 1004
            ride("90001", 400, 304),
            alight(1308)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(7600, result.getTotalCOP(), 
            "Bus -> bus should cost 7600 COP (no integration)");
        assertFalse(result.getSegments().isEmpty(), "Should have fare segments");
        assertFalse(result.isIntegrationApplied(), "No integration for bus -> bus");
    }
    
    @Test
    @DisplayName("5 mass-transit integrations costs 3820 + 750 = 4570 COP (surcharge at 5th)")
    void testFiveIntegrationsSurcharge() {
        Trip trip = createTrip(
            walk(50, 52),
            board("A", 240, 52),      // 1st: metro A, t0 = 52
            ride("A", 300, 292),
            alight(592),
            walk(20, 21),
            board("B", 240, 613),     // 2nd: metro B, t0 = 613 (integration 1)
            ride("B", 300, 261),
            alight(913),
            walk(20, 21),
            board("A", 240, 934),     // 3rd: metro A, t0 = 934 (integration 2)
            ride("A", 300, 261),
            alight(1234),
            walk(20, 21),
            board("J", 120, 1255),    // 4th: cable J, t0 = 1255 (integration 3)
            ride("J", 300, 181),
            alight(1555),
            walk(20, 21),
            board("B", 240, 1576),    // 5th: metro B, t0 = 1576 (integration 4)
            ride("B", 300, 261),
            alight(1876),
            walk(20, 21),
            board("1", 300, 1897)     // 6th: brt 1, t0 = 1897 (integration 5, +750 surcharge)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(4570, result.getTotalCOP(), 
            "5 integrations should cost 3820 + 750 = 4570 COP");
    }
    
    @Test
    @DisplayName("Mass-transit transfer after 90 minutes pays again (3820 + 3820)")
    void testTransferAfter90Minutes() {
        Trip trip = createTrip(
            walk(50, 52),
            board("A", 240, 52),      // First metro, t0 = 52
            ride("A", 1000, 292),
            alight(1292),
            walk(50, 52),
            board("B", 240, 1344),    // Second metro, t0 = 1344
            ride("B", 4000, 1292),    // Wait long enough to exceed 90 min
            alight(5636),
            walk(50, 52),
            board("J", 120, 5688)     // Third transit, t0 = 5688 (5636 sec = 94 min since first)
        );
        
        FareResult result = calculator.calculate(trip);
        
        // First metro: 3820
        // Second metro: free (within 90 min)
        // Third cable car: 3820 (more than 90 min since first, starts new window)
        assertEquals(7640, result.getTotalCOP(), 
            "Transfer after 90 minutes should pay full fare again");
    }
    
    @Test
    @DisplayName("Metro -> Integrada bus costs 4715 COP (transit+bus integration)")
    void testMetroToIntegradaBusIntegration() {
        Trip trip = createTrip(
            walk(100, 104),
            board("A", 240, 104),     // Metro, t0 = 104
            ride("A", 500, 344),
            alight(844),
            walk(50, 52),
            board("90001", 300, 896), // Integrada bus, t0 = 896 (792 sec = 13.2 min since metro)
            ride("90001", 600, 352),
            alight(1248)
        );
        
        FareResult result = calculator.calculate(trip);
        
        assertEquals(4715, result.getTotalCOP(), 
            "Metro -> Integrada bus should cost 4715 COP");
        assertTrue(result.isIntegrationApplied(), "Integration should be applied");
    }
    
    @Test
    @DisplayName("Surcharge at 8th integration (9 boardings)")
    void testEightIntegrationsSurcharge() {
        // Build a trip with 9 mass-transit boardings
        // In prototype logic: 1st boarding sets integrations=1, then 2nd increments to 2, etc.
        // So 9 boardings → integrations reaches 9 → surcharges at 5, 8, 9
        List<Step> steps = new ArrayList<>();
        steps.add(walk(50, 52));
        
        int time = 52;
        for (int i = 0; i < 9; i++) {  // 9 boardings
            String line = (i % 2 == 0) ? "A" : "B";
            steps.add(board(line, 240, time));
            time += 240;
            steps.add(ride(line, 300, time));
            time += 300;
            steps.add(alight(time));
            
            if (i < 8) {
                steps.add(walk(20, 21));
                time += 21;
            }
        }
        
        Trip trip = createTrip(steps.toArray(new Step[0]));
        FareResult result = calculator.calculate(trip);
        
        // Base: 3820, +750 at integration 5, +750 at 8, +750 at 9 = 6070
        assertEquals(6070, result.getTotalCOP(), 
            "9 boardings should cost 3820 + 750 + 750 + 750 = 6070 COP");
    }
    
    @Test
    @DisplayName("Surcharge at 9th integration")
    void testNineIntegrationsSurcharge() {
        // Build a trip with 9 mass-transit transfers
        List<Step> steps = new ArrayList<>();
        steps.add(walk(50, 52));
        
        int time = 52;
        for (int i = 0; i < 10; i++) {
            String line = (i % 2 == 0) ? "A" : "B";
            steps.add(board(line, 240, time));
            time += 240;
            steps.add(ride(line, 300, time));
            time += 300;
            steps.add(alight(time));
            
            if (i < 9) {
                steps.add(walk(20, 21));
                time += 21;
            }
        }
        
        Trip trip = createTrip(steps.toArray(new Step[0]));
        FareResult result = calculator.calculate(trip);
        
        // Base: 3820, +750 at 5th, +750 at 8th, +750 at 9th = 6070
        assertEquals(6070, result.getTotalCOP(), 
            "9 integrations should cost 3820 + 750 + 750 + 750 = 6070 COP");
    }
    
    // === Helper methods to create trip components ===
    
    private Trip createTrip(Step... steps) {
        List<Step> stepList = List.of(steps);
        int totalDuration = stepList.stream().mapToInt(Step::getDurationSec).sum();
        return new Trip(stepList, totalDuration, 0.0);
    }
    
    private Step walk(int meters, int durationSec) {
        Place from = new Place("A", new Coordinate(6.25, -75.57));
        return new Step(StepType.WALK, "Camina", durationSec, from);
    }
    
    private Step board(String routeId, int waitSec, int startTime) {
        Place at = new Place("Stop " + routeId, new Coordinate(6.25, -75.57));
        return new Step(StepType.BOARD, "Aborda", waitSec, at, Optional.of(routeId));
    }
    
    private Step ride(String routeId, int durationSec, int startTime) {
        Place at = new Place("In transit", new Coordinate(6.25, -75.56));
        return new Step(StepType.RIDE, "Viaja", durationSec, at, Optional.of(routeId));
    }
    
    private Step alight(int time) {
        Place at = new Place("Destination", new Coordinate(6.25, -75.56));
        return new Step(StepType.ALIGHT, "Baja", 0, at);
    }
}
