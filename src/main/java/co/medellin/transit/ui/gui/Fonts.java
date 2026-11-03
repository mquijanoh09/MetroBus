package co.medellin.transit.ui.gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads IBM Plex (SIL Open Font License 1.1, see fonts/OFL-IBM-Plex.txt) from
 * the jar, so the app looks the same on every computer without installing
 * anything. Falls back to the logical fonts if a file is missing.
 */
public final class Fonts {

    private static final Logger LOGGER = Logger.getLogger(Fonts.class.getName());

    static final String REGULAR = "IBMPlexSans-Regular";
    static final String MEDIUM = "IBMPlexSans-Medium";
    static final String SEMIBOLD = "IBMPlexSans-SemiBold";
    static final String MONO = "IBMPlexMono-Medium";

    private static final Map<String, Font> BASE = new HashMap<>();
    private static boolean registered;

    private Fonts() {
    }

    /** Loads the four font files once. Safe to call many times. */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        for (String name : new String[]{REGULAR, MEDIUM, SEMIBOLD, MONO}) {
            try (InputStream in = Fonts.class.getResourceAsStream("/fonts/" + name + ".ttf")) {
                if (in == null) {
                    LOGGER.warning("Font not found in jar: " + name);
                    continue;
                }
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                try {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                } catch (RuntimeException headless) {
                    // Registering is optional; deriving from the base font works anyway.
                }
                BASE.put(name, font);
            } catch (Exception e) {
                LOGGER.warning("Could not load font " + name + ": " + e.getMessage());
            }
        }
    }

    /** True when all four Plex files loaded (used by tests). */
    public static synchronized boolean plexLoaded() {
        register();
        return BASE.size() == 4;
    }

    static synchronized Font sans(String weight, float size) {
        register();
        Font base = BASE.get(weight);
        if (base == null) {
            return new Font(Font.SANS_SERIF, SEMIBOLD.equals(weight) ? Font.BOLD : Font.PLAIN, Math.round(size));
        }
        return base.deriveFont(size);
    }

    static synchronized Font mono(float size) {
        register();
        Font base = BASE.get(MONO);
        if (base == null) {
            return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
        }
        return base.deriveFont(size);
    }
}
