package co.medellin.transit.ui.gui;

import co.medellin.transit.ui.TripPlanner;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The application window. It opens on the home screen right away, while the
 * transport network loads in the background, and then lets the user move
 * between the home screen and the trip planner.
 *
 * <p>All public methods may be called from any thread; they hop onto the
 * Swing event thread themselves.</p>
 */
public final class MainWindow {

    static final String TITLE = "MetroBus Medellín";
    private static final String HOME = "home";
    private static final String PLANNER = "planner";

    private final JFrame frame;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final HomePanel homePanel;
    private PlannerPanel plannerPanel;

    private MainWindow() {
        frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(820, 600));
        frame.setSize(new Dimension(1000, 720));
        frame.setLocationRelativeTo(null);

        homePanel = new HomePanel(this::showPlanner, this::showHelp, () -> System.exit(0));
        content.add(homePanel, HOME);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new HeaderBar(), BorderLayout.NORTH);
        frame.getContentPane().add(content, BorderLayout.CENTER);
    }

    /**
     * Creates the window and shows the home screen in its loading state.
     * Blocks until the window is visible.
     */
    public static MainWindow open() {
        AtomicReference<MainWindow> ref = new AtomicReference<>();
        runOnEdtAndWait(() -> {
            Theme.installLookAndFeel();
            MainWindow window = new MainWindow();
            window.frame.setVisible(true);
            ref.set(window);
        });
        return ref.get();
    }

    /** Switches the home screen to its ready state once the network is loaded. */
    public void showReady(TripPlanner planner, String networkSummary) {
        SwingUtilities.invokeLater(() -> {
            plannerPanel = new PlannerPanel(planner, this::showHome);
            content.add(plannerPanel, PLANNER);
            homePanel.setReady(networkSummary);
        });
    }

    /**
     * Shows a fatal error in a dialog and blocks until the user closes it.
     * The caller decides whether to exit afterwards.
     */
    public void showFatalError(String title, String detail) {
        runOnEdtAndWait(() -> {
            homePanel.setFailed(title);
            JOptionPane.showMessageDialog(frame, detail, title, JOptionPane.ERROR_MESSAGE);
        });
    }

    void showPlanner() {
        if (plannerPanel != null) {
            cards.show(content, PLANNER);
            plannerPanel.focusOrigin();
        }
    }

    void showHome() {
        cards.show(content, HOME);
    }

    void showHelp() {
        JOptionPane.showMessageDialog(frame, HelpText.ADDRESSES, "Cómo escribir las direcciones",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** For tests: the frame being shown. */
    JFrame frame() {
        return frame;
    }

    /** For tests: the planner screen, or null while loading. */
    PlannerPanel plannerPanel() {
        return plannerPanel;
    }

    /** For tests: the home screen. */
    HomePanel homePanel() {
        return homePanel;
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
