package co.medellin.transit.ui.gui;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareConfig;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.StepType;
import co.medellin.transit.routing.Parameters;
import co.medellin.transit.routing.Router;
import co.medellin.transit.routing.StepBuilder;
import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.TripPlanner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Builds the Swing panels without opening a window (works on machines with no
 * display too) and checks what they show for real trips.
 */
class GuiPanelsTest {

    private static TripPlanner planner;

    private static String previousDataDirProperty;

    @BeforeAll
    static void loadRealNetwork() throws Exception {
        Path dataDir = Path.of("data");
        previousDataDirProperty = System.getProperty("transit.data.dir");
        System.setProperty("transit.data.dir", dataDir.toString());
        TransitGraph graph = new GraphBuilder().build(dataDir);
        planner = new TripPlanner(new Router(graph, new Parameters()), new AddressParser(),
                new AddressLocator(), new FareCalculator(FareConfig.defaults(), graph), new StepBuilder(graph));
    }

    @AfterAll
    static void restoreProperty() {
        if (previousDataDirProperty == null) {
            System.clearProperty("transit.data.dir");
        } else {
            System.setProperty("transit.data.dir", previousDataDirProperty);
        }
    }

    @Test
    void homeStartsLoadingAndBecomesReady() {
        HomePanel home = new HomePanel(() -> { }, () -> { }, () -> { });
        assertFalse(home.isPlanEnabled(), "Cannot plan before the network is loaded");
        assertTrue(home.statusText().startsWith("Cargando"));

        home.setReady("Red cargada.");
        assertTrue(home.isPlanEnabled());
        assertEquals("Listo. Red cargada.", home.statusText());
    }

    @Test
    void homeShowsFailure() {
        HomePanel home = new HomePanel(() -> { }, () -> { }, () -> { });
        home.setFailed("No se pudieron cargar los datos de transporte");
        assertFalse(home.isPlanEnabled());
        assertEquals("No se pudieron cargar los datos de transporte", home.statusText());
    }

    @Test
    void planButtonOpensPlanner() {
        boolean[] opened = {false};
        HomePanel home = new HomePanel(() -> opened[0] = true, () -> { }, () -> { });
        home.setReady("ok");
        home.planButton().doClick();
        assertTrue(opened[0]);
    }

    @Test
    void plannerShowsSummaryAndEveryStepOfARealTrip() {
        PlannerPanel panel = new PlannerPanel(planner, () -> { });
        PlanResult result = planner.plan("CL 10 43A-20", "CL 77 65-30");
        assertTrue(result.isSuccess(), result.getMessage());

        panel.show(result);

        String text = String.join("\n", labels(panel));
        assertTrue(text.contains("Ruta encontrada"));
        assertTrue(text.contains(Theme.pesos(result.getFare().getTotalCOP())), text);
        assertTrue(text.contains(Theme.duration(result.getTrip().getTotalDurationSec())), text);
        result.getTrip().getSteps().forEach(step ->
                assertTrue(text.contains(step.getDescription()), "Missing step: " + step.getDescription()));
        assertSame(result, panel.lastResult());
    }

    @Test
    void plannerShowsErrorMessage() {
        PlannerPanel panel = new PlannerPanel(planner, () -> { });
        PlanResult result = planner.plan("hola", "CL 77 65-30");

        panel.show(result);

        String text = String.join("\n", labels(panel));
        assertTrue(text.contains("no tiene un formato válido"), text);
        assertFalse(text.contains("Ruta encontrada"));
    }

    @Test
    void examplesInThePlannerAllWork() {
        for (String[] example : PlannerPanel.EXAMPLES) {
            PlanResult result = planner.plan(example[0], example[1]);
            assertTrue(result.isSuccess(), example[0] + " > " + example[1] + ": " + result.getMessage());
        }
    }

    @Test
    void formattingMatchesColombianConventions() {
        assertEquals("$3.800", Theme.pesos(3800));
        assertEquals("$11.420", Theme.pesos(11420));
        assertEquals("0 min", Theme.duration(0));
        assertEquals("2 min", Theme.duration(61));
        assertEquals("1 h 16 min", Theme.duration(76 * 60));
        assertEquals("349 m", Theme.meters(349.4));
        assertEquals("1,5 km", Theme.meters(1500));
    }

    @Test
    void everyStepTypeHasALabel() {
        for (StepType type : StepType.values()) {
            assertNotNull(StepRow.colorFor(type));
            assertFalse(StepRow.kindLabel(type).isBlank());
        }
    }

    private static List<String> labels(Container root) {
        List<String> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel label && label.getText() != null) {
                out.add(label.getText().replaceAll("<[^>]+>", "").replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&"));
            }
            if (c instanceof Container child) {
                out.addAll(labels(child));
            }
        }
        return out;
    }
}
