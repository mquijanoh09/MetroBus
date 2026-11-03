package co.medellin.transit.ui;

import co.medellin.transit.fares.FareResult;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Trip;

import java.util.Objects;

/**
 * Outcome of {@link TripPlanner#plan(String, String)}: either a trip with its
 * fare, or a status with a Spanish message to show the user.
 */
public final class PlanResult {

    /** What happened when planning the trip. */
    public enum Status {
        OK,
        EMPTY_INPUT,
        INVALID_FORMAT,
        NOT_FOUND,
        OUT_OF_BOUNDS,
        TOO_CLOSE,
        NO_ROUTE,
        ERROR
    }

    private final Status status;
    private final String message;
    private final Place origin;
    private final Place destination;
    private final Trip trip;
    private final FareResult fare;

    private PlanResult(Status status, String message, Place origin, Place destination, Trip trip, FareResult fare) {
        this.status = status;
        this.message = message;
        this.origin = origin;
        this.destination = destination;
        this.trip = trip;
        this.fare = fare;
    }

    public static PlanResult success(Place origin, Place destination, Trip trip, FareResult fare) {
        return new PlanResult(Status.OK, "Ruta encontrada",
                Objects.requireNonNull(origin), Objects.requireNonNull(destination),
                Objects.requireNonNull(trip), Objects.requireNonNull(fare));
    }

    public static PlanResult failure(Status status, String message) {
        if (status == Status.OK) {
            throw new IllegalArgumentException("A failure cannot have status OK");
        }
        return new PlanResult(status, Objects.requireNonNull(message), null, null, null, null);
    }

    public boolean isSuccess() {
        return status == Status.OK;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    /** Only set when {@link #isSuccess()} is true. */
    public Place getOrigin() {
        return origin;
    }

    /** Only set when {@link #isSuccess()} is true. */
    public Place getDestination() {
        return destination;
    }

    /** Only set when {@link #isSuccess()} is true. */
    public Trip getTrip() {
        return trip;
    }

    /** Only set when {@link #isSuccess()} is true. */
    public FareResult getFare() {
        return fare;
    }
}
