package co.medellin.transit.ui.gui.view;

import co.medellin.transit.model.Trip;
import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.gui.Theme;
import co.medellin.transit.ui.gui.kit.Text;
import co.medellin.transit.ui.gui.kit.TextLink;
import co.medellin.transit.ui.gui.model.Format;
import co.medellin.transit.ui.gui.model.Leg;
import co.medellin.transit.ui.gui.model.LegBuilder;
import co.medellin.transit.ui.gui.model.RouteCatalog;
import co.medellin.transit.ui.gui.state.AppController;
import co.medellin.transit.ui.gui.state.AppState;
import co.medellin.transit.ui.gui.state.ScreenLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

/**
 * The found route: header with "Editar", the big duration, the summary line
 * and the legs (DESIGN.md, section 5.3).
 */
public final class TripResult extends JPanel {

    private final AppController controller;
    private final RouteCatalog catalog;
    private PlanResult shown;
    private ScreenLayout shownLayout;
    private boolean shownExpanded;
    private JLabel figure;
    private List<Leg> legs = List.of();

    public TripResult(AppController controller, RouteCatalog catalog) {
        this.controller = controller;
        this.catalog = catalog;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void render(AppState s) {
        if (!s.showsResult()) {
            shown = null;
            return;
        }
        if (s.result() == shown && s.layout() == shownLayout && s.mapExpanded() == shownExpanded) {
            return;
        }
        boolean isNew = s.result() != shown;
        shown = s.result();
        shownLayout = s.layout();
        shownExpanded = s.mapExpanded();
        build(s);
        if (isNew) {
            figure.requestFocusInWindow();
        }
    }

    private void build(AppState s) {
        removeAll();
        PlanResult r = s.result();
        Trip trip = r.getTrip();
        legs = LegBuilder.legs(trip);

        JPanel header = new JPanel(new BorderLayout(Theme.S2, 0));
        header.setOpaque(false);
        JTextArea route = Text.wrapping(r.getOrigin().getName() + " → " + r.getDestination().getName(),
                Theme.hint(), Theme.INK_2);
        header.add(route, BorderLayout.CENTER);
        JPanel links = new JPanel();
        links.setOpaque(false);
        links.setLayout(new BoxLayout(links, BoxLayout.X_AXIS));
        if (s.layout() == ScreenLayout.COMPACT) {
            TextLink map = new TextLink(s.mapExpanded() ? "Reducir mapa" : "Ver mapa",
                    "Cambia el tamaño del mapa");
            map.addActionListener(e -> controller.toggleMap());
            links.add(map);
        }
        TextLink edit = new TextLink("Editar", "Vuelve al formulario sin borrar las direcciones. Atajo: Escape");
        edit.addActionListener(e -> controller.edit());
        links.add(edit);
        header.add(links, BorderLayout.EAST);
        add(left(header));
        add(Box.createVerticalStrut(Theme.S3));

        figure = Text.styled(Format.duration(trip.getTotalDurationSec()), Theme.figure(), Theme.INK);
        figure.setFocusable(true);
        figure.getAccessibleContext().setAccessibleName(spokenSummary(r, legs));
        add(left(figure));

        int transfers = LegBuilder.transfers(legs);
        // Non-breaking spaces inside each item, so lines only break between items
        List<String> items = new java.util.ArrayList<>(List.of(
                Format.pesos(r.getFare().getTotalCOP()),
                Format.meters(trip.getTotalWalkMeters()) + " a pie",
                Format.transfers(transfers)));
        if (r.getFare().isIntegrationApplied()) {
            items.add("integración aplicada");
        }
        String line = String.join(" · ", items.stream().map(i -> i.replace(' ', ' ')).toList());
        JTextArea summary = Text.wrapping(line, Theme.figureSmall(), Theme.INK);
        add(left(summary));
        add(Box.createVerticalStrut(Theme.S3));
        add(left(new HRule()));
        add(Box.createVerticalStrut(Theme.S2));

        for (Leg leg : legs) {
            add(left(new LegRow(leg, catalog)));
        }
        add(Box.createVerticalStrut(Theme.S3));
        add(left(Text.hint("Tarifa con tarjeta Cívica.")));
        revalidate();
        repaint();
    }

    static String spokenSummary(PlanResult r, List<Leg> legs) {
        int t = LegBuilder.transfers(legs);
        return "Ruta encontrada: " + Format.durationSpoken(r.getTrip().getTotalDurationSec()) + ", "
                + r.getFare().getTotalCOP() + " pesos, " + Format.transfers(t) + ".";
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    // For tests
    List<Leg> legs() {
        return legs;
    }

    JLabel figure() {
        return figure;
    }

    /** 1 px divider. */
    static final class HRule extends co.medellin.transit.ui.gui.kit.Canvas {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(10, 1);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 1);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            g.setColor(Theme.RULE);
            g.fillRect(0, 0, getWidth(), 1);
        }
    }
}
