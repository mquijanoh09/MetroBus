package co.medellin.transit.ui.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * Colors, fonts and small helpers shared by every screen of the window.
 */
final class Theme {

    static final Color PRIMARY = new Color(0x0B7A3E);
    static final Color PRIMARY_DARK = new Color(0x075A2D);
    static final Color PRIMARY_LIGHT = new Color(0xE6F4EC);
    static final Color ACCENT = new Color(0xF2B705);
    static final Color BACKGROUND = new Color(0xF5F7F6);
    static final Color SURFACE = Color.WHITE;
    static final Color TEXT = new Color(0x1C2321);
    static final Color TEXT_MUTED = new Color(0x5F6B66);
    static final Color BORDER = new Color(0xD9E0DC);
    static final Color ERROR = new Color(0xB3261E);
    static final Color ERROR_LIGHT = new Color(0xFCEBEA);
    static final Color WALK = new Color(0x7A8580);
    static final Color ALIGHT = new Color(0x1F4E79);

    private static final String FAMILY = pickFamily();

    private Theme() {
    }

    private static String pickFamily() {
        try {
            String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String wanted : new String[]{"Segoe UI", "Helvetica Neue", "Ubuntu", "DejaVu Sans"}) {
                if (Arrays.asList(installed).contains(wanted)) {
                    return wanted;
                }
            }
        } catch (RuntimeException ignored) {
            // Fall back to the logical font below.
        }
        return Font.SANS_SERIF;
    }

    static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    /**
     * Uses the cross-platform look and feel, so the window looks the same on
     * Windows, macOS and Linux and our button colors are respected.
     */
    static void installLookAndFeel() {
        try {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 14));
            UIManager.put("OptionPane.buttonFont", font(Font.PLAIN, 13));
        } catch (Exception ignored) {
            // The default look and feel works too.
        }
    }

    static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, PRIMARY, Color.WHITE);
        return button;
    }

    static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, SURFACE, PRIMARY_DARK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1, true),
                BorderFactory.createEmptyBorder(9, 18, 9, 18)));
        return button;
    }

    private static void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(font(Font.BOLD, 14));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static void muted(JComponent component, float size) {
        component.setFont(font(Font.PLAIN, size));
        component.setForeground(TEXT_MUTED);
    }

    /** Formats pesos the Colombian way: 3800 becomes "$3.800". */
    static String pesos(int amount) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"));
        format.setGroupingUsed(true);
        return "$" + format.format(amount);
    }

    /**
     * Formats seconds as minutes, or hours and minutes past one hour.
     * Rounds up, like the console output and the step descriptions.
     */
    static String duration(int seconds) {
        int minutes = (int) Math.ceil(seconds / 60.0);
        if (minutes < 60) {
            return minutes + " min";
        }
        return (minutes / 60) + " h " + (minutes % 60) + " min";
    }

    static String meters(double meters) {
        long rounded = Math.round(meters);
        if (rounded < 1000) {
            return rounded + " m";
        }
        return String.format(Locale.forLanguageTag("es-CO"), "%.1f km", rounded / 1000.0);
    }
}
