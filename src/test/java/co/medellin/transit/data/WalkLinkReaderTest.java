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
            1001,Estación Poblado,1002,Parque Berrio,stop-to-stop,450.5,360
            2001,San Antonio,2002,Aguacatala,stop-to-station,320.8,280
            3001,Niquia Station,3002,Bello Station,station-to-station,680.0,550
            """);
        
        // Read walk links
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Verify results
        assertEquals(3, links.size());
        
        WalkLink link1 = links.get(0);
        assertEquals(1001, link1.placeA());
        assertEquals("Estación Poblado", link1.nameA());
        assertEquals(1002, link1.placeB());
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
            1001,Place A,1002,Place B,stop-to-stop,450.5,360
            2001,Place C,2002,Place D,invalid-kind,320.8,280
            3001,Place E,3002,Place F,station-to-station,680.0,550
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
    void testSkipLinkWithNegativePlaceId(@TempDir Path tempDir) throws IOException {
        // Test that links with negative place IDs are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            1001,Valid A,1002,Valid B,stop-to-stop,450.5,360
            -1,Invalid A,2002,Place B,stop-to-stop,320.8,280
            3001,Place A,-5,Invalid B,stop-to-station,680.0,550
            4001,Valid C,4002,Valid D,station-to-station,500.0,400
            """);
        
        // Read walk links - should skip malformed ones
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 2 valid links
        assertEquals(2, links.size());
        assertEquals(1001, links.get(0).placeA());
        assertEquals(4001, links.get(1).placeA());
    }

    @Test
    void testSkipLinkWithEmptyNames(@TempDir Path tempDir) throws IOException {
        // Test that links with empty names are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            1001,Valid A,1002,Valid B,stop-to-stop,450.5,360
            2001,,2002,Place B,stop-to-stop,320.8,280
            3001,Place A,3002,,stop-to-station,680.0,550
            4001,Valid C,4002,Valid D,station-to-station,500.0,400
            """);
        
        // Read walk links - should skip malformed ones
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 2 valid links
        assertEquals(2, links.size());
        assertEquals("Valid A", links.get(0).nameA());
        assertEquals("Valid C", links.get(1).nameA());
    }

    @Test
    void testSkipLinkWithZeroDistance(@TempDir Path tempDir) throws IOException {
        // Test that links with zero distance are skipped
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, """
            place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
            1001,Valid A,1002,Valid B,stop-to-stop,450.5,360
            2001,Place A,2002,Place B,stop-to-stop,0,280
            3001,Valid C,3002,Valid D,station-to-station,500.0,400
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
            1001,Valid A,1002,Valid B,stop-to-stop,450.5,360
            2001,Place A,2002,Place B,stop-to-stop,-100,280
            3001,Valid C,3002,Valid D,station-to-station,500.0,400
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
            1001,Valid A,1002,Valid B,stop-to-stop,450.5,360
            2001,Place A,2002,Place B,stop-to-stop,320.8,0
            3001,Valid C,3002,Valid D,station-to-station,500.0,400
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
            1001,Valid A,1002,Valid B,stop-to-stop,450.5,360
            2001,Place A,2002,Place B,stop-to-stop,320.8,-50
            3001,Valid C,3002,Valid D,station-to-station,500.0,400
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
            1001,Valid A,1002,Valid B,stop-to-stop,450.5,360
            ,Place A,2002,Place B,stop-to-stop,320.8,280
            3001,Place C,,Place D,stop-to-station,680.0,550
            4001,Valid C,4002,Valid D,station-to-station,,400
            """);
        
        // Read walk links - should skip malformed ones
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Should have only 1 valid link
        assertEquals(1, links.size());
        assertEquals(1001, links.get(0).placeA());
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
            csv.append(String.format(java.util.Locale.US, "%d,Place %d,%d,Place %d,%s,%.1f,%d\n",
                i, i, i + 1, i + 1, kind, 400.0 + i, walkSec));
        }
        
        Path csvFile = tempDir.resolve("walk_links.csv");
        Files.writeString(csvFile, csv.toString());
        
        // Read walk links
        WalkLinkReader reader = new WalkLinkReader();
        List<WalkLink> links = reader.read(csvFile);
        
        // Verify all links loaded
        assertEquals(300, links.size());
        assertEquals(1, links.get(0).placeA());
        assertEquals(301, links.get(299).placeB());
    }
}
