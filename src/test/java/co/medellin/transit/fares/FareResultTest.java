package co.medellin.transit.fares;

import co.medellin.transit.model.RouteType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FareResult.
 */
class FareResultTest {

    @Test
    void testZeroFareResult() {
        FareResult result = FareResult.zero();
        
        assertEquals(0, result.getTotalCOP());
        assertEquals(0, result.getSegments().size());
        assertFalse(result.isIntegrationApplied());
        assertTrue(result.getSegments().isEmpty());
    }

    @Test
    void testSingleSegmentNoIntegration() {
        FareSegment segment = new FareSegment(RouteType.BUS, 3800, 3800, false);
        FareResult result = new FareResult(3800, Collections.singletonList(segment), false);
        
        assertEquals(3800, result.getTotalCOP());
        assertEquals(1, result.getSegments().size());
        assertFalse(result.isIntegrationApplied());
    }

    @Test
    void testMultipleSegmentsWithIntegration() {
        FareSegment segment1 = new FareSegment(RouteType.METRO, 3820, 3820, false);
        FareSegment segment2 = new FareSegment(RouteType.BUS, 3800, 895, true); // Discounted
        List<FareSegment> segments = Arrays.asList(segment1, segment2);
        
        FareResult result = new FareResult(4715, segments, true);
        
        assertEquals(4715, result.getTotalCOP());
        assertEquals(2, result.getSegments().size());
        assertTrue(result.isIntegrationApplied());
    }

    @Test
    void testGetSegmentsReturnsUnmodifiableList() {
        FareSegment segment = new FareSegment(RouteType.BUS, 3800, 3800, false);
        FareResult result = new FareResult(3800, Collections.singletonList(segment), false);
        
        List<FareSegment> segments = result.getSegments();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            segments.add(new FareSegment(RouteType.METRO, 3820, 3820, false));
        });
    }

    @Test
    void testToStringNoIntegration() {
        FareSegment segment = new FareSegment(RouteType.BUS, 3800, 3800, false);
        FareResult result = new FareResult(3800, Collections.singletonList(segment), false);
        
        String str = result.toString();
        
        assertTrue(str.contains("totalCOP=3800"));
        assertTrue(str.contains("segments=1"));
        assertTrue(str.contains("integrationApplied=false"));
    }

    @Test
    void testToStringWithIntegration() {
        FareSegment segment1 = new FareSegment(RouteType.METRO, 3820, 3820, false);
        FareSegment segment2 = new FareSegment(RouteType.BUS, 3800, 895, true);
        List<FareSegment> segments = Arrays.asList(segment1, segment2);
        
        FareResult result = new FareResult(4715, segments, true);
        String str = result.toString();
        
        assertTrue(str.contains("totalCOP=4715"));
        assertTrue(str.contains("segments=2"));
        assertTrue(str.contains("integrationApplied=true"));
    }
}
