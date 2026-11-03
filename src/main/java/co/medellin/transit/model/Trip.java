package co.medellin.transit.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a complete trip with ordered instruction steps.
 * Contains all the information needed to display routing results to the user,
 * including step-by-step instructions and summary statistics.
 */
public class Trip {
    private final List<Step> steps;
    private final int totalDurationSec;
    private final double totalWalkMeters;

    /**
     * Creates a new trip with the given steps and metrics.
     *
     * @param steps ordered list of trip steps
     * @param totalDurationSec total travel time in seconds
     * @param totalWalkMeters total walking distance in meters
     */
    public Trip(List<Step> steps, int totalDurationSec, double totalWalkMeters) {
        this.steps = new ArrayList<>(steps);
        this.totalDurationSec = totalDurationSec;
        this.totalWalkMeters = totalWalkMeters;
    }

    /**
     * Creates an empty trip with no steps.
     */
    public Trip() {
        this(new ArrayList<>(), 0, 0.0);
    }

    public List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public int getTotalDurationSec() {
        return totalDurationSec;
    }

    public double getTotalWalkMeters() {
        return totalWalkMeters;
    }

    /**
     * Gets the total duration in minutes (rounded up).
     *
     * @return total travel time in minutes
     */
    public int getTotalDurationMinutes() {
        return (int) Math.ceil(totalDurationSec / 60.0);
    }

    /**
     * Gets the number of steps in this trip.
     *
     * @return step count
     */
    public int getStepCount() {
        return steps.size();
    }

    @Override
    public String toString() {
        return "Trip{" +
                "stepCount=" + steps.size() +
                ", totalDurationSec=" + totalDurationSec +
                ", totalWalkMeters=" + totalWalkMeters +
                '}';
    }

    /**
     * Builder for constructing Trip instances incrementally.
     */
    public static class Builder {
        private final List<Step> steps = new ArrayList<>();
        private int totalDurationSec = 0;
        private double totalWalkMeters = 0.0;

        /**
         * Adds a step to the trip.
         *
         * @param step the step to add
         * @return this builder for chaining
         */
        public Builder addStep(Step step) {
            steps.add(step);
            totalDurationSec += step.getDurationSec();
            
            // Track walking distance if this is a walk step
            if (step.getType() == StepType.WALK) {
                // Walking distance calculation would need to be provided
                // For now, we just accumulate the duration-based estimate
                // Actual implementation should get distance from edge data
            }
            
            return this;
        }

        /**
         * Sets the total walking distance.
         *
         * @param meters total walking distance in meters
         * @return this builder for chaining
         */
        public Builder setTotalWalkMeters(double meters) {
            this.totalWalkMeters = meters;
            return this;
        }

        /**
         * Sets the total duration.
         *
         * @param seconds total duration in seconds
         * @return this builder for chaining
         */
        public Builder setTotalDurationSec(int seconds) {
            this.totalDurationSec = seconds;
            return this;
        }

        /**
         * Builds the Trip instance.
         *
         * @return a new Trip with the accumulated steps and metrics
         */
        public Trip build() {
            return new Trip(steps, totalDurationSec, totalWalkMeters);
        }
    }
}
