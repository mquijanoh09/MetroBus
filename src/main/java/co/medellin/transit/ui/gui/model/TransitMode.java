package co.medellin.transit.ui.gui.model;

import java.util.Set;

/**
 * The kinds of transport the interface tells apart, each with its own color
 * (see DESIGN.md, section 2.2).
 */
public enum TransitMode {
    METRO("Metro"),
    METROPLUS("Metroplús"),
    CABLE("Metrocable"),
    TRAM("Tranvía"),
    BUS("Bus"),
    WALK("A pie");

    private static final Set<String> METRO_LINES = Set.of("A", "B");
    private static final Set<String> METROPLUS_LINES = Set.of("1", "2");
    private static final Set<String> CABLE_LINES = Set.of("H", "J", "K", "L", "M", "P");
    private static final Set<String> TRAM_LINES = Set.of("T-A");

    private final String label;

    TransitMode(String label) {
        this.label = label;
    }

    /** Name shown to the user, in Spanish. */
    public String label() {
        return label;
    }

    /**
     * Mode of a route code as {@code Step.getRouteInfo()} returns it: a line
     * code of the mass transit system, or a five-digit bus {@code id_ruta}.
     */
    public static TransitMode ofRoute(String routeCode) {
        if (routeCode == null) {
            return BUS;
        }
        String code = routeCode.trim().toUpperCase();
        if (METRO_LINES.contains(code)) {
            return METRO;
        }
        if (METROPLUS_LINES.contains(code)) {
            return METROPLUS;
        }
        if (CABLE_LINES.contains(code)) {
            return CABLE;
        }
        if (TRAM_LINES.contains(code)) {
            return TRAM;
        }
        return BUS;
    }

    /** Mode named in the {@code mode} column of transit_lines.csv. */
    public static TransitMode ofCsvMode(String csvMode) {
        return switch (csvMode.trim().toLowerCase()) {
            case "metro" -> METRO;
            case "brt" -> METROPLUS;
            case "cable_car" -> CABLE;
            case "tram" -> TRAM;
            default -> BUS;
        };
    }
}
