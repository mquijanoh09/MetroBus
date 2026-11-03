package co.medellin.transit.model;

import java.util.Objects;

/**
 * Immutable representation of a transit stop (bus stop or metro station).
 * Represents a physical location where passengers can board/alight vehicles.
 */
public final class Stop {
    private final int id;
    private final String name;
    private final Coordinate coordinate;
    private final String address;

    /**
     * Creates a new stop.
     *
     * @param id unique identifier for the stop (e.g., id_paradero from data)
     * @param name human-readable stop name
     * @param coordinate geographic location of the stop
     * @param address street address or location description
     * @throws NullPointerException if name, coordinate, or address is null
     */
    public Stop(int id, String name, Coordinate coordinate, String address) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.coordinate = Objects.requireNonNull(coordinate, "coordinate cannot be null");
        this.address = Objects.requireNonNull(address, "address cannot be null");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return id == stop.id &&
               Objects.equals(name, stop.name) &&
               Objects.equals(coordinate, stop.coordinate) &&
               Objects.equals(address, stop.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, coordinate, address);
    }

    @Override
    public String toString() {
        return "Stop{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", coordinate=" + coordinate +
               ", address='" + address + '\'' +
               '}';
    }
}
