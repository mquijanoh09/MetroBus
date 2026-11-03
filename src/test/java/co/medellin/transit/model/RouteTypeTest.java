package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RouteTypeTest {

    @Test
    void testEnumValues() {
        RouteType[] types = RouteType.values();
        assertEquals(2, types.length);
        assertEquals(RouteType.BUS, types[0]);
        assertEquals(RouteType.METRO, types[1]);
    }

    @Test
    void testEnumValueOf() {
        assertEquals(RouteType.BUS, RouteType.valueOf("BUS"));
        assertEquals(RouteType.METRO, RouteType.valueOf("METRO"));
    }

    @Test
    void testEnumEquality() {
        RouteType bus1 = RouteType.BUS;
        RouteType bus2 = RouteType.BUS;
        RouteType metro = RouteType.METRO;
        
        assertEquals(bus1, bus2);
        assertNotEquals(bus1, metro);
    }

    @Test
    void testEnumToString() {
        assertEquals("BUS", RouteType.BUS.toString());
        assertEquals("METRO", RouteType.METRO.toString());
    }
}
