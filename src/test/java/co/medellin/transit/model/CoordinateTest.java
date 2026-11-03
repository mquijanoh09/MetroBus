package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordinateTest {

    @Test
    void testValidCoordinate() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        assertEquals(6.2442, coord.getLat(), 0.0001);
        assertEquals(-75.5812, coord.getLon(), 0.0001);
    }

    @Test
    void testBoundaryLatitudes() {
        // Test valid boundary values
        Coordinate north = new Coordinate(90.0, 0.0);
        assertEquals(90.0, north.getLat());
        
        Coordinate south = new Coordinate(-90.0, 0.0);
        assertEquals(-90.0, south.getLat());
    }

    @Test
    void testBoundaryLongitudes() {
        // Test valid boundary values
        Coordinate east = new Coordinate(0.0, 180.0);
        assertEquals(180.0, east.getLon());
        
        Coordinate west = new Coordinate(0.0, -180.0);
        assertEquals(-180.0, west.getLon());
    }

    @Test
    void testInvalidLatitudeTooHigh() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Coordinate(90.1, 0.0);
        });
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90"));
    }

    @Test
    void testInvalidLatitudeTooLow() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Coordinate(-90.1, 0.0);
        });
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90"));
    }

    @Test
    void testInvalidLongitudeTooHigh() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Coordinate(0.0, 180.1);
        });
        assertTrue(exception.getMessage().contains("Longitude must be between -180 and 180"));
    }

    @Test
    void testInvalidLongitudeTooLow() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Coordinate(0.0, -180.1);
        });
        assertTrue(exception.getMessage().contains("Longitude must be between -180 and 180"));
    }

    @Test
    void testEquality() {
        Coordinate coord1 = new Coordinate(6.2442, -75.5812);
        Coordinate coord2 = new Coordinate(6.2442, -75.5812);
        Coordinate coord3 = new Coordinate(6.2443, -75.5812);
        
        assertEquals(coord1, coord2);
        assertNotEquals(coord1, coord3);
        assertEquals(coord1, coord1); // reflexive
    }

    @Test
    void testHashCode() {
        Coordinate coord1 = new Coordinate(6.2442, -75.5812);
        Coordinate coord2 = new Coordinate(6.2442, -75.5812);
        
        assertEquals(coord1.hashCode(), coord2.hashCode());
    }

    @Test
    void testToString() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        String str = coord.toString();
        
        assertTrue(str.contains("6.2442"));
        assertTrue(str.contains("-75.5812"));
    }

    @Test
    void testImmutability() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        double originalLat = coord.getLat();
        double originalLon = coord.getLon();
        
        // Verify getters return the same values
        assertEquals(originalLat, coord.getLat());
        assertEquals(originalLon, coord.getLon());
    }
}
