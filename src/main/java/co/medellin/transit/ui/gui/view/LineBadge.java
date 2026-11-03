package co.medellin.transit.ui.gui.view;

import co.medellin.transit.ui.gui.Theme;
import co.medellin.transit.ui.gui.kit.Paint;
import co.medellin.transit.ui.gui.model.TransitMode;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Square-cornered badge with a line or route code: "A", "T-A", "263". */
public final class LineBadge extends co.medellin.transit.ui.gui.kit.Canvas {

    private final String code;
    private final TransitMode mode;

    public LineBadge(String code, TransitMode mode) {
        this.code = code;
        this.mode = mode;
        setFont(Theme.badge());
        getAccessibleContext().setAccessibleName(mode.label() + " " + code);
    }

    public String code() {
        return code;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(Math.max(28, fm.stringWidth(code) + 12), 22);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Paint.smooth(g);
        Dimension d = getPreferredSize();
        g2.setColor(Theme.badgeFill(mode));
        g2.fillRect(0, 0, d.width, d.height);
        g2.setColor(Theme.badgeText(mode));
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(code, (d.width - fm.stringWidth(code)) / 2, (d.height - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
