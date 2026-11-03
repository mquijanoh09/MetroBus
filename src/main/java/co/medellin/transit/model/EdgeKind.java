package co.medellin.transit.model;

/**
 * Represents the type of an edge in the transit graph.
 * 
 * <ul>
 * <li>WALK: Walking edge between two physical locations</li>
 * <li>BOARD: Boarding edge from a walk state to a transit state (getting on a vehicle)</li>
 * <li>RIDE: Riding edge between two stops/stations on the same route (traveling on a vehicle)</li>
 * <li>ALIGHT: Alighting edge from a transit state to a walk state (getting off a vehicle)</li>
 * </ul>
 */
public enum EdgeKind {
    /**
     * Walking between physical locations
     */
    WALK,
    
    /**
     * Boarding a vehicle (transitioning from walk state to transit state)
     */
    BOARD,
    
    /**
     * Riding on a vehicle between stops/stations on the same route
     */
    RIDE,
    
    /**
     * Alighting from a vehicle (transitioning from transit state to walk state)
     */
    ALIGHT
}
