package co.medellin.transit.model;

import java.util.Objects;

/**
 * Immutable representation of a transit route.
 * Can represent either a bus route or a metro line.
 */
public final class Route {
    private final String id;
    private final String displayName;
    private final RouteType type;
    private final String color;

    /**
     * Creates a new route.
     *
     * @param id unique identifier for the route
     * @param displayName human-readable route name
     * @param type route type (BUS or METRO)
     * @param color color code for route display (e.g., hex color)
     * @throws NullPointerException if any parameter is null
     */
    public Route(String id, String displayName, RouteType type, String color) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.color = Objects.requireNonNull(color, "color cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RouteType getType() {
        return type;
    }

    public String getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(id, route.id) &&
               Objects.equals(displayName, route.displayName) &&
               type == route.type &&
               Objects.equals(color, route.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, type, color);
    }

    @Override
    public String toString() {
        return "Route{" +
               "id='" + id + '\'' +
               ", displayName='" + displayName + '\'' +
               ", type=" + type +
               ", color='" + color + '\'' +
               '}';
    }
}
