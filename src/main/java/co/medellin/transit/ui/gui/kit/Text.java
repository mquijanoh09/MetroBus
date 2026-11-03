package co.medellin.transit.ui.gui.kit;

import co.medellin.transit.ui.gui.Theme;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;

/** Factory for the text styles of DESIGN.md, section 3. */
public final class Text {

    private Text() {
    }

    public static JLabel label(String upperText) {
        JLabel l = new JLabel(upperText.toUpperCase());
        l.setFont(Theme.label().deriveFont(Map.of(TextAttribute.TRACKING, 0.06f)));
        l.setForeground(Theme.INK_2);
        return l;
    }

    public static JLabel title(String text) {
        return styled(text, Theme.title(), Theme.INK);
    }

    public static JLabel hint(String text) {
        return styled(text, Theme.hint(), Theme.INK_2);
    }

    public static JLabel styled(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** Multi-line text that wraps at word boundaries and never cuts with "…". */
    public static JTextArea wrapping(String text, Font font, Color color) {
        JTextArea area = new JTextArea(text);
        area.setFont(font);
        area.setForeground(color);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setHighlighter(null);
        return area;
    }
}
