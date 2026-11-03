package co.medellin.transit.ui.gui;

import co.medellin.transit.ui.gui.model.TransitMode;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * The design system in code: every color, type style and spacing of
 * DESIGN.md (sections 2 to 4). No other class creates colors or fonts.
 */
public final class Theme {

    // 2.1 Base
    public static final Color PAPER = new Color(0xF6F5F1);
    public static final Color SURFACE = new Color(0xFFFFFF);
    public static final Color INK = new Color(0x16191D);
    public static final Color INK_2 = new Color(0x4A5058);
    public static final Color RULE = new Color(0xD8D6CF);
    public static final Color ERROR = new Color(0xB42318);

    // 2.2 Modes: badge fill, text on badge, stroke on the map
    public static final Color METRO = new Color(0x1F4FA3);
    public static final Color METROPLUS = new Color(0x1B7A4B);
    public static final Color CABLE = new Color(0xB8480F);
    public static final Color TRAM = new Color(0x9B1B47);
    public static final Color BUS_FILL = new Color(0xE8B000);
    public static final Color BUS_STROKE = new Color(0x9C6C00);
    public static final Color WALK = new Color(0x6B7077);

    /** Every color the interface may use, for the consistency test. */
    public static final Color[] PALETTE = {PAPER, SURFACE, INK, INK_2, RULE, ERROR,
            METRO, METROPLUS, CABLE, TRAM, BUS_FILL, BUS_STROKE, WALK};

    // 4. Spacing (8 px base)
    public static final int S1 = 4;
    public static final int S2 = 8;
    public static final int S3 = 16;
    public static final int S4 = 24;
    public static final int S5 = 32;
    public static final int RADIUS = 2;
    public static final int MIN_TARGET = 40;

    private Theme() {
    }

    public static Color badgeFill(TransitMode mode) {
        return switch (mode) {
            case METRO -> METRO;
            case METROPLUS -> METROPLUS;
            case CABLE -> CABLE;
            case TRAM -> TRAM;
            case BUS -> BUS_FILL;
            case WALK -> WALK;
        };
    }

    public static Color badgeText(TransitMode mode) {
        return mode == TransitMode.BUS ? INK : SURFACE;
    }

    public static Color stroke(TransitMode mode) {
        return mode == TransitMode.BUS ? BUS_STROKE : badgeFill(mode);
    }

    // 3. Type styles
    public static Font figure() {
        return Fonts.mono(40f);
    }

    public static Font figureSmall() {
        return Fonts.mono(18f);
    }

    public static Font title() {
        return Fonts.sans(Fonts.SEMIBOLD, 20f);
    }

    public static Font body() {
        return Fonts.sans(Fonts.REGULAR, 15f);
    }

    public static Font label() {
        return Fonts.sans(Fonts.MEDIUM, 12f);
    }

    public static Font hint() {
        return Fonts.sans(Fonts.REGULAR, 13f);
    }

    public static Font badge() {
        return Fonts.sans(Fonts.SEMIBOLD, 13f);
    }

    public static Font brand() {
        return Fonts.sans(Fonts.SEMIBOLD, 18f);
    }

    public static Font button() {
        return Fonts.sans(Fonts.MEDIUM, 15f);
    }

    /** Cross-platform look and feel, so our own painting is identical on every OS. */
    public static void install() {
        try {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // The default look and feel is fine too.
        }
        Fonts.register();
        UIManager.put("Panel.background", PAPER);
        UIManager.put("OptionPane.background", PAPER);
        UIManager.put("OptionPane.messageFont", body());
        UIManager.put("OptionPane.buttonFont", button());
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ToolTip.font", hint());
    }
}
