package co.medellin.transit.data;

import co.medellin.transit.model.Coordinate;

/**
 * Represents a transit station (metro, Metroplús, tranvía, or metrocable).
 * Parsed from transit_stations.json.
 */
public record Station(
    String name,
    String line,
    Coordinate coordinate
) {
    private static final double MIN_LAT = 6.15;
    private static final double MAX_LAT = 6.35;
    private static final double MIN_LON = -75.65;
    private static final double MAX_LON = -75.50;
    
    /**
     * Validates the station data.
     * @throws IllegalArgumentException if validation fails
     */
    public Station {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Station name cannot be empty");
        }
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Station line cannot be empty");
        }
        if (coordinate == null) {
            throw new IllegalArgumentException("Station coordinate cannot be null");
        }
        
        // Validate Medellín bounds
        double lat = coordinate.getLat();
        double lon = coordinate.getLon();
        if (lat < MIN_LAT || lat > MAX_LAT || lon < MIN_LON || lon > MAX_LON) {
            throw new IllegalArgumentException(
                String.format("Coordinate (%.4f, %.4f) is outside Medellín bounds", lat, lon)
            );
        }
    }
}
