package co.medellin.transit.fares;

import co.medellin.transit.model.RouteType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FareSegment.
 */
class FareSegmentTest {

    @Test
    void testCreateBusSegmentWithoutIntegration() {
        FareSegment segment = new FareSegment(RouteType.BUS, 3800, 3800, false);
        
        assertEquals(RouteType.BUS, segment.getMode());
        assertEquals(3800, segment.getBaseFareCOP());
        assertEquals(3800, segment.getAppliedFareCOP());
        assertFalse(segment.isIntegrationApplied());
    }

    @Test
    void testCreateMetroSegmentWithIntegration() {
        FareSegment segment = new FareSegment(RouteType.METRO, 3820, 1910, true);
        
        assertEquals(RouteType.METRO, segment.getMode());
        assertEquals(3820, segment.getBaseFareCOP());
        assertEquals(1910, segment.getAppliedFareCOP());
        assertTrue(segment.isIntegrationApplied());
    }

    @Test
    void testDiscountCalculation() {
        FareSegment segment = new FareSegment(RouteType.BUS, 3800, 2000, true);
        
        // Discount = base - applied
        assertEquals(1800, segment.getBaseFareCOP() - segment.getAppliedFareCOP());
    }

    @Test
    void testToStringFormat() {
        FareSegment segment = new FareSegment(RouteType.BUS, 3800, 3800, false);
        String str = segment.toString();
        
        assertTrue(str.contains("mode=BUS"));
        assertTrue(str.contains("baseFareCOP=3800"));
        assertTrue(str.contains("appliedFareCOP=3800"));
        assertTrue(str.contains("integrationApplied=false"));
    }

    @Test
    void testToStringWithIntegration() {
        FareSegment segment = new FareSegment(RouteType.METRO, 3820, 1910, true);
        String str = segment.toString();
        
        assertTrue(str.contains("mode=METRO"));
        assertTrue(str.contains("baseFareCOP=3820"));
        assertTrue(str.contains("appliedFareCOP=1910"));
        assertTrue(str.contains("integrationApplied=true"));
    }
}
