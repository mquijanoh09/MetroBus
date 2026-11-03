package co.medellin.transit.model;

import java.util.Optional;

/**
 * Represents a single step in a trip's instruction sequence.
 * Each step describes an action the user should take, such as walking, boarding,
 * riding, or alighting from a vehicle.
 */
public class Step {
    private final StepType type;
    private final String description;
    private final int durationSec;
    private final Place location;
    private final Optional<String> routeInfo;

    /**
     * Creates a new step with all details.
     *
     * @param type the type of step (WALK, BOARD, RIDE, ALIGHT)
     * @param description Spanish-language instruction for the user
     * @param durationSec time duration for this step in seconds
     * @param location where this step occurs
     * @param routeInfo optional route number/name if applicable
     */
    public Step(StepType type, String description, int durationSec, Place location, Optional<String> routeInfo) {
        this.type = type;
        this.description = description;
        this.durationSec = durationSec;
        this.location = location;
        this.routeInfo = routeInfo;
    }

    /**
     * Creates a new step without route information.
     *
     * @param type the type of step (WALK, BOARD, RIDE, ALIGHT)
     * @param description Spanish-language instruction for the user
     * @param durationSec time duration for this step in seconds
     * @param location where this step occurs
     */
    public Step(StepType type, String description, int durationSec, Place location) {
        this(type, description, durationSec, location, Optional.empty());
    }

    public StepType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public Place getLocation() {
        return location;
    }

    public Optional<String> getRouteInfo() {
        return routeInfo;
    }

    @Override
    public String toString() {
        return "Step{" +
                "type=" + type +
                ", description='" + description + '\'' +
                ", durationSec=" + durationSec +
                ", location=" + location +
                ", routeInfo=" + routeInfo +
                '}';
    }
}
