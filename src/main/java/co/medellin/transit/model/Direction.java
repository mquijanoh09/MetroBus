package co.medellin.transit.model;

/**
 * Travel direction for transit routes.
 * Routes typically have two directions of service.
 */
public enum Direction {
    /**
     * Outbound direction (typically away from city center).
     */
    OUTBOUND,

    /**
     * Inbound direction (typically toward city center).
     */
    INBOUND
}
