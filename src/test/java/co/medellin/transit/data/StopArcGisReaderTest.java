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
 * Unit tests for StopArcGisReader.
 */
class StopArcGisReaderTest {

    @Test
    void testReadValidStopsWithGeometry(@TempDir Path tempDir) throws IOException {
        // Create test ArcGIS JSON file with stop data
        Path jsonFile = tempDir.resolve("stops.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id_paradero": 101,
                    "nombre_paradero": "Estación Poblado",
                    "direccion": "Carrera 43A # 1 Sur - 100"
                  },
                  "geometry": {
                    "x": -75.5698,
                    "y": 6.2088
                  }
                },
                {
                  "attributes": {
                    "id_paradero": 102,
                    "nombre_paradero": "Parque Berrio",
                    "direccion": "Calle 51 # 52 - 83"
                  },
                  "geometry": {
                    "x": -75.5655,
                    "y": 6.2514
                  }
                }
              ]
            }
            """);
        
        // Read stops
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.read(jsonFile);
        
        // Verify results
        assertEquals(2, stops.size());
        
        Stop stop1 = stops.get(0);
        assertEquals(101, stop1.getId());
        assertEquals("Estación Poblado", stop1.getName());
        assertEquals("Carrera 43A # 1 Sur - 100", stop1.getAddress());
        assertEquals(6.2088, stop1.getCoordinate().getLat(), 0.0001);
        assertEquals(-75.5698, stop1.getCoordinate().getLon(), 0.0001);
        
        Stop stop2 = stops.get(1);
        assertEquals(102, stop2.getId());
        assertEquals("Parque Berrio", stop2.getName());
        assertEquals("Calle 51 # 52 - 83", stop2.getAddress());
        assertEquals(6.2514, stop2.getCoordinate().getLat(), 0.0001);
        assertEquals(-75.5655, stop2.getCoordinate().getLon(), 0.0001);
    }

    @Test
    void testReadStopsWithAlternativeFieldNames(@TempDir Path tempDir) throws IOException {
        // Test with alternative field names (OBJECTID, nombre)
        Path jsonFile = tempDir.resolve("stops.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "OBJECTID": 201,
                    "nombre": "Terminal Norte"
                  },
                  "geometry": {
                    "longitude": -75.5800,
                    "latitude": 6.2800
                  }
                }
              ]
            }
            """);
        
        // Read stops
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.read(jsonFile);
        
        // Verify
        assertEquals(1, stops.size());
        assertEquals(201, stops.get(0).getId());
        assertEquals("Terminal Norte", stops.get(0).getName());
        assertEquals(6.2800, stops.get(0).getCoordinate().getLat(), 0.0001);
        assertEquals(-75.5800, stops.get(0).getCoordinate().getLon(), 0.0001);
    }

    @Test
    void testReadStopsWithCoordinatesInAttributes(@TempDir Path tempDir) throws IOException {
        // Test stops with lat/lon in attributes instead of geometry
        Path jsonFile = tempDir.resolve("stops.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id_paradero": 301,
                    "nombre_paradero": "San Antonio",
                    "latitud": 6.2445,
                    "longitud": -75.5748,
                    "direccion": "Calle 44 # 51 - 10"
                  }
                }
              ]
            }
            """);
        
        // Read stops
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.read(jsonFile);
        
        // Verify
        assertEquals(1, stops.size());
        assertEquals(301, stops.get(0).getId());
        assertEquals("San Antonio", stops.get(0).getName());
        assertEquals(6.2445, stops.get(0).getCoordinate().getLat(), 0.0001);
        assertEquals(-75.5748, stops.get(0).getCoordinate().getLon(), 0.0001);
    }

    @Test
    void testReadStopsWithMissingAddress(@TempDir Path tempDir) throws IOException {
        // Test stops without address field (should default to empty string)
        Path jsonFile = tempDir.resolve("stops.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id_paradero": 401,
                    "nombre_paradero": "Unknown Location"
                  },
                  "geometry": {
                    "x": -75.5700,
                    "y": 6.2500
                  }
                }
              ]
            }
            """);
        
        // Read stops
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.read(jsonFile);
        
        // Verify
        assertEquals(1, stops.size());
        assertEquals("", stops.get(0).getAddress());
    }

    @Test
    void testSkipStopWithMissingId(@TempDir Path tempDir) throws IOException {
        // Test that stops without ID are skipped
        Path jsonFile = tempDir.resolve("stops.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id_paradero": 501,
                    "nombre_paradero": "Valid Stop"
                  },
                  "geometry": { "x": -75.57, "y": 6.25 }
                },
                {
                  "attributes": {
                    "nombre_paradero": "Missing ID Stop"
                  },
                  "geometry": { "x": -75.58, "y": 6.26 }
                },
                {
                  "attributes": {
                    "id_paradero": 502,
                    "nombre_paradero": "Another Valid Stop"
                  },
                  "geometry": { "x": -75.59, "y": 6.27 }
                }
              ]
            }
            """);
        
        // Read stops - should skip malformed one
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.read(jsonFile);
        
        // Should have only 2 valid stops
        assertEquals(2, stops.size());
        assertEquals(501, stops.get(0).getId());
        assertEquals(502, stops.get(1).getId());
    }

    @Test
    void testReadPaginatedStopFiles(@TempDir Path tempDir) throws IOException {
        // Create multiple paginated stop files
        Files.writeString(tempDir.resolve("stops_1.json"), """
            {
              "features": [
                {
                  "attributes": { "id_paradero": 1, "nombre_paradero": "Stop 1" },
                  "geometry": { "x": -75.57, "y": 6.25 }
                },
                {
                  "attributes": { "id_paradero": 2, "nombre_paradero": "Stop 2" },
                  "geometry": { "x": -75.58, "y": 6.26 }
                }
              ]
            }
            """);
        
        Files.writeString(tempDir.resolve("stops_2.json"), """
            {
              "features": [
                {
                  "attributes": { "id_paradero": 3, "nombre_paradero": "Stop 3" },
                  "geometry": { "x": -75.59, "y": 6.27 }
                }
              ]
            }
            """);
        
        // Read paginated stops
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> stops = reader.readPaginated(tempDir, "stops", ".json");
        
        // Should have all stops from all pages
        assertEquals(3, stops.size());
        assertEquals(1, stops.get(0).getId());
        assertEquals(2, stops.get(1).getId());
        assertEquals(3, stops.get(2).getId());
    }
}
