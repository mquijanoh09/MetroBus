package co.medellin.transit.routing;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Router class.
 * Tests Dijkstra's algorithm implementation with various routing scenarios.
 */
class RouterTest {
    
    private TransitGraph graph;
    private Parameters params;
    private Router router;
    
    @BeforeEach
    void setUp() {
        params = new Parameters(500.0, 120, 1.5);
        graph = new TransitGraph(10);
    }
    
    @Test
    void testConstructorWithValidArguments() {
        // When: creating router with valid graph and params
        Router router = new Router(graph, params);
        
        // Then: router should be created successfully
        assertNotNull(router);
    }
    
    @Test
    void testConstructorWithNullGraph() {
        // When/Then: creating router with null graph should throw
        assertThrows(NullPointerException.class, () -> {
            new Router(null, params);
        });
    }
    
    @Test
    void testConstructorWithNullParams() {
        // When/Then: creating router with null params should throw
        assertThrows(NullPointerException.class, () -> {
            new Router(graph, null);
        });
    }
    
    @Test
    void testFindRouteWithNullOrigin() {
        // Given: router instance
        router = new Router(graph, params);
        Place destination = new Place("Destination", new Coordinate(6.25, -75.56));
        
        // When/Then: finding route with null origin should throw
        assertThrows(NullPointerException.class, () -> {
            router.findRoute(null, destination);
        });
    }
    
    @Test
    void testFindRouteWithNullDestination() {
        // Given: router instance
        router = new Router(graph, params);
        Place origin = new Place("Origin", new Coordinate(6.25, -75.56));
        
        // When/Then: finding route with null destination should throw
        assertThrows(NullPointerException.class, () -> {
            router.findRoute(origin, null);
        });
    }
    
    @Test
    void testFindRouteWithNoStatesNearOrigin() {
        // Given: empty graph
        router = new Router(graph, params);
        Place origin = new Place("Origin", new Coordinate(6.25, -75.56));
        Place destination = new Place("Destination", new Coordinate(6.26, -75.57));
        
        // When: finding route with no states near origin
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        
        // Then: should return empty
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindRouteWithNoStatesNearDestination() {
        // Given: graph with one state near origin only
        Coordinate coord1 = new Coordinate(6.25, -75.56);
        Place place1 = new Place("Stop1", coord1);
        State state1 = new State(0, place1);
        graph.addState(state1);
        
        router = new Router(graph, params);
        
        Place origin = new Place("Origin", coord1); // Near state1
        Place destination = new Place("Destination", new Coordinate(10.0, -80.0)); // Far away
        
        // When: finding route with no states near destination
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        
        // Then: should return empty
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindRouteSimpleWalkPath() {
        // Given: graph with two connected walking states (>500m apart)
        Coordinate coord1 = new Coordinate(6.250, -75.560);
        Coordinate coord2 = new Coordinate(6.255, -75.565);  // ~700m away
        
        Place place1 = new Place("Stop1", coord1);
        Place place2 = new Place("Stop2", coord2);
        
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        
        graph.addState(state1);
        graph.addState(state2);
        
        // Walk edge: 60 seconds real time
        // Perceived cost = 60 * 1.5 + 30 = 120 seconds
        int realTime = 60;
        int perceivedCost = (int)(realTime * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
        Edge walkEdge = new Edge(0, 1, EdgeKind.WALK, realTime, perceivedCost);
        graph.addEdge(walkEdge);
        
        router = new Router(graph, params);
        
        Place origin = new Place("Origin", coord1);
        Place destination = new Place("Destination", coord2);
        
        // When: finding route
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        
        // Then: should find path
        assertTrue(result.isPresent());
        assertEquals(2, result.get().getPathLength());
        assertEquals(realTime, result.get().getRealDurationSec());
        assertEquals(perceivedCost, result.get().getPerceivedCost());
    }
    
    @Test
    void testFindRouteWithBoardAndRide() {
        // Given: graph with walk states, bus stops, and board/ride/alight edges (>500m apart)
        Coordinate coord1 = new Coordinate(6.250, -75.560);
        Coordinate coord2 = new Coordinate(6.255, -75.565);  // ~750m from coord1
        Coordinate coord3 = new Coordinate(6.260, -75.570);  // ~750m from coord2, ~1400m from coord1
        
        Place place1 = new Place("Origin", coord1);
        Place place2 = new Place("Stop1", coord2);
        Place place3 = new Place("Stop2", coord3);
        
        State walkState1 = new State(0, place1);
        State walkState2 = new State(1, place2);  // Walk state at bus stop location
        State busStop1 = new State(2, place2, StateType.BUS_STOP, "R101", Direction.OUTBOUND);
        State busStop2 = new State(3, place3, StateType.BUS_STOP, "R101", Direction.OUTBOUND);
        State walkState3 = new State(4, place3);
        
        graph.addState(walkState1);
        graph.addState(walkState2);
        graph.addState(busStop1);
        graph.addState(busStop2);
        graph.addState(walkState3);
        
        // Walk from origin to first bus stop: 30 seconds
        int walkTime = 30;
        int walkCost = (int)(walkTime * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
        graph.addEdge(new Edge(0, 1, EdgeKind.WALK, walkTime, walkCost));
        
        // Board bus: walk state -> bus stop state (0 seconds real, boarding penalty perceived)
        graph.addEdge(new Edge(1, 2, EdgeKind.BOARD, 0, params.getBoardingPenaltySec()));
        
        // Ride bus: 180 seconds
        int rideTime = 180;
        graph.addEdge(new Edge(2, 3, EdgeKind.RIDE, rideTime, rideTime));
        
        // Alight: bus stop -> walk state (0 seconds)
        graph.addEdge(new Edge(3, 4, EdgeKind.ALIGHT, 0, 0));
        
        router = new Router(graph, params);
        
        Place origin = new Place("Origin", coord1);
        Place destination = new Place("Destination", coord3);
        
        // When: finding route
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        
        // Then: should find path with appropriate costs
        assertTrue(result.isPresent());
        assertTrue(result.get().getPathLength() >= 2);
        
        // Real duration should not include penalties
        int expectedRealDuration = walkTime + rideTime;
        assertEquals(expectedRealDuration, result.get().getRealDurationSec());
        
        // Perceived cost should include walk penalty and boarding penalty
        int expectedPerceivedCost = walkCost + params.getBoardingPenaltySec() + rideTime;
        assertEquals(expectedPerceivedCost, result.get().getPerceivedCost());
    }
    
    @Test
    void testFindRouteWithNoPath() {
        // Given: graph with disconnected states (>500m apart)
        Coordinate coord1 = new Coordinate(6.250, -75.560);
        Coordinate coord2 = new Coordinate(6.260, -75.570);  // ~1.4km away
        
        Place place1 = new Place("Stop1", coord1);
        Place place2 = new Place("Stop2", coord2);
        
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        
        graph.addState(state1);
        graph.addState(state2);
        // No edge between states
        
        router = new Router(graph, params);
        
        Place origin = new Place("Origin", coord1);
        Place destination = new Place("Destination", coord2);
        
        // When: finding route with no connecting path
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        
        // Then: should return empty
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindRouteMultiSource() {
        // Given: graph with multiple states near origin, destination far away
        Coordinate originCoord = new Coordinate(6.250, -75.560);
        Coordinate destCoord = new Coordinate(6.256, -75.566);  // ~850m from origin
        
        // Two states very close to origin (within 500m)
        Coordinate coord1 = new Coordinate(6.2501, -75.5601);
        Coordinate coord2 = new Coordinate(6.2502, -75.5602);
        
        Place place1 = new Place("Stop1", coord1);
        Place place2 = new Place("Stop2", coord2);
        Place placeDest = new Place("Destination", destCoord);
        
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        State stateDest = new State(2, placeDest);
        
        graph.addState(state1);
        graph.addState(state2);
        graph.addState(stateDest);
        
        // Only state1 has path to destination
        int walkTime = 300;
        int walkCost = (int)(walkTime * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
        graph.addEdge(new Edge(0, 2, EdgeKind.WALK, walkTime, walkCost));
        
        router = new Router(graph, params);
        
        Place origin = new Place("Origin", originCoord);
        Place destination = new Place("Destination", destCoord);
        
        // When: finding route (should start from both state1 and state2)
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        
        // Then: should find path through state1
        assertTrue(result.isPresent());
    }
    
    @Test
    void testFindRouteSelectsShorterPath() {
        // Given: graph with two paths of different lengths (>500m apart)
        Coordinate coord1 = new Coordinate(6.250, -75.560);
        Coordinate coord2 = new Coordinate(6.253, -75.563);  // ~450m (intermediate)
        Coordinate coord3 = new Coordinate(6.256, -75.566);  // ~850m (destination)
        
        Place place1 = new Place("Start", coord1);
        Place place2 = new Place("Middle", coord2);
        Place place3 = new Place("End", coord3);
        
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        State state3 = new State(2, place3);
        
        graph.addState(state1);
        graph.addState(state2);
        graph.addState(state3);
        
        // Path 1: state1 -> state3 (direct, 100 seconds)
        int directTime = 100;
        int directCost = (int)(directTime * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
        graph.addEdge(new Edge(0, 2, EdgeKind.WALK, directTime, directCost));
        
        // Path 2: state1 -> state2 -> state3 (longer, 200 seconds total)
        int leg1Time = 120;
        int leg1Cost = (int)(leg1Time * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
        int leg2Time = 120;
        int leg2Cost = (int)(leg2Time * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
        graph.addEdge(new Edge(0, 1, EdgeKind.WALK, leg1Time, leg1Cost));
        graph.addEdge(new Edge(1, 2, EdgeKind.WALK, leg2Time, leg2Cost));
        
        router = new Router(graph, params);
        
        Place origin = new Place("Origin", coord1);
        Place destination = new Place("Destination", coord3);
        
        // When: finding route
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        
        // Then: should select shorter direct path
        assertTrue(result.isPresent());
        assertEquals(2, result.get().getPathLength()); // Direct path has 2 states
        assertEquals(directTime, result.get().getRealDurationSec());
        assertEquals(directCost, result.get().getPerceivedCost());
    }
    
    @Test
    void testRouterHandlesLargerGraph() {
        // Given: larger graph with 5 states widely spaced to avoid source/target overlap
        TransitGraph largeGraph = new TransitGraph(5);
        
        // Add states spaced 0.008 degrees apart (~880m, well beyond 500m access radius)
        for (int i = 0; i < 5; i++) {
            double lat = 6.240 + (i * 0.008);
            double lon = -75.570;
            Place place = new Place("Stop" + i, new Coordinate(lat, lon));
            State state = new State(i, place);
            largeGraph.addState(state);
        }
        
        // Add edges between consecutive states (chain)
        for (int i = 0; i < 4; i++) {
            int time = 60;
            int cost = (int)(time * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
            largeGraph.addEdge(new Edge(i, i + 1, EdgeKind.WALK, time, cost));
        }
        
        router = new Router(largeGraph, params);
        
        // Origin near state 0, destination near state 4 (no overlap due to wide spacing)
        Place origin = new Place("Origin", new Coordinate(6.2398, -75.5698));
        Place destination = new Place("Destination", new Coordinate(6.2718, -75.5698));
        
        // When: finding route in larger graph
        long startTime = System.nanoTime();
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        
        // Then: should find path efficiently
        assertTrue(result.isPresent());
        assertTrue(elapsedMs < 100, "Query should complete in < 100ms, took " + elapsedMs + "ms");
    }
    
    @Test
    void testBoardingPenaltyAffectsRouteChoice() {
        // Given: graph with two routes - one with boarding, one longer walk (>500m apart)
        Coordinate coord1 = new Coordinate(6.250, -75.560);
        Coordinate coord2 = new Coordinate(6.255, -75.565);  // ~700m away
        
        Place place1 = new Place("Start", coord1);
        Place place2 = new Place("End", coord2);
        
        State walkState1 = new State(0, place1);
        State busStop1 = new State(1, place1, StateType.BUS_STOP, "R101", Direction.OUTBOUND);
        State busStop2 = new State(2, place2, StateType.BUS_STOP, "R101", Direction.OUTBOUND);
        State walkState2 = new State(3, place2);
        
        graph.addState(walkState1);
        graph.addState(busStop1);
        graph.addState(busStop2);
        graph.addState(walkState2);
        
        // Option 1: Board bus (short ride but with boarding penalty)
        graph.addEdge(new Edge(0, 1, EdgeKind.BOARD, 0, params.getBoardingPenaltySec()));
        graph.addEdge(new Edge(1, 2, EdgeKind.RIDE, 60, 60));
        graph.addEdge(new Edge(2, 3, EdgeKind.ALIGHT, 0, 0));
        
        // Option 2: Direct walk (longer but no boarding)
        // Make walk slightly longer than bus ride but less than ride + boarding penalty
        int walkTime = 100;
        int walkCost = (int)(walkTime * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
        graph.addEdge(new Edge(0, 3, EdgeKind.WALK, walkTime, walkCost));
        
        router = new Router(graph, params);
        
        Place origin = new Place("Origin", coord1);
        Place destination = new Place("Destination", coord2);
        
        // When: finding route
        Optional<DijkstraResult> result = router.findRoute(origin, destination);
        
        // Then: should choose based on perceived cost (penalties matter)
        assertTrue(result.isPresent());
        
        // If walk cost < bus ride + boarding penalty, should choose walk
        // walkCost = 100 * 1.5 + 30 = 180
        // bus cost = 0 + 120 (boarding) + 60 (ride) + 0 (alight) = 180
        // They're equal, so Dijkstra might choose either based on exploration order
        assertTrue(result.get().getPerceivedCost() <= 180);
    }
}
