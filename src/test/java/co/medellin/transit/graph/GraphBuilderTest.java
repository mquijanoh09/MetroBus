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
     * Test that GraphBuilder can construct a minimal graph from edge data.
     */
    @Test
    void testBuildCreatesGraphFromEdgeData(@TempDir Path tempDir) throws IOException {
        GraphBuilder builder = new GraphBuilder();
        
        // Create a minimal walk_links.csv file
        String csvContent = """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            0,1,WALK,120,210
            1,2,WALK,90,165
            2,0,WALK,100,180
            """;
        
        Path walkLinksFile = tempDir.resolve("walk_links.csv");
        Files.writeString(walkLinksFile, csvContent);
        
        // Build the graph
        TransitGraph graph = builder.build(tempDir);
        
        // Verify graph was constructed
        assertNotNull(graph, "Graph should not be null");
        assertEquals(3, graph.getStateCount(), "Graph should have 3 states (0, 1, 2)");
        assertEquals(3, graph.getEdgeCount(), "Graph should have 3 edges");
        
        // Verify edges exist
        assertEquals(1, graph.getOutgoingEdges(0).size(), "State 0 should have 1 outgoing edge");
        assertEquals(1, graph.getOutgoingEdges(1).size(), "State 1 should have 1 outgoing edge");
        assertEquals(1, graph.getOutgoingEdges(2).size(), "State 2 should have 1 outgoing edge");
    }
    
    /**
     * Test that GraphBuilder handles multiple CSV files.
     */
    @Test
    void testBuildHandlesMultipleCsvFiles(@TempDir Path tempDir) throws IOException {
        GraphBuilder builder = new GraphBuilder();
        
        // Create walk_links.csv
        String walkCsv = """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            0,1,WALK,120,210
            """;
        Files.writeString(tempDir.resolve("walk_links.csv"), walkCsv);
        
        // Create bus_segments.csv
        String busCsv = """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            1,2,RIDE,180,180
            """;
        Files.writeString(tempDir.resolve("bus_segments.csv"), busCsv);
        
        // Create transit_lines.csv
        String transitCsv = """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            2,3,RIDE,240,240
            """;
        Files.writeString(tempDir.resolve("transit_lines.csv"), transitCsv);
        
        // Build the graph
        TransitGraph graph = builder.build(tempDir);
        
        // Verify graph combines all edges
        assertNotNull(graph, "Graph should not be null");
        assertEquals(4, graph.getStateCount(), "Graph should have 4 states");
        assertEquals(3, graph.getEdgeCount(), "Graph should have 3 edges total");
    }
    
    /**
     * Test that GraphBuilder handles CSV with alternative field names.
     */
    @Test
    void testBuildHandlesAlternativeFieldNames(@TempDir Path tempDir) throws IOException {
        GraphBuilder builder = new GraphBuilder();
        
        // Create CSV with alternative field names (from_id, to_id, kind, duration, cost)
        String csvContent = """
            from_id,to_id,kind,duration,cost
            0,1,WALK,60,120
            1,0,WALK,60,120
            """;
        
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, csvContent);
        
        // Build the graph
        TransitGraph graph = builder.build(tempDir);
        
        // Verify graph was constructed
        assertNotNull(graph, "Graph should not be null");
        assertEquals(2, graph.getStateCount(), "Graph should have 2 states");
        assertEquals(2, graph.getEdgeCount(), "Graph should have 2 edges");
    }
    
    /**
     * Test that GraphBuilder logs warnings for missing files but continues.
     */
    @Test
    void testBuildContinuesWhenSomeFilesAreMissing(@TempDir Path tempDir) throws IOException {
        GraphBuilder builder = new GraphBuilder();
        
        // Only create one of the three expected files
        String csvContent = """
            source_id,target_id,edge_kind,real_sec,perceived_cost
            0,1,WALK,120,210
            """;
        
        Files.writeString(tempDir.resolve("walk_links.csv"), csvContent);
        // bus_segments.csv and transit_lines.csv are missing
        
        // Build should succeed with warnings
        TransitGraph graph = builder.build(tempDir);
        
        assertNotNull(graph, "Graph should be built even with missing files");
        assertEquals(2, graph.getStateCount(), "Graph should have states from available data");
        assertEquals(1, graph.getEdgeCount(), "Graph should have edges from available data");
    }
}
