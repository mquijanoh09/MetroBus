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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
@EnabledIfSystemProperty(named = "test.data.directory", matches = ".*", disabledReason = "Real data directory not specified")
public class DataCountTest {

    private static final String DATA_DIR = System.getProperty("test.data.directory", 
        "C:\\Users\\marti\\Downloads\\transporte-medellin\\kiro\\data");

    @Test
    public void testStopCount() throws Exception {
        Path dataPath = Paths.get(DATA_DIR);
        
        // Load stops using StopArcGisReader with deduplication
        // The reader will automatically search in the raw/ subdirectory
        StopArcGisReader stopReader = new StopArcGisReader();
        List<Stop> stops = stopReader.readPaginated(dataPath, "stops", ".json");
        
        // Should load exactly 3,590 unique physical stops, not 10,043
        assertEquals(3590, stops.size(), 
            "Expected 3,590 unique physical stops after deduplication");
    }

    @Test
    public void testBusSegmentCount() throws Exception {
        Path dataPath = Paths.get(DATA_DIR);
        
        // Load bus segments
        BusSegmentReader segmentReader = new BusSegmentReader();
        List<BusSegment> segments = segmentReader.read(dataPath.resolve("bus_segments.csv"));
        
        assertEquals(9550, segments.size(), 
            "Expected 9,550 bus segments");
    }

    @Test
    public void testTransitLineCount() throws Exception {
        Path dataPath = Paths.get(DATA_DIR);
        
        // Load transit line segments
        TransitLineReader lineReader = new TransitLineReader();
        List<TransitSegment> segments = lineReader.read(dataPath.resolve("transit_lines.csv"));
        
        // Should load exactly 98 transit line segments (stations)
        assertEquals(98, segments.size(), 
            "Expected 98 transit line segments");
    }

    @Test
    public void testWalkLinkCount() throws Exception {
        Path dataPath = Paths.get(DATA_DIR);
        
        // Load walk links
        WalkLinkReader walkLinkReader = new WalkLinkReader();
        List<WalkLink> walkLinks = walkLinkReader.read(dataPath.resolve("walk_links.csv"));
        
        assertEquals(25865, walkLinks.size(), 
            "Expected 25,865 walk links (including those with empty names)");
    }

    @Test
    public void testGraphStateCount() throws Exception {
        Path dataPath = Paths.get(DATA_DIR);
        
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(dataPath);
        
        assertEquals(13895, graph.getStateCount(), 
            "Expected 13,895 states (nodes) in the graph");
    }

    @Test
    public void testGraphEdgeCount() throws Exception {
        Path dataPath = Paths.get(DATA_DIR);
        
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(dataPath);
        
        assertEquals(82160, graph.getEdgeCount(), 
            "Expected 82,160 edges in the graph");
    }
    
    @Test
    public void testGraphStateBreakdown() throws Exception {
        Path dataPath = Paths.get(DATA_DIR);
        
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(dataPath);
        
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
        Path dataPath = Paths.get(DATA_DIR);
        
        // Build complete graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(dataPath);
        
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
}
