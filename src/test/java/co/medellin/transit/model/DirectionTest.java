package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test
    void testEnumValues() {
        Direction[] directions = Direction.values();
        assertEquals(2, directions.length);
        assertEquals(Direction.OUTBOUND, directions[0]);
        assertEquals(Direction.INBOUND, directions[1]);
    }

    @Test
    void testEnumValueOf() {
        assertEquals(Direction.OUTBOUND, Direction.valueOf("OUTBOUND"));
        assertEquals(Direction.INBOUND, Direction.valueOf("INBOUND"));
    }

    @Test
    void testEnumEquality() {
        Direction outbound1 = Direction.OUTBOUND;
        Direction outbound2 = Direction.OUTBOUND;
        Direction inbound = Direction.INBOUND;
        
        assertEquals(outbound1, outbound2);
        assertNotEquals(outbound1, inbound);
    }

    @Test
    void testEnumToString() {
        assertEquals("OUTBOUND", Direction.OUTBOUND.toString());
        assertEquals("INBOUND", Direction.INBOUND.toString());
    }
}
