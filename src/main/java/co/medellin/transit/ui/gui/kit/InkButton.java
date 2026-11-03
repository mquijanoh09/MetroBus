package co.medellin.transit.ui.gui.kit;

import co.medellin.transit.ui.gui.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Buttons painted by hand so they look the same on every OS. PRIMARY is ink
 * with white text; QUIET is white with an ink outline.
 */
public class InkButton extends JButton {

    public enum Kind { PRIMARY, QUIET }

    private final Kind kind;

    public InkButton(String text, Kind kind, String accessibleDescription) {
        super(text);
        this.kind = kind;
        setFont(Theme.button());
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(text);
        getAccessibleContext().setAccessibleDescription(accessibleDescription);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        if (getAccessibleContext() != null) {
            getAccessibleContext().setAccessibleName(text);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(getText()) + 2 * Theme.S4;
        return new Dimension(Math.max(w, Theme.MIN_TARGET), 44);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Paint.smooth(g);
        int w = getWidth();
        int h = getHeight();
        boolean enabled = isEnabled();
        boolean pressed = getModel().isPressed();
        boolean hover = getModel().isRollover();

        Color fill;
        Color text;
        if (kind == Kind.PRIMARY) {
            fill = !enabled ? Theme.INK_2 : pressed ? Theme.INK_2 : Theme.INK;
            text = Theme.SURFACE;
        } else {
            fill = pressed || hover ? Theme.PAPER : Theme.SURFACE;
            text = enabled ? Theme.INK : Theme.INK_2;
        }
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, Theme.RADIUS * 2, Theme.RADIUS * 2);
        if (kind == Kind.QUIET) {
            g2.setColor(enabled ? Theme.INK : Theme.RULE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS * 2, Theme.RADIUS * 2);
        }
        if (kind == Kind.PRIMARY && hover && enabled && !pressed) {
            g2.setColor(Theme.SURFACE);
            g2.fillRect(Theme.S3, h - 8, w - 2 * Theme.S3, 1);
        }
        g2.setFont(getFont());
        g2.setColor(text);
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(getText())) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), tx, ty);
        if (isFocusOwner()) {
            if (kind == Kind.PRIMARY) {
                g2.setColor(Theme.SURFACE);
                g2.drawRect(3, 3, w - 7, h - 7);
            } else {
                Paint.focusRing(g2, w, h);
            }
        }
        g2.dispose();
    }
}
