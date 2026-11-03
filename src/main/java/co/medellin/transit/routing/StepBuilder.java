package co.medellin.transit.routing;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Edge;
import co.medellin.transit.model.EdgeKind;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.State;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Converts Dijkstra routing results into human-readable trip instructions in Spanish.
 * 
 * <p>This class transforms a sequence of states and edges from the routing algorithm
 * into user-friendly step-by-step instructions. Key responsibilities:
 * <ul>
 * <li>Convert WALK edges to "Caminar a {place}" steps</li>
 * <li>Convert BOARD edges to "Abordar {route} en {place}" steps</li>
 * <li>Consolidate consecutive RIDE edges into "Viajar en {route} por {duration} minutos"</li>
 * <li>Convert ALIGHT edges to "Bajar en {place}" steps</li>
 * <li>Omit short walks (<50m) between stops of the same route</li>
 * <li>Calculate total duration and walking distance</li>
 * </ul>
 * 
 * <p>All user-facing text is generated in Spanish.
 */
public final class StepBuilder {
    
    private static final double MINIMUM_WALK_METERS = 50.0;
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    
    private final TransitGraph graph;
    
    /**
     * Creates a new StepBuilder for the given transit graph.
     * 
     * @param graph the transit graph containing state and edge information
     * @throws NullPointerException if graph is null
     */
    public StepBuilder(TransitGraph graph) {
        if (graph == null) {
            throw new NullPointerException("graph cannot be null");
        }
        this.graph = graph;
    }
    
    /**
     * Builds a complete trip with human-readable instructions from a routing result.
     * 
     * <p>The method processes the path of states returned by Dijkstra's algorithm
     * and converts them into actionable steps. Consecutive RIDE edges on the same
     * route are consolidated into a single step showing the total travel time.
     * Very short walks between stops are omitted for clarity.
     * 
     * @param result the Dijkstra result containing the optimal path
     * @return a Trip with ordered steps and summary statistics
     * @throws NullPointerException if result is null
     * @throws IllegalArgumentException if the path contains fewer than 2 states
     */
    public Trip buildTrip(DijkstraResult result) {
        if (result == null) {
            throw new NullPointerException("result cannot be null");
        }
        
        List<State> path = result.getPath();
        
        if (path.size() < 2) {
            throw new IllegalArgumentException("Path must contain at least 2 states");
        }
        
        List<Step> steps = new ArrayList<>();
        double totalWalkMeters = 0.0;
        
        // Track state for consolidating ride segments
        String currentRoute = null;
        int rideDurationSec = 0;
        Place rideStartPlace = null;
        Place rideEndPlace = null;
        
        // Process each transition (edge) in the path
        for (int i = 0; i < path.size() - 1; i++) {
            State currentState = path.get(i);
            State nextState = path.get(i + 1);
            
            // Find the edge connecting these states
            Edge edge = findEdge(currentState.getId(), nextState.getId());
            
            if (edge == null) {
                throw new IllegalStateException(
                    String.format("No edge found between state %d and %d", 
                        currentState.getId(), nextState.getId())
                );
            }
            
            EdgeKind kind = edge.getKind();
            
            switch (kind) {
                case WALK:
                    // Flush any accumulated ride before processing walk
                    if (currentRoute != null) {
                        steps.add(createRideStep(currentRoute, rideDurationSec, rideEndPlace));
                        currentRoute = null;
                        rideDurationSec = 0;
                    }
                    
                    // Calculate walk distance
                    double walkDistance = calculateDistance(
                        currentState.getPlace().getCoord(),
                        nextState.getPlace().getCoord()
                    );
                    
                    // Only add walk steps that are >= 50m
                    if (walkDistance >= MINIMUM_WALK_METERS) {
                        Step walkStep = createWalkStep(nextState.getPlace(), edge.getRealSec());
                        steps.add(walkStep);
                        totalWalkMeters += walkDistance;
                    }
                    break;
                
                case BOARD:
                    // Flush any accumulated ride before boarding a new vehicle
                    if (currentRoute != null) {
                        steps.add(createRideStep(currentRoute, rideDurationSec, rideEndPlace));
                        currentRoute = null;
                        rideDurationSec = 0;
                    }
                    
                    // Add boarding step
                    String routeToBoard = nextState.getRouteId()
                        .orElseThrow(() -> new IllegalStateException(
                            "BOARD edge must lead to a state with a routeId"));
                    
                    Step boardStep = createBoardStep(
                        routeToBoard, 
                        currentState.getPlace()
                    );
                    steps.add(boardStep);
                    
                    // Start tracking this route
                    currentRoute = routeToBoard;
                    rideStartPlace = currentState.getPlace();
                    rideEndPlace = currentState.getPlace();
                    rideDurationSec = 0;
                    break;
                
                case RIDE:
                    // Accumulate ride time on the current route
                    if (currentRoute == null) {
                        throw new IllegalStateException(
                            "RIDE edge encountered without preceding BOARD");
                    }
                    
                    rideDurationSec += edge.getRealSec();
                    rideEndPlace = nextState.getPlace();
                    break;
                
                case ALIGHT:
                    // Flush the accumulated ride
                    if (currentRoute != null) {
                        steps.add(createRideStep(currentRoute, rideDurationSec, rideEndPlace));
                        currentRoute = null;
                        rideDurationSec = 0;
                    }
                    
                    // Add alighting step
                    Step alightStep = createAlightStep(nextState.getPlace(), edge.getRealSec());
                    steps.add(alightStep);
                    break;
            }
        }
        
        // Flush any remaining ride (in case path ends while on vehicle)
        if (currentRoute != null) {
            steps.add(createRideStep(currentRoute, rideDurationSec, rideEndPlace));
        }
        
        return new Trip(steps, result.getRealDurationSec(), totalWalkMeters);
    }
    
    /**
     * Finds an edge between two states in the graph.
     * 
     * @param sourceId source state ID
     * @param targetId target state ID
     * @return the edge, or null if not found
     */
    private Edge findEdge(int sourceId, int targetId) {
        List<Edge> outgoing = graph.getOutgoingEdges(sourceId);
        
        for (Edge edge : outgoing) {
            if (edge.getTargetId() == targetId) {
                return edge;
            }
        }
        
        return null;
    }
    
    /**
     * Creates a WALK step with Spanish instructions.
     * 
     * @param destination the place to walk to
     * @param durationSec walk duration in seconds
     * @return a WALK step
     */
    private Step createWalkStep(Place destination, int durationSec) {
        String description = String.format("Caminar a %s", destination.getName());
        return new Step(StepType.WALK, description, durationSec, destination);
    }
    
    /**
     * Creates a BOARD step with Spanish instructions.
     * 
     * @param routeId the route or line identifier
     * @param location where to board
     * @return a BOARD step
     */
    private Step createBoardStep(String routeId, Place location) {
        String description = String.format("Abordar %s en %s", routeId, location.getName());
        return new Step(StepType.BOARD, description, 0, location, Optional.of(routeId));
    }
    
    /**
     * Creates a RIDE step with Spanish instructions.
     * Consolidates multiple RIDE edges on the same route into a single step.
     * 
     * @param routeId the route or line identifier
     * @param durationSec total ride duration in seconds
     * @param endLocation where the ride ends
     * @return a RIDE step
     */
    private Step createRideStep(String routeId, int durationSec, Place endLocation) {
        int minutes = (int) Math.ceil(durationSec / 60.0);
        String description = String.format("Viajar en %s por %d minutos", routeId, minutes);
        return new Step(StepType.RIDE, description, durationSec, endLocation, Optional.of(routeId));
    }
    
    /**
     * Creates an ALIGHT step with Spanish instructions.
     * 
     * @param location where to alight
     * @param durationSec alight duration (usually short)
     * @return an ALIGHT step
     */
    private Step createAlightStep(Place location, int durationSec) {
        String description = String.format("Bajar en %s", location.getName());
        return new Step(StepType.ALIGHT, description, durationSec, location);
    }
    
    /**
     * Calculates the Haversine distance between two coordinates.
     * 
     * @param c1 first coordinate
     * @param c2 second coordinate
     * @return distance in meters
     */
    private double calculateDistance(Coordinate c1, Coordinate c2) {
        double lat1 = Math.toRadians(c1.getLat());
        double lat2 = Math.toRadians(c2.getLat());
        double deltaLat = Math.toRadians(c2.getLat() - c1.getLat());
        double deltaLon = Math.toRadians(c2.getLon() - c1.getLon());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.asin(Math.sqrt(a));
        
        return EARTH_RADIUS_METERS * c;
    }
}
