package co.medellin.transit.geocoding;

import co.medellin.transit.model.Coordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AddressLocator with reference-table interpolation model.
 * 
 * The AddressLocator now uses a reference table of 3,481 parsed stop addresses
 * and locates addresses by inverse-distance-weighted interpolation from the 4 closest points.
 * 
 * Tests updated to reflect:
 * - Interpolated coordinates (not fixed reference point at 6.25, -75.57)
 * - Real Medellin geography: calle numbers increase NORTHWARD, carrera numbers increase WESTWARD
 * - Reasonable tolerances for interpolation from real stop data
 */
class AddressLocatorTest {
    
    private AddressLocator locator;
    private AddressParser parser;
    
    @BeforeEach
    void setUp() {
        locator = new AddressLocator();
        parser = new AddressParser();
    }
    
    @Test
    void testReferencePoint() {
        // Calle 50 # 50 should be locatable via interpolation from nearby stops
        ParsedAddress address = new ParsedAddress(
            StreetType.CALLE,
            50.0,
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        assertTrue(result.isPresent(), "Reference point should be locatable");
        
        Coordinate coord = result.get();
        // With interpolation, coordinates will be near but not exactly at old reference point
        assertTrue(coord.getLat() > 6.20 && coord.getLat() < 6.30, "Latitude should be in reasonable range");
        assertTrue(coord.getLon() > -75.60 && coord.getLon() < -75.55, "Longitude should be in reasonable range");
        assertTrue(locator.isInBounds(coord), "Should be in bounds");
    }
    
    @Test
    void testCalleIncreaseGoesNorth() {
        // In real Medellin: higher Calle number = NORTH = higher latitude
        // Calle 60 # 50 should be north of Calle 50 # 50
        ParsedAddress calle50 = new ParsedAddress(
            StreetType.CALLE, 50.0, 50.0, Optional.empty(), Optional.empty()
        );
        ParsedAddress calle60 = new ParsedAddress(
            StreetType.CALLE, 60.0, 50.0, Optional.empty(), Optional.empty()
        );
        
        Optional<Coordinate> c50 = locator.locate(calle50);
        Optional<Coordinate> c60 = locator.locate(calle60);
        
        assertTrue(c50.isPresent() && c60.isPresent(), "Both addresses should locate");
        assertTrue(c60.get().getLat() > c50.get().getLat(), 
                   "Higher Calle number should have higher latitude (further north)");
        // Longitude should be approximately the same (same carrera)
        assertEquals(c50.get().getLon(), c60.get().getLon(), 0.01, 
                    "Same carrera should have similar longitude");
    }
    
    @Test
    void testCalleDecreaseGoesSouth() {
        // In real Medellin: lower Calle number = SOUTH = lower latitude
        // Calle 40 # 50 should be south of Calle 50 # 50
        ParsedAddress calle50 = new ParsedAddress(
            StreetType.CALLE, 50.0, 50.0, Optional.empty(), Optional.empty()
        );
        ParsedAddress calle40 = new ParsedAddress(
            StreetType.CALLE, 40.0, 50.0, Optional.empty(), Optional.empty()
        );
        
        Optional<Coordinate> c50 = locator.locate(calle50);
        Optional<Coordinate> c40 = locator.locate(calle40);
        
        assertTrue(c50.isPresent() && c40.isPresent(), "Both addresses should locate");
        assertTrue(c40.get().getLat() < c50.get().getLat(), 
                   "Lower Calle number should have lower latitude (further south)");
        assertEquals(c50.get().getLon(), c40.get().getLon(), 0.01, 
                    "Same carrera should have similar longitude");
    }
    
    @Test
    void testCarreraIncreaseGoesWest() {
        // Carrera numbers increase westward (lower longitude)
        // Calle 50 # 60 should be west of Calle 50 # 50
        ParsedAddress carr50 = new ParsedAddress(
            StreetType.CALLE, 50.0, 50.0, Optional.empty(), Optional.empty()
        );
        ParsedAddress carr60 = new ParsedAddress(
            StreetType.CALLE, 50.0, 60.0, Optional.empty(), Optional.empty()
        );
        
        Optional<Coordinate> c50 = locator.locate(carr50);
        Optional<Coordinate> c60 = locator.locate(carr60);
        
        assertTrue(c50.isPresent() && c60.isPresent(), "Both addresses should locate");
        assertTrue(c60.get().getLon() < c50.get().getLon(), 
                   "Higher Carrera number should have lower longitude (further west)");
        // Latitude should be approximately the same (same calle)
        assertEquals(c50.get().getLat(), c60.get().getLat(), 0.01, 
                    "Same calle should have similar latitude");
    }
    
    @Test
    void testCarreraDecreaseGoesEast() {
        // Carrera numbers decrease eastward (higher longitude)
        // Calle 50 # 40 should be east of Calle 50 # 50
        ParsedAddress carr50 = new ParsedAddress(
            StreetType.CALLE, 50.0, 50.0, Optional.empty(), Optional.empty()
        );
        ParsedAddress carr40 = new ParsedAddress(
            StreetType.CALLE, 50.0, 40.0, Optional.empty(), Optional.empty()
        );
        
        Optional<Coordinate> c50 = locator.locate(carr50);
        Optional<Coordinate> c40 = locator.locate(carr40);
        
        assertTrue(c50.isPresent() && c40.isPresent(), "Both addresses should locate");
        assertTrue(c40.get().getLon() > c50.get().getLon(), 
                   "Lower Carrera number should have higher longitude (further east)");
        assertEquals(c50.get().getLat(), c40.get().getLat(), 0.01, 
                    "Same calle should have similar latitude");
    }
    
    @Test
    void testCarreraAsMainStreet() {
        // Carrera 50 # 50 should also locate via interpolation
        ParsedAddress address = new ParsedAddress(
            StreetType.CARRERA,
            50.0,
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        assertTrue(result.isPresent(), "Carrera address should be locatable");
        assertTrue(locator.isInBounds(result.get()), "Should be in bounds");
    }
    
    @Test
    void testDiagonalStreetType() {
        // Diagonal addresses should be handled (simplified as Calle/Carrera)
        ParsedAddress address = new ParsedAddress(
            StreetType.DIAGONAL,
            50.0,
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        assertTrue(result.isPresent(), "Diagonal street type should be locatable");
    }
    
    @Test
    void testTransversalStreetType() {
        // Transversal addresses should be handled
        ParsedAddress address = new ParsedAddress(
            StreetType.TRANSVERSAL,
            50.0,
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        assertTrue(result.isPresent(), "Transversal street type should be locatable");
    }
    
    @Test
    void testCircularStreetType() {
        // Circular addresses should be handled
        ParsedAddress address = new ParsedAddress(
            StreetType.CIRCULAR,
            50.0,
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        assertTrue(result.isPresent(), "Circular street type should be locatable");
    }
    
    @Test
    void testWithinBounds() {
        // Address well within Medellin bounds
        ParsedAddress address = new ParsedAddress(
            StreetType.CALLE,
            50.0,
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        assertTrue(result.isPresent());
        assertTrue(locator.isInBounds(result.get()), "Coordinate should be within bounds");
    }
    
    @Test
    void testOutOfBoundsNorth() {
        // Very high Calle number - outside Medellin bounds
        ParsedAddress address = new ParsedAddress(
            StreetType.CALLE,
            200.0,
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        // Addresses outside Medellin must not locate
        assertTrue(result.isEmpty(), "Out-of-bounds address should not locate");
    }
    
    @Test
    void testOutOfBoundsSouth() {
        // Very low/negative Calle number - outside Medellin bounds
        ParsedAddress address = new ParsedAddress(
            StreetType.CALLE,
            -50.0,
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        // Addresses outside Medellin must not locate
        assertTrue(result.isEmpty(), "Out-of-bounds address should not locate");
    }
    
    @Test
    void testUnreliableExtrapolation() {
        // Address very far from any reference stops
        ParsedAddress address = new ParsedAddress(
            StreetType.CALLE,
            250.0,
            250.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        // Interpolation may fail to find nearby stops
        // This test just verifies the system handles it gracefully
        assertTrue(result.isEmpty() || !locator.isInBounds(result.get()), 
                  "Extreme address should not locate or be out of bounds");
    }
    
    @Test
    void testNullAddress() {
        Optional<Coordinate> result = locator.locate(null);
        assertTrue(result.isEmpty(), "Null address should return empty");
    }
    
    @Test
    void testIsInBoundsNullCoordinate() {
        assertFalse(locator.isInBounds(null), "Null coordinate should be out of bounds");
    }
    
    @Test
    void testIsInBoundsValidCoordinate() {
        Coordinate coord = new Coordinate(6.25, -75.57);
        assertTrue(locator.isInBounds(coord), "Center coordinate should be in bounds");
    }
    
    @Test
    void testIsInBoundsBoundaryNorth() {
        Coordinate coord = new Coordinate(6.35, -75.57);
        assertTrue(locator.isInBounds(coord), "North boundary should be in bounds");
    }
    
    @Test
    void testIsInBoundsBoundarySouth() {
        Coordinate coord = new Coordinate(6.15, -75.57);
        assertTrue(locator.isInBounds(coord), "South boundary should be in bounds");
    }
    
    @Test
    void testIsInBoundsBeyondNorth() {
        Coordinate coord = new Coordinate(6.36, -75.57);
        assertFalse(locator.isInBounds(coord), "Beyond north boundary should be out of bounds");
    }
    
    @Test
    void testIsInBoundsBeyondSouth() {
        Coordinate coord = new Coordinate(6.14, -75.57);
        assertFalse(locator.isInBounds(coord), "Beyond south boundary should be out of bounds");
    }
    
    @Test
    void testIsInBoundsBeyondEast() {
        Coordinate coord = new Coordinate(6.25, -75.49);
        assertFalse(locator.isInBounds(coord), "Beyond east boundary should be out of bounds");
    }
    
    @Test
    void testIsInBoundsBeyondWest() {
        Coordinate coord = new Coordinate(6.25, -75.66);
        assertFalse(locator.isInBounds(coord), "Beyond west boundary should be out of bounds");
    }
    
    @Test
    void testIntegrationWithParser() {
        // Full integration: parse and locate a real address
        String addressStr = "Calle 50 # 50 - 48";
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent(), "Address should parse successfully");
        
        Optional<Coordinate> coord = locator.locate(parsed.get());
        assertTrue(coord.isPresent(), "Parsed address should locate successfully");
        assertTrue(locator.isInBounds(coord.get()), "Located coordinate should be in bounds");
    }
    
    @Test
    void testIntegrationCalleWithSur() {
        // Test integration with cardinal direction (SUR makes Calle negative)
        String addressStr = "Calle 10 Sur # 50";
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent(), "Address with Sur should parse");
        
        Optional<Coordinate> coord = locator.locate(parsed.get());
        assertTrue(coord.isPresent(), "Calle Sur address should locate");
        
        // Calle -10 (Sur) is negative, so it's south of positive calle numbers
        // Negative calle should have lower latitude than positive calle
        ParsedAddress calle50 = new ParsedAddress(
            StreetType.CALLE, 50.0, 50.0, Optional.empty(), Optional.empty()
        );
        Optional<Coordinate> c50 = locator.locate(calle50);
        assertTrue(c50.isPresent());
        
        assertTrue(coord.get().getLat() < c50.get().getLat(), 
                  "Calle Sur (negative) should be south of positive Calle (lower latitude)");
    }
    
    @Test
    void testFractionalCalleNumber() {
        // Test address with letter suffix (e.g., 43A)
        ParsedAddress address = new ParsedAddress(
            StreetType.CALLE,
            43.075,  // 43A = 43 + 0.075
            50.0,
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        assertTrue(result.isPresent(), "Fractional Calle number should locate");
        
        // Calle 43.075 is lower than Calle 50, so should be south (lower latitude)
        ParsedAddress calle50 = new ParsedAddress(
            StreetType.CALLE, 50.0, 50.0, Optional.empty(), Optional.empty()
        );
        Optional<Coordinate> c50 = locator.locate(calle50);
        assertTrue(c50.isPresent());
        
        assertTrue(result.get().getLat() < c50.get().getLat(), 
                  "Calle 43A should be south of Calle 50 (lower latitude)");
    }
    
    @Test
    void testKnownLocation() {
        // Test a known location that should be reasonable
        // Calle 44 # 43A is a well-known area in El Poblado
        ParsedAddress address = new ParsedAddress(
            StreetType.CALLE,
            44.0,
            43.075,  // 43A
            Optional.empty(),
            Optional.empty()
        );
        
        Optional<Coordinate> result = locator.locate(address);
        assertTrue(result.isPresent(), "Known El Poblado address should locate");
        assertTrue(locator.isInBounds(result.get()), "El Poblado address should be in bounds");
        
        // Calle 44 < Calle 50, so should be south (lower latitude)
        ParsedAddress calle50 = new ParsedAddress(
            StreetType.CALLE, 50.0, 50.0, Optional.empty(), Optional.empty()
        );
        Optional<Coordinate> c50 = locator.locate(calle50);
        assertTrue(c50.isPresent());
        
        assertTrue(result.get().getLat() < c50.get().getLat(), 
                  "Calle 44 should be south of Calle 50 (lower latitude)");
        // Carrera 43 < Carrera 50, so should be east (higher longitude)
        assertTrue(result.get().getLon() > c50.get().getLon(), 
                  "Carrera 43 should be east of Carrera 50 (higher longitude)");
    }
}