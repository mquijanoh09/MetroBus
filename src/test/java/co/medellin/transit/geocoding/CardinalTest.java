package co.medellin.transit.geocoding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Cardinal enum.
 */
class CardinalTest {
    
    @Test
    void testFromStringNorte() {
        assertEquals(Cardinal.NORTE, Cardinal.fromString("NORTE"));
        assertEquals(Cardinal.NORTE, Cardinal.fromString("norte"));
        assertEquals(Cardinal.NORTE, Cardinal.fromString("Norte"));
    }
    
    @Test
    void testFromStringSur() {
        assertEquals(Cardinal.SUR, Cardinal.fromString("SUR"));
        assertEquals(Cardinal.SUR, Cardinal.fromString("sur"));
        assertEquals(Cardinal.SUR, Cardinal.fromString("Sur"));
    }
    
    @Test
    void testFromStringEste() {
        assertEquals(Cardinal.ESTE, Cardinal.fromString("ESTE"));
        assertEquals(Cardinal.ESTE, Cardinal.fromString("este"));
        assertEquals(Cardinal.ESTE, Cardinal.fromString("Este"));
    }
    
    @Test
    void testFromStringOeste() {
        assertEquals(Cardinal.OESTE, Cardinal.fromString("OESTE"));
        assertEquals(Cardinal.OESTE, Cardinal.fromString("oeste"));
        assertEquals(Cardinal.OESTE, Cardinal.fromString("Oeste"));
    }
    
    @Test
    void testFromStringInvalid() {
        assertNull(Cardinal.fromString("INVALID"));
        assertNull(Cardinal.fromString("SOUTH"));
        assertNull(Cardinal.fromString(""));
    }
    
    @Test
    void testFromStringNull() {
        assertNull(Cardinal.fromString(null));
    }
    
    @Test
    void testFromStringWithWhitespace() {
        assertEquals(Cardinal.SUR, Cardinal.fromString("  SUR  "));
        assertEquals(Cardinal.NORTE, Cardinal.fromString("  NORTE  "));
    }
}
