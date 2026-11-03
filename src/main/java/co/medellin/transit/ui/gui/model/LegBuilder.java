package co.medellin.transit.ui.gui.model;

import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups the backend's steps into legs: each WALK step is a walking leg, and
 * each BOARD, RIDE(s), ALIGHT run on the same route becomes one ride leg.
 */
public final class LegBuilder {

    private LegBuilder() {
    }

    public static List<Leg> legs(Trip trip) {
        List<Leg> legs = new ArrayList<>();
        List<Step> steps = trip.getSteps();
        int i = 0;
        while (i < steps.size()) {
            Step step = steps.get(i);
            if (step.getType() == StepType.WALK) {
                legs.add(new Leg(TransitMode.WALK, null, null, null,
                        step.getLocation().getName(), step.getLocation().getCoord(), step.getDurationSec()));
                i++;
                continue;
            }
            // A ride: BOARD? RIDE* ALIGHT? on one route
            String route = step.getRouteInfo().orElse(null);
            String fromName = step.getLocation().getName();
            var fromCoord = step.getLocation().getCoord();
            String toName = fromName;
            var toCoord = fromCoord;
            int duration = 0;
            while (i < steps.size()) {
                Step s = steps.get(i);
                if (s.getType() == StepType.WALK) {
                    break;
                }
                String sRoute = s.getRouteInfo().orElse(route);
                if (s.getType() == StepType.BOARD && s != step) {
                    break; // next ride starts
                }
                if (route == null) {
                    route = sRoute;
                }
                duration += s.getDurationSec();
                toName = s.getLocation().getName();
                toCoord = s.getLocation().getCoord();
                i++;
                if (s.getType() == StepType.ALIGHT) {
                    break;
                }
            }
            legs.add(new Leg(TransitMode.ofRoute(route), route, fromName, fromCoord, toName, toCoord, duration));
        }
        return legs;
    }

    /** Number of transfers: rides minus one, never negative. */
    public static int transfers(List<Leg> legs) {
        long rides = legs.stream().filter(l -> !l.isWalk()).count();
        return (int) Math.max(0, rides - 1);
    }
}
