package co.medellin.transit.ui.gui.state;

import co.medellin.transit.ui.PlanResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/** Each failure status has its DESIGN.md 5.4 message, in "tú" form. */
class MessagesTest {

    @ParameterizedTest
    @EnumSource(value = PlanResult.Status.class, names = "OK", mode = EnumSource.Mode.EXCLUDE)
    void everyFailureHasAMessageWithoutUsted(PlanResult.Status status) {
        String m = Messages.forFailure(PlanResult.failure(status, "x"), "CL 1");
        assertFalse(m.isBlank());
        assertFalse(m.toLowerCase().contains("usted"));
        assertFalse(m.contains("!"));
    }

    @ParameterizedTest
    @EnumSource(value = PlanResult.Status.class, names = {"NOT_FOUND", "OUT_OF_BOUNDS"})
    void outsideCoverageNamesTheAddress(PlanResult.Status status) {
        assertEquals("“CL 900 x CR 900” queda fuera del área que cubre MetroBus.",
                Messages.forFailure(PlanResult.failure(status, "x"), " CL 900 x CR 900 "));
    }
}
