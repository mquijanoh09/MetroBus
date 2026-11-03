package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaceTest {

    @Test
    void testValidPlace() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Parque Berrio", coord);
        
        assertEquals("Parque Berrio", place.getName());
        assertEquals(coord, place.getCoord());
    }

    @Test
    void testNullNameThrowsException() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Place(null, coord);
        });
        assertTrue(exception.getMessage().contains("name cannot be null"));
    }

    @Test
    void testNullCoordinateThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Place("Test Place", null);
        });
        assertTrue(exception.getMessage().contains("coord cannot be null"));
    }

    @Test
    void testEquality() {
        Coordinate coord1 = new Coordinate(6.2442, -75.5812);
        Coordinate coord2 = new Coordinate(6.2442, -75.5812);
        Coordinate coord3 = new Coordinate(6.2443, -75.5812);
        
        Place place1 = new Place("Parque Berrio", coord1);
        Place place2 = new Place("Parque Berrio", coord2);
        Place place3 = new Place("Different Name", coord1);
        Place place4 = new Place("Parque Berrio", coord3);
        
        assertEquals(place1, place2);
        assertNotEquals(place1, place3); // different name
        assertNotEquals(place1, place4); // different coordinate
    }

    @Test
    void testHashCode() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place1 = new Place("Parque Berrio", coord);
        Place place2 = new Place("Parque Berrio", coord);
        
        assertEquals(place1.hashCode(), place2.hashCode());
    }

    @Test
    void testToString() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Parque Berrio", coord);
        String str = place.toString();
        
        assertTrue(str.contains("Parque Berrio"));
        assertTrue(str.contains("Coordinate"));
    }

    @Test
    void testImmutability() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Parque Berrio", coord);
        
        // Verify getters return the same values
        assertEquals("Parque Berrio", place.getName());
        assertEquals(coord, place.getCoord());
    }
}
