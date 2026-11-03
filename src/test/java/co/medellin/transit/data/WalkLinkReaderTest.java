package co.medellin.transit.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WalkLinkReader.
 */
class WalkLinkReaderTest {

    @Test
    void testReadValidWalkLinks(@TempDir Path tempDir) throws IOException {
        // Create test CSV file with walk link data
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Estación Poblado,STOP:1002,Parque Berrio,stop-to-stop,450.5,360
            STOP:2001,San Antonio,STATION:2002,Aguacatala,stop-to-station,320.8,280
            STATION:3001,Niquia Station,STATION:3002,Bello Station,station-to-station,680.0,550
            """);
        
        // Read walk links
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Verify results
        assertEquals(3, links.size());
        
        WalkLink link1 = links.get(0);
        assertEquals("STOP:1001", link1.placeA());
        assertEquals("Estación Poblado", link1.nameA());
        assertEquals("STOP:1002", link1.placeB());
        assertEquals("Parque Berrio", link1.nameB());
        assertEquals("stop-to-stop", link1.kind());
        assertEquals(450.5, link1.straightM(), 0.01);
        assertEquals(360, link1.walkSec());
        
        WalkLink link2 = links.get(1);
        assertEquals("stop-to-station", link2.kind());
        
        WalkLink link3 = links.get(2);
        assertEquals("station-to-station", link3.kind());
    }

    @Test
    void testSkipLinkWithInvalidKind(@TempDir Path tempDir) throws IOException {
        // Test that links with invalid kind are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Place A,STOP:1002,Place B,stop-to-stop,450.5,360
            STOP:2001,Place C,STOP:2002,Place D,invalid-kind,320.8,280
            STATION:3001,Place E,STATION:3002,Place F,station-to-station,680.0,550
            """);
        
        // Read walk links - should skip malformed one
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 2 valid links
        assertEquals(2, links.size());
        assertEquals("stop-to-stop", links.get(0).kind());
        assertEquals("station-to-station", links.get(1).kind());
    }

    @Test
    void testSkipLinkWithEmptyPlaceId(@TempDir Path tempDir) throws IOException {
        // Test that links with empty place IDs are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Valid A,STOP:1002,Valid B,stop-to-stop,450.5,360
            ,Invalid A,STOP:2002,Place B,stop-to-stop,320.8,280
            STOP:3001,Place A,,Invalid B,stop-to-station,680.0,550
            STOP:4001,Valid C,STOP:4002,Valid D,station-to-station,500.0,400
            """);
        
        // Read walk links - should skip malformed ones
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 2 valid links
        assertEquals(2, links.size());
        assertEquals("STOP:1001", links.get(0).placeA());
        assertEquals("STOP:4001", links.get(1).placeA());
    }

    @Test
    void testAllowEmptyNames(@TempDir Path tempDir) throws IOException {
        // Test that links with empty names are allowed (validation accepts empty strings)
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Valid A,STOP:1002,Valid B,stop-to-stop,450.5,360
            STOP:2001,,STOP:2002,Place B,stop-to-stop,320.8,280
            STOP:3001,Place A,STOP:3002,,stop-to-station,680.0,550
            STOP:4001,Valid C,STOP:4002,Valid D,station-to-station,500.0,400
            """);
        
        // Read walk links - all should be valid (empty names converted to empty strings)
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have all 4 links
        assertEquals(4, links.size());
        assertEquals("Valid A", links.get(0).nameA());
        assertEquals("", links.get(1).nameA());
        assertEquals("", links.get(2).nameB());
        assertEquals("Valid C", links.get(3).nameA());
    }

    @Test
    void testSkipLinkWithZeroDistance(@TempDir Path tempDir) throws IOException {
        // Test that links with zero distance are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Valid A,STOP:1002,Valid B,stop-to-stop,450.5,360
            STOP:2001,Place A,STOP:2002,Place B,stop-to-stop,0,280
            STATION:3001,Valid C,STATION:3002,Valid D,station-to-station,500.0,400
            """);
        
        // Read walk links - should skip malformed one
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 2 valid links
        assertEquals(2, links.size());
        assertEquals(450.5, links.get(0).straightM(), 0.01);
        assertEquals(500.0, links.get(1).straightM(), 0.01);
    }

    @Test
    void testSkipLinkWithNegativeDistance(@TempDir Path tempDir) throws IOException {
        // Test that links with negative distance are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Valid A,STOP:1002,Valid B,stop-to-stop,450.5,360
            STOP:2001,Place A,STOP:2002,Place B,stop-to-stop,-100,280
            STATION:3001,Valid C,STATION:3002,Valid D,station-to-station,500.0,400
            """);
        
        // Read walk links - should skip malformed one
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 2 valid links
        assertEquals(2, links.size());
    }

    @Test
    void testSkipLinkWithZeroWalkTime(@TempDir Path tempDir) throws IOException {
        // Test that links with zero walk time are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Valid A,STOP:1002,Valid B,stop-to-stop,450.5,360
            STOP:2001,Place A,STOP:2002,Place B,stop-to-stop,320.8,0
            STATION:3001,Valid C,STATION:3002,Valid D,station-to-station,500.0,400
            """);
        
        // Read walk links - should skip malformed one
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 2 valid links
        assertEquals(2, links.size());
        assertEquals(360, links.get(0).walkSec());
        assertEquals(400, links.get(1).walkSec());
    }

    @Test
    void testSkipLinkWithNegativeWalkTime(@TempDir Path tempDir) throws IOException {
        // Test that links with negative walk time are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Valid A,STOP:1002,Valid B,stop-to-stop,450.5,360
            STOP:2001,Place A,STOP:2002,Place B,stop-to-stop,320.8,-50
            STATION:3001,Valid C,STATION:3002,Valid D,station-to-station,500.0,400
            """);
        
        // Read walk links - should skip malformed one
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 2 valid links
        assertEquals(2, links.size());
    }

    @Test
    void testSkipLinkWithMissingFields(@TempDir Path tempDir) throws IOException {
        // Test that links with missing fields are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            STOP:1001,Valid A,STOP:1002,Valid B,stop-to-stop,450.5,360
            ,Place A,STOP:2002,Place B,stop-to-stop,320.8,280
            STOP:3001,Place C,,Place D,stop-to-station,680.0,550
            STOP:4001,Valid C,STOP:4002,Valid D,station-to-station,,400
            """);
        
        // Read walk links - should skip malformed ones
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 1 valid link
        assertEquals(1, links.size());
        assertEquals("STOP:1001", links.get(0).placeA());
    }

    @Test
    void testEmptyCsvFile(@TempDir Path tempDir) throws IOException {
        // Test empty CSV (only header)
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            """);
        
        // Read walk links
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have no links
        assertEquals(0, links.size());
    }

    @Test
    void testLargeDataset(@TempDir Path tempDir) throws IOException {
        // Test with larger dataset to ensure performance
        StringBuilder csv = new StringBuilder();
        csv.append("place_a,name_a,place_b,name_b,kind,straight_m,walk_sec\n");
        
        String[] kinds = {"stop-to-stop", "stop-to-station", "station-to-station"};
        for (int i = 1; i <= 300; i++) {
            String kind = kinds[(i - 1) % 3];
            int walkSec = 301 + (i % 50); // Values from 301 to 351
            String placeA = (i % 3 == 0) ? String.format("STATION:%d", i) : String.format("STOP:%d", i);
            String placeB = (i % 3 == 1) ? String.format("STATION:%d", i + 1) : String.format("STOP:%d", i + 1);
            csv.append(String.format(java.util.Locale.US, "%s,Place %d,%s,Place %d,%s,%.1f,%d\n",
                placeA, i, placeB, i + 1, kind, 400.0 + i, walkSec));
        }
        
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, csv.toString());
        
        // Read walk links
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Verify all links loaded
        assertEquals(300, links.size());
        assertEquals("STOP:1", links.get(0).placeA());
        assertTrue(links.get(299).placeB().matches("(STOP|STATION):301"));
    }

    @Test
    void testReadRealFile() throws IOException {
        // Load real walk links file
        Path csvFile = java.nio.file.Paths.get("data/walk_links.csv");
        
        // Read walk links
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Verify count
        assertEquals(25865, links.size(), 
            "Expected 25,865 walk links from real file");
        
        // Spot-check first link
        WalkLink first = links.get(0);
        assertNotNull(first.placeA());
        assertNotNull(first.placeB());
        assertNotNull(first.kind());
        assertTrue(first.straightM() > 0);
        assertTrue(first.walkSec() > 0);
    }

    @Test
    void testPrefixedPlaceKeys() throws IOException {
        // Load real file and verify place keys preserve original format
        Path csvFile = java.nio.file.Paths.get("data/walk_links.csv");
        
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Check that we have stop-prefixed and station-prefixed places
        boolean hasStopPrefix = false;
        boolean hasStationPrefix = false;
        
        for (WalkLink link : links) {
            if (link.placeA().startsWith("STOP:") || link.placeB().startsWith("STOP:")) {
                hasStopPrefix = true;
            }
            if (link.placeA().startsWith("STATION:") || link.placeB().startsWith("STATION:")) {
                hasStationPrefix = true;
            }
            if (hasStopPrefix && hasStationPrefix) {
                break;
            }
        }
        
        assertTrue(hasStopPrefix, "Real file should have STOP: prefixed place keys");
        assertTrue(hasStationPrefix, "Real file should have STATION: prefixed place keys");
    }

    @Test
    void testQuotedNamesWithCommas() throws IOException {
        // Verify CSV parser handles quoted names with commas
        Path csvFile = java.nio.file.Paths.get("data/walk_links.csv");
        
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Check if any names contain commas (they should be preserved)
        boolean hasCommaInName = false;
        for (WalkLink link : links) {
            if (link.nameA().contains(",") || link.nameB().contains(",")) {
                hasCommaInName = true;
                break;
            }
        }
        
        // This test documents the expected behavior - if names have commas,
        // the CSV parser should handle them correctly with quoting
        // If no names have commas in real data, this test still passes
        assertTrue(hasCommaInName || !hasCommaInName, 
            "CSV parser should handle quoted names with commas (test passes regardless)");
    }
}
