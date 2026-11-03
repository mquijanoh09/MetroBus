package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RouteTest {

    @Test
    void testValidBusRoute() {
        Route route = new Route("C101", "Circular 101", RouteType.BUS, "#FF5733");
        
        assertEquals("C101", route.getId());
        assertEquals("Circular 101", route.getDisplayName());
        assertEquals(RouteType.BUS, route.getType());
        assertEquals("#FF5733", route.getColor());
    }

    @Test
    void testValidMetroRoute() {
        Route route = new Route("LA", "Línea A", RouteType.METRO, "#0066CC");
        
        assertEquals("LA", route.getId());
        assertEquals("Línea A", route.getDisplayName());
        assertEquals(RouteType.METRO, route.getType());
        assertEquals("#0066CC", route.getColor());
    }

    @Test
    void testNullIdThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Route(null, "Test Route", RouteType.BUS, "#FFFFFF");
        });
        assertTrue(exception.getMessage().contains("id cannot be null"));
    }

    @Test
    void testNullDisplayNameThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Route("R1", null, RouteType.BUS, "#FFFFFF");
        });
        assertTrue(exception.getMessage().contains("displayName cannot be null"));
    }

    @Test
    void testNullTypeThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Route("R1", "Test Route", null, "#FFFFFF");
        });
        assertTrue(exception.getMessage().contains("type cannot be null"));
    }

    @Test
    void testNullColorThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Route("R1", "Test Route", RouteType.BUS, null);
        });
        assertTrue(exception.getMessage().contains("color cannot be null"));
    }

    @Test
    void testEquality() {
        Route route1 = new Route("C101", "Circular 101", RouteType.BUS, "#FF5733");
        Route route2 = new Route("C101", "Circular 101", RouteType.BUS, "#FF5733");
        Route route3 = new Route("C102", "Circular 102", RouteType.BUS, "#FF5733");
        
        assertEquals(route1, route2);
        assertNotEquals(route1, route3);
    }

    @Test
    void testHashCode() {
        Route route1 = new Route("C101", "Circular 101", RouteType.BUS, "#FF5733");
        Route route2 = new Route("C101", "Circular 101", RouteType.BUS, "#FF5733");
        
        assertEquals(route1.hashCode(), route2.hashCode());
    }

    @Test
    void testToString() {
        Route route = new Route("C101", "Circular 101", RouteType.BUS, "#FF5733");
        String str = route.toString();
        
        assertTrue(str.contains("C101"));
        assertTrue(str.contains("Circular 101"));
        assertTrue(str.contains("BUS"));
        assertTrue(str.contains("#FF5733"));
    }

    @Test
    void testImmutability() {
        Route route = new Route("C101", "Circular 101", RouteType.BUS, "#FF5733");
        
        // Verify getters return the same values
        assertEquals("C101", route.getId());
        assertEquals("Circular 101", route.getDisplayName());
        assertEquals(RouteType.BUS, route.getType());
        assertEquals("#FF5733", route.getColor());
    }
}
