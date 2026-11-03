package co.medellin.transit.ui.gui.view;

import co.medellin.transit.ui.gui.Theme;
import co.medellin.transit.ui.gui.kit.Paint;
import co.medellin.transit.ui.gui.state.AppState;
import co.medellin.transit.ui.gui.state.ScreenLayout;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** 48 px paper bar with the wordmark and a 1 px ink rule below (DESIGN.md 5.1). */
public final class TopBar extends co.medellin.transit.ui.gui.kit.Canvas {

    static final String PLACE = "Medellín · Valle de Aburrá";
    private ScreenLayout layout = ScreenLayout.WIDE;

    public TopBar() {
        getAccessibleContext().setAccessibleName("MetroBus, rutas de transporte público de Medellín");
    }

    public void render(AppState state) {
        if (state.layout() != layout) {
            layout = state.layout();
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 48);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Paint.smooth(g);
        int w = getWidth();
        int h = getHeight();
        int margin = MainLayout.margin(layout);
        g2.setColor(Theme.PAPER);
        g2.fillRect(0, 0, w, h);
        g2.setColor(Theme.INK);
        g2.fillRect(0, h - 1, w, 1);
        g2.fillRect(margin, (h - 20) / 2, 8, 20);
        g2.setFont(Theme.brand());
        FontMetrics fm = g2.getFontMetrics();
        int base = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString("MetroBus", margin + 16, base);
        if (layout != ScreenLayout.COMPACT) {
            g2.setFont(Theme.hint());
            g2.setColor(Theme.INK_2);
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(PLACE, w - margin - fm2.stringWidth(PLACE), (h - fm2.getHeight()) / 2 + fm2.getAscent());
        }
        g2.dispose();
    }
}
