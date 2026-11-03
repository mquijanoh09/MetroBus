package co.medellin.transit.fares;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.RouteType;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Calculates fares for transit trips based on Medellín's fare rules.
 * 
 * <p>Fare Rules (January 2026):
 * <ul>
 * <li>Metro/Metroplús/Cable/Tram: 3,820 COP per boarding</li>
 * <li>Bus: 3,800 COP per boarding</li>
 * <li>Integration: When transferring between metro and bus within 90 minutes, 
 *     the total fare is 4,715 COP instead of paying both fares separately (savings of ~2,900 COP)</li>
 * <li>Walking: Free</li>
 * </ul>
 * 
 * <p>Integration is applied when:
 * <ul>
 * <li>First boarding is metro, second boarding is bus (within 90 minutes)</li>
 * <li>First boarding is bus, second boarding is metro (within 90 minutes)</li>
 * </ul>
 * 
 * <p>Integration is NOT applied when:
 * <ul>
 * <li>Both boardings are the same mode (metro→metro or bus→bus)</li>
 * <li>More than 90 minutes elapse between boardings</li>
 * <li>More than 2 boardings occur (integration only covers first 2 boardings)</li>
 * </ul>
 */
public class FareCalculator {
    private final FareConfig config;
    private final TransitGraph graph;
    private final Map<String, RouteType> routeTypeCache;

    /**
     * Creates a new FareCalculator with the specified configuration.
     *
     * @param config fare configuration
     * @param graph transit graph for route lookup
     */
    public FareCalculator(FareConfig config, TransitGraph graph) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
        this.routeTypeCache = new HashMap<>();
        buildRouteTypeCache();
    }

    /**
     * Builds a cache of route IDs to route types for fast lookup.
     * Routes starting with letters (A, B, etc.) are metro lines.
     * Routes starting with numbers are buses.
     */
    private void buildRouteTypeCache() {
        // This is a simplified heuristic. In a real implementation, 
        // the GraphBuilder should provide a route registry.
        // Metro lines in Medellín: A, B, C, H, J, K, L, M, P, T
        // Everything else is a bus route.
        
        // For now, we'll determine route type during calculation based on route ID format.
        // This will be populated lazily as we encounter routes.
    }

    /**
     * Determines the route type based on route ID.
     * Metro lines use letter identifiers (A, B, C, H, J, K, L, M, P, T).
     * Bus routes use numeric identifiers.
     *
     * @param routeId route identifier
     * @return RouteType (BUS or METRO)
     */
    private RouteType getRouteType(String routeId) {
        if (routeId == null || routeId.isEmpty()) {
            return RouteType.BUS; // Default to bus
        }

        // Check cache first
        if (routeTypeCache.containsKey(routeId)) {
            return routeTypeCache.get(routeId);
        }

        // Determine route type based on ID format
        // Metro lines in Medellín: A, B, C (metro), H, J, K, L, M, P, T (cable cars and tram)
        // These are single letters or short letter codes
        // Bus routes are typically numeric or alphanumeric starting with a number
        RouteType type;
        char firstChar = routeId.charAt(0);
        
        if (Character.isLetter(firstChar)) {
            // Single letter or letter-based route (metro/cable/tram/metroplus)
            type = RouteType.METRO;
        } else {
            // Numeric route (bus)
            type = RouteType.BUS;
        }

        // Cache the result
        routeTypeCache.put(routeId, type);
        return type;
    }

    /**
     * Calculates the fare for a complete trip.
     * 
     * <p>The algorithm:
     * <ol>
     * <li>Iterate through trip steps</li>
     * <li>For each BOARD step, record the boarding time and mode</li>
     * <li>Check if integration applies (different mode within 90 minutes)</li>
     * <li>Apply base fare or integrated fare accordingly</li>
     * <li>Sum all applied fares</li>
     * </ol>
     *
     * @param trip the trip to calculate fare for
     * @return FareResult with total fare and segment breakdown
     */
    public FareResult calculate(Trip trip) {
        Objects.requireNonNull(trip, "trip cannot be null");

        List<FareSegment> segments = new ArrayList<>();
        int totalFare = 0;
        boolean integrationApplied = false;

        // Track first boarding for integration detection
        Integer firstBoardingTimeSec = null;
        RouteType firstBoardingMode = null;
        int cumulativeTimeSec = 0;

        for (Step step : trip.getSteps()) {
            // Accumulate time to track when each step occurs
            int stepStartTime = cumulativeTimeSec;
            cumulativeTimeSec += step.getDurationSec();

            // Only process boarding steps
            if (step.getType() != StepType.BOARD) {
                continue;
            }

            // Get route info from step
            String routeId = step.getRouteInfo().orElse(null);
            if (routeId == null) {
                continue; // Skip if no route info
            }

            RouteType mode = getRouteType(routeId);
            int baseFare = (mode == RouteType.METRO) ? config.getMetroFareCOP() : config.getBusFareCOP();

            // Check for integration on second boarding
            if (firstBoardingTimeSec == null) {
                // First boarding - pay base fare
                segments.add(new FareSegment(mode, baseFare, baseFare, false));
                totalFare += baseFare;
                firstBoardingTimeSec = stepStartTime;
                firstBoardingMode = mode;
            } else {
                // Second or later boarding - check for integration
                int timeSinceFirstBoarding = stepStartTime - firstBoardingTimeSec;
                int minutesSinceFirstBoarding = timeSinceFirstBoarding / 60;

                boolean canApplyIntegration = segments.size() == 1 && // Only for first 2 boardings
                        minutesSinceFirstBoarding <= config.getIntegrationWindowMinutes() &&
                        mode != firstBoardingMode; // Different mode

                if (canApplyIntegration) {
                    // Apply integration: replace first fare + second fare with integrated fare
                    int firstFare = segments.get(0).getAppliedFareCOP();
                    int integratedTotal = config.getIntegratedFareCOP();
                    int secondFare = integratedTotal - firstFare;

                    segments.add(new FareSegment(mode, baseFare, secondFare, true));
                    totalFare += secondFare;
                    integrationApplied = true;
                } else {
                    // No integration - pay base fare
                    segments.add(new FareSegment(mode, baseFare, baseFare, false));
                    totalFare += baseFare;
                }
            }
        }

        // If no transit segments, return zero fare (walking only)
        if (segments.isEmpty()) {
            return FareResult.zero();
        }

        return new FareResult(totalFare, segments, integrationApplied);
    }
}
