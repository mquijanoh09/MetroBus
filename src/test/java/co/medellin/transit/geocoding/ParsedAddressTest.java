package co.medellin.transit.geocoding;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParsedAddress class.
 */
class ParsedAddressTest {
    
    @Test
    void testConstructorAndGetters() {
        ParsedAddress addr = new ParsedAddress(
            StreetType.CALLE,
            50.0,
            53.0,
            Optional.of(48),
            Optional.of(Cardinal.SUR)
        );
        
        assertEquals(StreetType.CALLE, addr.getStreetType());
        assertEquals(50.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(53.0, addr.getSecondaryNumber(), 0.001);
        assertTrue(addr.getBuildingNumber().isPresent());
        assertEquals(48, addr.getBuildingNumber().get());
        assertTrue(addr.getCardinal().isPresent());
        assertEquals(Cardinal.SUR, addr.getCardinal().get());
    }
    
    @Test
    void testNoBuildingNumber() {
        ParsedAddress addr = new ParsedAddress(
            StreetType.CARRERA,
            70.0,
            48.0,
            Optional.empty(),
            Optional.empty()
        );
        
        assertFalse(addr.getBuildingNumber().isPresent());
        assertFalse(addr.getCardinal().isPresent());
    }
    
    @Test
    void testNegativeSecondaryNumber() {
        ParsedAddress addr = new ParsedAddress(
            StreetType.CALLE,
            12.0,
            -50.0,  // SUR makes it negative
            Optional.empty(),
            Optional.of(Cardinal.SUR)
        );
        
        assertEquals(-50.0, addr.getSecondaryNumber(), 0.001);
    }
    
    @Test
    void testFractionalNumbers() {
        ParsedAddress addr = new ParsedAddress(
            StreetType.CALLE,
            43.075,  // 43A
            20.225,  // 20C
            Optional.of(15),
            Optional.empty()
        );
        
        assertEquals(43.075, addr.getPrimaryNumber(), 0.001);
        assertEquals(20.225, addr.getSecondaryNumber(), 0.001);
    }
    
    @Test
    void testToString() {
        ParsedAddress addr = new ParsedAddress(
            StreetType.CALLE,
            50.0,
            53.0,
            Optional.of(48),
            Optional.of(Cardinal.SUR)
        );
        
        String str = addr.toString();
        assertTrue(str.contains("CALLE"));
        assertTrue(str.contains("50"));
        assertTrue(str.contains("53"));
        assertTrue(str.contains("48"));
        assertTrue(str.contains("SUR"));
    }
    
    @Test
    void testToStringWithoutOptionals() {
        ParsedAddress addr = new ParsedAddress(
            StreetType.CARRERA,
            70.0,
            48.0,
            Optional.empty(),
            Optional.empty()
        );
        
        String str = addr.toString();
        assertTrue(str.contains("CARRERA"));
        assertTrue(str.contains("70"));
        assertTrue(str.contains("48"));
    }
}
