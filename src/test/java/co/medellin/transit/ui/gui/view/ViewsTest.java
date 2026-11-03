package co.medellin.transit.ui.gui.view;

import co.medellin.transit.ui.TripPlanner;
import co.medellin.transit.ui.gui.RealNetwork;
import co.medellin.transit.ui.gui.model.RouteCatalog;
import co.medellin.transit.ui.gui.state.AppController;
import co.medellin.transit.ui.gui.state.AppState;
import co.medellin.transit.ui.gui.state.AppStore;
import co.medellin.transit.ui.gui.state.Phase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Builds the real views without a window (works on machines with no display)
 * and checks what they show and what screen readers get.
 */
class ViewsTest {

    private static TripPlanner planner;
    private AppStore store;
    private AppController controller;
    private TripPanel panel;

    @BeforeAll
    static void load() throws Exception {
        planner = RealNetwork.planner();
    }

    @BeforeEach
    void setUp() {
        store = new AppStore(AppState.initial(), Runnable::run);
        controller = new AppController(store, Runnable::run);
        panel = new TripPanel(controller, new RouteCatalog(RealNetwork.DATA));
        store.subscribe(panel::render);
    }

    @Test
    void loadingDisablesSearchButKeepsFieldsUsable() {
        assertEquals("Cargando la red…", panel.form().searchButton().getText());
        assertFalse(panel.form().searchButton().isEnabled());
        assertTrue(panel.form().originField().isEnabled());
    }

    @Test
    void queuedSearchIsAnnounced() {
        controller.editTexts("CL 10 43A-20", "CL 77 65-30");
        controller.search();
        assertEquals("Buscará al terminar de cargar…", panel.form().searchButton().getText());
    }

    @Test
    void typingUpdatesTheState() {
        panel.form().originField().setText("CL 50 53-48");
        assertEquals("CL 50 53-48", store.state().originText());
    }

    @Test
    void metroResultShowsGroupedLegsAndSummary() {
        controller.plannerReady(planner::plan);
        controller.editTexts("CL 10 43A-20", "CL 77 65-30");
        controller.search();
        assertEquals(Phase.RESULT, store.state().phase());

        TripResult result = panel.result();
        String text = String.join("\n", texts(result));
        assertTrue(text.contains("De Poblado a Caribe"), text);
        assertTrue(text.contains("Metro línea A"), text);
        assertTrue(text.contains("$3.820"), text);
        assertTrue(text.contains("sin transbordos"), text);
        assertFalse(text.contains("Caminar a"), "Uses the imperative 'Camina', not the backend's infinitive");
        assertTrue(text.contains("Camina a Poblado"), text);
        assertTrue(result.figure().getAccessibleContext().getAccessibleName().startsWith("Ruta encontrada: "));
    }

    @Test
    void busResultShowsPublicRouteCodeNotInternalId() {
        controller.plannerReady(planner::plan);
        controller.editTexts("CR 82 x CL 51B", "CL 57A 30-87");
        controller.search();
        List<LegRow> rows = find(panel.result(), LegRow.class);
        LegRow bus = rows.stream().filter(r -> r.badgeCode() != null).findFirst().orElseThrow();
        assertEquals("310", bus.badgeCode());
        assertEquals("Bus · Directa Circular", bus.secondaryText());
    }

    @Test
    void errorShowsMessageUnderTheForm() {
        controller.plannerReady(planner::plan);
        controller.editTexts("hola", "CL 77 65-30");
        controller.search();
        assertTrue(panel.form().errorText().startsWith("No reconocemos “hola”"));
        assertTrue(panel.form().originField().hasError());
        assertFalse(panel.form().destinationField().hasError());
    }

    @Test
    void editReturnsToTheFormWithTextsKept() {
        controller.plannerReady(planner::plan);
        controller.pickExample(1);
        controller.edit();
        assertEquals(AppController.EXAMPLES[1][0], panel.form().originField().getText());
        assertEquals("", panel.form().errorText());
    }

    @Test
    void everyFocusableControlHasAnAccessibleName() {
        controller.plannerReady(planner::plan);
        assertAllNamed(panel.form());
        controller.pickExample(0);
        assertAllNamed(panel.result());
        assertNotNull(new NetworkMap().getAccessibleContext().getAccessibleName());
        assertNotNull(new TopBar().getAccessibleContext().getAccessibleName());
    }

    private static void assertAllNamed(Container root) {
        for (Component c : all(root)) {
            if (c.isFocusable() && c instanceof javax.swing.JComponent jc && !(c instanceof JTextArea)
                    && !(c instanceof javax.swing.JScrollBar) && !(c instanceof javax.swing.JViewport)
                    && !(c instanceof javax.swing.JPanel) && !(c instanceof javax.swing.JScrollPane)) {
                String name = jc.getAccessibleContext().getAccessibleName();
                assertTrue(name != null && !name.isBlank(), "No accessible name: " + c.getClass().getSimpleName());
            }
        }
    }

    private static List<String> texts(Container root) {
        List<String> out = new ArrayList<>();
        for (Component c : all(root)) {
            if (c instanceof JLabel l && l.getText() != null) {
                out.add(l.getText());
            } else if (c instanceof JTextArea a) {
                out.add(a.getText());
            } else if (c instanceof LegRow r) {
                out.add(r.mainText());
                if (r.secondaryText() != null) {
                    out.add(r.secondaryText());
                }
            }
        }
        return out;
    }

    private static <T> List<T> find(Container root, Class<T> type) {
        return all(root).stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static List<Component> all(Container root) {
        List<Component> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            out.add(c);
            if (c instanceof Container k) {
                out.addAll(all(k));
            }
        }
        return out;
    }
}
