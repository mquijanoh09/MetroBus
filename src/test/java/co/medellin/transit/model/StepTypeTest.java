package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StepTypeTest {

    @Test
    void testEnumValues() {
        StepType[] values = StepType.values();
        
        assertEquals(4, values.length);
        assertEquals(StepType.WALK, values[0]);
        assertEquals(StepType.BOARD, values[1]);
        assertEquals(StepType.RIDE, values[2]);
        assertEquals(StepType.ALIGHT, values[3]);
    }

    @Test
    void testValueOf() {
        assertEquals(StepType.WALK, StepType.valueOf("WALK"));
        assertEquals(StepType.BOARD, StepType.valueOf("BOARD"));
        assertEquals(StepType.RIDE, StepType.valueOf("RIDE"));
        assertEquals(StepType.ALIGHT, StepType.valueOf("ALIGHT"));
    }

    @Test
    void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            StepType.valueOf("INVALID");
        });
    }

    @Test
    void testEnumEquality() {
        StepType walk1 = StepType.WALK;
        StepType walk2 = StepType.valueOf("WALK");
        
        assertSame(walk1, walk2);
        assertEquals(walk1, walk2);
    }

    @Test
    void testEnumSwitchCoverage() {
        // Verify all enum values can be used in switch statements
        for (StepType type : StepType.values()) {
            String result = switch (type) {
                case WALK -> "walking";
                case BOARD -> "boarding";
                case RIDE -> "riding";
                case ALIGHT -> "alighting";
            };
            assertNotNull(result);
        }
    }
}
