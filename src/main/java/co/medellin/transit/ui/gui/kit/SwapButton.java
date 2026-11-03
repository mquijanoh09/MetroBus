package co.medellin.transit.ui.gui.kit;

import co.medellin.transit.ui.gui.Theme;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Square button with two drawn arrows that swaps origin and destination. */
public class SwapButton extends JButton {

    public SwapButton() {
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Intercambiar origen y destino (Alt+I)");
        getAccessibleContext().setAccessibleName("Intercambiar origen y destino");
        getAccessibleContext().setAccessibleDescription("Atajo: Alt más I");
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Theme.MIN_TARGET + 4, Theme.MIN_TARGET + 4);
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
        int w = getWidth();
        int h = getHeight();
        g2.setColor(getModel().isRollover() ? Theme.PAPER : Theme.SURFACE);
        g2.fillRect(0, 0, w, h);
        g2.setColor(Theme.RULE);
        g2.drawRect(0, 0, w - 1, h - 1);
        g2.setColor(isEnabled() ? Theme.INK : Theme.INK_2);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = w / 2;
        int cy = h / 2;
        // up arrow (left) and down arrow (right)
        g2.drawLine(cx - 5, cy + 8, cx - 5, cy - 8);
        g2.drawLine(cx - 9, cy - 4, cx - 5, cy - 8);
        g2.drawLine(cx - 1, cy - 4, cx - 5, cy - 8);
        g2.drawLine(cx + 5, cy - 8, cx + 5, cy + 8);
        g2.drawLine(cx + 1, cy + 4, cx + 5, cy + 8);
        g2.drawLine(cx + 9, cy + 4, cx + 5, cy + 8);
        if (isFocusOwner()) {
            Paint.focusRing(g2, w, h);
        }
        g2.dispose();
    }
}
