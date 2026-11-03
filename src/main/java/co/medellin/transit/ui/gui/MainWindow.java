package co.medellin.transit.ui.gui;

import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.TripPlanner;
import co.medellin.transit.ui.gui.model.LegBuilder;
import co.medellin.transit.ui.gui.model.NetworkLines;
import co.medellin.transit.ui.gui.model.RouteCatalog;
import co.medellin.transit.ui.gui.model.TransitMode;
import co.medellin.transit.ui.gui.state.AppController;
import co.medellin.transit.ui.gui.state.AppState;
import co.medellin.transit.ui.gui.state.AppStore;
import co.medellin.transit.ui.gui.state.ScreenLayout;
import co.medellin.transit.ui.gui.view.MainLayout;
import co.medellin.transit.ui.gui.view.NetworkMap;
import co.medellin.transit.ui.gui.view.TopBar;
import co.medellin.transit.ui.gui.view.TripPanel;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * The application window. It wires the store, the controller and the views,
 * opens before the graph loads, and lays the views out for the window width.
 *
 * <p>Public methods may be called from any thread.</p>
 */
public final class MainWindow {

    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
    static final String TITLE = "MetroBus · Medellín";

    private final JFrame frame;
    private final JPanel root = new JPanel(new BorderLayout());
    private final JPanel body = new JPanel(new BorderLayout());
    private final TopBar topBar = new TopBar();
    private final NetworkMap map = new NetworkMap();
    private final TripPanel tripPanel;
    private final AppController controller;
    private final RouteCatalog catalog;
    private final ExecutorService background = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "metrobus-search");
        t.setDaemon(true);
        return t;
    });
    private ScreenLayout appliedLayout;
    private boolean appliedExpanded;

    private MainWindow(Path dataDir) {
        AppStore store = new AppStore(AppState.initial(), SwingUtilities::invokeLater);
        controller = new AppController(store, background);
        catalog = new RouteCatalog(dataDir);
        tripPanel = new TripPanel(controller, catalog);

        frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(400, 560));
        frame.setSize(new Dimension(1120, 720));
        frame.setLocationRelativeTo(null);
        frame.getAccessibleContext().setAccessibleDescription(
                "Planifica viajes en transporte público en Medellín");

        root.setBackground(Theme.PAPER);
        body.setBackground(Theme.PAPER);
        root.add(topBar, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        frame.setContentPane(root);
        installKeys();

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                controller.resized(root.getWidth());
            }
        });
        store.subscribe(this::render);
    }

    /** Creates the window, shows it and starts reading the map lines. Blocks until visible. */
    public static MainWindow open(Path dataDir) {
        long start = System.nanoTime();
        AtomicReference<MainWindow> ref = new AtomicReference<>();
        runOnEdtAndWait(() -> {
            Theme.install();
            MainWindow w = new MainWindow(dataDir);
            w.frame.setVisible(true);
            w.controller.resized(w.root.getWidth());
            ref.set(w);
        });
        MainWindow window = ref.get();
        LOGGER.info(String.format("Window visible in %d ms", (System.nanoTime() - start) / 1_000_000));
        try {
            long t = System.nanoTime();
            NetworkLines lines = NetworkLines.load(dataDir);
            window.controller.networkLoaded(lines);
            LOGGER.info(String.format("Map lines loaded: %d lines in %d ms", lines.lines().size(),
                    (System.nanoTime() - t) / 1_000_000));
        } catch (Exception e) {
            LOGGER.warning("Could not read transit_lines.csv for the map: " + e.getMessage());
        }
        return window;
    }

    /** The graph is ready; searching is enabled (and a queued search runs). */
    public void showReady(TripPlanner planner, String networkSummary) {
        LOGGER.info("Planner ready. " + networkSummary);
        controller.plannerReady((origin, destination) -> {
            long t = System.nanoTime();
            PlanResult result = planner.plan(origin, destination);
            if (result.isSuccess()) {
                // Lazy: read the bus catalog off the UI thread the first time a bus appears
                LegBuilder.legs(result.getTrip()).stream()
                        .filter(l -> l.mode() == TransitMode.BUS)
                        .forEach(l -> catalog.find(l.routeCode()));
            }
            LOGGER.info(String.format("Search finished in %d ms: %s", (System.nanoTime() - t) / 1_000_000,
                    result.getStatus()));
            return result;
        });
    }

    /** Shows a fatal error in the window and in a dialog; blocks until the dialog closes. */
    public void showFatalError(String title, String detail) {
        controller.loadFailed(title + ". " + detail.replace('\n', ' '));
        runOnEdtAndWait(() -> JOptionPane.showMessageDialog(frame, detail, title, JOptionPane.ERROR_MESSAGE));
    }

    private void render(AppState s) {
        topBar.render(s);
        if (s.layout() != appliedLayout || s.mapExpanded() != appliedExpanded) {
            applyLayout(s.layout(), s.mapExpanded());
        }
        tripPanel.render(s);
        map.render(s);
    }

    /** Rearranges map and panel for the layout (DESIGN.md 4.1). */
    private void applyLayout(ScreenLayout layout, boolean expanded) {
        appliedLayout = layout;
        appliedExpanded = expanded;
        body.removeAll();
        if (layout == ScreenLayout.COMPACT) {
            int h = expanded ? Math.max(MainLayout.COMPACT_MAP_HEIGHT, (root.getHeight() - 48) / 2)
                    : MainLayout.COMPACT_MAP_HEIGHT;
            map.setPreferredSize(new Dimension(10, h));
            body.add(map, BorderLayout.NORTH);
            body.add(tripPanel, BorderLayout.CENTER);
        } else {
            tripPanel.setPreferredSize(new Dimension(MainLayout.panelWidth(layout), 10));
            body.add(tripPanel, BorderLayout.WEST);
            body.add(map, BorderLayout.CENTER);
        }
        body.revalidate();
        body.repaint();
    }

    private void installKeys() {
        JComponent c = root;
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK), "swap");
        c.getActionMap().put("swap", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.swap();
            }
        });
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "edit");
        c.getActionMap().put("edit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.edit();
            }
        });
    }

    // For tests and screenshots
    JFrame frame() {
        return frame;
    }

    AppController controller() {
        return controller;
    }

    TripPanel tripPanel() {
        return tripPanel;
    }

    NetworkMap map() {
        return map;
    }

    private static void runOnEdtAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not update the window", e.getCause());
        }
    }
}
