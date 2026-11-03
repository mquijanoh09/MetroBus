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

    /** Which input caused a failure, so the interface can point at it. */
    public enum Field {
        NONE,
        ORIGIN,
        DESTINATION
    }

    private final Status status;
    private final Field field;
    private final String message;
    private final Place origin;
    private final Place destination;
    private final Trip trip;
    private final FareResult fare;

    private PlanResult(Status status, Field field, String message, Place origin, Place destination,
                       Trip trip, FareResult fare) {
        this.status = status;
        this.field = field;
        this.message = message;
        this.origin = origin;
        this.destination = destination;
        this.trip = trip;
        this.fare = fare;
    }

    public static PlanResult success(Place origin, Place destination, Trip trip, FareResult fare) {
        return new PlanResult(Status.OK, Field.NONE, "Ruta encontrada",
                Objects.requireNonNull(origin), Objects.requireNonNull(destination),
                Objects.requireNonNull(trip), Objects.requireNonNull(fare));
    }

    public static PlanResult failure(Status status, String message) {
        return failure(status, message, Field.NONE);
    }

    public static PlanResult failure(Status status, String message, Field field) {
        if (status == Status.OK) {
            throw new IllegalArgumentException("A failure cannot have status OK");
        }
        return new PlanResult(status, Objects.requireNonNull(field), Objects.requireNonNull(message),
                null, null, null, null);
    }

    public boolean isSuccess() {
        return status == Status.OK;
    }

    public Status getStatus() {
        return status;
    }

    /** The input that caused the failure, or {@link Field#NONE}. */
    public Field getField() {
        return field;
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
