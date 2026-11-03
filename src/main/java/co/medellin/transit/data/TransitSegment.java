package co.medellin.transit.data;

import co.medellin.transit.model.Coordinate;

import java.util.Optional;

/**
 * Represents a segment of a metro/metroplus/tranvía/metrocable line.
 * Parsed from transit_lines.csv.
 */
public record TransitSegment(
    String line,
    String mode,
    int order,
    String station,
    Coordinate coordinate,
    Optional<Double> distToNextM,
    Optional<Integer> secToNext
) {
    private static final double MIN_LAT = 6.15;
    private static final double MAX_LAT = 6.35;
    private static final double MIN_LON = -75.65;
    private static final double MAX_LON = -75.50;
    
    private static final java.util.Set<String> VALID_MODES = java.util.Set.of(
        "Metro", "Metroplus", "Tranvía", "Metrocable"
    );
    
    /**
     * Validates the transit segment data.
     * @throws IllegalArgumentException if validation fails
     */
    public TransitSegment {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Line cannot be empty");
        }
        if (!VALID_MODES.contains(mode)) {
            throw new IllegalArgumentException(
                "Mode must be one of " + VALID_MODES + ", got: " + mode
            );
        }
        if (order < 0) {
            throw new IllegalArgumentException("Order must be >= 0, got: " + order);
        }
        if (station == null || station.trim().isEmpty()) {
            throw new IllegalArgumentException("Station cannot be empty");
        }
        if (coordinate == null) {
            throw new IllegalArgumentException("Coordinate cannot be null");
        }
        // Validate Medellín bounds
        double lat = coordinate.getLat();
        double lon = coordinate.getLon();
        if (lat < MIN_LAT || lat > MAX_LAT || lon < MIN_LON || lon > MAX_LON) {
            throw new IllegalArgumentException(
                String.format("Coordinate (%.4f, %.4f) is outside Medellín bounds", lat, lon)
            );
        }
        if (distToNextM == null) {
            throw new IllegalArgumentException("distToNextM cannot be null (use Optional.empty() for no next station)");
        }
        if (secToNext == null) {
            throw new IllegalArgumentException("secToNext cannot be null (use Optional.empty() for no next station)");
        }
        // If present, must be >= 0
        if (distToNextM.isPresent() && distToNextM.get() < 0) {
            throw new IllegalArgumentException("Distance to next must be >= 0, got: " + distToNextM.get());
        }
        if (secToNext.isPresent() && secToNext.get() < 0) {
            throw new IllegalArgumentException("Seconds to next must be >= 0, got: " + secToNext.get());
        }
    }
}
