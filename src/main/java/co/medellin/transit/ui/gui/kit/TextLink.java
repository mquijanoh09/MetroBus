package co.medellin.transit.ui.gui.kit;

import co.medellin.transit.ui.gui.Theme;

import javax.swing.JButton;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** An underlined text action, used for examples and "Editar". */
public class TextLink extends JButton {

    public TextLink(String text, String accessibleDescription) {
        super(text);
        setFont(Theme.body());
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setHorizontalAlignment(LEFT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(text);
        getAccessibleContext().setAccessibleDescription(accessibleDescription);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 8, Theme.MIN_TARGET - 8);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Paint.smooth(g);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(isEnabled() ? Theme.INK : Theme.INK_2);
        g2.drawString(getText(), 4, ty);
        int tw = fm.stringWidth(getText());
        g2.setColor(getModel().isRollover() ? Theme.INK : Theme.RULE);
        g2.fillRect(4, ty + 3, tw, getModel().isRollover() ? 2 : 1);
        if (isFocusOwner()) {
            Paint.focusRing(g2, getWidth(), getHeight());
        }
        g2.dispose();
    }
}
