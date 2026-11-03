package co.medellin.transit.ui.gui.view;

import co.medellin.transit.ui.gui.Theme;
import co.medellin.transit.ui.gui.kit.Paint;
import co.medellin.transit.ui.gui.kit.Text;
import co.medellin.transit.ui.gui.model.Format;
import co.medellin.transit.ui.gui.model.Leg;
import co.medellin.transit.ui.gui.model.RouteCatalog;
import co.medellin.transit.ui.gui.model.TransitMode;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** One leg: badge or dotted walk mark, main text, secondary text, minutes. */
final class LegRow extends JPanel {

    private final String mainText;
    private final String secondaryText;
    private final String badgeCode;

    LegRow(Leg leg, RouteCatalog catalog) {
        super(new BorderLayout(Theme.S2 + 4, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Theme.S2, 0, Theme.S2, 0));

        if (leg.isWalk()) {
            mainText = "Camina a " + Format.place(leg.toName());
            secondaryText = null;
            badgeCode = null;
        } else {
            mainText = "De " + Format.place(leg.fromName()) + " a " + Format.place(leg.toName());
            if (leg.mode() == TransitMode.BUS) {
                var label = catalog.find(leg.routeCode());
                badgeCode = label.map(RouteCatalog.RouteLabel::code).orElse(leg.routeCode());
                secondaryText = label.filter(l -> !l.name().isBlank())
                        .map(l -> "Bus · " + l.name()).orElse("Bus");
            } else {
                badgeCode = leg.routeCode();
                secondaryText = leg.mode().label() + " línea " + leg.routeCode();
            }
        }

        JComponent mark = leg.isWalk() ? new WalkMark() : new LineBadge(badgeCode, leg.mode());
        JPanel markBox = new JPanel(new BorderLayout());
        markBox.setOpaque(false);
        markBox.setPreferredSize(new Dimension(Math.max(36, mark.getPreferredSize().width), 22));
        markBox.add(mark, BorderLayout.NORTH);
        add(markBox, BorderLayout.WEST);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JTextArea main = Text.wrapping(mainText, Theme.body(), Theme.INK);
        main.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(main);
        if (secondaryText != null) {
            JTextArea sec = Text.wrapping(secondaryText, Theme.hint(), Theme.INK_2);
            sec.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(sec);
        }
        add(texts, BorderLayout.CENTER);

        JLabel minutes = Text.styled(Format.duration(leg.durationSec()), Theme.figureSmall().deriveFont(15f),
                Theme.INK_2);
        minutes.setVerticalAlignment(JLabel.TOP);
        add(minutes, BorderLayout.EAST);

        getAccessibleContext().setAccessibleName(mainText
                + (secondaryText == null ? "" : ", " + secondaryText) + ", " + Format.durationSpoken(leg.durationSec()));
    }

    String mainText() {
        return mainText;
    }

    String secondaryText() {
        return secondaryText;
    }

    String badgeCode() {
        return badgeCode;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /** Dotted vertical walk mark. */
    private static final class WalkMark extends co.medellin.transit.ui.gui.kit.Canvas {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(28, 22);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Paint.smooth(g);
            g2.setColor(Theme.WALK);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[]{0.1f, 6f}, 0f));
            g2.drawLine(14, 3, 14, getHeight() - 3);
            g2.dispose();
        }
    }
}
