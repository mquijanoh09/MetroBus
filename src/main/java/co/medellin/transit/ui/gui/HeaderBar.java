package co.medellin.transit.ui.gui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Green bar at the top of the window with the app's name and a small logo.
 */
final class HeaderBar extends JComponent {

    HeaderBar() {
        setPreferredSize(new Dimension(800, 64));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY_DARK, w, 0, Theme.PRIMARY));
        g2.fillRect(0, 0, w, h);
        g2.setColor(Theme.ACCENT);
        g2.fillRect(0, h - 4, w, 4);

        paintLogo(g2, 20, (h - 4 - 36) / 2, 36);

        g2.setColor(java.awt.Color.WHITE);
        g2.setFont(Theme.font(Font.BOLD, 22));
        g2.drawString(MainWindow.TITLE, 68, h / 2 + 4);
        g2.setFont(Theme.font(Font.PLAIN, 13));
        String tagline = "Rutas de transporte público";
        int tw = g2.getFontMetrics().stringWidth(tagline);
        if (w - tw - 24 > 330) {
            g2.drawString(tagline, w - tw - 24, h / 2 + 3);
        }
        g2.dispose();
    }

    /** A white circle with a stylised train line and two stations. */
    private static void paintLogo(Graphics2D g2, int x, int y, int size) {
        g2.setColor(java.awt.Color.WHITE);
        g2.fillOval(x, y, size, size);
        g2.setColor(Theme.PRIMARY);
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int pad = size / 4;
        g2.drawLine(x + pad, y + size - pad, x + size - pad, y + pad);
        g2.setColor(Theme.ACCENT);
        int dot = size / 4;
        g2.fillOval(x + pad - dot / 2, y + size - pad - dot / 2, dot, dot);
        g2.fillOval(x + size - pad - dot / 2, y + pad - dot / 2, dot, dot);
    }
}
