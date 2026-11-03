package co.medellin.transit.ui.gui.view;

import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.gui.Theme;
import co.medellin.transit.ui.gui.kit.AddressField;
import co.medellin.transit.ui.gui.kit.InkButton;
import co.medellin.transit.ui.gui.kit.Paint;
import co.medellin.transit.ui.gui.kit.Text;
import co.medellin.transit.ui.gui.kit.SwapButton;
import co.medellin.transit.ui.gui.kit.TextLink;
import co.medellin.transit.ui.gui.state.AppController;
import co.medellin.transit.ui.gui.state.AppState;
import co.medellin.transit.ui.gui.state.Phase;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * "¿A dónde vas?": origin and destination joined by a rail, the search button,
 * the error line and the example trips.
 */
public final class TripForm extends JPanel {

    private final AppController controller;
    private final AddressField origin = new AddressField("Origen", "CL 10 43A-20");
    private final AddressField destination = new AddressField("Destino", "CL 77 65-30");
    private final SwapButton swap = new SwapButton();
    private final InkButton search = new InkButton("Buscar ruta", InkButton.Kind.PRIMARY,
            "Busca la ruta más rápida en transporte público");
    private final JPanel errorBox = new JPanel(new BorderLayout());
    private final JTextArea errorText = Text.wrapping("", Theme.body(), Theme.ERROR);
    private boolean syncing;
    private Phase lastPhase;

    public TripForm(AppController controller) {
        this.controller = controller;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel title = Text.title("¿A dónde vas?");
        add(left(title));
        add(Box.createVerticalStrut(Theme.S3));
        add(left(fields()));
        add(Box.createVerticalStrut(Theme.S3));
        add(left(search));
        add(Box.createVerticalStrut(Theme.S2));

        errorBox.setOpaque(false);
        errorBox.add(new Rule(), BorderLayout.WEST);
        errorText.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, Theme.S2 + 2, 2, 0));
        errorBox.add(errorText, BorderLayout.CENTER);
        errorBox.setVisible(false);
        add(left(errorBox));
        add(Box.createVerticalStrut(Theme.S4));

        add(left(Text.hint("Prueba con:")));
        for (int i = 0; i < AppController.EXAMPLES.length; i++) {
            String[] ex = AppController.EXAMPLES[i];
            TextLink link = new TextLink(ex[0] + " → " + ex[1], "Busca la ruta de este ejemplo");
            int index = i;
            link.addActionListener(e -> controller.pickExample(index));
            add(left(link));
        }

        DocumentListener push = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        };
        origin.getDocument().addDocumentListener(push);
        destination.getDocument().addDocumentListener(push);
        origin.addActionListener(e -> destination.requestFocusInWindow());
        destination.addActionListener(e -> controller.search());
        swap.addActionListener(e -> controller.swap());
        search.addActionListener(e -> controller.search());
    }

    private void changed() {
        if (!syncing) {
            controller.editTexts(origin.getText(), destination.getText());
        }
    }

    private JComponent fields() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 4;
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(18, 0, 0, Theme.S2);
        p.add(new RailMark(), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        p.add(Text.label("Origen"), c);
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(Theme.S1, 0, Theme.S2, 0);
        p.add(origin, c);
        c.gridy = 2;
        c.insets = new Insets(0, 0, Theme.S1, 0);
        p.add(Text.label("Destino"), c);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(destination, c);

        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 4;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(14, Theme.S2, 0, 0);
        p.add(swap, c);

        // Tab order follows the eye: origin, destination, swap, search
        p.setFocusTraversalPolicyProvider(false);
        return p;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    public void render(AppState s) {
        syncing = true;
        try {
            origin.setTextIfChanged(s.originText());
            destination.setTextIfChanged(s.destinationText());
        } finally {
            syncing = false;
        }

        boolean loading = s.phase() == Phase.LOADING;
        boolean searching = s.phase() == Phase.SEARCHING;
        boolean fatal = s.phase() == Phase.FATAL;
        if (loading) {
            search.setText(s.searchQueued() ? "Buscará al terminar de cargar…" : "Cargando la red…");
        } else if (searching) {
            search.setText("Buscando…");
        } else {
            search.setText("Buscar ruta");
        }
        search.setEnabled(s.canSearch());
        origin.setEnabled(!searching && !fatal);
        destination.setEnabled(!searching && !fatal);
        swap.setEnabled(!searching && !fatal);

        boolean showError = (s.phase() == Phase.ERROR || fatal) && s.errorMessage() != null;
        errorBox.setVisible(showError);
        errorText.setText(showError ? s.errorMessage() : "");
        origin.setError(showError && s.errorField() == PlanResult.Field.ORIGIN);
        destination.setError(showError && s.errorField() == PlanResult.Field.DESTINATION);
        String errorDesc = showError ? s.errorMessage() : null;
        origin.getAccessibleContext().setAccessibleDescription(
                origin.hasError() ? errorDesc : "Ejemplo: CL 10 43A-20");
        destination.getAccessibleContext().setAccessibleDescription(
                destination.hasError() ? errorDesc : "Ejemplo: CL 77 65-30");

        if (s.phase() == Phase.ERROR && lastPhase != Phase.ERROR) {
            if (destination.hasError()) {
                destination.requestFocusInWindow();
            } else {
                origin.requestFocusInWindow();
            }
        }
        lastPhase = s.phase();
        revalidate();
        repaint();
    }

    public void focusOrigin() {
        origin.requestFocusInWindow();
    }

    // For tests
    AddressField originField() {
        return origin;
    }

    AddressField destinationField() {
        return destination;
    }

    InkButton searchButton() {
        return search;
    }

    String errorText() {
        return errorBox.isVisible() ? errorText.getText() : "";
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /** Two dots joined by a line, like a metro line from origin to destination. */
    private static final class RailMark extends co.medellin.transit.ui.gui.kit.Canvas {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(12, 60);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Paint.smooth(g);
            int cx = getWidth() / 2;
            int top = 22 - 18 + 4;
            int bottom = getHeight() - 22;
            g2.setColor(Theme.INK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(cx, top, cx, bottom);
            g2.fillOval(cx - 5, top - 5, 10, 10);
            g2.setColor(Theme.SURFACE);
            g2.fillOval(cx - 5, bottom - 5, 10, 10);
            g2.setColor(Theme.INK);
            g2.drawOval(cx - 5, bottom - 5, 10, 10);
            g2.dispose();
        }
    }

    /** 2 px error rule at the left of the message. */
    private static final class Rule extends co.medellin.transit.ui.gui.kit.Canvas {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(2, 10);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.ERROR);
            g.fillRect(0, 0, 2, getHeight());
        }
    }
}
