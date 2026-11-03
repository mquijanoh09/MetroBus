package co.medellin.transit.fares;

import co.medellin.transit.model.RouteType;

/**
 * Represents a single fare segment in a trip.
 * Each boarding action generates a fare segment.
 */
public class FareSegment {
    private final RouteType mode;
    private final int baseFareCOP;
    private final int appliedFareCOP;
    private final boolean integrationApplied;

    /**
     * Creates a new fare segment.
     *
     * @param mode the transit mode (BUS or METRO)
     * @param baseFareCOP the standard fare for this mode
     * @param appliedFareCOP the actual fare charged (may be discounted)
     * @param integrationApplied whether integration discount was applied
     */
    public FareSegment(RouteType mode, int baseFareCOP, int appliedFareCOP, boolean integrationApplied) {
        this.mode = mode;
        this.baseFareCOP = baseFareCOP;
        this.appliedFareCOP = appliedFareCOP;
        this.integrationApplied = integrationApplied;
    }

    public RouteType getMode() {
        return mode;
    }

    public int getBaseFareCOP() {
        return baseFareCOP;
    }

    public int getAppliedFareCOP() {
        return appliedFareCOP;
    }

    public boolean isIntegrationApplied() {
        return integrationApplied;
    }

    @Override
    public String toString() {
        return "FareSegment{" +
                "mode=" + mode +
                ", baseFareCOP=" + baseFareCOP +
                ", appliedFareCOP=" + appliedFareCOP +
                ", integrationApplied=" + integrationApplied +
                '}';
    }
}
