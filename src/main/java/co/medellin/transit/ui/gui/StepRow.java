package co.medellin.transit.ui.gui;

import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * One step of the trip drawn as a point on a vertical timeline, with its
 * description and duration.
 */
final class StepRow extends JPanel {

    private static final int RAIL_WIDTH = 44;

    private final Color color;
    private final boolean first;
    private final boolean last;
    private final boolean riding;

    StepRow(Step step, boolean first, boolean last) {
        super(new BorderLayout(8, 0));
        this.color = colorFor(step.getType());
        this.first = first;
        this.last = last;
        this.riding = step.getType() == StepType.RIDE;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, RAIL_WIDTH, 6, 8));

        JLabel kind = new JLabel(kindLabel(step.getType()).toUpperCase());
        kind.setFont(Theme.font(Font.BOLD, 11));
        kind.setForeground(color);
        JLabel text = new JLabel("<html>" + escape(step.getDescription()) + "</html>");
        text.setFont(Theme.font(step.getType() == StepType.BOARD ? Font.BOLD : Font.PLAIN, 14));
        text.setForeground(Theme.TEXT);

        JPanel texts = new JPanel(new BorderLayout());
        texts.setOpaque(false);
        texts.add(kind, BorderLayout.NORTH);
        texts.add(text, BorderLayout.CENTER);
        add(texts, BorderLayout.CENTER);

        if (step.getDurationSec() > 0) {
            JLabel minutes = new JLabel(Theme.duration(step.getDurationSec()));
            minutes.setFont(Theme.font(Font.BOLD, 13));
            minutes.setForeground(Theme.TEXT_MUTED);
            add(minutes, BorderLayout.EAST);
        }
    }

    static Color colorFor(StepType type) {
        return switch (type) {
            case WALK -> Theme.WALK;
            case BOARD, RIDE -> Theme.PRIMARY;
            case ALIGHT -> Theme.ALIGHT;
        };
    }

    static String kindLabel(StepType type) {
        return switch (type) {
            case WALK -> "Caminar";
            case BOARD -> "Abordar";
            case RIDE -> "Viajar";
            case ALIGHT -> "Bajar";
        };
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = RAIL_WIDTH / 2;
        int cy = 18;
        int h = getHeight();

        g2.setColor(riding ? color : Theme.BORDER);
        g2.setStroke(new BasicStroke(riding ? 6f : 3f));
        if (!first) {
            g2.drawLine(cx, 0, cx, cy);
        }
        if (!last) {
            g2.drawLine(cx, cy, cx, h);
        }

        int r = riding ? 5 : 8;
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
        g2.dispose();
    }
}
