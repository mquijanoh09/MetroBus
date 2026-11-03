package co.medellin.transit.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GraphBuilder.
 * Tests graph construction from CSV data files.
 */
class GraphBuilderTest {
    
    /**
     * Test that GraphBuilder throws IOException when data directory doesn't exist.
     */
    @Test
    void testBuildThrowsExceptionWhenDirectoryDoesNotExist() {
        GraphBuilder builder = new GraphBuilder();
        Path nonExistentDir = Path.of("nonexistent_directory_12345");
        
        IOException exception = assertThrows(IOException.class, () -> {
            builder.build(nonExistentDir);
        });
        
        assertTrue(exception.getMessage().contains("no encontrado"),
            "Error message should indicate directory not found");
    }
    
    /**
     * Test that GraphBuilder throws IOException when path is not a directory.
     */
    @Test
    void testBuildThrowsExceptionWhenPathIsNotDirectory(@TempDir Path tempDir) throws IOException {
        GraphBuilder builder = new GraphBuilder();
        
        // Create a file instead of a directory
        Path file = tempDir.resolve("not_a_directory.txt");
        Files.writeString(file, "test content");
        
        IOException exception = assertThrows(IOException.class, () -> {
            builder.build(file);
        });
        
        assertTrue(exception.getMessage().contains("no es un directorio"),
            "Error message should indicate path is not a directory");
    }
    
    /**
     * Test that GraphBuilder throws IOException when data directory is empty.
     */
    @Test
    void testBuildThrowsExceptionWhenDataDirectoryIsEmpty(@TempDir Path tempDir) {
        GraphBuilder builder = new GraphBuilder();
        
        IOException exception = assertThrows(IOException.class, () -> {
            builder.build(tempDir);
        });
        
        assertTrue(exception.getMessage().contains("vacío") || 
                   exception.getMessage().contains("empty"),
            "Error message should indicate graph is empty");
    }
    
    /**
     * Test that GraphBuilder can construct a minimal bus graph from real data format.
     * Uses the documented format from graph-structure.md sections 2.1, 2.3.
     */
    @Test
    void testBuildMinimalBusGraph(@TempDir Path tempDir) throws IOException {
        GraphBuilder builder = new GraphBuilder();
        
        // Create data directory structure
        Path rawDir = tempDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        // Create stops_0.json with 3 stops in ArcGIS format
        String stopsJson = """
{
  "features": [
    {"attributes":{"OBJECTID":1,"id_paradero":100,"direccion":"Test Stop 1"},"geometry":{"x":-75.57,"y":6.25}},
    {"attributes":{"OBJECTID":2,"id_paradero":101,"direccion":"Test Stop 2"},"geometry":{"x":-75.56,"y":6.26}},
    {"attributes":{"OBJECTID":3,"id_paradero":102,"direccion":"Test Stop 3"},"geometry":{"x":-75.55,"y":6.27}}
  ]
}
""";
        Files.writeString(rawDir.resolve("stops_0.json"), stopsJson);
        
        // Create routes.json with 1 route in ArcGIS format
        String routesJson = """
{
  "features": [
    {"attributes":{"id_ruta":"101","codigo":"T1","nombre":"Test Route 1","tipo":"Directa Centro"}}
  ]
}
""";
        Files.writeString(rawDir.resolve("routes.json"), routesJson);
        
        // Create bus_segments.csv with 2 segments
        String busSegmentsCsv = """
route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
101,outbound,100,101,1,2,1000,1200,1200,road
101,outbound,101,102,2,3,1100,1300,1300,road
""";
        Files.writeString(tempDir.resolve("bus_segments.csv"), busSegmentsCsv);
        
        // Create empty transit_lines.csv with header
        String transitLinesCsv = "line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next\n";
        Files.writeString(tempDir.resolve("transit_lines.csv"), transitLinesCsv);
        
        // Create empty walk_links.csv with header
        String walkLinksCsv = "place_a,name_a,place_b,name_b,kind,straight_m,walk_sec\n";
        Files.writeString(tempDir.resolve("walk_links.csv"), walkLinksCsv);
        
        // Build the graph
        TransitGraph graph = builder.build(tempDir);
        
        // Verify graph was constructed
        assertNotNull(graph, "Graph should not be null");
        
        // Expected states:
        // - 3 WALK states (one per stop: 100, 101, 102)
        // - 3 BUS_STOP states (one per stop on route 101, outbound direction)
        // Total: 6 states
        assertEquals(6, graph.getStateCount(), "Graph should have 6 states (3 WALK + 3 BUS_STOP)");
        
        // Expected edges:
        // - 2 RIDE edges (from bus segments: 100→101, 101→102)
        // - 3 BOARD edges (WALK → BUS_STOP for each stop)
        // - 3 ALIGHT edges (BUS_STOP → WALK for each stop)
        // Total: 8 edges (no walk links, so no WALK edges)
        assertEquals(8, graph.getEdgeCount(), "Graph should have 8 edges (2 RIDE + 3 BOARD + 3 ALIGHT)");
    }
    
    /**
     * Test that GraphBuilder handles multiple data sources (bus, transit, walk).
     * Uses real data format from graph-structure.md to verify all three sources combine correctly.
     */
    @Test
    void testBuildHandlesMultipleCsvFiles(@TempDir Path tempDir) throws IOException {
        GraphBuilder builder = new GraphBuilder();
        
        // Create data directory structure
        Path rawDir = tempDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        // Create stops_0.json with 4 stops
        String stopsJson = """
{
  "features": [
    {"attributes":{"OBJECTID":1,"id_paradero":100,"direccion":"Bus Stop 1"},"geometry":{"x":-75.57,"y":6.25}},
    {"attributes":{"OBJECTID":2,"id_paradero":101,"direccion":"Bus Stop 2"},"geometry":{"x":-75.56,"y":6.26}},
    {"attributes":{"OBJECTID":3,"id_paradero":200,"direccion":"Isolated Stop 1"},"geometry":{"x":-75.55,"y":6.27}},
    {"attributes":{"OBJECTID":4,"id_paradero":300,"direccion":"Isolated Stop 2"},"geometry":{"x":-75.54,"y":6.28}}
  ]
}
""";
        Files.writeString(rawDir.resolve("stops_0.json"), stopsJson);
        
        // Create routes.json with 1 bus route
        String routesJson = """
{
  "features": [
    {"attributes":{"id_ruta":"101","codigo":"T1","nombre":"Test Route","tipo":"Integrada"}}
  ]
}
""";
        Files.writeString(rawDir.resolve("routes.json"), routesJson);
        
        // Create bus_segments.csv with 1 segment
        String busSegmentsCsv = """
route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
101,outbound,100,101,1,2,1000,1200,1200,road
""";
        Files.writeString(tempDir.resolve("bus_segments.csv"), busSegmentsCsv);
        
        // Create transit_lines.csv with 2 stations on line A
        String transitLinesCsv = """
line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
A,metro,1,Station 1,6.25,-75.57,1500,90
A,metro,2,Station 2,6.26,-75.56,,
""";
        Files.writeString(tempDir.resolve("transit_lines.csv"), transitLinesCsv);
        
        // Create walk_links.csv with 1 bidirectional link between isolated stops
        String walkLinksCsv = """
place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
STOP:200,Isolated Stop 1,STOP:300,Isolated Stop 2,stop_stop,250,195
""";
        Files.writeString(tempDir.resolve("walk_links.csv"), walkLinksCsv);
        
        // Build the graph
        TransitGraph graph = builder.build(tempDir);
        
        // Verify graph combines all edges
        assertNotNull(graph, "Graph should not be null");
        
        // Expected states:
        // - 6 WALK states: 4 stops (100, 101, 200, 300) + 2 stations (Station 1, Station 2)
        // - 2 BUS_STOP states: stops 100, 101 on route 101 outbound
        // - 4 METRO_STATION states: 2 stations × 2 directions (outbound, inbound)
        // Total: 12 states
        assertEquals(12, graph.getStateCount(), "Graph should have 12 states (6 WALK + 2 BUS + 4 METRO)");
        
        // Expected edges:
        // - 3 RIDE edges: 1 bus (100→101) + 2 transit (bidirectional: St1→St2, St2→St1)
        // - 6 BOARD edges: 2 bus + 4 metro (2 stations × 2 directions)
        // - 6 ALIGHT edges: 2 bus + 4 metro
        // - 2 WALK edges: 1 link × 2 directions (200↔300)
        // Total: 17 edges
        assertEquals(17, graph.getEdgeCount(), "Graph should have 17 edges (3 RIDE + 6 BOARD + 6 ALIGHT + 2 WALK)");
    }
    

    
    /**
     * Test that GraphBuilder logs warnings for missing files but continues.
     * Uses real walk_links.csv format with prefixed place IDs.
     */
    @Test
    void testBuildContinuesWhenSomeFilesAreMissing(@TempDir Path tempDir) throws IOException {
        GraphBuilder builder = new GraphBuilder();
        
        // Create data directory structure
        Path rawDir = tempDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        // Create stops_0.json with 2 stops
        String stopsJson = """
{
  "features": [
    {"attributes":{"OBJECTID":1,"id_paradero":100,"direccion":"Test Stop 1"},"geometry":{"x":-75.57,"y":6.25}},
    {"attributes":{"OBJECTID":2,"id_paradero":101,"direccion":"Test Stop 2"},"geometry":{"x":-75.56,"y":6.26}}
  ]
}
""";
        Files.writeString(rawDir.resolve("stops_0.json"), stopsJson);
        
        // Create routes.json (empty features array)
        String routesJson = """
{
  "features": []
}
""";
        Files.writeString(rawDir.resolve("routes.json"), routesJson);
        
        // Only create walk_links.csv (bus_segments and transit_lines missing)
        String walkLinksCsv = """
place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
STOP:100,Test Stop 1,STOP:101,Test Stop 2,stop_stop,250,195
""";
        Files.writeString(tempDir.resolve("walk_links.csv"), walkLinksCsv);
        // bus_segments.csv and transit_lines.csv are intentionally missing
        
        // Build should succeed with warnings
        TransitGraph graph = builder.build(tempDir);
        
        assertNotNull(graph, "Graph should be built even with missing files");
        
        // Expected: 2 WALK states (one per stop) + 2 WALK edges (bidirectional link)
        assertEquals(2, graph.getStateCount(), "Graph should have 2 WALK states from available data");
        assertEquals(2, graph.getEdgeCount(), "Graph should have 2 WALK edges (bidirectional)");
    }
}
