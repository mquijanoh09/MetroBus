package co.medellin.transit.data;

import co.medellin.transit.model.Stop;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test pagination support in ArcGisReader.
 * Verifies both offset-based and page-based naming patterns.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaginationTest {
    
    private static Path testDataDir;
    
    @BeforeAll
    static void setUp() throws IOException {
        testDataDir = Files.createTempDirectory("pagination-test-");
        System.out.println("Pagination test data directory: " + testDataDir);
    }
    
    @AfterAll
    static void tearDown() throws IOException {
        if (testDataDir != null && Files.exists(testDataDir)) {
            Files.walk(testDataDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
        }
    }
    
    /**
     * Test offset-based pagination in raw/ subdirectory.
     * Files: raw/stops_0.json, raw/stops_2000.json, raw/stops_4000.json
     */
    @Test
    @Order(1)
    @DisplayName("Offset-based pagination with raw/ subdirectory")
    void testOffsetBasedPaginationInRaw() throws IOException {
        // Create raw/ subdirectory
        Path rawDir = testDataDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        // Create offset-based files
        createStopFile(rawDir.resolve("stops_0.json"), 1001, "Stop A");
        createStopFile(rawDir.resolve("stops_2000.json"), 1002, "Stop B");
        createStopFile(rawDir.resolve("stops_4000.json"), 1003, "Stop C");
        
        // Read paginated files
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.readPaginated(testDataDir, "stops", ".json");
        
        // Verify all stops loaded in order
        assertEquals(3, stops.size(), "Expected 3 stops from offset-based files");
        assertEquals("Stop A", stops.get(0).getName(), "First stop from stops_0.json");
        assertEquals("Stop B", stops.get(1).getName(), "Second stop from stops_2000.json");
        assertEquals("Stop C", stops.get(2).getName(), "Third stop from stops_4000.json");
        
        System.out.println("✓ Offset-based pagination in raw/ subdirectory works");
    }
    
    /**
     * Test page-based pagination in base directory (legacy support).
     * Files: stops_1.json, stops_2.json, stops_3.json
     */
    @Test
    @Order(2)
    @DisplayName("Page-based pagination (legacy fallback)")
    void testPageBasedPagination() throws IOException {
        // Create page-based files in base directory
        createStopFile(testDataDir.resolve("routes_1.json"), 2001, "Route A");
        createStopFile(testDataDir.resolve("routes_2.json"), 2002, "Route B");
        createStopFile(testDataDir.resolve("routes_3.json"), 2003, "Route C");
        
        // Read paginated files
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.readPaginated(testDataDir, "routes", ".json");
        
        // Verify all stops loaded in order
        assertEquals(3, stops.size(), "Expected 3 stops from page-based files");
        assertEquals("Route A", stops.get(0).getName(), "First stop from routes_1.json");
        assertEquals("Route B", stops.get(1).getName(), "Second stop from routes_2.json");
        assertEquals("Route C", stops.get(2).getName(), "Third stop from routes_3.json");
        
        System.out.println("✓ Page-based pagination (legacy) works");
    }
    
    /**
     * Test offset-based takes precedence over page-based.
     * When both patterns exist, offset-based should be used.
     */
    @Test
    @Order(3)
    @DisplayName("Offset-based takes precedence over page-based")
    void testOffsetBasedPrecedence() throws IOException {
        Path rawDir = testDataDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        // Create offset-based files in raw/
        createStopFile(rawDir.resolve("mixed_0.json"), 3001, "Offset A");
        createStopFile(rawDir.resolve("mixed_1000.json"), 3002, "Offset B");
        
        // Create page-based files in base directory
        createStopFile(testDataDir.resolve("mixed_1.json"), 9001, "Page A");
        createStopFile(testDataDir.resolve("mixed_2.json"), 9002, "Page B");
        
        // Read paginated files
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.readPaginated(testDataDir, "mixed", ".json");
        
        // Should use offset-based from raw/ directory
        assertEquals(2, stops.size(), "Expected 2 stops from offset-based files");
        assertEquals("Offset A", stops.get(0).getName(), "Should use offset-based, not page-based");
        assertEquals("Offset B", stops.get(1).getName(), "Should use offset-based, not page-based");
        
        System.out.println("✓ Offset-based takes precedence over page-based");
    }
    
    /**
     * Test correct numerical sorting of offset files.
     * Files: stops_0.json, stops_100.json, stops_2000.json
     * Should load in numerical order (0, 100, 2000), not lexical (0, 100, 2000 happens to be same)
     * Better test: 0, 1000, 500 -> should be 0, 500, 1000
     */
    @Test
    @Order(4)
    @DisplayName("Numerical sorting of offset files")
    void testNumericalSorting() throws IOException {
        Path rawDir = testDataDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        // Create files in non-sorted order
        createStopFile(rawDir.resolve("sorted_0.json"), 4001, "First");
        createStopFile(rawDir.resolve("sorted_1000.json"), 4002, "Third");
        createStopFile(rawDir.resolve("sorted_500.json"), 4003, "Second");
        
        // Read paginated files
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.readPaginated(testDataDir, "sorted", ".json");
        
        // Should be sorted numerically by offset
        assertEquals(3, stops.size(), "Expected 3 stops");
        assertEquals("First", stops.get(0).getName(), "sorted_0.json should be first");
        assertEquals("Second", stops.get(1).getName(), "sorted_500.json should be second (numerical sort)");
        assertEquals("Third", stops.get(2).getName(), "sorted_1000.json should be third");
        
        System.out.println("✓ Numerical sorting works correctly");
    }
    
    /**
     * Test empty result when no files found.
     */
    @Test
    @Order(5)
    @DisplayName("Empty result when no pagination files found")
    void testNoFilesFound() throws IOException {
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.readPaginated(testDataDir, "nonexistent", ".json");
        
        assertEquals(0, stops.size(), "Expected empty list when no files found");
        
        System.out.println("✓ Empty result for missing files works");
    }
    
    /**
     * Helper method to create a simple stop JSON file.
     */
    private void createStopFile(Path path, int objectId, String name) throws IOException {
        String content = String.format(
            "{\n" +
            "  \"features\": [\n" +
            "    {\n" +
            "      \"attributes\": {\n" +
            "        \"OBJECTID\": %d,\n" +
            "        \"nombre\": \"%s\",\n" +
            "        \"latitud\": 6.234,\n" +
            "        \"longitud\": -75.567\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n",
            objectId, name
        );
        Files.writeString(path, content);
    }
}
