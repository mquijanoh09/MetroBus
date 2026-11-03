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
 * Unit tests for StationArcGisReader.
 */
class StationArcGisReaderTest {

    @Test
    void testReadRealFile() throws IOException {
        // Load real transit stations file
        Path jsonFile = Paths.get("data/raw/transit_stations.json");
        
        // Read stations
        StationArcGisReader reader = new StationArcGisReader();
        List<Station> stations = reader.read(jsonFile);
        
        // Verify count
        assertEquals(81, stations.size(), 
            "Expected 81 transit stations from real file");
        
        // Spot-check first station
        Station first = stations.get(0);
        assertNotNull(first.name());
        assertFalse(first.name().isEmpty());
        assertNotNull(first.line());
        assertFalse(first.line().isEmpty());
        assertNotNull(first.coordinate());
        
        // Verify coordinates are in Medellín bounds
        assertTrue(first.coordinate().getLat() >= 6.15 && first.coordinate().getLat() <= 6.35);
        assertTrue(first.coordinate().getLon() >= -75.65 && first.coordinate().getLon() <= -75.50);
    }

    @Test
    void testGeometryParsing(@TempDir Path tempDir) throws IOException {
        // Create test ArcGIS JSON with station data
        Path jsonFile = tempDir.resolve("stations.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "nombre": "Niquía",
                    "linea": "Línea A"
                  },
                  "geometry": {
                    "x": -75.544312,
                    "y": 6.337817
                  }
                },
                {
                  "attributes": {
                    "nombre": "Aguacatala",
                    "linea": "Línea A"
                  },
                  "geometry": {
                    "x": -75.581625,
                    "y": 6.194430
                  }
                }
              ]
            }
            """);
        
        // Read stations
        StationArcGisReader reader = new StationArcGisReader();
        List<Station> stations = reader.read(jsonFile);
        
        // Verify results
        assertEquals(2, stations.size());
        
        Station station1 = stations.get(0);
        assertEquals("Niquía", station1.name());
        assertEquals("Línea A", station1.line());
        assertEquals(6.337817, station1.coordinate().getLat(), 0.0001);
        assertEquals(-75.544312, station1.coordinate().getLon(), 0.0001);
        
        Station station2 = stations.get(1);
        assertEquals("Aguacatala", station2.name());
        assertEquals("Línea A", station2.line());
        assertEquals(6.194430, station2.coordinate().getLat(), 0.0001);
        assertEquals(-75.581625, station2.coordinate().getLon(), 0.0001);
    }

    @Test
    void testMalformedFeature(@TempDir Path tempDir) throws IOException {
        // Test that stations with missing required fields are skipped
        Path jsonFile = tempDir.resolve("stations.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "nombre": "Valid Station",
                    "linea": "Línea A"
                  },
                  "geometry": { "x": -75.57, "y": 6.25 }
                },
                {
                  "attributes": {
                    "linea": "Línea B"
                  },
                  "geometry": { "x": -75.58, "y": 6.26 }
                },
                {
                  "attributes": {
                    "nombre": "Missing Line"
                  },
                  "geometry": { "x": -75.59, "y": 6.27 }
                },
                {
                  "attributes": {
                    "nombre": "Missing Geometry",
                    "linea": "Línea C"
                  }
                },
                {
                  "attributes": {
                    "nombre": "Another Valid",
                    "linea": "Línea D"
                  },
                  "geometry": { "x": -75.60, "y": 6.28 }
                }
              ]
            }
            """);
        
        // Read stations - should skip malformed ones
        StationArcGisReader reader = new StationArcGisReader();
        List<Station> stations = reader.read(jsonFile);
        
        // Should have only 2 valid stations
        assertEquals(2, stations.size());
        assertEquals("Valid Station", stations.get(0).name());
        assertEquals("Another Valid", stations.get(1).name());
    }
}
