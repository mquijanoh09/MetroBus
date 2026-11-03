package co.medellin.transit.data;

import co.medellin.transit.model.Edge;
import co.medellin.transit.model.EdgeKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GraphCsvReader.
 */
class GraphCsvReaderTest {

    @Test
    void testReadValidGraphEdges(@TempDir Path tempDir) throws IOException {
        // Create test CSV file with edge data
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            0,1,WALK,300,480
            1,2,BOARD,0,120
            2,3,RIDE,180,180
            3,4,ALIGHT,30,30
            """);
        
        // Read edges
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Verify results
        assertEquals(4, edges.size());
        
        Edge edge1 = edges.get(0);
        assertEquals(0, edge1.getSourceId());
        assertEquals(1, edge1.getTargetId());
        assertEquals(EdgeKind.WALK, edge1.getKind());
        assertEquals(300, edge1.getRealSec());
        assertEquals(480, edge1.getPerceivedCost());
        
        Edge edge2 = edges.get(1);
        assertEquals(1, edge2.getSourceId());
        assertEquals(2, edge2.getTargetId());
        assertEquals(EdgeKind.BOARD, edge2.getKind());
        assertEquals(0, edge2.getRealSec());
        assertEquals(120, edge2.getPerceivedCost());
        
        Edge edge3 = edges.get(2);
        assertEquals(EdgeKind.RIDE, edge3.getKind());
        
        Edge edge4 = edges.get(3);
        assertEquals(EdgeKind.ALIGHT, edge4.getKind());
    }

    @Test
    void testReadEdgesWithAlternativeFieldNames(@TempDir Path tempDir) throws IOException {
        // Test with alternative field names
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, """
            from_id,to_id,type,duration,cost
            10,11,walk,240,360
            11,12,ride,150,150
            """);
        
        // Read edges
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Verify
        assertEquals(2, edges.size());
        assertEquals(10, edges.get(0).getSourceId());
        assertEquals(11, edges.get(0).getTargetId());
        assertEquals(EdgeKind.WALK, edges.get(0).getKind());
        assertEquals(240, edges.get(0).getRealSec());
        assertEquals(360, edges.get(0).getPerceivedCost());
    }

    @Test
    void testReadEdgesWithMissingPerceivedCost(@TempDir Path tempDir) throws IOException {
        // Test edges without perceived_cost field (should default to real_sec)
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, """
            source_id,target_id,edge_kind,real_sec
            20,21,RIDE,180
            21,22,WALK,240
            """);
        
        // Read edges
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Verify perceived cost defaults to real_sec
        assertEquals(2, edges.size());
        assertEquals(180, edges.get(0).getRealSec());
        assertEquals(180, edges.get(0).getPerceivedCost());  // Default to real_sec
        assertEquals(240, edges.get(1).getRealSec());
        assertEquals(240, edges.get(1).getPerceivedCost());
    }

    @Test
    void testReadEdgesWithCaseInsensitiveEdgeKind(@TempDir Path tempDir) throws IOException {
        // Test that edge kind parsing is case-insensitive
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            30,31,walk,200,300
            31,32,BOARD,0,120
            32,33,Ride,150,150
            33,34,aLiGhT,25,25
            """);
        
        // Read edges
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Verify all edge kinds parsed correctly
        assertEquals(4, edges.size());
        assertEquals(EdgeKind.WALK, edges.get(0).getKind());
        assertEquals(EdgeKind.BOARD, edges.get(1).getKind());
        assertEquals(EdgeKind.RIDE, edges.get(2).getKind());
        assertEquals(EdgeKind.ALIGHT, edges.get(3).getKind());
    }

    @Test
    void testSkipEdgeWithInvalidEdgeKind(@TempDir Path tempDir) throws IOException {
        // Test that edges with invalid edge kind are skipped
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            40,41,WALK,200,300
            41,42,INVALID_KIND,100,100
            42,43,RIDE,150,150
            """);
        
        // Read edges - should skip malformed one
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Should have only 2 valid edges
        assertEquals(2, edges.size());
        assertEquals(40, edges.get(0).getSourceId());
        assertEquals(42, edges.get(1).getSourceId());
    }

    @Test
    void testSkipEdgeWithMissingSourceId(@TempDir Path tempDir) throws IOException {
        // Test that edges without source ID are skipped
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            50,51,WALK,200,300
            ,52,RIDE,100,100
            52,53,ALIGHT,50,50
            """);
        
        // Read edges - should skip malformed one
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Should have only 2 valid edges
        assertEquals(2, edges.size());
        assertEquals(50, edges.get(0).getSourceId());
        assertEquals(52, edges.get(1).getSourceId());
    }

    @Test
    void testSkipEdgeWithInvalidNumbers(@TempDir Path tempDir) throws IOException {
        // Test that edges with invalid numbers are skipped
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            60,61,WALK,200,300
            62,63,RIDE,not_a_number,100
            63,64,ALIGHT,50,50
            """);
        
        // Read edges - should skip malformed one
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Should have only 2 valid edges
        assertEquals(2, edges.size());
        assertEquals(60, edges.get(0).getSourceId());
        assertEquals(63, edges.get(1).getSourceId());
    }

    @Test
    void testEmptyCsvFile(@TempDir Path tempDir) throws IOException {
        // Test empty CSV (only header)
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            """);
        
        // Read edges
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Should have no edges
        assertEquals(0, edges.size());
    }

    @Test
    void testLargeGraphPerformance(@TempDir Path tempDir) throws IOException {
        // Test that large CSV files are handled efficiently
        // Create CSV with many edges (simulating ~82,160 edges performance requirement)
        StringBuilder csv = new StringBuilder();
        csv.append("source_id,target_id,edge_kind,real_sec,perceived_cost\n");
        
        for (int i = 0; i < 5000; i++) {
            int source = i;
            int target = i + 1;
            csv.append(String.format("%d,%d,RIDE,120,120\n", source, target));
        }
        
        Path csvFile = tempDir.resolve("edges.csv");
        Files.writeString(csvFile, csv.toString());
        
        // Read edges
        GraphCsvReader reader = new GraphCsvReader();
        List<Edge> edges = reader.read(csvFile);
        
        // Verify all edges loaded
        assertEquals(5000, edges.size());
        assertEquals(0, edges.get(0).getSourceId());
        assertEquals(4999, edges.get(4999).getSourceId());
    }
}
