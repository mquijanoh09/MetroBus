package co.medellin.transit.ui.gui.model;

import co.medellin.transit.model.Coordinate;

import java.util.Objects;

/**
 * One part of a trip as the user thinks of it: a walk, or one ride on one
 * line from where you board to where you get off.
 *
 * @param mode        transport mode; {@link TransitMode#WALK} for walks
 * @param routeCode   route code from the backend ("A", "T-A", "90111"); null for walks
 * @param fromName    where the leg starts (boarding stop); null for walks
 * @param fromCoord   coordinate of the start; may be null for walks
 * @param toName      where the leg ends
 * @param toCoord     coordinate of the end
 * @param durationSec time spent on this leg
 */
public record Leg(TransitMode mode, String routeCode, String fromName, Coordinate fromCoord,
                  String toName, Coordinate toCoord, int durationSec) {

    public Leg {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(toName, "toName");
        if (durationSec < 0) {
            throw new IllegalArgumentException("durationSec must be >= 0");
        }
    }

    public boolean isWalk() {
        return mode == TransitMode.WALK;
    }
}
