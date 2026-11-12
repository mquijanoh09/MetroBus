package co.medellin.transit.routing;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Edge;
import co.medellin.transit.model.EdgeKind;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.State;
import co.medellin.transit.model.StateType;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Computes optimal transit routes using multi-source/multi-target Dijkstra's algorithm.
 * 
 * <p>The router finds the shortest path (in terms of perceived cost) between an origin
 * and destination, considering:
 * <ul>
 *   <li>Real travel time (walking, riding transit)</li>
 *   <li>Boarding penalties to discourage excessive transfers</li>
 *   <li>Walking penalties to make walking "feel" longer in optimization</li>
 * </ul>
 * 
 * <p>Multi-source/multi-target optimization:
 * <ul>
 *   <li>Initializes with all states within access radius of origin (500m)</li>
 *   <li>Terminates when best path to any target state is found</li>
 *   <li>Early termination: stops when pq.min > bestTargetCost (optimal solution proven)</li>
 * </ul>
 * 
 * <p>Performance characteristics:
 * <ul>
 *   <li>Query time: target <100ms per route on 13,895-node graph</li>
 *   <li>Memory: reuses distance/previous arrays across queries</li>
 *   <li>Data structures: primitive int[] arrays, binary heap priority queue</li>
 * </ul>
 */
public class Router {
    private static final Logger LOGGER = Logger.getLogger(Router.class.getName());
    
    private final TransitGraph graph;
    private final Parameters params;
    
    // Reusable arrays for Dijkstra (avoid allocations per query)
    private final int[] distances;
    private final int[] previous;
    private final boolean[] visited;
    
    /**
     * Creates a new Router with the given graph and parameters.
     * 
     * @param graph transit graph to route on
     * @param params routing parameters (access radius, penalties)
     * @throws NullPointerException if graph or params is null
     */
    public Router(TransitGraph graph, Parameters params) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
        this.params = Objects.requireNonNull(params, "params cannot be null");
        
        int stateCount = graph.getStateCount();
        this.distances = new int[stateCount];
        this.previous = new int[stateCount];
        this.visited = new boolean[stateCount];
        
        LOGGER.log(Level.INFO, "Router initialized for graph with {0} states, {1} edges", 
                   new Object[]{stateCount, graph.getEdgeCount()});
    }
    
    /**
     * Finds optimal route from origin to destination.
     * 
     * @param origin starting place
     * @param destination ending place
     * @return routing result containing path and costs, or empty if no route exists
     * @throws NullPointerException if origin or destination is null
     */
    public Optional<DijkstraResult> findRoute(Place origin, Place destination) {
        Objects.requireNonNull(origin, "origin cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        
        long startTime = System.nanoTime();
        
        // Phase 1: Find accessible states within access radius
        // CRITICAL: Only use WALK states as sources/targets to ensure proper BOARD edges
        List<State> allSourceStates = graph.getStatesNearPlace(origin, params.getAccessRadiusMeters());
        List<State> allTargetStates = graph.getStatesNearPlace(destination, params.getAccessRadiusMeters());
        
        // Filter to only WALK states (prevents starting from BUS_STOP/METRO_STATION states
        // which would cause RIDE edges without preceding BOARD edges)
        List<State> sourceStates = allSourceStates.stream()
            .filter(s -> s.getType() == StateType.WALK)
            .toList();
        List<State> targetStates = allTargetStates.stream()
            .filter(s -> s.getType() == StateType.WALK)
            .toList();
        
        if (sourceStates.isEmpty()) {
            LOGGER.log(Level.FINE, "No WALK states within {0}m of origin", params.getAccessRadiusMeters());
            return Optional.empty();
        }
        
        if (targetStates.isEmpty()) {
            LOGGER.log(Level.FINE, "No WALK states within {0}m of destination", params.getAccessRadiusMeters());
            return Optional.empty();
        }
        
        LOGGER.log(Level.FINE, "Found {0} source states, {1} target states", 
                   new Object[]{sourceStates.size(), targetStates.size()});
        
        // Phase 2: Run multi-source/multi-target Dijkstra with access/egress walking
        Optional<DijkstraResult> result = dijkstra(sourceStates, targetStates, origin, destination);
        
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        
        if (result.isPresent()) {
            LOGGER.log(Level.FINE, "Route found in {0}ms: {1} states, perceived cost {2}s", 
                       new Object[]{elapsedMs, result.get().getPath().size(), 
                                   result.get().getPerceivedCost()});
        } else {
            LOGGER.log(Level.FINE, "No route found in {0}ms", elapsedMs);
        }
        
        return result;
    }
    
    /**
     * Multi-source/multi-target Dijkstra's algorithm with walk-only candidate.
     * 
     * <p>Algorithm:
     * <ol>
     *   <li>Initialize walk-only candidate as best (walking directly)</li>
     *   <li>Initialize all sources with access walk cost</li>
     *   <li>Extract minimum cost node from priority queue</li>
     *   <li>If target reached, add egress walk penalty and compare with best</li>
     *   <li>Early termination: when pq.min >= best (optimal proven)</li>
     *   <li>Relax outgoing edges with perceived cost</li>
     *   <li>If walking is as fast in real time, return walk-only trip</li>
     * </ol>
     * 
     * @param sources starting states
     * @param targets destination states
     * @param origin actual origin place
     * @param destination actual destination place
     * @return best path or empty if no path exists
     */
    private Optional<DijkstraResult> dijkstra(List<State> sources, List<State> targets, Place origin, Place destination) {
        // Initialize
        Arrays.fill(distances, Integer.MAX_VALUE);
        Arrays.fill(previous, -1);
        Arrays.fill(visited, false);
        
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Set<Integer> sourceSet = new HashSet<>();
        Map<Integer, TargetInfo> targetMap = new HashMap<>();
        
        // Calculate direct walk as the initial best (walk-only candidate)
        double directDistMeters = haversineDistance(origin.getCoord(), destination.getCoord());
        double directWalkMeters = directDistMeters * params.getWalkStreetFactor();
        int directWalkSec = (int) Math.round(directWalkMeters / params.getWalkingSpeedMps());
        int directWalkPerceived = (int) Math.round(directWalkSec * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
        
        int bestPerceivedCost = directWalkPerceived;
        int bestTargetId = -1;
        int bestAccessWalkSec = 0;
        double bestAccessWalkMeters = 0;
        int bestEgressWalkSec = 0;
        double bestEgressWalkMeters = 0;
        
        // Add all sources with walking cost from origin (access walking)
        Map<Integer, AccessInfo> accessMap = new HashMap<>();
        for (State source : sources) {
            sourceSet.add(source.getId());
            // Calculate walking distance and time
            double distanceMeters = haversineDistance(origin.getCoord(), source.getPlace().getCoord());
            double walkDistanceMeters = distanceMeters * params.getWalkStreetFactor();
            int walkTimeSec = (int) Math.round(walkDistanceMeters / params.getWalkingSpeedMps());
            // Apply walk penalty for perceived cost
            int perceivedCost = (int) Math.round(walkTimeSec * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
            
            distances[source.getId()] = perceivedCost;
            pq.offer(new Node(source.getId(), perceivedCost));
            accessMap.put(source.getId(), new AccessInfo(walkDistanceMeters, walkTimeSec));
        }
        
        // Build target map with egress walk info
        for (State target : targets) {
            double distanceMeters = haversineDistance(target.getPlace().getCoord(), destination.getCoord());
            double walkDistanceMeters = distanceMeters * params.getWalkStreetFactor();
            int walkTimeSec = (int) Math.round(walkDistanceMeters / params.getWalkingSpeedMps());
            int perceivedCost = (int) Math.round(walkTimeSec * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
            
            targetMap.put(target.getId(), new TargetInfo(walkDistanceMeters, walkTimeSec, perceivedCost));
        }
        
        // Main Dijkstra loop
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int currentId = current.stateId;
            int currentCost = current.cost;
            
            // Early termination: if current cost >= best, all remaining will be worse
            if (currentCost >= bestPerceivedCost) {
                break;
            }
            
            // Skip if already visited with better cost
            if (visited[currentId]) {
                continue;
            }
            visited[currentId] = true;
            
            // Check if this is a target (only WALK states can be targets)
            TargetInfo targetInfo = targetMap.get(currentId);
            if (targetInfo != null) {
                // Add egress walk penalty to get total perceived cost
                int totalPerceivedCost = currentCost + targetInfo.perceivedCost;
                
                if (totalPerceivedCost < bestPerceivedCost) {
                    bestPerceivedCost = totalPerceivedCost;
                    bestTargetId = currentId;
                    bestEgressWalkMeters = targetInfo.meters;
                    bestEgressWalkSec = targetInfo.seconds;
                }
            }
            
            // Relax outgoing edges
            for (Edge edge : graph.getOutgoingEdges(currentId)) {
                int neighborId = edge.getTargetId();
                
                // Skip if already visited
                if (visited[neighborId]) {
                    continue;
                }
                
                int newCost = currentCost + edge.getPerceivedCost();
                
                if (newCost < distances[neighborId]) {
                    distances[neighborId] = newCost;
                    previous[neighborId] = currentId;
                    pq.offer(new Node(neighborId, newCost));
                }
            }
        }
        
        // If no path found through graph, return empty (walk-only will be handled by caller if appropriate)
        if (bestTargetId == -1) {
            return Optional.empty();
        }
        
        // Reconstruct path
        List<State> path = reconstructPath(bestTargetId);
        
        // Find access info for the first state in path
        int firstStateId = path.get(0).getId();
        AccessInfo accessInfo = accessMap.get(firstStateId);
        if (accessInfo != null) {
            bestAccessWalkMeters = accessInfo.meters;
            bestAccessWalkSec = accessInfo.seconds;
        }
        
        // Calculate real duration (without penalties) including access and egress walks
        int graphRealDuration = calculateRealDuration(path);
        int totalRealDuration = bestAccessWalkSec + graphRealDuration + bestEgressWalkSec;
        
        // Rule: If walking all the way is as fast or faster in REAL time, return walk-only
        // BUT: Only if we actually found a transit path to compare against
        if (bestTargetId != -1 && directWalkSec <= totalRealDuration) {
            // Return a walk-only result: single-state path with walk time
            State walkOnlyState = path.get(0); // Use first source state as placeholder
            return Optional.of(new DijkstraResult(
                Collections.singletonList(walkOnlyState),
                directWalkPerceived,
                directWalkSec,
                origin.getCoord(),
                destination.getCoord(),
                directWalkMeters,
                directWalkSec,
                0,
                0
            ));
        }
        
        return Optional.of(new DijkstraResult(
            path,
            bestPerceivedCost,
            totalRealDuration,
            origin.getCoord(),
            destination.getCoord(),
            bestAccessWalkMeters,
            bestAccessWalkSec,
            bestEgressWalkMeters,
            bestEgressWalkSec
        ));
    }
    
    /**
     * Access walk information for a source state.
     */
    private static class AccessInfo {
        final double meters;
        final int seconds;
        
        AccessInfo(double meters, int seconds) {
            this.meters = meters;
            this.seconds = seconds;
        }
    }
    
    /**
     * Target information including egress walk cost.
     */
    private static class TargetInfo {
        final double meters;
        final int seconds;
        final int perceivedCost;
        
        TargetInfo(double meters, int seconds, int perceivedCost) {
            this.meters = meters;
            this.seconds = seconds;
            this.perceivedCost = perceivedCost;
        }
    }
    
    /**
     * Reconstructs the path from source to target using previous[] array.
     * 
     * @param targetId ending state ID
     * @return ordered list of states from source to target
     */
    private List<State> reconstructPath(int targetId) {
        LinkedList<State> path = new LinkedList<>();
        int currentId = targetId;
        
        while (currentId != -1) {
            path.addFirst(graph.getState(currentId));
            currentId = previous[currentId];
        }
        
        return path;
    }
    
    /**
     * Calculates real travel duration (without penalties) by traversing path.
     * 
     * @param path ordered list of states
     * @return real duration in seconds
     */
    private int calculateRealDuration(List<State> path) {
        int duration = 0;
        
        for (int i = 0; i < path.size() - 1; i++) {
            State current = path.get(i);
            State next = path.get(i + 1);
            
            // Find the edge between current and next
            for (Edge edge : graph.getOutgoingEdges(current.getId())) {
                if (edge.getTargetId() == next.getId()) {
                    duration += edge.getRealSec();
                    LOGGER.log(Level.FINE, "Edge {0}->{1} ({2}): +{3}s, total={4}s", 
                        new Object[]{current.getId(), next.getId(), edge.getKind(), edge.getRealSec(), duration});
                    break;
                }
            }
        }
        
        LOGGER.log(Level.FINE, "Total graph duration: {0}s", duration);
        return duration;
    }
    
    /**
     * Priority queue node for Dijkstra's algorithm.
     * Represents a state with its associated cost.
     */
    private static class Node {
        final int stateId;
        final int cost;
        
        Node(int stateId, int cost) {
            this.stateId = stateId;
            this.cost = cost;
        }
    }
    /**
     * Calculates the great-circle distance between two coordinates using the Haversine formula.
     * 
     * @param c1 first coordinate
     * @param c2 second coordinate
     * @return distance in meters
     */
    private double haversineDistance(Coordinate c1, Coordinate c2) {
        final double EARTH_RADIUS_METERS = 6371000.0;
        
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