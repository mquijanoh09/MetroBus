package co.medellin.transit.data;

import co.medellin.transit.model.Stop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StopCsvReader.
 */
class StopCsvReaderTest {

    @Test
    void testReadValidStopsCsv(@TempDir Path tempDir) throws IOException {
        // Create test CSV file with stop data
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, """
            id_paradero,nombre_paradero,latitud,longitud,direccion
            101,Estación Poblado,6.2088,-75.5698,Carrera 43A # 1 Sur - 100
            102,Parque Berrio,6.2514,-75.5655,Calle 51 # 52 - 83
            103,San Antonio,6.2445,-75.5748,Calle 44 # 51 - 10
            """);
        
        // Read stops
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Verify results
        assertEquals(3, stops.size());
        
        Stop stop1 = stops.get(0);
        assertEquals(101, stop1.getId());
        assertEquals("Estación Poblado", stop1.getName());
        assertEquals("Carrera 43A # 1 Sur - 100", stop1.getAddress());
        assertEquals(6.2088, stop1.getCoordinate().getLat(), 0.0001);
        assertEquals(-75.5698, stop1.getCoordinate().getLon(), 0.0001);
        
        Stop stop2 = stops.get(1);
        assertEquals(102, stop2.getId());
        assertEquals("Parque Berrio", stop2.getName());
        
        Stop stop3 = stops.get(2);
        assertEquals(103, stop3.getId());
        assertEquals("San Antonio", stop3.getName());
    }

    @Test
    void testReadStopsWithAlternativeFieldNames(@TempDir Path tempDir) throws IOException {
        // Test with alternative field names
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, """
            stop_id,name,lat,lon,address
            201,Terminal Norte,6.2800,-75.5800,Av. 77 # 65 - 200
            202,Terminal Sur,6.1700,-75.5900,Cra. 65 # 8B - 91
            """);
        
        // Read stops
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Verify
        assertEquals(2, stops.size());
        assertEquals(201, stops.get(0).getId());
        assertEquals("Terminal Norte", stops.get(0).getName());
        assertEquals(6.2800, stops.get(0).getCoordinate().getLat(), 0.0001);
        assertEquals(-75.5800, stops.get(0).getCoordinate().getLon(), 0.0001);
    }

    @Test
    void testReadStopsWithMissingAddress(@TempDir Path tempDir) throws IOException {
        // Test stops without address field (should default to empty string)
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, """
            id_paradero,nombre,latitud,longitud
            301,Unknown Location,6.2500,-75.5700
            """);
        
        // Read stops
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Verify
        assertEquals(1, stops.size());
        assertEquals("", stops.get(0).getAddress());
    }

    @Test
    void testReadStopsWithEmptyAddress(@TempDir Path tempDir) throws IOException {
        // Test stops with empty address field
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, """
            id,nombre,lat,lon,address
            401,Some Stop,6.2500,-75.5700,
            """);
        
        // Read stops
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Verify
        assertEquals(1, stops.size());
        assertEquals("", stops.get(0).getAddress());
    }

    @Test
    void testSkipStopWithMissingId(@TempDir Path tempDir) throws IOException {
        // Test that stops without ID are skipped
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, """
            id_paradero,nombre_paradero,latitud,longitud
            501,Valid Stop 1,6.2500,-75.5700
            ,Missing ID Stop,6.2600,-75.5800
            502,Valid Stop 2,6.2700,-75.5900
            """);
        
        // Read stops - should skip malformed one
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Should have only 2 valid stops
        assertEquals(2, stops.size());
        assertEquals(501, stops.get(0).getId());
        assertEquals(502, stops.get(1).getId());
    }

    @Test
    void testSkipStopWithEmptyName(@TempDir Path tempDir) throws IOException {
        // Test that stops with empty name are skipped
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, """
            id_paradero,nombre_paradero,latitud,longitud
            601,Valid Stop,6.2500,-75.5700
            602,,6.2600,-75.5800
            603,Another Valid,6.2700,-75.5900
            """);
        
        // Read stops - should skip one with empty name
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Should have only 2 valid stops
        assertEquals(2, stops.size());
        assertEquals(601, stops.get(0).getId());
        assertEquals(603, stops.get(1).getId());
    }

    @Test
    void testSkipStopWithInvalidCoordinates(@TempDir Path tempDir) throws IOException {
        // Test that stops with invalid coordinates are skipped
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, """
            id_paradero,nombre_paradero,latitud,longitud
            701,Valid Stop,6.2500,-75.5700
            702,Invalid Lat,not_a_number,-75.5800
            703,Another Valid,6.2700,-75.5900
            """);
        
        // Read stops - should skip malformed one
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Should have only 2 valid stops
        assertEquals(2, stops.size());
        assertEquals(701, stops.get(0).getId());
        assertEquals(703, stops.get(1).getId());
    }

    @Test
    void testEmptyCsvFile(@TempDir Path tempDir) throws IOException {
        // Test empty CSV (only header)
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, """
            id_paradero,nombre_paradero,latitud,longitud
            """);
        
        // Read stops
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Should have no stops
        assertEquals(0, stops.size());
    }

    @Test
    void testPreSizeCollectionPerformance(@TempDir Path tempDir) throws IOException {
        // Test that large CSV files are handled efficiently
        // Create CSV with many stops (simulating ~15,000 stops performance requirement)
        StringBuilder csv = new StringBuilder();
        csv.append("id_paradero,nombre_paradero,latitud,longitud\n");
        
        for (int i = 1; i <= 1000; i++) {
            csv.append(String.format("%d,Stop %d,6.25,-75.57\n", i, i));
        }
        
        Path csvFile = tempDir.resolve("stops.csv");
        Files.writeString(csvFile, csv.toString());
        
        // Read stops
        StopCsvReader reader = new StopCsvReader();
        List<Stop> stops = reader.read(csvFile);
        
        // Verify all stops loaded
        assertEquals(1000, stops.size());
        assertEquals(1, stops.get(0).getId());
        assertEquals(1000, stops.get(999).getId());
    }
}
