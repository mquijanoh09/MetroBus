package co.medellin.transit.ui.gui.kit;

import co.medellin.transit.ui.gui.Theme;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Shared painting helpers: antialiasing and the focus ring (DESIGN.md 2.3). */
public final class Paint {

    private Paint() {
    }

    public static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /** 2 px ink ring drawn just inside the component's bounds. */
    public static void focusRing(Graphics2D g2, int width, int height) {
        g2.setColor(Theme.INK);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(1, 1, width - 3, height - 3);
    }
}
