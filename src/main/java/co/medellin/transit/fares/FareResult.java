package co.medellin.transit.fares;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of fare calculation for a complete trip.
 * Contains total fare and per-segment breakdown.
 */
public class FareResult {
    private final int totalCOP;
    private final List<FareSegment> segments;
    private final boolean integrationApplied;

    /**
     * Creates a new fare result.
     *
     * @param totalCOP total fare in Colombian Pesos
     * @param segments list of fare segments
     * @param integrationApplied whether any integration discount was applied
     */
    public FareResult(int totalCOP, List<FareSegment> segments, boolean integrationApplied) {
        this.totalCOP = totalCOP;
        this.segments = new ArrayList<>(segments);
        this.integrationApplied = integrationApplied;
    }

    /**
     * Creates a zero-fare result (for walking-only trips).
     *
     * @return FareResult with zero cost
     */
    public static FareResult zero() {
        return new FareResult(0, Collections.emptyList(), false);
    }

    public int getTotalCOP() {
        return totalCOP;
    }

    public List<FareSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public boolean isIntegrationApplied() {
        return integrationApplied;
    }

    @Override
    public String toString() {
        return "FareResult{" +
                "totalCOP=" + totalCOP +
                ", segments=" + segments.size() +
                ", integrationApplied=" + integrationApplied +
                '}';
    }
}
