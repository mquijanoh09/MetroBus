package co.medellin.transit.ui.gui.view;

import co.medellin.transit.ui.gui.Theme;
import co.medellin.transit.ui.gui.model.RouteCatalog;
import co.medellin.transit.ui.gui.state.AppController;
import co.medellin.transit.ui.gui.state.AppState;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Left (or full-width) column: the form, or the result once a route is found.
 * Scrolls vertically on its own; text wraps to the column width.
 */
public final class TripPanel extends JPanel {

    private final TripForm form;
    private final TripResult result;
    private final Column column = new Column();
    private final JScrollPane scroll;

    public TripPanel(AppController controller, RouteCatalog catalog) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(Theme.PAPER);
        form = new TripForm(controller);
        result = new TripResult(controller, catalog);
        column.setLayout(new BorderLayout());
        column.setOpaque(false);
        column.add(form, BorderLayout.NORTH);
        scroll = new JScrollPane(column, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.PAPER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    public void render(AppState s) {
        int m = MainLayout.margin(s.layout());
        column.setBorder(BorderFactory.createEmptyBorder(m, m, m, m));
        form.render(s);
        result.render(s);
        java.awt.Component wanted = s.showsResult() ? result : form;
        if (column.getComponentCount() == 0 || column.getComponent(0) != wanted) {
            column.removeAll();
            column.add(wanted, BorderLayout.NORTH);
            column.revalidate();
            column.repaint();
            scroll.getVerticalScrollBar().setValue(0);
            if (wanted == form && s.canSearch()) {
                form.focusOrigin();
            }
        }
    }

    public TripForm form() {
        return form;
    }

    public TripResult result() {
        return result;
    }

    /** Tracks the viewport width so wrapped text reflows when the window changes size. */
    private static final class Column extends JPanel implements Scrollable {
        Column() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return r.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
