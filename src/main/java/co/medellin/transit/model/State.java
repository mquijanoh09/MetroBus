package co.medellin.transit.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a state (node) in the transit routing graph.
 * 
 * <p>A state can be one of three types:
 * <ul>
 * <li>Walk state: (place, WALK) - represents a physical location where a person can walk</li>
 * <li>Bus stop state: (STOP:id, route, direction) - represents being at a bus stop on a specific route and direction</li>
 * <li>Metro station state: (STATION:name, line, direction) - represents being at a metro station on a specific line and direction</li>
 * </ul>
 * 
 * <p>Each state has a unique ID (0 to n-1) used for graph algorithms.
 * For transit states (bus stops and metro stations), routeId and direction are present.
 * For walk states, routeId and direction are empty.
 */
public final class State {
    private final int id;
    private final Place place;
    private final StateType type;
    private final Optional<String> routeId;
    private final Optional<Direction> direction;
    
    /**
     * Creates a walk state (no route or direction).
     * 
     * @param id unique state identifier (0 to n-1)
     * @param place physical location
     */
    public State(int id, Place place) {
        this.id = id;
        this.place = Objects.requireNonNull(place, "place cannot be null");
        this.type = StateType.WALK;
        this.routeId = Optional.empty();
        this.direction = Optional.empty();
    }
    
    /**
     * Creates a transit state (bus stop or metro station with route and direction).
     * RouteId is interned for memory optimization.
     * 
     * @param id unique state identifier (0 to n-1)
     * @param place physical location
     * @param type state type (BUS_STOP or METRO_STATION)
     * @param routeId route or line identifier
     * @param direction travel direction
     */
    public State(int id, Place place, StateType type, String routeId, Direction direction) {
        this.id = id;
        this.place = Objects.requireNonNull(place, "place cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        // Intern routeId to save memory - many states share same route IDs
        this.routeId = Optional.of(Objects.requireNonNull(routeId, "routeId cannot be null").intern());
        this.direction = Optional.of(Objects.requireNonNull(direction, "direction cannot be null"));
        
        if (type == StateType.WALK) {
            throw new IllegalArgumentException("Use the two-argument constructor for WALK states");
        }
    }
    
    /**
     * Returns the unique state identifier.
     * 
     * @return state ID (0 to n-1)
     */
    public int getId() {
        return id;
    }
    
    /**
     * Returns the physical location of this state.
     * 
     * @return place
     */
    public Place getPlace() {
        return place;
    }
    
    /**
     * Returns the type of this state.
     * 
     * @return state type (WALK, BUS_STOP, or METRO_STATION)
     */
    public StateType getType() {
        return type;
    }
    
    /**
     * Returns the route or line identifier if this is a transit state.
     * 
     * @return route ID or empty if this is a walk state
     */
    public Optional<String> getRouteId() {
        return routeId;
    }
    
    /**
     * Returns the travel direction if this is a transit state.
     * 
     * @return direction or empty if this is a walk state
     */
    public Optional<Direction> getDirection() {
        return direction;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return id == state.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("State{id=").append(id);
        sb.append(", type=").append(type);
        sb.append(", place=").append(place.getName());
        
        if (routeId.isPresent()) {
            sb.append(", route=").append(routeId.get());
        }
        if (direction.isPresent()) {
            sb.append(", direction=").append(direction.get());
        }
        
        sb.append("}");
        return sb.toString();
    }
}
