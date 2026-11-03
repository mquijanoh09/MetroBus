package co.medellin.transit.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for GraphBuilder with raw transit data.
 * Verifies that the builder correctly constructs a graph from bus segments,
 * transit lines, and walk links.
 */
class GraphBuilderIntegrationTest {
    
    @Test
    void testBuildGraphFromRawTransitData(@TempDir Path tempDir) throws IOException {
        // Create test data directory structure
        Path rawDir = tempDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        // Create minimal stops data (stops_0.json)
        String stopsJson = """
{
  "features": [
    {"attributes":{"OBJECTID":1,"id_paradero":100,"nombre_paradero":"Parada A","direccion":"Calle 1"},"geometry":{"x":-75.57,"y":6.25}},
    {"attributes":{"OBJECTID":2,"id_paradero":101,"nombre_paradero":"Parada B","direccion":"Calle 2"},"geometry":{"x":-75.56,"y":6.26}},
    {"attributes":{"OBJECTID":3,"id_paradero":102,"nombre_paradero":"Parada C","direccion":"Calle 3"},"geometry":{"x":-75.55,"y":6.27}}
  ]
}
""";
        Files.writeString(rawDir.resolve("stops_0.json"), stopsJson);
        
        // Create minimal routes data
        String routesJson = """
{
  "features": [
    {"attributes":{"OBJECTID":1,"cod_ruta":"C101","nombre":"Ruta 101","tipo_servicio":"Integrada","color":"#FF0000"}}
  ]
}
""";
        Files.writeString(rawDir.resolve("routes.json"), routesJson);
        
        // Create bus segments CSV
        String busSegmentsCsv = """
route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
C101,0,100,101,1,2,1000,1200,1150,road
C101,0,101,102,2,3,1100,1300,1250,road
""";
        Files.writeString(tempDir.resolve("bus_segments.csv"), busSegmentsCsv);
        
        // Create transit lines CSV (empty for this test)
        String transitLinesCsv = "line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next\n";
        Files.writeString(tempDir.resolve("transit_lines.csv"), transitLinesCsv);
        
        // Create walk links CSV
        String walkLinksCsv = """
place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
100,Parada A,101,Parada B,stop-to-stop,1000,800
101,Parada B,102,Parada C,stop-to-stop,1100,880
""";
        Files.writeString(tempDir.resolve("walk_links.csv"), walkLinksCsv);
        
        // Build the graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(tempDir);
        
        // Verify graph structure
        assertNotNull(graph, "Graph should not be null");
        assertTrue(graph.getStateCount() > 0, "Graph should have states");
        assertTrue(graph.getEdgeCount() > 0, "Graph should have edges");
        
        // We should have:
        // - 3 WALK states (one per stop)
        // - 3 BUS_STOP states (one per stop on route C101 direction 0)
        // Total: 6 states
        assertEquals(6, graph.getStateCount(), "Should have 6 states");
        
        // We should have:
        // - 2 RIDE edges (bus segments)
        // - 3 BOARD edges (WALK -> BUS_STOP for each stop)
        // - 3 ALIGHT edges (BUS_STOP -> WALK for each stop)
        // - 4 WALK edges (2 bidirectional walk links)
        // Total: 12 edges
        assertEquals(12, graph.getEdgeCount(), "Should have 12 edges");
        
        System.out.println("✓ GraphBuilder integration test passed");
        System.out.println("  States: " + graph.getStateCount());
        System.out.println("  Edges: " + graph.getEdgeCount());
    }
    
    @Test
    void testBuildGraphWithTransitLines(@TempDir Path tempDir) throws IOException {
        // Create test data directory structure
        Path rawDir = tempDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        // Create minimal stops data (empty for this test)
        String stopsJson = """
{
  "features": []
}
""";
        Files.writeString(rawDir.resolve("stops_0.json"), stopsJson);
        
        // Create minimal routes data
        String routesJson = """
{
  "features": []
}
""";
        Files.writeString(rawDir.resolve("routes.json"), routesJson);
        
        // Create bus segments CSV (empty)
        String busSegmentsCsv = "route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method\n";
        Files.writeString(tempDir.resolve("bus_segments.csv"), busSegmentsCsv);
        
        // Create transit lines CSV with metro stations
        String transitLinesCsv = """
line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
A,Metro,0,Niquía,6.30,-75.58,1500,90
A,Metro,1,Bello,6.31,-75.57,1600,95
A,Metro,2,Madera,6.32,-75.56,1700,100
""";
        Files.writeString(tempDir.resolve("transit_lines.csv"), transitLinesCsv);
        
        // Create walk links CSV (empty)
        String walkLinksCsv = "place_a,name_a,place_b,name_b,kind,straight_m,walk_sec\n";
        Files.writeString(tempDir.resolve("walk_links.csv"), walkLinksCsv);
        
        // Build the graph
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(tempDir);
        
        // Verify graph structure
        assertNotNull(graph, "Graph should not be null");
        assertTrue(graph.getStateCount() > 0, "Graph should have states");
        assertTrue(graph.getEdgeCount() > 0, "Graph should have edges");
        
        // We should have:
        // - 0 WALK states (no stops)
        // - 3 METRO_STATION states (one per station on line A)
        // Total: 3 states
        assertEquals(3, graph.getStateCount(), "Should have 3 states");
        
        // We should have:
        // - 2 RIDE edges (connecting 3 stations)
        // Total: 2 edges
        assertEquals(2, graph.getEdgeCount(), "Should have 2 edges");
        
        System.out.println("✓ GraphBuilder transit lines test passed");
        System.out.println("  States: " + graph.getStateCount());
        System.out.println("  Edges: " + graph.getEdgeCount());
    }
}
