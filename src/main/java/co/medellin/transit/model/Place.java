package co.medellin.transit.model;

import java.util.Objects;

/**
 * Immutable representation of a physical location in the transit system.
 * Can represent a bus stop, metro station, or address point.
 */
public final class Place {
    private final String name;
    private final Coordinate coord;

    /**
     * Creates a new place with string interning for memory optimization.
     * Place names are interned to reduce memory footprint when many states
     * reference the same location names.
     *
     * @param name descriptive name of the location
     * @param coord geographic coordinate
     * @throws NullPointerException if name or coord is null
     */
    public Place(String name, Coordinate coord) {
        Objects.requireNonNull(name, "name cannot be null");
        // Intern string to save memory - many states share same place names
        this.name = name.intern();
        this.coord = Objects.requireNonNull(coord, "coord cannot be null");
    }

    public String getName() {
        return name;
    }

    public Coordinate getCoord() {
        return coord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return Objects.equals(name, place.name) &&
               Objects.equals(coord, place.coord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, coord);
    }

    @Override
    public String toString() {
        return "Place{name='" + name + "', coord=" + coord + "}";
    }
}
