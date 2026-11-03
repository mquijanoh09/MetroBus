package co.medellin.transit.ui;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareConfig;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Step;
import co.medellin.transit.routing.Parameters;
import co.medellin.transit.routing.Router;
import co.medellin.transit.routing.StepBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests TripPlanner against the real Medellín data in ./data (no mocks).
 */
class TripPlannerTest {

    private static TripPlanner planner;

    private static String previousDataDirProperty;

    @BeforeAll
    static void loadRealNetwork() throws Exception {
        Path dataDir = Path.of("data");
        previousDataDirProperty = System.getProperty("transit.data.dir");
        System.setProperty("transit.data.dir", dataDir.toString());
        TransitGraph graph = new GraphBuilder().build(dataDir);
        planner = new TripPlanner(
                new Router(graph, new Parameters()),
                new AddressParser(),
                new AddressLocator(),
                new FareCalculator(FareConfig.defaults(), graph),
                new StepBuilder(graph));
    }

    @AfterAll
    static void restoreProperty() {
        if (previousDataDirProperty == null) {
            System.clearProperty("transit.data.dir");
        } else {
            System.setProperty("transit.data.dir", previousDataDirProperty);
        }
    }

    /** The four example trips offered in the window must all find a route. */
    @ParameterizedTest
    @CsvSource({
            "CL 10 43A-20, CL 77 65-30",
            "CL 50 53-48, CR 70 x CL 48",
            "CR 80 32-15, CL 51 46-10",
            "CL 67 43-26, CL 71 65-173"
    })
    void exampleTripsFindARoute(String origin, String destination) {
        PlanResult result = planner.plan(origin, destination);

        assertTrue(result.isSuccess(), () -> "Expected a route, got: " + result.getMessage());
        assertFalse(result.getTrip().getSteps().isEmpty());
        assertTrue(result.getTrip().getTotalDurationSec() > 0);
        assertTrue(result.getFare().getTotalCOP() > 0, "A trip that uses transit must cost something");
        assertEquals(origin, result.getOrigin().getName());
        assertEquals(destination, result.getDestination().getName());
    }

    @Test
    void metroTripUsesLineA() {
        PlanResult result = planner.plan("CL 10 43A-20", "CL 77 65-30");

        assertTrue(result.isSuccess(), result.getMessage());
        boolean usesLineA = result.getTrip().getSteps().stream()
                .map(Step::getRouteInfo)
                .anyMatch(info -> info.isPresent() && info.get().equals("A"));
        assertTrue(usesLineA, "Expected the trip to ride Metro line A");
        assertEquals(3820, result.getFare().getTotalCOP());
    }

    @Test
    void emptyOriginIsRejected() {
        PlanResult result = planner.plan("   ", "CL 77 65-30");
        assertEquals(PlanResult.Status.EMPTY_INPUT, result.getStatus());
        assertTrue(result.getMessage().contains("origen"));
    }

    @Test
    void emptyDestinationIsRejected() {
        PlanResult result = planner.plan("CL 10 43A-20", null);
        assertEquals(PlanResult.Status.EMPTY_INPUT, result.getStatus());
        assertTrue(result.getMessage().contains("destino"));
    }

    @Test
    void textThatIsNotAnAddressIsRejected() {
        PlanResult result = planner.plan("hola", "CL 10 43A-20");
        assertEquals(PlanResult.Status.INVALID_FORMAT, result.getStatus());
        assertTrue(result.getMessage().contains("origen"));
        assertNull(result.getTrip());
    }

    @Test
    void invalidDestinationNamesTheDestination() {
        PlanResult result = planner.plan("CL 10 43A-20", "hola");
        assertEquals(PlanResult.Status.INVALID_FORMAT, result.getStatus());
        assertTrue(result.getMessage().contains("destino"));
    }

    @Test
    void sameAddressIsTooClose() {
        PlanResult result = planner.plan("CL 10 43A-20", "CL 10 43A-20");
        assertEquals(PlanResult.Status.TOO_CLOSE, result.getStatus());
    }

    /** Every example in the help dialog and the planner hint must be a valid address. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Calle 10 # 43A - 20",
            "Carrera 43A # 1 Sur - 100",
            "CL 50 53-48",
            "CR 70 x CL 48",
            "Calle 10 # 50 - 30",
            "CL 50 x CR 70"
    })
    void helpExamplesAreValidAddresses(String address) {
        assertTrue(new AddressParser().parse(address).isPresent(), "Help shows an address the parser rejects: " + address);
    }
}
