package co.medellin.transit.data;

import co.medellin.transit.model.Route;
import co.medellin.transit.model.RouteType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RouteCsvReader and RouteArcGisReader.
 */
class RouteReadersTest {

    // ========== RouteCsvReader Tests ==========

    @Test
    void testReadValidRoutesCsv(@TempDir Path tempDir) throws IOException {
        // Create test CSV file with route data
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            id_ruta,nombre_ruta,tipo_ruta,color
            301,Ruta 301,BUS,#FF0000
            A,Línea A,METRO,#00AA00
            302,Ruta 302,BUS,#0066CC
            """);
        
        // Read routes
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Verify results
        assertEquals(3, routes.size());
        
        Route route1 = routes.get(0);
        assertEquals("301", route1.getId());
        assertEquals("Ruta 301", route1.getDisplayName());
        assertEquals(RouteType.BUS, route1.getType());
        assertEquals("#FF0000", route1.getColor());
        
        Route route2 = routes.get(1);
        assertEquals("A", route2.getId());
        assertEquals("Línea A", route2.getDisplayName());
        assertEquals(RouteType.METRO, route2.getType());
        assertEquals("#00AA00", route2.getColor());
    }

    @Test
    void testReadRoutesWithAlternativeFieldNames(@TempDir Path tempDir) throws IOException {
        // Test with alternative field names
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            route_id,name,type
            401,Express Route,B
            M1,Metro Line 1,M
            """);
        
        // Read routes
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Verify
        assertEquals(2, routes.size());
        assertEquals("401", routes.get(0).getId());
        assertEquals(RouteType.BUS, routes.get(0).getType());
        assertEquals("M1", routes.get(1).getId());
        assertEquals(RouteType.METRO, routes.get(1).getType());
    }

    @Test
    void testReadRoutesWithDefaultValues(@TempDir Path tempDir) throws IOException {
        // Test routes with missing optional fields
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            id_ruta,tipo_ruta
            501,BUS
            502,METRO
            """);
        
        // Read routes
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Verify defaults applied
        assertEquals(2, routes.size());
        assertEquals("Ruta 501", routes.get(0).getDisplayName());  // Default name
        assertEquals("#0066CC", routes.get(0).getColor());  // Default bus color
        assertEquals("Ruta 502", routes.get(1).getDisplayName());
        assertEquals("#00AA00", routes.get(1).getColor());  // Default metro color
    }

    @Test
    void testReadRoutesWithMissingColor(@TempDir Path tempDir) throws IOException {
        // Test routes without color field
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            id_ruta,nombre_ruta,tipo_ruta
            601,Bus 601,BUS
            602,Metro 602,METRO
            """);
        
        // Read routes
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Verify default colors applied
        assertEquals(2, routes.size());
        assertEquals("#0066CC", routes.get(0).getColor());
        assertEquals("#00AA00", routes.get(1).getColor());
    }

    @Test
    void testReadRoutesWithColorWithoutHash(@TempDir Path tempDir) throws IOException {
        // Test routes with color codes without # prefix
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            id_ruta,nombre_ruta,tipo_ruta,color
            701,Route 701,BUS,FF5500
            """);
        
        // Read routes
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Verify # prefix added
        assertEquals(1, routes.size());
        assertEquals("#FF5500", routes.get(0).getColor());
    }

    @Test
    void testSkipRouteWithMissingId(@TempDir Path tempDir) throws IOException {
        // Test that routes without ID are skipped
        Path csvFile = tempDir.resolve("routes.csv");
        Files.writeString(csvFile, """
            id_ruta,nombre_ruta,tipo_ruta
            801,Valid Route 1,BUS
            ,Missing ID Route,BUS
            802,Valid Route 2,METRO
            """);
        
        // Read routes - should skip malformed one
        RouteCsvReader reader = new RouteCsvReader();
        List<Route> routes = reader.read(csvFile);
        
        // Should have only 2 valid routes
        assertEquals(2, routes.size());
        assertEquals("801", routes.get(0).getId());
        assertEquals("802", routes.get(1).getId());
    }

    // ========== RouteArcGisReader Tests ==========

    @Test
    void testReadValidRoutesArcGis(@TempDir Path tempDir) throws IOException {
        // Create test ArcGIS JSON file with route data
        Path jsonFile = tempDir.resolve("routes.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id_ruta": "301",
                    "nombre_ruta": "Ruta 301",
                    "tipo_ruta": "BUS",
                    "color": "FF0000"
                  }
                },
                {
                  "attributes": {
                    "id_ruta": "A",
                    "nombre_ruta": "Línea A",
                    "tipo_ruta": "METRO",
                    "color": "00AA00"
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
        assertEquals("301", route1.getId());
        assertEquals("Ruta 301", route1.getDisplayName());
        assertEquals(RouteType.BUS, route1.getType());
        assertEquals("#FF0000", route1.getColor());
        
        Route route2 = routes.get(1);
        assertEquals("A", route2.getId());
        assertEquals("Línea A", route2.getDisplayName());
        assertEquals(RouteType.METRO, route2.getType());
        assertEquals("#00AA00", route2.getColor());
    }

    @Test
    void testReadRoutesArcGisWithObjectId(@TempDir Path tempDir) throws IOException {
        // Test with OBJECTID instead of id_ruta
        Path jsonFile = tempDir.resolve("routes.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "OBJECTID": 401,
                    "nombre": "Express Route",
                    "tipo": "BUS"
                  }
                }
              ]
            }
            """);
        
        // Read routes
        RouteArcGisReader reader = new RouteArcGisReader();
        List<Route> routes = reader.read(jsonFile);
        
        // Verify
        assertEquals(1, routes.size());
        assertEquals("401", routes.get(0).getId());  // Converted from int to string
        assertEquals("Express Route", routes.get(0).getDisplayName());
        assertEquals(RouteType.BUS, routes.get(0).getType());
    }

    @Test
    void testReadRoutesArcGisWithDefaultValues(@TempDir Path tempDir) throws IOException {
        // Test routes with missing optional fields
        Path jsonFile = tempDir.resolve("routes.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id_ruta": "501"
                  }
                },
                {
                  "attributes": {
                    "id_ruta": "502",
                    "tipo_ruta": "METRO"
                  }
                }
              ]
            }
            """);
        
        // Read routes
        RouteArcGisReader reader = new RouteArcGisReader();
        List<Route> routes = reader.read(jsonFile);
        
        // Verify defaults applied
        assertEquals(2, routes.size());
        assertEquals("Ruta 501", routes.get(0).getDisplayName());  // Default name
        assertEquals(RouteType.BUS, routes.get(0).getType());  // Default type
        assertEquals("#0066CC", routes.get(0).getColor());  // Default bus color
        
        assertEquals("Ruta 502", routes.get(1).getDisplayName());
        assertEquals(RouteType.METRO, routes.get(1).getType());
        assertEquals("#00AA00", routes.get(1).getColor());  // Default metro color
    }

    @Test
    void testSkipRouteArcGisWithMissingId(@TempDir Path tempDir) throws IOException {
        // Test that routes without ID are skipped
        Path jsonFile = tempDir.resolve("routes.json");
        Files.writeString(jsonFile, """
            {
              "features": [
                {
                  "attributes": {
                    "id_ruta": "601",
                    "nombre_ruta": "Valid Route"
                  }
                },
                {
                  "attributes": {
                    "nombre_ruta": "Missing ID Route"
                  }
                },
                {
                  "attributes": {
                    "id_ruta": "602",
                    "nombre_ruta": "Another Valid"
                  }
                }
              ]
            }
            """);
        
        // Read routes - should skip malformed one
        RouteArcGisReader reader = new RouteArcGisReader();
        List<Route> routes = reader.read(jsonFile);
        
        // Should have only 2 valid routes
        assertEquals(2, routes.size());
        assertEquals("601", routes.get(0).getId());
        assertEquals("602", routes.get(1).getId());
    }
}
