package co.medellin.transit.model;

/**
 * Types of steps in a trip instruction sequence.
 * Each step represents a distinct action the user takes during their journey.
 */
public enum StepType {
    /**
     * Walking step - user walks between locations
     */
    WALK,
    
    /**
     * Boarding step - user boards a vehicle (bus or metro)
     */
    BOARD,
    
    /**
     * Riding step - user travels on a vehicle
     */
    RIDE,
    
    /**
     * Alighting step - user exits a vehicle
     */
    ALIGHT
}
