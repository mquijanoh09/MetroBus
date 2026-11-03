package co.medellin.transit.model;

import java.util.Objects;

/**
 * Immutable geographic coordinate with validation.
 * Represents a point on Earth using latitude and longitude.
 */
public final class Coordinate {
    private final double lat;
    private final double lon;

    /**
     * Creates a new coordinate with validation.
     *
     * @param lat latitude in degrees (-90 to 90)
     * @param lon longitude in degrees (-180 to 180)
     * @throws IllegalArgumentException if coordinates are out of valid range
     */
    public Coordinate(double lat, double lon) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException(
                "Latitude must be between -90 and 90, got: " + lat
            );
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException(
                "Longitude must be between -180 and 180, got: " + lon
            );
        }
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return Double.compare(that.lat, lat) == 0 &&
               Double.compare(that.lon, lon) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon);
    }

    @Override
    public String toString() {
        return "Coordinate{lat=" + lat + ", lon=" + lon + "}";
    }
}
