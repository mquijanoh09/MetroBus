package co.medellin.transit.ui.gui.view;

import co.medellin.transit.ui.gui.Theme;
import co.medellin.transit.ui.gui.state.ScreenLayout;

/** Sizes that depend on the layout (DESIGN.md, section 4.1). */
public final class MainLayout {

    public static final int COMPACT_MAP_HEIGHT = 180;

    private MainLayout() {
    }

    public static int margin(ScreenLayout layout) {
        return switch (layout) {
            case COMPACT -> Theme.S3;
            case MEDIUM -> Theme.S4;
            case WIDE -> Theme.S5;
        };
    }

    public static int gap(ScreenLayout layout) {
        return layout == ScreenLayout.COMPACT ? Theme.S3 : Theme.S4;
    }

    /** Width of the trip panel; -1 means the full width. */
    public static int panelWidth(ScreenLayout layout) {
        return switch (layout) {
            case COMPACT -> -1;
            case MEDIUM -> 360;
            case WIDE -> 400;
        };
    }
}
