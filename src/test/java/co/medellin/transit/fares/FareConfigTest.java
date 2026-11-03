package co.medellin.transit.fares;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FareConfig.
 */
class FareConfigTest {

    @Test
    void testDefaults() {
        FareConfig config = FareConfig.defaults();
        
        assertEquals(3820, config.getMetroFareCOP());
        assertEquals(3800, config.getBusFareCOP());
        assertEquals(4715, config.getIntegratedFareCOP());
        assertEquals(90, config.getIntegrationWindowMinutes());
    }

    @Test
    void testCustomValues() {
        FareConfig config = new FareConfig(3000, 2950, 3500, 90);
        
        assertEquals(3000, config.getMetroFareCOP());
        assertEquals(2950, config.getBusFareCOP());
        assertEquals(3500, config.getIntegratedFareCOP());
        assertEquals(90, config.getIntegrationWindowMinutes());
    }

    @Test
    void testFromProperties() throws IOException {
        // This test loads from config.properties in resources
        FareConfig config = FareConfig.fromProperties();
        
        // Should load values from config.properties
        // Based on config.properties: metroFare=3820, busFare=3800, integratedFare=4715
        assertEquals(3820, config.getMetroFareCOP());
        assertEquals(3800, config.getBusFareCOP());
        assertEquals(4715, config.getIntegratedFareCOP());
        assertEquals(90, config.getIntegrationWindowMinutes());
    }

    @Test
    void testToString() {
        FareConfig config = new FareConfig(3000, 2950, 3500, 90);
        String str = config.toString();
        
        assertTrue(str.contains("metroFareCOP=3000"));
        assertTrue(str.contains("busFareCOP=2950"));
        assertTrue(str.contains("integratedFareCOP=3500"));
        assertTrue(str.contains("integrationWindowMinutes=90"));
    }

    @Test
    void testZeroFareEdgeCase() {
        // Edge case: zero fares
        FareConfig config = new FareConfig(0, 0, 0, 90);
        
        assertEquals(0, config.getMetroFareCOP());
        assertEquals(0, config.getBusFareCOP());
        assertEquals(0, config.getIntegratedFareCOP());
    }
}
