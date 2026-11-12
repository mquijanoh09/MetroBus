package co.medellin.transit.fares;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Calculates fares for transit trips based on Medellín's fare rules.
 * 
 * <p>Fare Rules (January 2026, from docs/fares.md):
 * <ul>
 * <li>Metro/Metroplús/Cable/Tram (mass transit): 3,820 COP per boarding</li>
 * <li>Bus: 3,800 COP per boarding</li>
 * <li>Mass-transit integration: Within 90 minutes, transfers between metro/brt/cable/tram
 *     cost no extra fare, except +750 COP surcharge when integration count reaches 5, 8, or 9</li>
 * <li>Bus + Metro integration: When an "Integrada" bus is used right before or after mass transit,
 *     within 90 minutes, the bus and transit pair costs 4,715 COP total</li>
 * <li>No integration: Directa Centro, Circular, Directa No Centro buses, or bus→bus transfers
 *     pay full fare each time</li>
 * <li>Walking: Free</li>
 * </ul>
 * 
 * <p>This implementation is a direct port of reference/prototype.py Network.fare() function.
 */
public class FareCalculator {
    private static final int FARE_TRANSIT = 3820;
    private static final int FARE_BUS = 3800;
    private static final int FARE_INTEGRATED = 4715;
    private static final int FARE_WINDOW_SEC = 90 * 60; // 90 minutes
    private static final int TRANSIT_SURCHARGE = 750;
    private static final Set<Integer> SURCHARGE_AT = Set.of(5, 8, 9);
    
    private final FareConfig config;
    private final TransitGraph graph;

    /**
     * Creates a new FareCalculator with the specified configuration.
     *
     * @param config fare configuration (currently unused, keeping for compatibility)
     * @param graph transit graph for route lookup
     */
    public FareCalculator(FareConfig config, TransitGraph graph) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
    }

    /**
     * Calculates the fare for a complete trip.
     * 
     * <p>Direct port of reference/prototype.py Network.fare() algorithm:
     * <pre>
     * cost = 0; transitStart = null; integrations = 0; last = null
     * for each ride step p:
     *   if p is mass transit:
     *      if transitStart != null and p.t0 − transitStart ≤ 90 min:
     *          integrations += 1; if integrations in {5, 8, 9}: cost += 750
     *      else if last is an Integrada bus, ≤ 90 min, not yet integrated:
     *          cost += 4,715 − 3,800; transitStart = last.t0; integrations = 1; mark last integrated
     *      else:
     *          cost += 3,820; transitStart = p.t0; integrations = 1
     *   if p is a bus:
     *      if p is Integrada, last was transit and not integrated, and ≤ 90 min since transitStart:
     *          cost += 4,715 − 3,820; mark p integrated
     *      else:
     *          cost += 3,800
     *   last = p
     * </pre>
     *
     * @param trip the trip to calculate fare for
     * @return FareResult with total fare and segment breakdown
     */
    public FareResult calculate(Trip trip) {
        Objects.requireNonNull(trip, "trip cannot be null");

        List<FareSegment> segments = new ArrayList<>();
        int cost = 0;
        Integer transitStart = null;  // t0 of first transit in current integration window
        int integrations = 0;         // count of integrations in current window
        RideInfo last = null;         // previous ride step info
        Set<Integer> integrated = new HashSet<>();  // indices of integrated ride steps
        
        // Track ride steps and their t0 values
        List<RideInfo> rides = extractRideInfo(trip);
        
        for (int i = 0; i < rides.size(); i++) {
            RideInfo p = rides.get(i);
            
            if (p.isMassTransit) {
                // This is mass transit (metro, brt, cable_car, tram)
                if (transitStart != null && p.t0 - transitStart <= FARE_WINDOW_SEC) {
                    // Transfer within mass-transit system
                    integrations++;
                    if (SURCHARGE_AT.contains(integrations)) {
                        cost += TRANSIT_SURCHARGE;
                    }
                    // No additional base fare for this transfer
                } else if (last != null && !last.isMassTransit && isIntegradaBus(last.routeId) 
                        && p.t0 - last.t0 <= FARE_WINDOW_SEC && !integrated.contains(last.index)) {
                    // Integrated bus → transit: pair costs 4,715
                    // We already paid 3,800 for the bus, so pay 4,715 - 3,800 = 915 more
                    cost += FARE_INTEGRATED - FARE_BUS;
                    transitStart = last.t0;
                    integrations = 1;
                    integrated.add(last.index);
                } else {
                    // New transit boarding, not integrated
                    cost += FARE_TRANSIT;
                    transitStart = p.t0;
                    integrations = 1;
                }
            } else {
                // This is a bus
                if (isIntegradaBus(p.routeId) && last != null && last.isMassTransit 
                        && !integrated.contains(last.index) && transitStart != null 
                        && p.t0 - transitStart <= FARE_WINDOW_SEC) {
                    // Transit → integrated bus: pair costs 4,715
                    // We already paid 3,820 for transit, so pay 4,715 - 3,820 = 895 more
                    cost += FARE_INTEGRATED - FARE_TRANSIT;
                    integrated.add(i);
                } else {
                    // Regular bus boarding
                    cost += FARE_BUS;
                }
            }
            
            last = p;
        }
        
        // Build fare segments (simplified - just track total)
        if (cost > 0) {
            // For now, create a single aggregate segment
            // Could be enhanced to track per-boarding segments
            segments.add(new FareSegment(null, cost, cost, !integrated.isEmpty()));
        }
        
        return new FareResult(cost, segments, !integrated.isEmpty());
    }
    
    /**
     * Extracts ride information from trip steps.
     * Each ride has a t0 (start time when BOARD occurs) and other metadata.
     * 
     * @param trip the trip
     * @return list of ride information
     */
    private List<RideInfo> extractRideInfo(Trip trip) {
        List<RideInfo> rides = new ArrayList<>();
        int cumulativeTime = 0;
        int rideIndex = 0;
        
        for (Step step : trip.getSteps()) {
            if (step.getType() == StepType.BOARD) {
                // Record t0 BEFORE the wait
                String routeId = step.getRouteInfo().orElse(null);
                if (routeId != null) {
                    boolean isMassTransit = isMassTransit(routeId);
                    rides.add(new RideInfo(rideIndex++, routeId, cumulativeTime, isMassTransit));
                }
            }
            // Accumulate time
            cumulativeTime += step.getDurationSec();
        }
        
        return rides;
    }
    
    /**
     * Determines if a route is mass transit (metro, brt, cable_car, tram).
     * 
     * @param routeId route identifier
     * @return true if mass transit, false if bus
     */
    private boolean isMassTransit(String routeId) {
        // Check if it's a transit line (has a mode in the graph)
        String mode = graph.getLineMode(routeId);
        return mode != null; // If it has a mode, it's mass transit
    }
    
    /**
     * Determines if a bus route is "Integrada" type.
     * 
     * @param routeId route identifier
     * @return true if Integrada, false otherwise
     */
    private boolean isIntegradaBus(String routeId) {
        String routeType = graph.getRouteType(routeId);
        return "Integrada".equals(routeType);
    }
    
    /**
     * Internal class to track ride information.
     */
    private static class RideInfo {
        final int index;
        final String routeId;
        final int t0;  // Start time when BOARD occurs (before wait)
        final boolean isMassTransit;
        
        RideInfo(int index, String routeId, int t0, boolean isMassTransit) {
            this.index = index;
            this.routeId = routeId;
            this.t0 = t0;
            this.isMassTransit = isMassTransit;
        }
    }
}
