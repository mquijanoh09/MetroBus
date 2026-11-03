package co.medellin.transit.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransitLineReader.
 */
class TransitLineReaderTest {

    @Test
    void testReadValidTransitSegments(@TempDir Path tempDir) throws IOException {
        // Create test CSV file with transit line data
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,Niquia,6.3100,-75.5650,1200.5,90
            A,Metro,1,Bello,6.3050,-75.5670,850.0,65
            B,Metroplus,0,Aranjuez,6.2800,-75.5700,600.0,50
            T-A,Tranvía,0,Oriente,6.2500,-75.5600,400.0,40
            L,Metrocable,0,Santo Domingo,6.3200,-75.5400,500.0,60
            """);
        
        // Read transit segments
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Verify results
        assertEquals(5, segments.size());
        
        TransitSegment seg1 = segments.get(0);
        assertEquals("A", seg1.line());
        assertEquals("Metro", seg1.mode());
        assertEquals(0, seg1.order());
        assertEquals("Niquia", seg1.station());
        assertEquals(6.3100, seg1.coordinate().getLat(), 0.0001);
        assertEquals(-75.5650, seg1.coordinate().getLon(), 0.0001);
        assertEquals(1200.5, seg1.distToNextM(), 0.01);
        assertEquals(90, seg1.secToNext());
        
        TransitSegment seg2 = segments.get(1);
        assertEquals("A", seg2.line());
        assertEquals("Metro", seg2.mode());
        assertEquals(1, seg2.order());
        
        TransitSegment seg3 = segments.get(2);
        assertEquals("Metroplus", seg3.mode());
        
        TransitSegment seg4 = segments.get(3);
        assertEquals("Tranvía", seg4.mode());
        
        TransitSegment seg5 = segments.get(4);
        assertEquals("Metrocable", seg5.mode());
    }

    @Test
    void testSkipSegmentWithInvalidMode(@TempDir Path tempDir) throws IOException {
        // Test that segments with invalid mode are skipped
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,Niquia,6.3100,-75.5650,1200.5,90
            B,Bus,1,Invalid Mode,6.3050,-75.5670,850.0,65
            C,Metroplus,2,Valid,6.2800,-75.5700,600.0,50
            """);
        
        // Read transit segments - should skip malformed one
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("Metro", segments.get(0).mode());
        assertEquals("Metroplus", segments.get(1).mode());
    }

    @Test
    void testSkipSegmentWithCoordinatesOutOfBounds(@TempDir Path tempDir) throws IOException {
        // Test that segments with coordinates outside Medellín bounds are skipped
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,Valid Station,6.2500,-75.5700,1200.5,90
            B,Metro,1,Too Far North,6.4000,-75.5670,850.0,65
            C,Metro,2,Too Far South,6.1000,-75.5700,600.0,50
            D,Metro,3,Too Far East,6.2500,-75.4000,400.0,40
            E,Metro,4,Too Far West,6.2500,-75.7000,500.0,60
            F,Metro,5,Another Valid,6.3000,-75.5600,300.0,30
            """);
        
        // Read transit segments - should skip out-of-bounds ones
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("Valid Station", segments.get(0).station());
        assertEquals("Another Valid", segments.get(1).station());
    }

    @Test
    void testSkipSegmentWithNegativeOrder(@TempDir Path tempDir) throws IOException {
        // Test that segments with negative order are skipped
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,Valid,6.2500,-75.5700,1200.5,90
            A,Metro,-1,Invalid,6.2600,-75.5750,850.0,65
            A,Metro,1,Another Valid,6.2700,-75.5800,600.0,50
            """);
        
        // Read transit segments - should skip malformed one
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals(0, segments.get(0).order());
        assertEquals(1, segments.get(1).order());
    }

    @Test
    void testSkipSegmentWithNegativeDistance(@TempDir Path tempDir) throws IOException {
        // Test that segments with negative distances are skipped
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,Valid,6.2500,-75.5700,1200.5,90
            A,Metro,1,Invalid Distance,6.2600,-75.5750,-100.0,65
            A,Metro,2,Another Valid,6.2700,-75.5800,600.0,50
            """);
        
        // Read transit segments - should skip malformed one
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("Valid", segments.get(0).station());
        assertEquals("Another Valid", segments.get(1).station());
    }

    @Test
    void testSkipSegmentWithNegativeTime(@TempDir Path tempDir) throws IOException {
        // Test that segments with negative time are skipped
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,Valid,6.2500,-75.5700,1200.5,90
            A,Metro,1,Invalid Time,6.2600,-75.5750,850.0,-30
            A,Metro,2,Another Valid,6.2700,-75.5800,600.0,50
            """);
        
        // Read transit segments - should skip malformed one
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("Valid", segments.get(0).station());
        assertEquals("Another Valid", segments.get(1).station());
    }

    @Test
    void testSkipSegmentWithEmptyLine(@TempDir Path tempDir) throws IOException {
        // Test that segments with empty line are skipped
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,Valid,6.2500,-75.5700,1200.5,90
            ,Metro,1,Empty Line,6.2600,-75.5750,850.0,65
            B,Metro,2,Another Valid,6.2700,-75.5800,600.0,50
            """);
        
        // Read transit segments - should skip malformed one
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("A", segments.get(0).line());
        assertEquals("B", segments.get(1).line());
    }

    @Test
    void testSkipSegmentWithEmptyStation(@TempDir Path tempDir) throws IOException {
        // Test that segments with empty station are skipped
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,Valid,6.2500,-75.5700,1200.5,90
            A,Metro,1,,6.2600,-75.5750,850.0,65
            A,Metro,2,Another Valid,6.2700,-75.5800,600.0,50
            """);
        
        // Read transit segments - should skip malformed one
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have only 2 valid segments
        assertEquals(2, segments.size());
        assertEquals("Valid", segments.get(0).station());
        assertEquals("Another Valid", segments.get(1).station());
    }

    @Test
    void testAllowZeroDistanceAndTime(@TempDir Path tempDir) throws IOException {
        // Test that zero distance and time are allowed (last station on line)
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            A,Metro,0,First,6.2500,-75.5700,1200.5,90
            A,Metro,1,Last,6.2600,-75.5750,0,0
            """);
        
        // Read transit segments - both should be valid
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have 2 valid segments
        assertEquals(2, segments.size());
        assertEquals(0, segments.get(1).distToNextM(), 0.01);
        assertEquals(0, segments.get(1).secToNext());
    }

    @Test
    void testEmptyCsvFile(@TempDir Path tempDir) throws IOException {
        // Test empty CSV (only header)
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, """
            line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
            """);
        
        // Read transit segments
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Should have no segments
        assertEquals(0, segments.size());
    }

    @Test
    void testLargeDataset(@TempDir Path tempDir) throws IOException {
        // Test with larger dataset
        StringBuilder csv = new StringBuilder();
        csv.append("line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next\n");
        
        for (int i = 0; i < 200; i++) {
            double lat = 6.20 + (i * 0.0005); // Stay in bounds: 6.15 to 6.35
            double lon = -75.60 + (i * 0.0002); // Stay in bounds: -75.65 to -75.50
            int secToNext = 41 + (i % 20); // Values from 41 to 60
            String lineId = String.format(java.util.Locale.US, "Line%d", i % 5);
            csv.append(String.format(java.util.Locale.US, "%s,Metro,%d,Station %d,%.4f,%.4f,%.1f,%d\n",
                lineId, i, i, lat, lon, 500.0 + i, secToNext));
        }
        
        Path csvFile = tempDir.resolve("transit_lines.csv");
        Files.writeString(csvFile, csv.toString());
        
        // Read transit segments
        TransitLineReader reader = new TransitLineReader();
        List<TransitSegment> segments = reader.read(csvFile);
        
        // Verify all segments loaded
        assertEquals(200, segments.size());
        assertEquals(0, segments.get(0).order());
        assertEquals(199, segments.get(199).order());
    }
}
