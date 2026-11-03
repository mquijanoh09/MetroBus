package co.medellin.transit.data;

import co.medellin.transit.model.Coordinate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ArcGisReader base class.
 */
class ArcGisReaderTest {
    
    /**
     * Simple test record class
     */
    static class TestFeature {
        final int id;
        final String name;
        final Coordinate coord;
        
        TestFeature(int id, String name, Coordinate coord) {
            this.id = id;
            this.name = name;
            this.coord = coord;
        }
    }
    
    /**
     * Test implementation of ArcGisReader
     */
    static class TestArcGisReader extends ArcGisReader<TestFeature> {
        @Override
        protected Optional<TestFeature> parseFeature(String featureJson, int featureNumber) {
            String attributes = extractAttributes(featureJson);
            
            int id = getIntField(attributes, "id");
            Optional<String> name = getStringField(attributes, "name");
            
            Optional<String> geometryJson = extractGeometry(featureJson);
            Coordinate coord = geometryJson
                .map(this::parseGeometryCoordinate)
                .orElse(new Coordinate(0, 0));
            
            return Optional.of(new TestFeature(id, name.orElse(""), coord));
        }
    }
    
    @Test
    void testReadValidArcGisJson(@TempDir Path tempDir) throws IOException {
        // Create test ArcGIS JSON file
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id": 1,
                    "name": "Stop A"
                  },
                  "geometry": {
                    "x": -75.5,
                    "y": 6.25
                  }
                },
                {
                  "attributes": {
                    "id": 2,
                    "name": "Stop B"
                  },
                  "geometry": {
                    "x": -75.6,
                    "y": 6.30
                  }
                }
              ]
            }
            """);
        
        // Read JSON
        TestArcGisReader reader = new TestArcGisReader();
        List<TestFeature> features = reader.read(jsonFile);
        
        // Verify results
        assertEquals(2, features.size());
        
        assertEquals(1, features.get(0).id);
        assertEquals("Stop A", features.get(0).name);
        assertEquals(6.25, features.get(0).coord.getLat(), 0.001);
        assertEquals(-75.5, features.get(0).coord.getLon(), 0.001);
        
        assertEquals(2, features.get(1).id);
        assertEquals("Stop B", features.get(1).name);
        assertEquals(6.30, features.get(1).coord.getLat(), 0.001);
        assertEquals(-75.6, features.get(1).coord.getLon(), 0.001);
    }
    
    @Test
    void testSkipMalformedFeatures(@TempDir Path tempDir) throws IOException {
        // Create JSON with malformed features
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id": 1,
                    "name": "Valid"
                  },
                  "geometry": {
                    "x": -75.5,
                    "y": 6.25
                  }
                },
                {
                  "attributes": {
                    "name": "Missing ID"
                  },
                  "geometry": {
                    "x": -75.6,
                    "y": 6.30
                  }
                },
                {
                  "attributes": {
                    "id": 3,
                    "name": "Also Valid"
                  },
                  "geometry": {
                    "x": -75.55,
                    "y": 6.28
                  }
                }
              ]
            }
            """);
        
        // Read JSON - should skip malformed feature
        TestArcGisReader reader = new TestArcGisReader();
        List<TestFeature> features = reader.read(jsonFile);
        
        // Should have only 2 valid features
        assertEquals(2, features.size());
        assertEquals(1, features.get(0).id);
        assertEquals(3, features.get(1).id);
    }
    
    @Test
    void testEmptyFeaturesArray(@TempDir Path tempDir) throws IOException {
        // Create JSON with empty features array
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, """
            {
              "features": []
            }
            """);
        
        // Read JSON
        TestArcGisReader reader = new TestArcGisReader();
        List<TestFeature> features = reader.read(jsonFile);
        
        // Should have no features
        assertEquals(0, features.size());
    }
    
    @Test
    void testReadPaginatedFiles(@TempDir Path tempDir) throws IOException {
        // Create multiple paginated files
        Files.writeString(tempDir.resolve("stops_1.json"), """
            {
              "features": [
                {
                  "attributes": {
                    "id": 1,
                    "name": "Stop 1"
                  },
                  "geometry": { "x": -75.5, "y": 6.25 }
                }
              ]
            }
            """);
        
        Files.writeString(tempDir.resolve("stops_2.json"), """
            {
              "features": [
                {
                  "attributes": {
                    "id": 2,
                    "name": "Stop 2"
                  },
                  "geometry": { "x": -75.6, "y": 6.30 }
                }
              ]
            }
            """);
        
        Files.writeString(tempDir.resolve("stops_3.json"), """
            {
              "features": [
                {
                  "attributes": {
                    "id": 3,
                    "name": "Stop 3"
                  },
                  "geometry": { "x": -75.55, "y": 6.28 }
                }
              ]
            }
            """);
        
        // Read paginated files
        TestArcGisReader reader = new TestArcGisReader();
        List<TestFeature> features = reader.readPaginated(tempDir, "stops", ".json");
        
        // Should have all features from all pages
        assertEquals(3, features.size());
        assertEquals(1, features.get(0).id);
        assertEquals(2, features.get(1).id);
        assertEquals(3, features.get(2).id);
    }
    
    @Test
    void testGeometryWithLongitudeLatitude(@TempDir Path tempDir) throws IOException {
        // Create JSON using longitude/latitude format instead of x/y
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id": 1,
                    "name": "Stop"
                  },
                  "geometry": {
                    "longitude": -75.5,
                    "latitude": 6.25
                  }
                }
              ]
            }
            """);
        
        // Read JSON
        TestArcGisReader reader = new TestArcGisReader();
        List<TestFeature> features = reader.read(jsonFile);
        
        // Verify coordinate parsed correctly
        assertEquals(1, features.size());
        assertEquals(6.25, features.get(0).coord.getLat(), 0.001);
        assertEquals(-75.5, features.get(0).coord.getLon(), 0.001);
    }
    
    @Test
    void testNoFeaturesField(@TempDir Path tempDir) throws IOException {
        // Create JSON without features field
        Path jsonFile = tempDir.resolve("test.json");
        Files.writeString(jsonFile, """
            {
              "some_other_field": []
            }
            """);
        
        // Read JSON - should return empty list
        TestArcGisReader reader = new TestArcGisReader();
        List<TestFeature> features = reader.read(jsonFile);
        
        assertEquals(0, features.size());
    }
}
