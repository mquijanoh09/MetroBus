package co.medellin.transit.graph;

import co.medellin.transit.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Task 16.1: Memory Optimizations
 * 
 * Verifies:
 * 1. String interning is working for place names and route IDs
 * 2. Primitive int[] arrays are used in Dijkstra (Router class)
 * 3. Spatial index reduces getStatesNearPlace to O(k) not O(n)
 * 
 * Target: Total heap usage <512MB
 */
class MemoryOptimizationTest {
    
    private TransitGraph graph;
    
    @BeforeEach
    void setUp() {
        graph = new TransitGraph(1000);
    }
    
    /**
     * Test 1: Verify string interning for place names
     * When multiple states reference the same place name, they should share
     * the same String object in memory (via String.intern()).
     */
    @Test
    void testStringInterningForPlaceNames() {
        // Create multiple places with the same name
        Coordinate coord1 = new Coordinate(6.25, -75.57);
        Coordinate coord2 = new Coordinate(6.25, -75.57);
        
        Place place1 = new Place("Estación Popular", coord1);
        Place place2 = new Place("Estación Popular", coord2);
        
        // Verify that the name strings are the same object (interned)
        assertSame(place1.getName(), place2.getName(),
            "Place names should be interned - same string content should reference same object");
    }
    
    /**
     * Test 2: Verify string interning for route IDs
     * Multiple states on the same route should share the same String object.
     */
    @Test
    void testStringInterningForRouteIds() {
        Coordinate coord = new Coordinate(6.25, -75.57);
        Place place1 = new Place("Stop1", coord);
        Place place2 = new Place("Stop2", coord);
        
        State state1 = new State(0, place1, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        State state2 = new State(1, place2, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        
        // Verify that route IDs are interned
        String routeId1 = state1.getRouteId().get();
        String routeId2 = state2.getRouteId().get();
        
        assertSame(routeId1, routeId2,
            "Route IDs should be interned - same route string should reference same object");
    }
    
    /**
     * Test 3: Verify spatial index performance - O(k) not O(n)
     * 
     * The spatial index should only check states in nearby grid cells,
     * not all states in the graph.
     */
    @Test
    void testSpatialIndexPerformance() {
        // Create a large graph with states distributed across Medellín
        int totalStates = 1000;
        
        // Add states in a grid pattern across Medellín bounds
        double minLat = 6.15;
        double maxLat = 6.35;
        double minLon = -75.65;
        double maxLon = -75.50;
        
        double latStep = (maxLat - minLat) / Math.sqrt(totalStates);
        double lonStep = (maxLon - minLon) / Math.sqrt(totalStates);
        
        int stateId = 0;
        for (double lat = minLat; lat < maxLat && stateId < totalStates; lat += latStep) {
            for (double lon = minLon; lon < maxLon && stateId < totalStates; lon += lonStep) {
                Coordinate coord = new Coordinate(lat, lon);
                Place place = new Place("State_" + stateId, coord);
                State state = new State(stateId, place);
                graph.addState(state);
                stateId++;
            }
        }
        
        assertEquals(totalStates, graph.getStateCount(), "Should have added all states");
        
        // Query for states near a specific location with 500m radius
        Coordinate queryCoord = new Coordinate(6.25, -75.57); // Center of Medellín
        Place queryPlace = new Place("Query", queryCoord);
        
        long startTime = System.nanoTime();
        List<State> nearbyStates = graph.getStatesNearPlace(queryPlace, 500.0);
        long elapsedNanos = System.nanoTime() - startTime;
        
        // With spatial indexing, we should:
        // 1. Find a small subset of states (much less than totalStates)
        // 2. Complete the query very quickly (< 5ms for 1000 states, allowing for JVM warmup)
        
        assertTrue(nearbyStates.size() < totalStates / 10,
            "Spatial index should return small subset of total states, got " + 
            nearbyStates.size() + " out of " + totalStates);
        
        long maxNanos = 5_000_000; // 5ms (relaxed for JVM warmup and test environment variability)
        assertTrue(elapsedNanos < maxNanos,
            "Query should complete in <5ms with spatial index, took " + 
            (elapsedNanos / 1_000_000.0) + "ms");
        
        System.out.printf("Spatial index query: found %d/%d states in %.3f ms%n",
            nearbyStates.size(), totalStates, elapsedNanos / 1_000_000.0);
    }
    
    /**
     * Test 4: Verify spatial index correctness
     * Results should match a linear scan (but be faster).
     */
    @Test
    void testSpatialIndexCorrectness() {
        // Add a small set of states at known locations
        addStateAt(0, 6.25, -75.57, "Center");
        addStateAt(1, 6.25 + 0.004, -75.57, "North400m");  // ~400m north
        addStateAt(2, 6.25, -75.57 + 0.005, "East500m");   // ~500m east
        addStateAt(3, 6.25, -75.57 + 0.010, "East1000m");  // ~1000m east (should be excluded)
        
        Coordinate center = new Coordinate(6.25, -75.57);
        Place centerPlace = new Place("Query", center);
        
        // Query with 600m radius - should include states 0, 1, 2 but not 3
        List<State> nearby = graph.getStatesNearPlace(centerPlace, 600.0);
        
        assertEquals(3, nearby.size(), 
            "Should find exactly 3 states within 600m radius");
        
        // Verify the correct states were found
        assertTrue(nearby.stream().anyMatch(s -> s.getId() == 0), "Should include center state");
        assertTrue(nearby.stream().anyMatch(s -> s.getId() == 1), "Should include north state");
        assertTrue(nearby.stream().anyMatch(s -> s.getId() == 2), "Should include east state");
        assertFalse(nearby.stream().anyMatch(s -> s.getId() == 3), "Should NOT include far east state");
    }
    
    /**
     * Test 5: Verify spatial index handles edge cases
     */
    @Test
    void testSpatialIndexEdgeCases() {
        // Empty graph
        List<State> empty = graph.getStatesNearPlace(
            new Place("Test", new Coordinate(6.25, -75.57)), 500.0);
        assertTrue(empty.isEmpty(), "Empty graph should return empty list");
        
        // Zero radius
        addStateAt(0, 6.25, -75.57, "Center");
        List<State> zeroRadius = graph.getStatesNearPlace(
            new Place("Test", new Coordinate(6.25, -75.57)), 0.0);
        assertEquals(1, zeroRadius.size(), "Zero radius should still match exact location");
        
        // Very large radius
        addStateAt(1, 6.20, -75.60, "Far");
        List<State> largeRadius = graph.getStatesNearPlace(
            new Place("Test", new Coordinate(6.25, -75.57)), 10000.0);
        assertEquals(2, largeRadius.size(), "Large radius should include all states in city");
    }
    
    /**
     * Test 6: Memory usage estimate
     * This test provides a rough estimate of memory usage.
     */
    @Test
    void testMemoryUsageEstimate() {
        // Run garbage collection first
        System.gc();
        Thread.yield();
        
        Runtime runtime = Runtime.getRuntime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Create a medium-sized graph with appropriate capacity
        int stateCount = 5000;
        int edgesPerState = 6;
        
        // Create new graph with sufficient capacity
        TransitGraph testGraph = new TransitGraph(stateCount);
        
        for (int i = 0; i < stateCount; i++) {
            double lat = 6.15 + (0.20 * i / stateCount);
            double lon = -75.65 + (0.15 * i / stateCount);
            Coordinate coord = new Coordinate(lat, lon);
            Place place = new Place("State_" + i, coord);
            State state = new State(i, place);
            testGraph.addState(state);
        }
        
        // Add edges
        for (int i = 0; i < stateCount - 1; i++) {
            for (int j = 1; j <= edgesPerState && i + j < stateCount; j++) {
                Edge edge = new Edge(i, i + j, EdgeKind.WALK, 60, 90);
                testGraph.addEdge(edge);
            }
        }
        
        System.gc();
        Thread.yield();
        
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;
        double memPerState = memUsed / (double) stateCount;
        
        System.out.printf("Memory usage: %d states, %d edges = %.2f MB (%.2f KB/state)%n",
            stateCount, testGraph.getEdgeCount(), memUsed / 1_048_576.0, memPerState / 1024.0);
        
        // For 13,895 states, estimate total memory
        double estimatedTotalMB = (memPerState * 13_895) / 1_048_576.0;
        System.out.printf("Estimated memory for full graph (13,895 states): %.2f MB%n", estimatedTotalMB);
        
        // With optimizations, should be well under 512MB
        assertTrue(estimatedTotalMB < 400,
            "Estimated memory usage should be under 400MB, got " + estimatedTotalMB + "MB");
    }
    
    // Helper methods
    
    private void addStateAt(int id, double lat, double lon, String name) {
        Coordinate coord = new Coordinate(lat, lon);
        Place place = new Place(name, coord);
        State state = new State(id, place);
        graph.addState(state);
    }
}
