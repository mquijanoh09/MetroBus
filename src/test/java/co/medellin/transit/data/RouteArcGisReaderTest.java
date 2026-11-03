package co.medellin.transit.data;

import co.medellin.transit.model.Route;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RouteArcGisReader.
 */
class RouteArcGisReaderTest {

    @Test
    void testReadRealFile() throws IOException {
        // Load real routes file
        Path jsonFile = Paths.get("data/raw/routes.json");
        
        // Read routes
        RouteArcGisReader reader = new RouteArcGisReader();
        List<Route> routes = reader.read(jsonFile);
        
        // Verify count
        assertEquals(252, routes.size(), 
            "Expected 252 routes from real file");
        
        // Spot-check first route
        Route first = routes.get(0);
        assertTrue(first.getId() > 0, "Route ID should be positive");
        assertNotNull(first.getCodigo());
        assertFalse(first.getCodigo().isEmpty());
        assertNotNull(first.getNombre());
        assertFalse(first.getNombre().isEmpty());
        assertNotNull(first.getTipo());
        assertFalse(first.getTipo().isEmpty());
    }

    @Test
    void testFieldExtraction(@TempDir Path tempDir) throws IOException {
        // Create test ArcGIS JSON with route data matching real format
        Path jsonFile = tempDir.resolve("routes.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "objectid": 1,
                    "id_ruta": "90111",
                    "codigo": "263",
                    "nombre": "12 de Octubre - Calle 90",
                    "tipo": "Directa Centro"
                  }
                },
                {
                  "attributes": {
                    "objectid": 2,
                    "id_ruta": "90098",
                    "codigo": "260",
                    "nombre": "Torres",
                    "tipo": "Integrada"
                  }
                }
              ]
            }
            """);
        
        // Read routes
        RouteArcGisReader reader = new RouteArcGisReader();
        List<Route> routes = reader.read(jsonFile);
        
        // Verify results
        assertEquals(2, routes.size());
        
        Route route1 = routes.get(0);
        assertEquals(90111, route1.getId());
        assertEquals("263", route1.getCodigo());
        assertEquals("12 de Octubre - Calle 90", route1.getNombre());
        assertEquals("Directa Centro", route1.getTipo());
        
        Route route2 = routes.get(1);
        assertEquals(90098, route2.getId());
        assertEquals("260", route2.getCodigo());
        assertEquals("Torres", route2.getNombre());
        assertEquals("Integrada", route2.getTipo());
    }

    @Test
    void testMalformedFeature(@TempDir Path tempDir) throws IOException {
        // Test that routes with missing required fields are skipped
        Path jsonFile = tempDir.resolve("routes.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id_ruta": "90111",
                    "codigo": "263",
                    "nombre": "Valid Route",
                    "tipo": "Directa Centro"
                  }
                },
                {
                  "attributes": {
                    "codigo": "999",
                    "nombre": "Missing ID",
                    "tipo": "Directa Centro"
                  }
                },
                {
                  "attributes": {
                    "id_ruta": "90098",
                    "nombre": "Missing Codigo",
                    "tipo": "Integrada"
                  }
                },
                {
                  "attributes": {
                    "id_ruta": "90099",
                    "codigo": "260",
                    "tipo": "Missing Nombre"
                  }
                },
                {
                  "attributes": {
                    "id_ruta": "90100",
                    "codigo": "261",
                    "nombre": "Missing Tipo"
                  }
                },
                {
                  "attributes": {
                    "id_ruta": "90200",
                    "codigo": "265",
                    "nombre": "Another Valid",
                    "tipo": "Directa Centro"
                  }
                }
              ]
            }
            """);
        
        // Read routes - should skip malformed ones
        RouteArcGisReader reader = new RouteArcGisReader();
        List<Route> routes = reader.read(jsonFile);
        
        // Should have only 2 valid routes
        assertEquals(2, routes.size());
        assertEquals(90111, routes.get(0).getId());
        assertEquals(90200, routes.get(1).getId());
    }
}
