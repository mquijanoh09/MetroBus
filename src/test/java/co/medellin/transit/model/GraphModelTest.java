package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for graph model classes (State, Edge, StateType, EdgeKind).
 */
class GraphModelTest {
    
    @Test
    void testStateTypeEnum() {
        assertEquals(3, StateType.values().length);
        assertNotNull(StateType.valueOf("WALK"));
        assertNotNull(StateType.valueOf("BUS_STOP"));
        assertNotNull(StateType.valueOf("METRO_STATION"));
    }
    
    @Test
    void testEdgeKindEnum() {
        assertEquals(4, EdgeKind.values().length);
        assertNotNull(EdgeKind.valueOf("WALK"));
        assertNotNull(EdgeKind.valueOf("BOARD"));
        assertNotNull(EdgeKind.valueOf("RIDE"));
        assertNotNull(EdgeKind.valueOf("ALIGHT"));
    }
    
    @Test
    void testDirectionEnum() {
        assertEquals(2, Direction.values().length);
        assertNotNull(Direction.valueOf("OUTBOUND"));
        assertNotNull(Direction.valueOf("INBOUND"));
    }
    
    @Test
    void testWalkStateCreation() {
        Coordinate coord = new Coordinate(6.25, -75.57);
        Place place = new Place("Test Location", coord);
        State walkState = new State(0, place);
        
        assertEquals(0, walkState.getId());
        assertEquals(place, walkState.getPlace());
        assertEquals(StateType.WALK, walkState.getType());
        assertTrue(walkState.getRouteId().isEmpty());
        assertTrue(walkState.getDirection().isEmpty());
    }
    
    @Test
    void testBusStopStateCreation() {
        Coordinate coord = new Coordinate(6.25, -75.57);
        Place place = new Place("Bus Stop 123", coord);
        State busStop = new State(1, place, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        
        assertEquals(1, busStop.getId());
        assertEquals(place, busStop.getPlace());
        assertEquals(StateType.BUS_STOP, busStop.getType());
        assertTrue(busStop.getRouteId().isPresent());
        assertEquals("301", busStop.getRouteId().get());
        assertTrue(busStop.getDirection().isPresent());
        assertEquals(Direction.OUTBOUND, busStop.getDirection().get());
    }
    
    @Test
    void testMetroStationStateCreation() {
        Coordinate coord = new Coordinate(6.25, -75.57);
        Place place = new Place("Poblado Station", coord);
        State metroStation = new State(2, place, StateType.METRO_STATION, "A", Direction.INBOUND);
        
        assertEquals(2, metroStation.getId());
        assertEquals(place, metroStation.getPlace());
        assertEquals(StateType.METRO_STATION, metroStation.getType());
        assertTrue(metroStation.getRouteId().isPresent());
        assertEquals("A", metroStation.getRouteId().get());
        assertTrue(metroStation.getDirection().isPresent());
        assertEquals(Direction.INBOUND, metroStation.getDirection().get());
    }
    
    @Test
    void testStateWithWalkTypeThrowsException() {
        Coordinate coord = new Coordinate(6.25, -75.57);
        Place place = new Place("Test", coord);
        
        assertThrows(IllegalArgumentException.class, () -> {
            new State(0, place, StateType.WALK, "route", Direction.OUTBOUND);
        });
    }
    
    @Test
    void testStateNullValidation() {
        assertThrows(NullPointerException.class, () -> {
            new State(0, null);
        });
        
        Coordinate coord = new Coordinate(6.25, -75.57);
        Place place = new Place("Test", coord);
        
        assertThrows(NullPointerException.class, () -> {
            new State(0, place, StateType.BUS_STOP, null, Direction.OUTBOUND);
        });
        
        assertThrows(NullPointerException.class, () -> {
            new State(0, place, StateType.BUS_STOP, "301", null);
        });
    }
    
    @Test
    void testStateEquality() {
        Coordinate coord1 = new Coordinate(6.25, -75.57);
        Place place1 = new Place("Location A", coord1);
        
        Coordinate coord2 = new Coordinate(6.26, -75.58);
        Place place2 = new Place("Location B", coord2);
        
        State state1 = new State(5, place1);
        State state2 = new State(5, place2); // Same ID, different place
        State state3 = new State(6, place1); // Different ID, same place
        
        assertEquals(state1, state2); // Equality based on ID only
        assertNotEquals(state1, state3);
    }
    
    @Test
    void testEdgeCreation() {
        Edge edge = new Edge(0, 1, EdgeKind.WALK, 300, 480);
        
        assertEquals(0, edge.getSourceId());
        assertEquals(1, edge.getTargetId());
        assertEquals(EdgeKind.WALK, edge.getKind());
        assertEquals(300, edge.getRealSec());
        assertEquals(480, edge.getPerceivedCost());
    }
    
    @Test
    void testEdgeWithBoardingPenalty() {
        // Board edge with boarding penalty (+120 seconds)
        Edge boardEdge = new Edge(0, 1, EdgeKind.BOARD, 0, 120);
        
        assertEquals(0, boardEdge.getRealSec());
        assertEquals(120, boardEdge.getPerceivedCost());
    }
    
    @Test
    void testEdgeWithWalkingPenalty() {
        // Walking edge with penalty: walkTime * 1.5 + 30
        int walkTime = 300;
        int perceivedCost = (int)(walkTime * 1.5) + 30;
        Edge walkEdge = new Edge(0, 1, EdgeKind.WALK, walkTime, perceivedCost);
        
        assertEquals(300, walkEdge.getRealSec());
        assertEquals(480, walkEdge.getPerceivedCost());
    }
    
    @Test
    void testEdgeValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Edge(-1, 1, EdgeKind.WALK, 100, 100);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new Edge(0, -1, EdgeKind.WALK, 100, 100);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new Edge(0, 1, EdgeKind.WALK, -100, 100);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new Edge(0, 1, EdgeKind.WALK, 100, -100);
        });
        
        assertThrows(NullPointerException.class, () -> {
            new Edge(0, 1, null, 100, 100);
        });
    }
    
    @Test
    void testEdgeEquality() {
        Edge edge1 = new Edge(0, 1, EdgeKind.WALK, 300, 480);
        Edge edge2 = new Edge(0, 1, EdgeKind.WALK, 300, 480);
        Edge edge3 = new Edge(0, 2, EdgeKind.WALK, 300, 480);
        
        assertEquals(edge1, edge2);
        assertNotEquals(edge1, edge3);
    }
    
    @Test
    void testEdgeToString() {
        Edge edge = new Edge(0, 1, EdgeKind.RIDE, 120, 120);
        String str = edge.toString();
        
        assertTrue(str.contains("0"));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("RIDE"));
        assertTrue(str.contains("120"));
    }
    
    @Test
    void testStateToString() {
        Coordinate coord = new Coordinate(6.25, -75.57);
        Place place = new Place("Test Stop", coord);
        
        State walkState = new State(0, place);
        String walkStr = walkState.toString();
        assertTrue(walkStr.contains("id=0"));
        assertTrue(walkStr.contains("WALK"));
        assertTrue(walkStr.contains("Test Stop"));
        
        State busStop = new State(1, place, StateType.BUS_STOP, "301", Direction.OUTBOUND);
        String busStr = busStop.toString();
        assertTrue(busStr.contains("id=1"));
        assertTrue(busStr.contains("BUS_STOP"));
        assertTrue(busStr.contains("301"));
        assertTrue(busStr.contains("OUTBOUND"));
    }
}
