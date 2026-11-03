package co.medellin.transit.routing;

import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.State;
import co.medellin.transit.model.StateType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DijkstraResult class.
 * Tests path storage, cost tracking, and accessor methods.
 */
class DijkstraResultTest {
    
    @Test
    void testConstructorWithValidArguments() {
        // Given: valid path and costs
        Place place1 = new Place("Start", new Coordinate(6.25, -75.56));
        Place place2 = new Place("End", new Coordinate(6.26, -75.57));
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        List<State> path = Arrays.asList(state1, state2);
        
        // When: creating result
        DijkstraResult result = new DijkstraResult(path, 120, 100);
        
        // Then: should store values correctly
        assertEquals(path, result.getPath());
        assertEquals(120, result.getPerceivedCost());
        assertEquals(100, result.getRealDurationSec());
    }
    
    @Test
    void testConstructorWithNullPath() {
        // When/Then: creating result with null path should throw
        assertThrows(NullPointerException.class, () -> {
            new DijkstraResult(null, 120, 100);
        });
    }
    
    @Test
    void testConstructorWithEmptyPath() {
        // When/Then: creating result with empty path should throw
        assertThrows(IllegalArgumentException.class, () -> {
            new DijkstraResult(Collections.emptyList(), 120, 100);
        });
    }
    
    @Test
    void testConstructorWithNegativePerceivedCost() {
        // Given: valid path
        Place place = new Place("Start", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = Arrays.asList(state);
        
        // When/Then: creating result with negative perceived cost should throw
        assertThrows(IllegalArgumentException.class, () -> {
            new DijkstraResult(path, -10, 100);
        });
    }
    
    @Test
    void testConstructorWithNegativeRealDuration() {
        // Given: valid path
        Place place = new Place("Start", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = Arrays.asList(state);
        
        // When/Then: creating result with negative real duration should throw
        assertThrows(IllegalArgumentException.class, () -> {
            new DijkstraResult(path, 120, -100);
        });
    }
    
    @Test
    void testGetPath() {
        // Given: result with path
        Place place1 = new Place("Start", new Coordinate(6.25, -75.56));
        Place place2 = new Place("Middle", new Coordinate(6.255, -75.565));
        Place place3 = new Place("End", new Coordinate(6.26, -75.57));
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        State state3 = new State(2, place3);
        List<State> path = Arrays.asList(state1, state2, state3);
        DijkstraResult result = new DijkstraResult(path, 200, 180);
        
        // When: getting path
        List<State> retrievedPath = result.getPath();
        
        // Then: should return correct path
        assertEquals(3, retrievedPath.size());
        assertEquals(state1, retrievedPath.get(0));
        assertEquals(state2, retrievedPath.get(1));
        assertEquals(state3, retrievedPath.get(2));
    }
    
    @Test
    void testGetPathIsUnmodifiable() {
        // Given: result with path
        Place place = new Place("Start", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = new ArrayList<>(Arrays.asList(state));
        DijkstraResult result = new DijkstraResult(path, 120, 100);
        
        // When/Then: attempting to modify returned path should throw
        assertThrows(UnsupportedOperationException.class, () -> {
            result.getPath().add(new State(1, place));
        });
    }
    
    @Test
    void testGetPerceivedCost() {
        // Given: result with specific perceived cost
        Place place = new Place("Start", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = Arrays.asList(state);
        DijkstraResult result = new DijkstraResult(path, 250, 200);
        
        // When: getting perceived cost
        int cost = result.getPerceivedCost();
        
        // Then: should return correct value
        assertEquals(250, cost);
    }
    
    @Test
    void testGetRealDurationSec() {
        // Given: result with specific real duration
        Place place = new Place("Start", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = Arrays.asList(state);
        DijkstraResult result = new DijkstraResult(path, 250, 200);
        
        // When: getting real duration
        int duration = result.getRealDurationSec();
        
        // Then: should return correct value
        assertEquals(200, duration);
    }
    
    @Test
    void testGetPathLength() {
        // Given: result with 5-state path
        List<State> path = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Place place = new Place("Stop" + i, new Coordinate(6.25 + i * 0.01, -75.56));
            State state = new State(i, place);
            path.add(state);
        }
        DijkstraResult result = new DijkstraResult(path, 300, 250);
        
        // When: getting path length
        int length = result.getPathLength();
        
        // Then: should return correct count
        assertEquals(5, length);
    }
    
    @Test
    void testGetOrigin() {
        // Given: result with multi-state path
        Place place1 = new Place("Origin", new Coordinate(6.25, -75.56));
        Place place2 = new Place("Middle", new Coordinate(6.255, -75.565));
        Place place3 = new Place("Destination", new Coordinate(6.26, -75.57));
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        State state3 = new State(2, place3);
        List<State> path = Arrays.asList(state1, state2, state3);
        DijkstraResult result = new DijkstraResult(path, 200, 180);
        
        // When: getting origin
        State origin = result.getOrigin();
        
        // Then: should return first state
        assertEquals(state1, origin);
        assertEquals("Origin", origin.getPlace().getName());
    }
    
    @Test
    void testGetDestination() {
        // Given: result with multi-state path
        Place place1 = new Place("Origin", new Coordinate(6.25, -75.56));
        Place place2 = new Place("Middle", new Coordinate(6.255, -75.565));
        Place place3 = new Place("Destination", new Coordinate(6.26, -75.57));
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        State state3 = new State(2, place3);
        List<State> path = Arrays.asList(state1, state2, state3);
        DijkstraResult result = new DijkstraResult(path, 200, 180);
        
        // When: getting destination
        State destination = result.getDestination();
        
        // Then: should return last state
        assertEquals(state3, destination);
        assertEquals("Destination", destination.getPlace().getName());
    }
    
    @Test
    void testGetOriginWithSingleStatePathString() {
        // Given: result with single-state path
        Place place = new Place("OnlyStop", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = Arrays.asList(state);
        DijkstraResult result = new DijkstraResult(path, 0, 0);
        
        // When: getting origin and destination
        State origin = result.getOrigin();
        State destination = result.getDestination();
        
        // Then: should be same state
        assertEquals(state, origin);
        assertEquals(state, destination);
        assertSame(origin, destination);
    }
    
    @Test
    void testToString() {
        // Given: result with specific values
        Place place1 = new Place("Start", new Coordinate(6.25, -75.56));
        Place place2 = new Place("End", new Coordinate(6.26, -75.57));
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        List<State> path = Arrays.asList(state1, state2);
        DijkstraResult result = new DijkstraResult(path, 150, 120);
        
        // When: converting to string
        String str = result.toString();
        
        // Then: should contain key information
        assertTrue(str.contains("pathLength=2"));
        assertTrue(str.contains("perceivedCost=150"));
        assertTrue(str.contains("realDuration=120"));
    }
    
    @Test
    void testEquals() {
        // Given: two results with same values
        Place place1 = new Place("Start", new Coordinate(6.25, -75.56));
        Place place2 = new Place("End", new Coordinate(6.26, -75.57));
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        List<State> path1 = Arrays.asList(state1, state2);
        List<State> path2 = Arrays.asList(state1, state2);
        
        DijkstraResult result1 = new DijkstraResult(path1, 150, 120);
        DijkstraResult result2 = new DijkstraResult(path2, 150, 120);
        
        // Then: should be equal
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }
    
    @Test
    void testNotEquals() {
        // Given: two results with different values
        Place place1 = new Place("Start", new Coordinate(6.25, -75.56));
        Place place2 = new Place("End", new Coordinate(6.26, -75.57));
        State state1 = new State(0, place1);
        State state2 = new State(1, place2);
        List<State> path = Arrays.asList(state1, state2);
        
        DijkstraResult result1 = new DijkstraResult(path, 150, 120);
        DijkstraResult result2 = new DijkstraResult(path, 200, 120);
        DijkstraResult result3 = new DijkstraResult(path, 150, 150);
        
        // Then: should not be equal
        assertNotEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertNotEquals(result2, result3);
    }
    
    @Test
    void testPerceivedCostCanBeGreaterThanRealDuration() {
        // Given: result where penalties make perceived cost higher
        Place place = new Place("Start", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = Arrays.asList(state);
        
        // Perceived cost includes penalties, real duration does not
        int realDuration = 100;
        int perceivedCost = 250; // Higher due to penalties
        
        // When: creating result
        DijkstraResult result = new DijkstraResult(path, perceivedCost, realDuration);
        
        // Then: should accept and store correctly
        assertEquals(250, result.getPerceivedCost());
        assertEquals(100, result.getRealDurationSec());
        assertTrue(result.getPerceivedCost() > result.getRealDurationSec());
    }
    
    @Test
    void testPerceivedCostCanEqualRealDuration() {
        // Given: result where costs are equal (no penalties)
        Place place = new Place("Start", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = Arrays.asList(state);
        
        int duration = 180;
        
        // When: creating result with equal costs
        DijkstraResult result = new DijkstraResult(path, duration, duration);
        
        // Then: should be valid
        assertEquals(duration, result.getPerceivedCost());
        assertEquals(duration, result.getRealDurationSec());
    }
    
    @Test
    void testZeroCostsAreValid() {
        // Given: result with zero costs (edge case)
        Place place = new Place("Start", new Coordinate(6.25, -75.56));
        State state = new State(0, place);
        List<State> path = Arrays.asList(state);
        
        // When: creating result with zero costs
        DijkstraResult result = new DijkstraResult(path, 0, 0);
        
        // Then: should be valid
        assertEquals(0, result.getPerceivedCost());
        assertEquals(0, result.getRealDurationSec());
        assertEquals(1, result.getPathLength());
    }
    
    @Test
    void testLargePathLength() {
        // Given: result with many states
        List<State> path = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Place place = new Place("Stop" + i, new Coordinate(6.25 + i * 0.001, -75.56));
            State state = new State(i, place);
            path.add(state);
        }
        
        // When: creating result
        DijkstraResult result = new DijkstraResult(path, 5000, 4000);
        
        // Then: should handle large paths
        assertEquals(100, result.getPathLength());
        assertEquals(path.get(0), result.getOrigin());
        assertEquals(path.get(99), result.getDestination());
    }
}
