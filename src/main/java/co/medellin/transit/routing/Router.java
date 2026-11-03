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
     * Multi-source/multi-target Dijkstra's algorithm.
     * 
     * <p>Algorithm:
     * <ol>
     *   <li>Initialize all sources with distance 0</li>
     *   <li>Extract minimum cost node from priority queue</li>
     *   <li>If target reached, track best target cost</li>
     *   <li>Early termination: when pq.min > bestTargetCost (optimal proven)</li>
     *   <li>Relax outgoing edges with perceived cost</li>
     *   <li>Reconstruct path from previous[] array</li>
     * </ol>
     * 
     * @param sources starting states
     * @param targets destination states
     * @return best path or empty if no path exists
     */
    private Optional<DijkstraResult> dijkstra(List<State> sources, List<State> targets, Place origin, Place destination) {
        // Initialize
        Arrays.fill(distances, Integer.MAX_VALUE);
        Arrays.fill(previous, -1);
        Arrays.fill(visited, false);
        
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Set<Integer> sourceSet = new HashSet<>();
        Set<Integer> targetSet = new HashSet<>();
        
        // Add all sources with walking cost from origin (access walking)
        for (State source : sources) {
            sourceSet.add(source.getId());
            // Calculate walking distance and time
            double distanceMeters = haversineDistance(origin.getCoord(), source.getPlace().getCoord());
            double walkDistanceMeters = distanceMeters * params.getWalkStreetFactor();
            int walkTimeSec = (int) Math.round(walkDistanceMeters / params.getWalkingSpeedMps());
            // Apply walk penalty
            int perceivedCost = (int) Math.round(walkTimeSec * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
            
            distances[source.getId()] = perceivedCost;
            pq.offer(new Node(source.getId(), perceivedCost));
        }
        
        // Build target set for fast lookup
        for (State target : targets) {
            targetSet.add(target.getId());
        }
        
        int bestTargetCost = Integer.MAX_VALUE;
        int bestTargetId = -1;
        
        // Main Dijkstra loop
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int currentId = current.stateId;
            int currentCost = current.cost;
            
            // Skip if already visited with better cost
            if (visited[currentId]) {
                continue;
            }
            visited[currentId] = true;
            
            // Check if this is a target AND we've actually traversed at least one edge
            // (previous[currentId] != -1 means we reached this via an edge, not just initialization)
            if (targetSet.contains(currentId) && previous[currentId] != -1) {
                if (currentCost < bestTargetCost) {
                    bestTargetCost = currentCost;
                    bestTargetId = currentId;
                }
                // Early termination: if min pq cost > best target, we're done
                // (all remaining paths will be worse)
                if (!pq.isEmpty() && pq.peek().cost > bestTargetCost) {
                    break;
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
        
        // Check if we found a path
        if (bestTargetId == -1) {
            return Optional.empty();
        }
        
        // Reconstruct path
        List<State> path = reconstructPath(bestTargetId);
        
        // Calculate real duration (without penalties)
        int realDurationSec = calculateRealDuration(path);
        
        return Optional.of(new DijkstraResult(path, bestTargetCost, realDurationSec));
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
                    break;
                }
            }
        }
        
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