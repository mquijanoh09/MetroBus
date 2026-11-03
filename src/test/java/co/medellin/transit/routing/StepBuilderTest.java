package co.medellin.transit.routing;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StepBuilder.
 * Tests conversion of routing paths to human-readable trip instructions.
 */
class StepBuilderTest {
    
    private TransitGraph graph;
    private StepBuilder builder;
    
    @BeforeEach
    void setUp() {
        graph = new TransitGraph(10);
        builder = new StepBuilder(graph);
    }
    
    @Test
    void testConstructorThrowsOnNullGraph() {
        assertThrows(NullPointerException.class, () -> new StepBuilder(null));
    }
    
    @Test
    void testBuildTripThrowsOnNullResult() {
        assertThrows(NullPointerException.class, () -> builder.buildTrip(null));
    }
    
    @Test
    void testBuildTripThrowsOnShortPath() {
        // Path with only 1 state is invalid
        Place origin = new Place("Origin", new Coordinate(6.25, -75.57));
        State state0 = new State(0, origin);
        graph.addState(state0);
        
        List<State> path = Arrays.asList(state0);
        DijkstraResult result = new DijkstraResult(path, 0, 0);
        
        assertThrows(IllegalArgumentException.class, () -> builder.buildTrip(result));
    }
    
    @Test
    void testSimpleWalk() {
        // Setup: Origin -> Walk -> Destination
        Place origin = new Place("Origin", new Coordinate(6.25, -75.57));
        Place destination = new Place("Destination", new Coordinate(6.251, -75.571));
        
        State state0 = new State(0, origin);
        State state1 = new State(1, destination);
        
        graph.addState(state0);
        graph.addState(state1);
        
        // Add walk edge (about 150m)
        Edge walk = new Edge(0, 1, EdgeKind.WALK, 120, 150);
        graph.addEdge(walk);
        
        List<State> path = Arrays.asList(state0, state1);
        DijkstraResult result = new DijkstraResult(path, 150, 120);
        
        Trip trip = builder.buildTrip(result);
        
        assertNotNull(trip);
        assertEquals(120, trip.getTotalDurationSec());
        assertEquals(1, trip.getSteps().size());
        
        Step step = trip.getSteps().get(0);
        assertEquals(StepType.WALK, step.getType());
        assertTrue(step.getDescription().contains("Caminar a Destination"));
        assertEquals(120, step.getDurationSec());
    }
    
    @Test
    void testShortWalkOmitted() {
        // Setup: Origin -> Short Walk (< 50m) -> Destination
        Place origin = new Place("Origin", new Coordinate(6.25, -75.57));
        Place destination = new Place("Destination", new Coordinate(6.2501, -75.5701));
        
        State state0 = new State(0, origin);
        State state1 = new State(1, destination);
        
        graph.addState(state0);
        graph.addState(state1);
        
        // Add short walk edge (about 15m)
        Edge walk = new Edge(0, 1, EdgeKind.WALK, 20, 30);
        graph.addEdge(walk);
        
        List<State> path = Arrays.asList(state0, state1);
        DijkstraResult result = new DijkstraResult(path, 30, 20);
        
        Trip trip = builder.buildTrip(result);
        
        assertNotNull(trip);
        assertEquals(20, trip.getTotalDurationSec());
        // Short walk should be omitted
        assertEquals(0, trip.getSteps().size());
        // Walking distance should not be accumulated for omitted walks
        assertEquals(0.0, trip.getTotalWalkMeters(), 0.1);
    }
    
    @Test
    void testSimpleBusTrip() {
        // Setup: Walk -> Board -> Ride -> Alight -> Walk
        Place origin = new Place("Calle 1", new Coordinate(6.25, -75.57));
        Place busStop1 = new Place("Parada A", new Coordinate(6.251, -75.571));
        Place busStop2 = new Place("Parada B", new Coordinate(6.26, -75.58));
        Place destination = new Place("Calle 2", new Coordinate(6.261, -75.581));
        
        State state0 = new State(0, origin);
        State state1 = new State(1, busStop1);
        State state2 = new State(2, busStop1, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state3 = new State(3, busStop2, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state4 = new State(4, busStop2);
        State state5 = new State(5, destination);
        
        graph.addState(state0);
        graph.addState(state1);
        graph.addState(state2);
        graph.addState(state3);
        graph.addState(state4);
        graph.addState(state5);
        
        // Add edges
        graph.addEdge(new Edge(0, 1, EdgeKind.WALK, 120, 150));       // Walk to stop
        graph.addEdge(new Edge(1, 2, EdgeKind.BOARD, 0, 120));        // Board bus
        graph.addEdge(new Edge(2, 3, EdgeKind.RIDE, 300, 300));       // Ride bus
        graph.addEdge(new Edge(3, 4, EdgeKind.ALIGHT, 10, 10));       // Alight
        graph.addEdge(new Edge(4, 5, EdgeKind.WALK, 90, 120));        // Walk to destination
        
        List<State> path = Arrays.asList(state0, state1, state2, state3, state4, state5);
        DijkstraResult result = new DijkstraResult(path, 700, 520);
        
        Trip trip = builder.buildTrip(result);
        
        assertNotNull(trip);
        assertEquals(520, trip.getTotalDurationSec());
        assertEquals(5, trip.getSteps().size());
        
        // Verify step sequence
        Step step0 = trip.getSteps().get(0);
        assertEquals(StepType.WALK, step0.getType());
        assertTrue(step0.getDescription().contains("Caminar a Parada A"));
        
        Step step1 = trip.getSteps().get(1);
        assertEquals(StepType.BOARD, step1.getType());
        assertTrue(step1.getDescription().contains("Abordar 301 en Parada A"));
        assertTrue(step1.getRouteInfo().isPresent());
        assertEquals("301", step1.getRouteInfo().get());
        
        Step step2 = trip.getSteps().get(2);
        assertEquals(StepType.RIDE, step2.getType());
        assertTrue(step2.getDescription().contains("Viajar en 301 por"));
        assertTrue(step2.getDescription().contains("minutos"));
        
        Step step3 = trip.getSteps().get(3);
        assertEquals(StepType.ALIGHT, step3.getType());
        assertTrue(step3.getDescription().contains("Bajar en Parada B"));
        
        Step step4 = trip.getSteps().get(4);
        assertEquals(StepType.WALK, step4.getType());
        assertTrue(step4.getDescription().contains("Caminar a Calle 2"));
    }
    
    @Test
    void testConsolidateConsecutiveRides() {
        // Setup: Board -> Ride -> Ride -> Ride -> Alight
        // Multiple consecutive rides on same route should be consolidated
        Place busStop1 = new Place("Parada A", new Coordinate(6.25, -75.57));
        Place busStop2 = new Place("Parada B", new Coordinate(6.26, -75.58));
        Place busStop3 = new Place("Parada C", new Coordinate(6.27, -75.59));
        Place busStop4 = new Place("Parada D", new Coordinate(6.28, -75.60));
        
        State state0 = new State(0, busStop1);
        State state1 = new State(1, busStop1, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state2 = new State(2, busStop2, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state3 = new State(3, busStop3, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state4 = new State(4, busStop4, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state5 = new State(5, busStop4);
        
        graph.addState(state0);
        graph.addState(state1);
        graph.addState(state2);
        graph.addState(state3);
        graph.addState(state4);
        graph.addState(state5);
        
        // Add edges
        graph.addEdge(new Edge(0, 1, EdgeKind.BOARD, 0, 120));
        graph.addEdge(new Edge(1, 2, EdgeKind.RIDE, 180, 180));  // 3 min
        graph.addEdge(new Edge(2, 3, EdgeKind.RIDE, 240, 240));  // 4 min
        graph.addEdge(new Edge(3, 4, EdgeKind.RIDE, 300, 300));  // 5 min
        graph.addEdge(new Edge(4, 5, EdgeKind.ALIGHT, 10, 10));
        
        List<State> path = Arrays.asList(state0, state1, state2, state3, state4, state5);
        DijkstraResult result = new DijkstraResult(path, 850, 730);
        
        Trip trip = builder.buildTrip(result);
        
        assertNotNull(trip);
        assertEquals(3, trip.getSteps().size());
        
        Step boardStep = trip.getSteps().get(0);
        assertEquals(StepType.BOARD, boardStep.getType());
        
        Step rideStep = trip.getSteps().get(1);
        assertEquals(StepType.RIDE, rideStep.getType());
        // Total ride time: 180 + 240 + 300 = 720 seconds = 12 minutes
        assertEquals(720, rideStep.getDurationSec());
        assertTrue(rideStep.getDescription().contains("Viajar en 301 por 12 minutos"));
        
        Step alightStep = trip.getSteps().get(2);
        assertEquals(StepType.ALIGHT, alightStep.getType());
    }
    
    @Test
    void testMultiTransferTrip() {
        // Setup: Walk -> Board Bus -> Ride -> Alight -> Walk -> Board Metro -> Ride -> Alight
        Place origin = new Place("Origen", new Coordinate(6.24, -75.56));
        Place busStop = new Place("Parada Bus", new Coordinate(6.25, -75.57));
        Place transfer = new Place("Transferencia", new Coordinate(6.251, -75.571));
        Place metroStation = new Place("Estación Metro", new Coordinate(6.252, -75.572));
        Place metroStop2 = new Place("Estación 2", new Coordinate(6.26, -75.58));
        
        State state0 = new State(0, origin);
        State state1 = new State(1, busStop);
        State state2 = new State(2, busStop, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state3 = new State(3, transfer, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state4 = new State(4, transfer);
        State state5 = new State(5, metroStation);
        State state6 = new State(6, metroStation, StateType.METRO_STATION, "Línea A", Direction.OUTBOUND);
        State state7 = new State(7, metroStop2, StateType.METRO_STATION, "Línea A", Direction.OUTBOUND);
        State state8 = new State(8, metroStop2);
        
        graph.addState(state0);
        graph.addState(state1);
        graph.addState(state2);
        graph.addState(state3);
        graph.addState(state4);
        graph.addState(state5);
        graph.addState(state6);
        graph.addState(state7);
        graph.addState(state8);
        
        // Add edges for complete trip
        graph.addEdge(new Edge(0, 1, EdgeKind.WALK, 120, 150));       // Walk to bus stop
        graph.addEdge(new Edge(1, 2, EdgeKind.BOARD, 0, 120));        // Board bus
        graph.addEdge(new Edge(2, 3, EdgeKind.RIDE, 300, 300));       // Ride bus
        graph.addEdge(new Edge(3, 4, EdgeKind.ALIGHT, 10, 10));       // Alight from bus
        graph.addEdge(new Edge(4, 5, EdgeKind.WALK, 120, 150));       // Walk to metro
        graph.addEdge(new Edge(5, 6, EdgeKind.BOARD, 0, 120));        // Board metro
        graph.addEdge(new Edge(6, 7, EdgeKind.RIDE, 240, 240));       // Ride metro
        graph.addEdge(new Edge(7, 8, EdgeKind.ALIGHT, 10, 10));       // Alight from metro
        
        List<State> path = Arrays.asList(state0, state1, state2, state3, state4, state5, state6, state7, state8);
        DijkstraResult result = new DijkstraResult(path, 1100, 920);
        
        Trip trip = builder.buildTrip(result);
        
        assertNotNull(trip);
        // Should have: WALK, BOARD (bus), RIDE (bus), ALIGHT, WALK, BOARD (metro), RIDE (metro), ALIGHT
        assertEquals(8, trip.getSteps().size());
        
        // Find the board metro step
        boolean foundMetroBoard = false;
        for (Step step : trip.getSteps()) {
            if (step.getType() == StepType.BOARD && 
                step.getDescription().contains("Línea A")) {
                foundMetroBoard = true;
                break;
            }
        }
        assertTrue(foundMetroBoard, "Should have boarding step for metro");
    }
    
    @Test
    void testRideDurationRoundedUpMinutes() {
        // Test that ride duration is properly rounded up to minutes
        Place stop1 = new Place("Stop 1", new Coordinate(6.25, -75.57));
        Place stop2 = new Place("Stop 2", new Coordinate(6.26, -75.58));
        
        State state0 = new State(0, stop1);
        State state1 = new State(1, stop1, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state2 = new State(2, stop2, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state3 = new State(3, stop2);
        
        graph.addState(state0);
        graph.addState(state1);
        graph.addState(state2);
        graph.addState(state3);
        
        // 90 seconds ride = 1.5 minutes, should round up to 2 minutes
        graph.addEdge(new Edge(0, 1, EdgeKind.BOARD, 0, 120));
        graph.addEdge(new Edge(1, 2, EdgeKind.RIDE, 90, 90));
        graph.addEdge(new Edge(2, 3, EdgeKind.ALIGHT, 10, 10));
        
        List<State> path = Arrays.asList(state0, state1, state2, state3);
        DijkstraResult result = new DijkstraResult(path, 220, 100);
        
        Trip trip = builder.buildTrip(result);
        
        Step rideStep = trip.getSteps().stream()
            .filter(s -> s.getType() == StepType.RIDE)
            .findFirst()
            .orElseThrow();
        
        assertTrue(rideStep.getDescription().contains("2 minutos"));
    }
    
    @Test
    void testSpanishDescriptions() {
        // Verify all descriptions are in Spanish
        Place origin = new Place("Origen", new Coordinate(6.25, -75.57));
        Place busStop = new Place("Parada", new Coordinate(6.251, -75.571));
        Place destination = new Place("Destino", new Coordinate(6.26, -75.58));
        
        State state0 = new State(0, origin);
        State state1 = new State(1, busStop);
        State state2 = new State(2, busStop, StateType.BUS_STOP, "Ruta 301", Direction.OUTBOUND);
        State state3 = new State(3, destination, StateType.BUS_STOP, "Ruta 301", Direction.OUTBOUND);
        State state4 = new State(4, destination);
        
        graph.addState(state0);
        graph.addState(state1);
        graph.addState(state2);
        graph.addState(state3);
        graph.addState(state4);
        
        graph.addEdge(new Edge(0, 1, EdgeKind.WALK, 120, 150));
        graph.addEdge(new Edge(1, 2, EdgeKind.BOARD, 0, 120));
        graph.addEdge(new Edge(2, 3, EdgeKind.RIDE, 300, 300));
        graph.addEdge(new Edge(3, 4, EdgeKind.ALIGHT, 10, 10));
        
        List<State> path = Arrays.asList(state0, state1, state2, state3, state4);
        DijkstraResult result = new DijkstraResult(path, 550, 430);
        
        Trip trip = builder.buildTrip(result);
        
        // Check for Spanish keywords in descriptions
        boolean hasCaminar = trip.getSteps().stream()
            .anyMatch(s -> s.getDescription().contains("Caminar"));
        boolean hasAbordar = trip.getSteps().stream()
            .anyMatch(s -> s.getDescription().contains("Abordar"));
        boolean hasViajar = trip.getSteps().stream()
            .anyMatch(s -> s.getDescription().contains("Viajar"));
        boolean hasBajar = trip.getSteps().stream()
            .anyMatch(s -> s.getDescription().contains("Bajar"));
        
        assertTrue(hasCaminar, "Should have 'Caminar' in walk step");
        assertTrue(hasAbordar, "Should have 'Abordar' in board step");
        assertTrue(hasViajar, "Should have 'Viajar' in ride step");
        assertTrue(hasBajar, "Should have 'Bajar' in alight step");
    }
}
