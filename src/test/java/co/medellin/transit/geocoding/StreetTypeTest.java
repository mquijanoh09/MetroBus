package co.medellin.transit.geocoding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StreetType enum.
 */
class StreetTypeTest {
    
    @Test
    void testFromStringCalle() {
        assertEquals(StreetType.CALLE, StreetType.fromString("CALLE"));
        assertEquals(StreetType.CALLE, StreetType.fromString("CL"));
        assertEquals(StreetType.CALLE, StreetType.fromString("calle"));
        assertEquals(StreetType.CALLE, StreetType.fromString("cl"));
    }
    
    @Test
    void testFromStringCarrera() {
        assertEquals(StreetType.CARRERA, StreetType.fromString("CARRERA"));
        assertEquals(StreetType.CARRERA, StreetType.fromString("CR"));
        assertEquals(StreetType.CARRERA, StreetType.fromString("KR"));
        assertEquals(StreetType.CARRERA, StreetType.fromString("CRA"));
        assertEquals(StreetType.CARRERA, StreetType.fromString("carrera"));
        assertEquals(StreetType.CARRERA, StreetType.fromString("kr"));
    }
    
    @Test
    void testFromStringDiagonal() {
        assertEquals(StreetType.DIAGONAL, StreetType.fromString("DIAGONAL"));
        assertEquals(StreetType.DIAGONAL, StreetType.fromString("DG"));
        assertEquals(StreetType.DIAGONAL, StreetType.fromString("diagonal"));
        assertEquals(StreetType.DIAGONAL, StreetType.fromString("dg"));
    }
    
    @Test
    void testFromStringTransversal() {
        assertEquals(StreetType.TRANSVERSAL, StreetType.fromString("TRANSVERSAL"));
        assertEquals(StreetType.TRANSVERSAL, StreetType.fromString("TV"));
        assertEquals(StreetType.TRANSVERSAL, StreetType.fromString("transversal"));
        assertEquals(StreetType.TRANSVERSAL, StreetType.fromString("tv"));
    }
    
    @Test
    void testFromStringCircular() {
        assertEquals(StreetType.CIRCULAR, StreetType.fromString("CIRCULAR"));
        assertEquals(StreetType.CIRCULAR, StreetType.fromString("circular"));
    }
    
    @Test
    void testFromStringInvalid() {
        assertNull(StreetType.fromString("AVENIDA"));
        assertNull(StreetType.fromString("INVALID"));
        assertNull(StreetType.fromString(""));
    }
    
    @Test
    void testFromStringNull() {
        assertNull(StreetType.fromString(null));
    }
    
    @Test
    void testFromStringWithWhitespace() {
        assertEquals(StreetType.CALLE, StreetType.fromString("  CALLE  "));
        assertEquals(StreetType.CARRERA, StreetType.fromString("  CR  "));
    }
}
