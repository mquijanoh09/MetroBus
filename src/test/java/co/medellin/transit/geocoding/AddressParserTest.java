package co.medellin.transit.geocoding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AddressParser.
 * Tests cover valid formats, edge cases, and rejection criteria from the spec.
 */
class AddressParserTest {
    
    private AddressParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new AddressParser();
    }
    
    // ========== Valid Address Tests ==========
    
    @Test
    void testSimpleCalleAddress() {
        // "cl 50 # 53-48" → calle 50, carrera 53, building 48
        Optional<ParsedAddress> result = parser.parse("cl 50 # 53-48");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CALLE, addr.getStreetType());
        assertEquals(50.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(53.0, addr.getSecondaryNumber(), 0.001);
        assertTrue(addr.getBuildingNumber().isPresent());
        assertEquals(48, addr.getBuildingNumber().get());
    }
    
    @Test
    void testAddressWithLetterSuffix() {
        // "Calle 10 # 43A - 20" → calle 10, carrera 43.075 (A=0.075), building 20
        Optional<ParsedAddress> result = parser.parse("Calle 10 # 43A - 20");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CALLE, addr.getStreetType());
        assertEquals(10.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(43.075, addr.getSecondaryNumber(), 0.001);
        assertTrue(addr.getBuildingNumber().isPresent());
        assertEquals(20, addr.getBuildingNumber().get());
    }
    
    @Test
    void testAddressWithSurOnPrimary() {
        // "CL 12 Sur x CR 50" → calle -12 (SUR makes negative), carrera 50
        Optional<ParsedAddress> result = parser.parse("CL 12 Sur x CR 50");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CALLE, addr.getStreetType());
        assertEquals(-12.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(50.0, addr.getSecondaryNumber(), 0.001);
        assertTrue(addr.getCardinal().isPresent());
        assertEquals(Cardinal.SUR, addr.getCardinal().get());
    }
    
    @Test
    void testAddressWithSurOnSecondary() {
        // "CR 70 x CL 48 SUR" → carrera 70, calle -48
        Optional<ParsedAddress> result = parser.parse("CR 70 x CL 48 SUR");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CARRERA, addr.getStreetType());
        assertEquals(70.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(-48.0, addr.getSecondaryNumber(), 0.001);
    }
    
    @Test
    void testCarreraWithKRSynonym() {
        // "KR 43A # 1 - 100" → carrera 43.075
        Optional<ParsedAddress> result = parser.parse("KR 43A # 1 - 100");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CARRERA, addr.getStreetType());
        assertEquals(43.075, addr.getPrimaryNumber(), 0.001);
    }
    
    @Test
    void testCarreraWithCRASynonym() {
        // "CRA 50 # 10 - 20"
        Optional<ParsedAddress> result = parser.parse("CRA 50 # 10 - 20");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CARRERA, addr.getStreetType());
        assertEquals(50.0, addr.getPrimaryNumber(), 0.001);
    }
    
    @Test
    void testDiagonalAddress() {
        // "DIAGONAL 45 # 20 - 30"
        Optional<ParsedAddress> result = parser.parse("DIAGONAL 45 # 20 - 30");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.DIAGONAL, addr.getStreetType());
        assertEquals(45.0, addr.getPrimaryNumber(), 0.001);
    }
    
    @Test
    void testDiagonalWithDGAbbreviation() {
        // "DG 50 # 25 - 10"
        Optional<ParsedAddress> result = parser.parse("DG 50 # 25 - 10");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.DIAGONAL, addr.getStreetType());
    }
    
    @Test
    void testTransversalAddress() {
        // "TRANSVERSAL 30 # 15 - 5"
        Optional<ParsedAddress> result = parser.parse("TRANSVERSAL 30 # 15 - 5");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.TRANSVERSAL, addr.getStreetType());
    }
    
    @Test
    void testTransversalWithTVAbbreviation() {
        // "TV 40 # 20 - 10"
        Optional<ParsedAddress> result = parser.parse("TV 40 # 20 - 10");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.TRANSVERSAL, addr.getStreetType());
    }
    
    @Test
    void testCircularAddress() {
        // "CIRCULAR 1 # 50 - 100"
        Optional<ParsedAddress> result = parser.parse("CIRCULAR 1 # 50 - 100");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CIRCULAR, addr.getStreetType());
    }
    
    @Test
    void testAddressWithNoSeparator() {
        // "Calle 50 No. 53 - 48"
        Optional<ParsedAddress> result = parser.parse("Calle 50 No. 53 - 48");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CALLE, addr.getStreetType());
        assertEquals(50.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(53.0, addr.getSecondaryNumber(), 0.001);
    }
    
    @Test
    void testAddressWithNoDegreeSymbol() {
        // "Calle 50 N° 53 - 48"
        Optional<ParsedAddress> result = parser.parse("Calle 50 N° 53 - 48");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CALLE, addr.getStreetType());
    }
    
    @Test
    void testAddressWithConSeparator() {
        // "CL 50 con CR 53"
        Optional<ParsedAddress> result = parser.parse("CL 50 con CR 53");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CALLE, addr.getStreetType());
        assertEquals(50.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(53.0, addr.getSecondaryNumber(), 0.001);
    }
    
    @Test
    void testAddressWithNoBuildingNumber() {
        // "CL 50 # 53"
        Optional<ParsedAddress> result = parser.parse("CL 50 # 53");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(StreetType.CALLE, addr.getStreetType());
        assertEquals(50.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(53.0, addr.getSecondaryNumber(), 0.001);
        assertFalse(addr.getBuildingNumber().isPresent());
    }
    
    @Test
    void testAddressWithMultipleLetters() {
        // "CL 43B # 20C - 15" → 43.15, 20.225
        Optional<ParsedAddress> result = parser.parse("CL 43B # 20C - 15");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(43.15, addr.getPrimaryNumber(), 0.001);  // B = 0.15
        assertEquals(20.225, addr.getSecondaryNumber(), 0.001);  // C = 0.225
    }
    
    // ========== Edge Case Tests ==========
    
    @Test
    void testCaseInsensitivity() {
        // Should parse regardless of case
        Optional<ParsedAddress> result = parser.parse("calle 50 # 53-48");
        assertTrue(result.isPresent());
        
        result = parser.parse("CALLE 50 # 53-48");
        assertTrue(result.isPresent());
        
        result = parser.parse("CaLLe 50 # 53-48");
        assertTrue(result.isPresent());
    }
    
    @Test
    void testExtraWhitespace() {
        // Should handle extra spaces
        Optional<ParsedAddress> result = parser.parse("CL  50   #   53  -  48");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(50.0, addr.getPrimaryNumber(), 0.001);
        assertEquals(53.0, addr.getSecondaryNumber(), 0.001);
    }
    
    @Test
    void testAddressWithComma() {
        // Should drop everything after comma
        Optional<ParsedAddress> result = parser.parse("CL 50 # 53-48, Medellín");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(48, addr.getBuildingNumber().get());
    }
    
    @Test
    void testNorteCardinal() {
        // "CL 10 Norte # 50 - 20"
        Optional<ParsedAddress> result = parser.parse("CL 10 Norte # 50 - 20");
        
        assertTrue(result.isPresent());
        ParsedAddress addr = result.get();
        assertEquals(10.0, addr.getPrimaryNumber(), 0.001);
        assertTrue(addr.getCardinal().isPresent());
        assertEquals(Cardinal.NORTE, addr.getCardinal().get());
    }
    
    // ========== Invalid Address Tests ==========
    
    @Test
    void testInvalidFormat() {
        // "hola mundo" - not an address format
        Optional<ParsedAddress> result = parser.parse("hola mundo");
        assertFalse(result.isPresent());
    }
    
    @Test
    void testRuralAddress() {
        // "VDA EL YOLOMBO" - rural addresses not supported
        Optional<ParsedAddress> result = parser.parse("VDA EL YOLOMBO");
        assertFalse(result.isPresent());
    }
    
    @Test
    void testOutOfRangeAddress() {
        // "CL 900 x CR 900" - out of range
        Optional<ParsedAddress> result = parser.parse("CL 900 x CR 900");
        assertFalse(result.isPresent());
    }
    
    @Test
    void testMissingSecondaryNumber() {
        // "CL 50 #" - incomplete
        Optional<ParsedAddress> result = parser.parse("CL 50 #");
        assertFalse(result.isPresent());
    }
    
    @Test
    void testMissingPrimaryNumber() {
        // "CL # 50 - 48" - missing primary
        Optional<ParsedAddress> result = parser.parse("CL # 50 - 48");
        assertFalse(result.isPresent());
    }
    
    @Test
    void testNullAddress() {
        Optional<ParsedAddress> result = parser.parse(null);
        assertFalse(result.isPresent());
    }
    
    @Test
    void testEmptyAddress() {
        Optional<ParsedAddress> result = parser.parse("");
        assertFalse(result.isPresent());
    }
    
    @Test
    void testWhitespaceOnlyAddress() {
        Optional<ParsedAddress> result = parser.parse("   ");
        assertFalse(result.isPresent());
    }
    
    @Test
    void testInvalidStreetType() {
        // "AVENIDA 50 # 53 - 48" - AVENIDA not supported
        Optional<ParsedAddress> result = parser.parse("AVENIDA 50 # 53 - 48");
        assertFalse(result.isPresent());
    }
    
    // ========== Letter Conversion Tests ==========
    
    @Test
    void testLetterConversionA() {
        Optional<ParsedAddress> result = parser.parse("CL 43A # 20 - 15");
        assertTrue(result.isPresent());
        assertEquals(43.075, result.get().getPrimaryNumber(), 0.001);
    }
    
    @Test
    void testLetterConversionB() {
        Optional<ParsedAddress> result = parser.parse("CL 43B # 20 - 15");
        assertTrue(result.isPresent());
        assertEquals(43.15, result.get().getPrimaryNumber(), 0.001);
    }
    
    @Test
    void testLetterConversionC() {
        Optional<ParsedAddress> result = parser.parse("CL 43C # 20 - 15");
        assertTrue(result.isPresent());
        assertEquals(43.225, result.get().getPrimaryNumber(), 0.001);
    }
    
    @Test
    void testLetterConversionD() {
        Optional<ParsedAddress> result = parser.parse("CL 43D # 20 - 15");
        assertTrue(result.isPresent());
        assertEquals(43.3, result.get().getPrimaryNumber(), 0.001);
    }
    
    @Test
    void testLetterConversionZ() {
        Optional<ParsedAddress> result = parser.parse("CL 43Z # 20 - 15");
        assertTrue(result.isPresent());
        assertEquals(43.0 + 26 * 0.075, result.get().getPrimaryNumber(), 0.001);
    }
    
    @Test
    void testAddressWithoutSeparator() {
        // "CL 50 53-48" has no # separator (format used in test_cases.csv)
        Optional<ParsedAddress> result = parser.parse("CL 50 53-48");
        assertTrue(result.isPresent(), "Should parse CL 50 53-48");
        assertEquals(StreetType.CALLE, result.get().getStreetType());
        assertEquals(50.0, result.get().getPrimaryNumber(), 0.001);
        assertEquals(53.0, result.get().getSecondaryNumber(), 0.001);
        assertEquals(48, result.get().getBuildingNumber().orElse(-1));
    }
    
    @Test
    void testTrailingDashWithDoubleLetter() {
        // "CL 48EE 99D-" has a two-letter suffix and no house number
        Optional<ParsedAddress> result = parser.parse("CL 48EE 99D-");
        assertTrue(result.isPresent(), "Should parse CL 48EE 99D-");
        assertEquals(48.0 + 5 * 0.075, result.get().getPrimaryNumber(), 0.001);
        assertEquals(99.0 + 4 * 0.075, result.get().getSecondaryNumber(), 0.001);
        assertEquals(0, result.get().getBuildingNumber().orElse(-1));
    }
    
    @Test
    void testCarreraWithTrailingDash() {
        Optional<ParsedAddress> result = parser.parse("CR 83 89B-");
        assertTrue(result.isPresent(), "Should parse CR 83 89B-");
        assertEquals(StreetType.CARRERA, result.get().getStreetType());
        assertEquals(83.0, result.get().getPrimaryNumber(), 0.001);
        assertEquals(89.0 + 2 * 0.075, result.get().getSecondaryNumber(), 0.001);
    }
    
    @Test
    void testIntersectionWithLowercaseX() {
        Optional<ParsedAddress> result = parser.parse("CR 70 x CL 48");
        assertTrue(result.isPresent(), "Should parse CR 70 x CL 48");
        assertEquals(StreetType.CARRERA, result.get().getStreetType());
        assertEquals(70.0, result.get().getPrimaryNumber(), 0.001);
        assertEquals(48.0, result.get().getSecondaryNumber(), 0.001);
    }
    
    @Test
    void testIntersectionWithSur() {
        Optional<ParsedAddress> result = parser.parse("CL 12 Sur x CR 50");
        assertTrue(result.isPresent(), "Should parse CL 12 Sur x CR 50");
        assertEquals(StreetType.CALLE, result.get().getStreetType());
        assertEquals(50.0, result.get().getSecondaryNumber(), 0.001);
    }
}
