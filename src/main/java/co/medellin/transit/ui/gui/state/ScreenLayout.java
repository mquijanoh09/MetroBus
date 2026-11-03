package co.medellin.transit.ui.gui.state;

/** The three layouts of DESIGN.md, section 4.1, chosen by window width. */
public enum ScreenLayout {
    COMPACT,
    MEDIUM,
    WIDE;

    public static final int MEDIUM_MIN_WIDTH = 760;
    public static final int WIDE_MIN_WIDTH = 1100;

    public static ScreenLayout forWidth(int width) {
        if (width >= WIDE_MIN_WIDTH) {
            return WIDE;
        }
        if (width >= MEDIUM_MIN_WIDTH) {
            return MEDIUM;
        }
        return COMPACT;
    }
}
