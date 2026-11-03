package co.medellin.transit.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusSegmentReader.
 */
class BusSegmentReaderTest {

    @Test
    void testReadValidBusSegments(@TempDir Path tempDir) throws IOException {
        // Create test CSV file with bus segment data
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, """
            route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
            301A,0,1001,1002,1,2,250.5,320.8,300.0,calculated
            301A,0,1002,1003,2,3,180.2,210.5,200.0,estimated
            C101,1,2001,2002,5,6,450.0,520.0,500.0,measured
            """);
        
        // Read bus segments
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Verify results
        assertEquals(3, segments.size());
        
        BusSegment seg1 = segments.get(0);
        assertEquals("301A", seg1.routeId());
        assertEquals(0, seg1.direction());
        assertEquals(1001, seg1.fromStopId());
        assertEquals(1002, seg1.toStopId());
        assertEquals(1, seg1.fromSeq());
        assertEquals(2, seg1.toSeq());
        assertEquals(250.5, seg1.straightM(), 0.01);
        assertEquals(320.8, seg1.roadM(), 0.01);
        assertEquals(300.0, seg1.usedM(), 0.01);
        assertEquals("calculated", seg1.method());
        
        BusSegment seg2 = segments.get(1);
        assertEquals("301A", seg2.routeId());
        assertEquals(0, seg2.direction());
        assertEquals(1002, seg2.fromStopId());
        
        BusSegment seg3 = segments.get(2);
        assertEquals("C101", seg3.routeId());
        assertEquals(1, seg3.direction());
        assertEquals(2001, seg3.fromStopId());
    }

    @Test
    void testSkipSegmentWithInvalidDirection(@TempDir Path tempDir) throws IOException {
        // Test that segments with invalid direction (not 0 or 1) are skipped
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, """
            route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
            301A,0,1001,1002,1,2,250.5,320.8,300.0,calculated
            301B,2,2001,2002,1,2,180.2,210.5,200.0,invalid_direction
            301C,1,3001,3002,1,2,450.0,520.0,500.0,measured
            """);
        
        // Read bus segments - should skip malformed one
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("301A", segments.get(0).routeId());
        assertEquals("301C", segments.get(1).routeId());
    }

    @Test
    void testSkipSegmentWithNegativeStopId(@TempDir Path tempDir) throws IOException {
        // Test that segments with negative stop IDs are skipped
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, """
            route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
            301A,0,1001,1002,1,2,250.5,320.8,300.0,valid
            301B,0,-1,2002,1,2,180.2,210.5,200.0,invalid_from
            301C,0,3001,-5,1,2,450.0,520.0,500.0,invalid_to
            301D,1,4001,4002,1,2,350.0,400.0,380.0,valid
            """);
        
        // Read bus segments - should skip malformed ones
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("301A", segments.get(0).routeId());
        assertEquals("301D", segments.get(1).routeId());
    }

    @Test
    void testSkipSegmentWithNegativeSequence(@TempDir Path tempDir) throws IOException {
        // Test that segments with negative sequence numbers are skipped
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, """
            route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
            301A,0,1001,1002,1,2,250.5,320.8,300.0,valid
            301B,0,2001,2002,-1,2,180.2,210.5,200.0,invalid_from_seq
            301C,0,3001,3002,1,-2,450.0,520.0,500.0,invalid_to_seq
            """);
        
        // Read bus segments - should skip malformed ones
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Should have only 1 valid segment
        assertEquals(1, segments.size());
        assertEquals("301A", segments.get(0).routeId());
    }

    @Test
    void testSkipSegmentWithInvalidDistances(@TempDir Path tempDir) throws IOException {
        // Test that segments with zero or negative distances are skipped
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, """
            route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
            301A,0,1001,1002,1,2,250.5,320.8,300.0,valid
            301B,0,2001,2002,1,2,0,210.5,200.0,zero_straight
            301C,0,3001,3002,1,2,450.0,-10,500.0,negative_road
            301D,0,4001,4002,1,2,350.0,400.0,0,zero_used
            """);
        
        // Read bus segments - should skip malformed ones
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Should have only 1 valid segment
        assertEquals(1, segments.size());
        assertEquals("301A", segments.get(0).routeId());
    }

    @Test
    void testSkipSegmentWithEmptyRouteId(@TempDir Path tempDir) throws IOException {
        // Test that segments with empty route ID are skipped
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, """
            route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
            301A,0,1001,1002,1,2,250.5,320.8,300.0,valid
            ,0,2001,2002,1,2,180.2,210.5,200.0,empty_route_id
            301C,1,3001,3002,1,2,450.0,520.0,500.0,valid
            """);
        
        // Read bus segments - should skip malformed one
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("301A", segments.get(0).routeId());
        assertEquals("301C", segments.get(1).routeId());
    }

    @Test
    void testSkipSegmentWithMissingFields(@TempDir Path tempDir) throws IOException {
        // Test that segments with missing numeric fields are skipped
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, """
            route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
            301A,0,1001,1002,1,2,250.5,320.8,300.0,valid
            301B,0,,2002,1,2,180.2,210.5,200.0,missing_from_stop
            301C,0,3001,3002,1,2,450.0,,500.0,missing_road_m
            """);
        
        // Read bus segments - should skip malformed ones
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Should have only 1 valid segment
        assertEquals(1, segments.size());
        assertEquals("301A", segments.get(0).routeId());
    }

    @Test
    void testEmptyCsvFile(@TempDir Path tempDir) throws IOException {
        // Test empty CSV (only header)
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, """
            route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
            """);
        
        // Read bus segments
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Should have no segments
        assertEquals(0, segments.size());
    }

    @Test
    void testLargeDataset(@TempDir Path tempDir) throws IOException {
        // Test with larger dataset to ensure performance
        StringBuilder csv = new StringBuilder();
        csv.append("route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method\n");
        
        for (int i = 1; i <= 500; i++) {
            String routeId = String.format(java.util.Locale.US, "R%03d", ((i - 1) % 100) + 1);
            csv.append(String.format(java.util.Locale.US, "%s,0,%d,%d,%d,%d,%.1f,%.1f,%.1f,calculated\n",
                routeId, i, i + 1, i, i + 1, 250.0 + i, 300.0 + i, 280.0 + i));
        }
        
        Path csvFile = tempDir.resolve("bus_segments.csv");
        Files.writeString(csvFile, csv.toString());
        
        // Read bus segments
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Verify all segments loaded
        assertEquals(500, segments.size());
        assertEquals(1, segments.get(0).fromStopId());
        assertEquals(501, segments.get(499).toStopId());
    }

    @Test
    void testReadRealFile() throws IOException {
        // Load real bus segments file
        Path csvFile = Paths.get("data/bus_segments.csv");
        
        // Read bus segments
        BusSegmentReader reader = new BusSegmentReader();
        List<BusSegment> segments = reader.read(csvFile);
        
        // Verify count
        assertEquals(9550, segments.size(), 
            "Expected 9,550 bus segments from real file");
        
        // Spot-check first segment
        BusSegment first = segments.get(0);
        assertNotNull(first.routeId());
        assertFalse(first.routeId().isEmpty());
        assertTrue(first.direction() == 0 || first.direction() == 1);
        assertTrue(first.fromStopId() >= 0);
        assertTrue(first.toStopId() >= 0);
        assertTrue(first.usedM() > 0);
    }
}
