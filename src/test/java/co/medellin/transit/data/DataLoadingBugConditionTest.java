package co.medellin.transit.data;

import co.medellin.transit.model.Stop;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for fixed data loading behaviors.
 * These tests verify that previous bugs have been resolved.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataLoadingBugConditionTest {
    
    private static Path testDataDir;
    
    @BeforeAll
    static void setUp() throws IOException {
        testDataDir = Files.createTempDirectory("transit-test-");
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
                        // Ignore cleanup errors
                    }
                });
        }
    }
    
    /**
     * Test that specialized readers handle raw transit schema correctly.
     */
    @Test
    @Order(1)
    @DisplayName("Fixed: Specialized readers parse raw transit schema")
    void testRawTransitSchemaWorks() throws IOException {
        // BusSegmentReader should handle raw transit schema
        Path csvFile = testDataDir.resolve("bus_segments.csv");
        String csvContent = "route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method\n" +
            "301,0,1001,1002,1,2,250.5,300.2,285.7,bus\n" +
            "301,0,1002,1003,2,3,180.3,210.8,195.4,bus\n";
        Files.writeString(csvFile, csvContent);
        
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        assertEquals(2, segments.size(), "Expected 2 bus segments to be parsed");
        assertEquals("301", segments.get(0).routeId());
        assertEquals(1001, segments.get(0).fromStopId());
    }
    
    /**
     * Test that ArcGisReader finds files in raw/ subdirectory with offset-based naming.
     */
    @Test
    @Order(2)
    @DisplayName("Fixed: ArcGisReader reads from raw/ subdirectory with offset-based naming")
    void testOffsetBasedPaginationInRaw() throws IOException {
        Path rawDir = testDataDir.resolve("raw");
        Files.createDirectories(rawDir);
        
        Path jsonFile = rawDir.resolve("stops_0.json");
        String jsonContent = "{\n" +
            "  \"features\": [\n" +
            "    {\n" +
            "      \"attributes\": {\"OBJECTID\": 1, \"direccion\": \"Test Stop\"},\n" +
            "      \"geometry\": {\"x\": -75.567, \"y\": 6.234}\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
        Files.writeString(jsonFile, jsonContent);
        
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.readPaginated(testDataDir, "stops", ".json");
        
        assertEquals(1, stops.size(), "Expected 1 stop from raw/stops_0.json");
        assertEquals("Test Stop", stops.get(0).getName());
    }
    
    /**
     * Test that nested geometry is parsed correctly.
     */
    @Test
    @Order(3)
    @DisplayName("Fixed: Nested geometry parsing works")
    void testNestedGeometryParsing() throws IOException {
        Path jsonFile = testDataDir.resolve("stops_test.json");
        String jsonContent = "{\n" +
            "  \"features\": [\n" +
            "    {\n" +
            "      \"attributes\": {\n" +
            "        \"OBJECTID\": 1,\n" +
            "        \"direccion\": \"Test Stop with Nested Geometry\"\n" +
            "      },\n" +
            "      \"geometry\": {\n" +
            "        \"x\": -75.567890,\n" +
            "        \"y\": 6.234567\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";
        Files.writeString(jsonFile, jsonContent);
        
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.read(jsonFile);
        
        assertEquals(1, stops.size(), "Expected 1 stop with nested geometry");
        assertEquals(-75.567890, stops.get(0).getCoordinate().getLon(), 0.0001);
        assertEquals(6.234567, stops.get(0).getCoordinate().getLat(), 0.0001);
    }
}
