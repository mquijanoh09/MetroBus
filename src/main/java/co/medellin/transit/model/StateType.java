package co.medellin.transit.model;

/**
 * Represents the type of a state in the transit graph.
 * 
 * <ul>
 * <li>WALK: Represents a walkable location (place, WALK)</li>
 * <li>BUS_STOP: Represents a bus stop on a specific route and direction (STOP:id, route, dir)</li>
 * <li>METRO_STATION: Represents a metro station on a specific line and direction (STATION:name, line, dir)</li>
 * </ul>
 */
public enum StateType {
    /**
     * A walkable location state
     */
    WALK,
    
    /**
     * A bus stop state on a specific route and direction
     */
    BUS_STOP,
    
    /**
     * A metro station state on a specific line and direction
     */
    METRO_STATION
}
