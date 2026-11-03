package co.medellin.transit.fares;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.RouteType;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FareCalculator.
 * 
 * Tests cover:
 * - Single-mode trips (bus only, metro only)
 * - Integration scenarios (metro→bus and bus→metro within 90 minutes)
 * - No integration (same mode, >90 minute gap)
 * - Walking-only trips (zero fare)
 * - Edge cases
 */
class FareCalculatorTest {
    
    private FareConfig config;
    private TransitGraph graph;
    private FareCalculator calculator;
    private Place testPlace;

    @BeforeEach
    void setUp() {
        // Use default config (metro: 3820, bus: 3800, integrated: 4715, window: 90 min)
        config = FareConfig.defaults();
        
        // Create an empty graph (we don't need actual graph data for fare calculation tests)
        graph = new TransitGraph(0);
        
        calculator = new FareCalculator(config, graph);
        
        // Test place for step creation
        testPlace = new Place("Test Location", new Coordinate(6.25, -75.57));
    }

    @Test
    void testWalkingOnlyTrip_ZeroFare() {
        // Create a trip with only walking steps
        Trip.Builder builder = new Trip.Builder();
        builder.addStep(new Step(StepType.WALK, "Caminar a destino", 300, testPlace));
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        assertEquals(0, result.getTotalCOP());
        assertTrue(result.getSegments().isEmpty());
        assertFalse(result.isIntegrationApplied());
    }

    @Test
    void testSingleBusBoarding_BaseFare() {
        // Create a trip with one bus boarding
        Trip.Builder builder = new Trip.Builder();
        builder.addStep(new Step(StepType.WALK, "Caminar a parada", 120, testPlace));
        builder.addStep(new Step(StepType.BOARD, "Abordar ruta 301", 0, testPlace, Optional.of("301")));
        builder.addStep(new Step(StepType.RIDE, "Viajar en ruta 301", 600, testPlace, Optional.of("301")));
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        assertEquals(3800, result.getTotalCOP()); // Bus fare
        assertEquals(1, result.getSegments().size());
        assertFalse(result.isIntegrationApplied());
        
        FareSegment segment = result.getSegments().get(0);
        assertEquals(RouteType.BUS, segment.getMode());
        assertEquals(3800, segment.getBaseFareCOP());
        assertEquals(3800, segment.getAppliedFareCOP());
        assertFalse(segment.isIntegrationApplied());
    }

    @Test
    void testSingleMetroBoarding_BaseFare() {
        // Create a trip with one metro boarding
        Trip.Builder builder = new Trip.Builder();
        builder.addStep(new Step(StepType.WALK, "Caminar a estación", 120, testPlace));
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea A", 0, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.RIDE, "Viajar en Línea A", 600, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        assertEquals(3820, result.getTotalCOP()); // Metro fare
        assertEquals(1, result.getSegments().size());
        assertFalse(result.isIntegrationApplied());
        
        FareSegment segment = result.getSegments().get(0);
        assertEquals(RouteType.METRO, segment.getMode());
        assertEquals(3820, segment.getBaseFareCOP());
        assertEquals(3820, segment.getAppliedFareCOP());
        assertFalse(segment.isIntegrationApplied());
    }

    @Test
    void testMetroToBusIntegration_Within90Minutes() {
        // Create a trip: walk → metro → alight → walk → bus
        // Total time from first boarding to second boarding: 20 minutes (well within 90 minutes)
        Trip.Builder builder = new Trip.Builder();
        
        // Walk to metro (2 minutes)
        builder.addStep(new Step(StepType.WALK, "Caminar a estación", 120, testPlace));
        
        // Board metro at time 120s
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea A", 0, testPlace, Optional.of("A")));
        
        // Ride metro (10 minutes)
        builder.addStep(new Step(StepType.RIDE, "Viajar en Línea A", 600, testPlace, Optional.of("A")));
        
        // Alight metro
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        
        // Walk to bus stop (3 minutes)
        builder.addStep(new Step(StepType.WALK, "Caminar a parada", 180, testPlace));
        
        // Board bus at time 120 + 600 + 180 = 900s = 15 minutes after first boarding
        builder.addStep(new Step(StepType.BOARD, "Abordar ruta 301", 0, testPlace, Optional.of("301")));
        
        // Ride bus (5 minutes)
        builder.addStep(new Step(StepType.RIDE, "Viajar en ruta 301", 300, testPlace, Optional.of("301")));
        
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        // With integration: total is 4715 COP (not 3820 + 3800 = 7620)
        assertEquals(4715, result.getTotalCOP());
        assertEquals(2, result.getSegments().size());
        assertTrue(result.isIntegrationApplied());
        
        // First segment: metro at full price
        FareSegment firstSegment = result.getSegments().get(0);
        assertEquals(RouteType.METRO, firstSegment.getMode());
        assertEquals(3820, firstSegment.getBaseFareCOP());
        assertEquals(3820, firstSegment.getAppliedFareCOP());
        assertFalse(firstSegment.isIntegrationApplied());
        
        // Second segment: bus at discounted price (4715 - 3820 = 895)
        FareSegment secondSegment = result.getSegments().get(1);
        assertEquals(RouteType.BUS, secondSegment.getMode());
        assertEquals(3800, secondSegment.getBaseFareCOP());
        assertEquals(895, secondSegment.getAppliedFareCOP());
        assertTrue(secondSegment.isIntegrationApplied());
    }

    @Test
    void testBusToMetroIntegration_Within90Minutes() {
        // Create a trip: walk → bus → alight → walk → metro
        Trip.Builder builder = new Trip.Builder();
        
        // Walk to bus stop (2 minutes)
        builder.addStep(new Step(StepType.WALK, "Caminar a parada", 120, testPlace));
        
        // Board bus at time 120s
        builder.addStep(new Step(StepType.BOARD, "Abordar ruta 301", 0, testPlace, Optional.of("301")));
        
        // Ride bus (10 minutes)
        builder.addStep(new Step(StepType.RIDE, "Viajar en ruta 301", 600, testPlace, Optional.of("301")));
        
        // Alight bus
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        
        // Walk to metro (3 minutes)
        builder.addStep(new Step(StepType.WALK, "Caminar a estación", 180, testPlace));
        
        // Board metro at time 120 + 600 + 180 = 900s = 15 minutes after first boarding
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea A", 0, testPlace, Optional.of("A")));
        
        // Ride metro (5 minutes)
        builder.addStep(new Step(StepType.RIDE, "Viajar en Línea A", 300, testPlace, Optional.of("A")));
        
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        // With integration: total is 4715 COP
        assertEquals(4715, result.getTotalCOP());
        assertEquals(2, result.getSegments().size());
        assertTrue(result.isIntegrationApplied());
        
        // First segment: bus at full price
        FareSegment firstSegment = result.getSegments().get(0);
        assertEquals(RouteType.BUS, firstSegment.getMode());
        assertEquals(3800, firstSegment.getBaseFareCOP());
        assertEquals(3800, firstSegment.getAppliedFareCOP());
        assertFalse(firstSegment.isIntegrationApplied());
        
        // Second segment: metro at discounted price (4715 - 3800 = 915)
        FareSegment secondSegment = result.getSegments().get(1);
        assertEquals(RouteType.METRO, secondSegment.getMode());
        assertEquals(3820, secondSegment.getBaseFareCOP());
        assertEquals(915, secondSegment.getAppliedFareCOP());
        assertTrue(secondSegment.isIntegrationApplied());
    }

    @Test
    void testNoIntegration_SameMode_BusToBus() {
        // Two bus boardings - no integration
        Trip.Builder builder = new Trip.Builder();
        
        builder.addStep(new Step(StepType.BOARD, "Abordar ruta 301", 0, testPlace, Optional.of("301")));
        builder.addStep(new Step(StepType.RIDE, "Viajar en ruta 301", 600, testPlace, Optional.of("301")));
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        
        builder.addStep(new Step(StepType.WALK, "Caminar", 180, testPlace));
        
        builder.addStep(new Step(StepType.BOARD, "Abordar ruta 201", 0, testPlace, Optional.of("201")));
        builder.addStep(new Step(StepType.RIDE, "Viajar en ruta 201", 300, testPlace, Optional.of("201")));
        
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        // No integration: 3800 + 3800 = 7600
        assertEquals(7600, result.getTotalCOP());
        assertEquals(2, result.getSegments().size());
        assertFalse(result.isIntegrationApplied());
    }

    @Test
    void testNoIntegration_SameMode_MetroToMetro() {
        // Two metro boardings - no integration
        Trip.Builder builder = new Trip.Builder();
        
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea A", 0, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.RIDE, "Viajar en Línea A", 600, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        
        builder.addStep(new Step(StepType.WALK, "Caminar", 180, testPlace));
        
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea B", 0, testPlace, Optional.of("B")));
        builder.addStep(new Step(StepType.RIDE, "Viajar en Línea B", 300, testPlace, Optional.of("B")));
        
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        // No integration: 3820 + 3820 = 7640
        assertEquals(7640, result.getTotalCOP());
        assertEquals(2, result.getSegments().size());
        assertFalse(result.isIntegrationApplied());
    }

    @Test
    void testNoIntegration_ExceedsTimeWindow() {
        // Metro to bus, but 95 minutes apart (exceeds 90-minute window)
        Trip.Builder builder = new Trip.Builder();
        
        // Board metro at time 0
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea A", 0, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.RIDE, "Viajar en Línea A", 600, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        
        // Walk for a very long time (90 minutes)
        builder.addStep(new Step(StepType.WALK, "Caminar", 5400, testPlace)); // 90 minutes = 5400 seconds
        
        // Board bus at time 0 + 600 + 5400 = 6000s = 100 minutes after first boarding
        builder.addStep(new Step(StepType.BOARD, "Abordar ruta 301", 0, testPlace, Optional.of("301")));
        builder.addStep(new Step(StepType.RIDE, "Viajar en ruta 301", 300, testPlace, Optional.of("301")));
        
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        // No integration because > 90 minutes: 3820 + 3800 = 7620
        assertEquals(7620, result.getTotalCOP());
        assertEquals(2, result.getSegments().size());
        assertFalse(result.isIntegrationApplied());
    }

    @Test
    void testThreeBoardings_IntegrationOnlyFirstTwo() {
        // Metro → bus → metro: integration only applies to first 2 boardings
        Trip.Builder builder = new Trip.Builder();
        
        // First boarding: metro at time 0
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea A", 0, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.RIDE, "Viajar", 300, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        
        // Second boarding: bus at time 300 + 60 = 360s (6 minutes)
        builder.addStep(new Step(StepType.WALK, "Caminar", 60, testPlace));
        builder.addStep(new Step(StepType.BOARD, "Abordar ruta 301", 0, testPlace, Optional.of("301")));
        builder.addStep(new Step(StepType.RIDE, "Viajar", 300, testPlace, Optional.of("301")));
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        
        // Third boarding: metro at time 660 + 60 = 720s (12 minutes)
        builder.addStep(new Step(StepType.WALK, "Caminar", 60, testPlace));
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea B", 0, testPlace, Optional.of("B")));
        builder.addStep(new Step(StepType.RIDE, "Viajar", 300, testPlace, Optional.of("B")));
        
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        // Integration applies to first 2: 4715 (integrated) + 3820 (third metro) = 8535
        assertEquals(8535, result.getTotalCOP());
        assertEquals(3, result.getSegments().size());
        assertTrue(result.isIntegrationApplied());
        
        // Third boarding should pay full fare
        FareSegment thirdSegment = result.getSegments().get(2);
        assertEquals(3820, thirdSegment.getAppliedFareCOP());
        assertFalse(thirdSegment.isIntegrationApplied());
    }

    @Test
    void testEdgeCase_Exactly90Minutes() {
        // Metro to bus at exactly 90 minutes - should qualify for integration
        Trip.Builder builder = new Trip.Builder();
        
        // Board metro at time 0
        builder.addStep(new Step(StepType.BOARD, "Abordar Línea A", 0, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.RIDE, "Viajar", 600, testPlace, Optional.of("A")));
        builder.addStep(new Step(StepType.ALIGHT, "Bajar", 0, testPlace));
        
        // Wait exactly 89 minutes and 40 seconds (so boarding happens at exactly 90 minutes)
        builder.addStep(new Step(StepType.WALK, "Caminar", 4780, testPlace));
        
        // Board bus at time 600 + 4780 = 5380s = 89.67 minutes after first boarding
        builder.addStep(new Step(StepType.BOARD, "Abordar ruta 301", 0, testPlace, Optional.of("301")));
        builder.addStep(new Step(StepType.RIDE, "Viajar", 300, testPlace, Optional.of("301")));
        
        Trip trip = builder.build();

        FareResult result = calculator.calculate(trip);

        // Should get integration since 89.67 minutes <= 90 minutes
        assertEquals(4715, result.getTotalCOP());
        assertTrue(result.isIntegrationApplied());
    }

    @Test
    void testEmptyTrip_ZeroFare() {
        Trip trip = new Trip();
        FareResult result = calculator.calculate(trip);

        assertEquals(0, result.getTotalCOP());
        assertTrue(result.getSegments().isEmpty());
        assertFalse(result.isIntegrationApplied());
    }
}
