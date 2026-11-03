package co.medellin.transit.graph;

import co.medellin.transit.data.*;
import co.medellin.transit.model.Edge;
import co.medellin.transit.model.EdgeKind;
import co.medellin.transit.model.State;
import co.medellin.transit.model.StateType;
import co.medellin.transit.model.Stop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test that validates the exact data counts from the Medellin transit dataset.
 * 
 * Reference counts (from working Python prototype):
 * 
 * States: 13,895 total
 * - WALK: 3,665 (3,590 stops + 75 stations)
 * - BUS_STOP: 10,034
 * - METRO_STATION: 196
 * 
 * Edges: 82,160 total
 * - Bus RIDE: 9,796 (9,550 CSV rows + 246 turnaround links)
 * - Transit RIDE: 174
 * - BOARD: 10,230 (10,034 bus + 196 metro)
 * - ALIGHT: 10,230 (10,034 bus + 196 metro)
 * - WALK: 51,730 (25,865 links × 2 directions)
 * 
 * This test ensures GraphBuilder constructs the graph correctly with all required components.
 */
public class DataCountTest {

    private static final Path DATA_DIR = Paths.get("data");

    @Test
    public void testStopCount() throws Exception {
        // Load stops using StopArcGisReader with deduplication
        // The reader will automatically search in the raw/ subdirectory
        StopArcGisReader stopReader = new StopArcGisReader();
        List<Stop> stops = stopReader.readPaginated(DATA_DIR, "stops", ".json");
        
        // Should load exactly 3,590 unique physical stops, not 10,043
        assertEquals(3590, stops.size(), 
            "Expected 3,590 unique physical stops after deduplication");
    }

    @Test
    public void testBusSegmentCount() throws Exception {
        // Load bus segments
        BusSegmentReader segmentReader = new BusSegmentReader();
        List<BusSegment> segments = segmentReader.read(DATA_DIR.resolve("bus_segments.csv"));
        
        assertEquals(9550, segments.size(), 
            "Expected 9,550 bus segments");
    }

    @Test
    public void testTransitLineCount() throws Exception {
        // Load transit line segments
        TransitLineReader lineReader = new TransitLineReader();
        List<TransitSegment> segments = lineReader.read(DATA_DIR.resolve("transit_lines.csv"));
        
        // Should load exactly 98 transit line segments (stations)
        assertEquals(98, segments.size(), 
            "Expected 98 transit line segments");
    }

    @Test
    public void testWalkLinkCount() throws Exception {
        // Load walk links
        WalkLinkReader walkLinkReader = new WalkLinkReader();
        List<WalkLink> walkLinks = walkLinkReader.read(DATA_DIR.resolve("walk_links.csv"));
        
        assertEquals(25865, walkLinks.size(), 
            "Expected 25,865 walk links (including those with empty names)");
    }

    @Test
    public void testGraphStateCount() throws Exception {
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(DATA_DIR);
        
        assertEquals(13895, graph.getStateCount(), 
            "Expected 13,895 states (nodes) in the graph");
    }

    @Test
    public void testGraphEdgeCount() throws Exception {
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(DATA_DIR);
        
        assertEquals(82160, graph.getEdgeCount(), 
            "Expected 82,160 edges in the graph");
    }
    
    @Test
    public void testGraphStateBreakdown() throws Exception {
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(DATA_DIR);
        
        // Count states by type
        int walkStates = 0;
        int busStopStates = 0;
        int metroStationStates = 0;
        
        for (State state : graph.getStates()) {
            switch (state.getType()) {
                case WALK:
                    walkStates++;
                    break;
                case BUS_STOP:
                    busStopStates++;
                    break;
                case METRO_STATION:
                    metroStationStates++;
                    break;
            }
        }
        
        assertEquals(3665, walkStates, 
            "Expected 3,665 WALK states (3,590 stops + 75 stations)");
        assertEquals(10034, busStopStates, 
            "Expected 10,034 BUS_STOP states");
        assertEquals(196, metroStationStates, 
            "Expected 196 METRO_STATION states");
    }
    
    @Test
    public void testGraphEdgeBreakdown() throws Exception {
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(DATA_DIR);
        
        // Count edges by kind
        int rideEdges = 0;
        int boardEdges = 0;
        int alightEdges = 0;
        int walkEdges = 0;
        
        for (State state : graph.getStates()) {
            for (Edge edge : graph.getOutgoingEdges(state.getId())) {
                switch (edge.getKind()) {
                    case RIDE:
                        rideEdges++;
                        break;
                    case BOARD:
                        boardEdges++;
                        break;
                    case ALIGHT:
                        alightEdges++;
                        break;
                    case WALK:
                        walkEdges++;
                        break;
                }
            }
        }
        
        // Total RIDE edges (bus + transit): 9,796 + 174 = 9,970
        assertEquals(9970, rideEdges, 
            "Expected 9,970 RIDE edges (9,796 bus + 174 transit)");
        assertEquals(10230, boardEdges, 
            "Expected 10,230 BOARD edges (10,034 bus + 196 metro)");
        assertEquals(10230, alightEdges, 
            "Expected 10,230 ALIGHT edges (10,034 bus + 196 metro)");
        assertEquals(51730, walkEdges, 
            "Expected 51,730 WALK edges (25,865 links × 2 directions)");
    }
    
    @Test
    public void testParqueBerrioConnectivity() throws Exception {
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(DATA_DIR);
        
        // Find Parque Berrío WALK state
        State parqueBerrioWalk = null;
        for (State state : graph.getStates()) {
            if (state.getType() == StateType.WALK && 
                state.getPlace().getName().contains("Parque Berrío")) {
                parqueBerrioWalk = state;
                break;
            }
        }
        
        assertNotNull(parqueBerrioWalk, "Parque Berrío WALK state should exist");
        
        // Collect all WALK states
        List<State> allWalkStates = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.WALK)
            .toList();
        
        assertEquals(3665, allWalkStates.size(), 
            "Should have 3,665 WALK states to test connectivity");
        
        // Test forward reachability: Parque Berrío → all WALK states
        Set<Integer> reachableFromParque = bfsForward(graph, parqueBerrioWalk.getId());
        
        // Filter to only WALK states
        Set<Integer> reachableWalkStatesFromParque = reachableFromParque.stream()
            .filter(id -> {
                State state = graph.getState(id);
                return state != null && state.getType() == StateType.WALK;
            })
            .collect(Collectors.toSet());
        
        List<State> unreachableFromParque = allWalkStates.stream()
            .filter(s -> !reachableWalkStatesFromParque.contains(s.getId()))
            .toList();
        
        if (!unreachableFromParque.isEmpty()) {
            String unreachableNames = unreachableFromParque.stream()
                .limit(10)
                .map(s -> s.getPlace().getName())
                .collect(Collectors.joining(", "));
            fail(String.format("Found %d WALK states unreachable FROM Parque Berrío. First 10: %s",
                unreachableFromParque.size(), unreachableNames));
        }
        
        // Test backward reachability: all WALK states → Parque Berrío
        Set<Integer> canReachParque = bfsBackward(graph, parqueBerrioWalk.getId());
        
        // Filter to only WALK states
        Set<Integer> walkStatesThatCanReachParque = canReachParque.stream()
            .filter(id -> {
                State state = graph.getState(id);
                return state != null && state.getType() == StateType.WALK;
            })
            .collect(Collectors.toSet());
        
        List<State> cannotReachParque = allWalkStates.stream()
            .filter(s -> !walkStatesThatCanReachParque.contains(s.getId()))
            .toList();
        
        if (!cannotReachParque.isEmpty()) {
            String unreachableNames = cannotReachParque.stream()
                .limit(10)
                .map(s -> s.getPlace().getName())
                .collect(Collectors.joining(", "));
            fail(String.format("Found %d WALK states that cannot reach Parque Berrío. First 10: %s",
                cannotReachParque.size(), unreachableNames));
        }
        
        // If we got here, full bidirectional connectivity is confirmed
        assertEquals(3665, reachableWalkStatesFromParque.size(), 
            "All 3,665 WALK states should be reachable from Parque Berrío");
        assertEquals(3665, walkStatesThatCanReachParque.size(), 
            "All 3,665 WALK states should be able to reach Parque Berrío");
    }
    
    /**
     * BFS forward: find all states reachable from startId by following outgoing edges.
     */
    private Set<Integer> bfsForward(TransitGraph graph, int startId) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        
        queue.add(startId);
        visited.add(startId);
        
        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            
            for (Edge edge : graph.getOutgoingEdges(currentId)) {
                int targetId = edge.getTargetId();
                if (!visited.contains(targetId)) {
                    visited.add(targetId);
                    queue.add(targetId);
                }
            }
        }
        
        return visited;
    }
    
    /**
     * BFS backward: find all states that can reach targetId by reversing edge direction.
     */
    private Set<Integer> bfsBackward(TransitGraph graph, int targetId) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        
        // Build reverse edge map
        Map<Integer, List<Integer>> reverseEdges = new HashMap<>();
        for (State state : graph.getStates()) {
            for (Edge edge : graph.getOutgoingEdges(state.getId())) {
                reverseEdges.computeIfAbsent(edge.getTargetId(), k -> new ArrayList<>())
                    .add(state.getId());
            }
        }
        
        queue.add(targetId);
        visited.add(targetId);
        
        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            
            List<Integer> predecessors = reverseEdges.getOrDefault(currentId, List.of());
            for (int predId : predecessors) {
                if (!visited.contains(predId)) {
                    visited.add(predId);
                    queue.add(predId);
                }
            }
        }
        
        return visited;
    }
}
