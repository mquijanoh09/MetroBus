package co.medellin.transit.data;

import co.medellin.transit.model.Route;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RouteCsvReader and RouteArcGisReader.
 * Updated to match new Route model with int id, codigo, nombre, tipo fields.
 */
class RouteReadersTest {

    // ========== RouteCsvReader Tests ==========

    @Test
    void testReadValidRoutesCsv(@TempDir Path tempDir) throws IOException {
        // Create test CSV file with route data
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            id_ruta,codigo,nombre,tipo
            90111,263,12 de Octubre - Calle 90,Directa Centro
            90098,260,Torres,Integrada
            90200,265,Circular,Directa Centro
            """);
        
        // Read routes
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Verify results
        assertEquals(3, routes.size());
        
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
    void testReadRoutesWithAlternativeFieldNames(@TempDir Path tempDir) throws IOException {
        // Test with alternative field names
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            route_id,codigo,name,type
            90401,401,Express Route,Directa Centro
            90402,M1,Metro Line 1,Integrada
            """);
        
        // Read routes
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Verify
        assertEquals(2, routes.size());
        assertEquals(90401, routes.get(0).getId());
        assertEquals("401", routes.get(0).getCodigo());
        assertEquals(90402, routes.get(1).getId());
        assertEquals("M1", routes.get(1).getCodigo());
    }

    @Test
    void testReadRoutesWithDefaultValues(@TempDir Path tempDir) throws IOException {
        // Test routes with missing optional fields (codigo and nombre default to id-based values)
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            id_ruta,tipo
            90501,Directa Centro
            90502,Integrada
            """);
        
        // Read routes
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Verify defaults applied
        assertEquals(2, routes.size());
        assertEquals("90501", routes.get(0).getCodigo());  // Default codigo = id as string
        assertEquals("Ruta 90501", routes.get(0).getNombre());  // Default nombre
        assertEquals("90502", routes.get(1).getCodigo());
        assertEquals("Ruta 90502", routes.get(1).getNombre());
    }

    @Test
    void testSkipRouteWithMissingId(@TempDir Path tempDir) throws IOException {
        // Test that routes without ID are skipped
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            id_ruta,codigo,nombre,tipo
            90801,801,Valid Route 1,Directa Centro
            ,999,Missing ID Route,Integrada
            90802,802,Valid Route 2,Directa Centro
            """);
        
        // Read routes - should skip malformed one
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Should have only 2 valid routes
        assertEquals(2, routes.size());
        assertEquals(90801, routes.get(0).getId());
        assertEquals(90802, routes.get(1).getId());
    }

    // ========== RouteArcGisReader Tests ==========
    // Note: These were previously in this file but moved to RouteArcGisReaderTest.java
    // The old tests tested color and RouteType enum which no longer exist in the Route model.
    // The new Route model has: int id, String codigo, String nombre, String tipo
}
