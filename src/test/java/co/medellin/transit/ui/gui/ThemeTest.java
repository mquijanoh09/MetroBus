package co.medellin.transit.ui.gui;

import co.medellin.transit.ui.gui.model.TransitMode;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** The design system matches DESIGN.md: colors listed, contrast met, fonts bundled. */
class ThemeTest {

    @Test
    void everyColorConstantIsInThePalette() throws Exception {
        for (Field f : Theme.class.getDeclaredFields()) {
            if (f.getType() == Color.class && Modifier.isStatic(f.getModifiers())) {
                Color c = (Color) f.get(null);
                assertTrue(Arrays.asList(Theme.PALETTE).contains(c), f.getName() + " is not in PALETTE");
            }
        }
        assertEquals(13, Theme.PALETTE.length, "DESIGN.md lists 13 colors");
    }

    @Test
    void textContrastIsAtLeast45() {
        assertTrue(contrast(Theme.INK, Theme.PAPER) >= 4.5);
        assertTrue(contrast(Theme.INK_2, Theme.PAPER) >= 4.5);
        assertTrue(contrast(Theme.ERROR, Theme.PAPER) >= 4.5);
        for (TransitMode m : TransitMode.values()) {
            if (m == TransitMode.WALK) {
                continue;
            }
            double c = contrast(Theme.badgeText(m), Theme.badgeFill(m));
            assertTrue(c >= 4.5, m + " badge text contrast " + c);
        }
    }

    @Test
    void mapStrokesAreAtLeast3() {
        for (TransitMode m : TransitMode.values()) {
            double c = contrast(Theme.stroke(m), Theme.PAPER);
            assertTrue(c >= 3.0, m + " stroke contrast " + c);
        }
    }

    @Test
    void plexFontsAreBundled() {
        assertTrue(Fonts.plexLoaded(), "The four IBM Plex files must be in src/main/resources/fonts");
        assertNotNull(Fonts.class.getResource("/fonts/OFL-IBM-Plex.txt"), "The OFL license must ship with the fonts");
    }

    static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        double[] v = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        for (int i = 0; i < 3; i++) {
            v[i] = v[i] <= 0.03928 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }
}
