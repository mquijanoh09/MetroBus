package co.medellin.transit.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteTest {

    @Test
    void testRouteCreation() {
        Route route = new Route(90111, "263", "12 de Octubre", "Directa Centro");
        
        assertEquals(90111, route.getId());
        assertEquals("263", route.getCodigo());
        assertEquals("12 de Octubre", route.getNombre());
        assertEquals("Directa Centro", route.getTipo());
    }

    @Test
    void testRouteWithDifferentFields() {
        Route route = new Route(90098, "260", "Torres", "Integrada");
        
        assertEquals(90098, route.getId());
        assertEquals("260", route.getCodigo());
        assertEquals("Torres", route.getNombre());
        assertEquals("Integrada", route.getTipo());
    }

    @Test
    void testNullCodigoThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new Route(90111, null, "Test", "Directa Centro")
        );
    }

    @Test
    void testNullNombreThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new Route(90111, "263", null, "Directa Centro")
        );
    }

    @Test
    void testNullTipoThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new Route(90111, "263", "Test", null)
        );
    }

    @Test
    void testRouteEquality() {
        Route route1 = new Route(90111, "263", "12 de Octubre", "Directa Centro");
        Route route2 = new Route(90111, "263", "12 de Octubre", "Directa Centro");
        Route route3 = new Route(90098, "260", "Torres", "Integrada");
        
        assertEquals(route1, route2);
        assertNotEquals(route1, route3);
    }

    @Test
    void testRouteHashCode() {
        Route route1 = new Route(90111, "263", "12 de Octubre", "Directa Centro");
        Route route2 = new Route(90111, "263", "12 de Octubre", "Directa Centro");
        
        assertEquals(route1.hashCode(), route2.hashCode());
    }

    @Test
    void testRouteToString() {
        Route route = new Route(90111, "263", "12 de Octubre", "Directa Centro");
        String str = route.toString();
        
        assertTrue(str.contains("90111"));
        assertTrue(str.contains("263"));
        assertTrue(str.contains("12 de Octubre"));
        assertTrue(str.contains("Directa Centro"));
    }

    @Test
    void testRouteImmutability() {
        // Route is immutable - all fields are final
        // This test just verifies the constructor and getters work correctly
        Route route = new Route(90111, "263", "Test Route", "Directa Centro");
        
        assertEquals(90111, route.getId());
        assertEquals("263", route.getCodigo());
        assertEquals("Test Route", route.getNombre());
        assertEquals("Directa Centro", route.getTipo());
    }
}
