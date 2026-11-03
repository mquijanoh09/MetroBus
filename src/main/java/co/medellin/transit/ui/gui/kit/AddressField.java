package co.medellin.transit.ui.gui.kit;

import co.medellin.transit.ui.gui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Text field for an address: white, 1 px rule border, ink border with focus,
 * red border when it caused the error, and a placeholder hint.
 */
public class AddressField extends JTextField {

    private final String placeholder;
    private boolean error;

    public AddressField(String accessibleName, String placeholder) {
        this.placeholder = placeholder;
        setFont(Theme.body());
        setForeground(Theme.INK);
        setCaretColor(Theme.INK);
        setSelectionColor(Theme.RULE);
        setSelectedTextColor(Theme.INK);
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        getAccessibleContext().setAccessibleName(accessibleName);
        getAccessibleContext().setAccessibleDescription("Ejemplo: " + placeholder);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    public void setError(boolean error) {
        if (this.error != error) {
            this.error = error;
            repaint();
        }
    }

    public boolean hasError() {
        return error;
    }

    /** Replaces the text only if it differs, so the caret does not jump while typing. */
    public void setTextIfChanged(String text) {
        if (!getText().equals(text)) {
            setText(text);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 44);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 44);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = Paint.smooth(g);
        if (getText().isEmpty() && !isFocusOwner()) {
            g2.setFont(Theme.body());
            g2.setColor(Theme.INK_2);
            FontMetrics fm = g2.getFontMetrics();
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, getInsets().left, ty);
        }
        int w = getWidth();
        int h = getHeight();
        if (error) {
            g2.setColor(Theme.ERROR);
            g2.fillRect(0, 0, w, 2);
            g2.fillRect(0, h - 2, w, 2);
            g2.fillRect(0, 0, 2, h);
            g2.fillRect(w - 2, 0, 2, h);
        } else if (isFocusOwner()) {
            Paint.focusRing(g2, w, h);
        } else {
            g2.setColor(Theme.RULE);
            g2.drawRect(0, 0, w - 1, h - 1);
        }
        g2.dispose();
    }
}
