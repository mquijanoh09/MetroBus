package co.medellin.transit.data;

/**
 * Represents a segment of a bus route connecting two stops.
 * Parsed from bus_segments.csv.
 */
public record BusSegment(
    String routeId,
    int direction,
    int fromStopId,
    int toStopId,
    int fromSeq,
    int toSeq,
    double straightM,
    double roadM,
    double usedM,
    String method
) {
    /**
     * Validates the bus segment data.
     * @throws IllegalArgumentException if validation fails
     */
    public BusSegment {
        if (routeId == null || routeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Route ID cannot be empty");
        }
        if (direction != 0 && direction != 1) {
            throw new IllegalArgumentException("Direction must be 0 or 1, got: " + direction);
        }
        if (fromStopId < 0) {
            throw new IllegalArgumentException("From stop ID must be >= 0, got: " + fromStopId);
        }
        if (toStopId < 0) {
            throw new IllegalArgumentException("To stop ID must be >= 0, got: " + toStopId);
        }
        if (fromSeq < 0) {
            throw new IllegalArgumentException("From sequence must be >= 0, got: " + fromSeq);
        }
        if (toSeq < 0) {
            throw new IllegalArgumentException("To sequence must be >= 0, got: " + toSeq);
        }
        if (straightM <= 0) {
            throw new IllegalArgumentException("Straight distance must be > 0, got: " + straightM);
        }
        if (roadM < 0) {
            throw new IllegalArgumentException("Road distance must be >= 0, got: " + roadM);
        }
        if (usedM <= 0) {
            throw new IllegalArgumentException("Used distance must be > 0, got: " + usedM);
        }
    }
}
