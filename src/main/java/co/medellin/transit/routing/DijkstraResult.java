package co.medellin.transit.routing;

import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.State;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of Dijkstra's routing algorithm containing the optimal path and associated costs.
 * 
 * <p>Contains two cost metrics:
 * <ul>
 *   <li><b>Perceived cost</b>: Used for route optimization, includes penalties for boarding and walking</li>
 *   <li><b>Real duration</b>: Actual travel time without penalties, displayed to user</li>
 * </ul>
 * 
 * <p>The path is an ordered list of states from origin to destination, where consecutive
 * states are connected by edges in the transit graph.
 * 
 * <p>Access and egress walking (from actual origin to first stop, and from last stop to destination)
 * are tracked separately since they occur outside the graph.
 */
public class DijkstraResult {
    private final List<State> path;
    private final int perceivedCost;
    private final int realDurationSec;
    private final Coordinate actualOrigin;
    private final Coordinate actualDestination;
    private final double accessWalkMeters;
    private final int accessWalkTimeSec;
    private final double egressWalkMeters;
    private final int egressWalkTimeSec;
    
    /**
     * Creates a new DijkstraResult without access/egress walking.
     * Used for backward compatibility and testing.
     * 
     * @param path ordered list of states from origin to destination
     * @param perceivedCost optimization cost including penalties (seconds)
     * @param realDurationSec actual travel time without penalties (seconds)
     */
    public DijkstraResult(List<State> path, int perceivedCost, int realDurationSec) {
        this(path, perceivedCost, realDurationSec, null, null, 0, 0, 0, 0);
    }
    
    /**
     * Creates a new DijkstraResult with access/egress walking information.
     * 
     * @param path ordered list of states from origin to destination
     * @param perceivedCost optimization cost including penalties (seconds)
     * @param realDurationSec actual travel time without penalties (seconds)
     * @param actualOrigin user's actual origin coordinate
     * @param actualDestination user's actual destination coordinate
     * @param accessWalkMeters walking distance from origin to first stop (meters)
     * @param accessWalkTimeSec walking time from origin to first stop (seconds, no penalty)
     * @param egressWalkMeters walking distance from last stop to destination (meters)
     * @param egressWalkTimeSec walking time from last stop to destination (seconds, no penalty)
     */
    public DijkstraResult(List<State> path, int perceivedCost, int realDurationSec,
                         Coordinate actualOrigin, Coordinate actualDestination,
                         double accessWalkMeters, int accessWalkTimeSec,
                         double egressWalkMeters, int egressWalkTimeSec) {
        Objects.requireNonNull(path, "path cannot be null");
        
        if (path.isEmpty()) {
            throw new IllegalArgumentException("path cannot be empty");
        }
        
        if (perceivedCost < 0) {
            throw new IllegalArgumentException("perceivedCost must be non-negative");
        }
        
        if (realDurationSec < 0) {
            throw new IllegalArgumentException("realDurationSec must be non-negative");
        }
        
        this.path = Collections.unmodifiableList(path);
        this.perceivedCost = perceivedCost;
        this.realDurationSec = realDurationSec;
        this.actualOrigin = actualOrigin;
        this.actualDestination = actualDestination;
        this.accessWalkMeters = accessWalkMeters;
        this.accessWalkTimeSec = accessWalkTimeSec;
        this.egressWalkMeters = egressWalkMeters;
        this.egressWalkTimeSec = egressWalkTimeSec;
    }
    
    /**
     * Returns the path as an ordered list of states from origin to destination.
     * 
     * @return unmodifiable path list
     */
    public List<State> getPath() {
        return path;
    }
    
    /**
     * Returns the perceived cost used for optimization (includes penalties).
     * This is the cost that Dijkstra's algorithm minimized.
     * 
     * @return perceived cost in seconds
     */
    public int getPerceivedCost() {
        return perceivedCost;
    }
    
    /**
     * Returns the real travel duration without penalties.
     * This is the actual time the trip will take.
     * 
     * @return real duration in seconds
     */
    public int getRealDurationSec() {
        return realDurationSec;
    }
    
    /**
     * Returns the actual origin coordinate (user's location).
     * 
     * @return origin coordinate or null if not tracking access walking
     */
    public Coordinate getActualOrigin() {
        return actualOrigin;
    }
    
    /**
     * Returns the actual destination coordinate (user's location).
     * 
     * @return destination coordinate or null if not tracking egress walking
     */
    public Coordinate getActualDestination() {
        return actualDestination;
    }
    
    /**
     * Returns the walking distance from actual origin to first stop.
     * 
     * @return access walk distance in meters
     */
    public double getAccessWalkMeters() {
        return accessWalkMeters;
    }
    
    /**
     * Returns the walking time from actual origin to first stop (without penalty).
     * 
     * @return access walk time in seconds
     */
    public int getAccessWalkTimeSec() {
        return accessWalkTimeSec;
    }
    
    /**
     * Returns the walking distance from last stop to actual destination.
     * 
     * @return egress walk distance in meters
     */
    public double getEgressWalkMeters() {
        return egressWalkMeters;
    }
    
    /**
     * Returns the walking time from last stop to actual destination (without penalty).
     * 
     * @return egress walk time in seconds
     */
    public int getEgressWalkTimeSec() {
        return egressWalkTimeSec;
    }
    
    /**
     * Returns the number of states in the path.
     * 
     * @return path length
     */
    public int getPathLength() {
        return path.size();
    }
    
    /**
     * Returns the origin state (first state in path).
     * 
     * @return origin state
     */
    public State getOrigin() {
        return path.get(0);
    }
    
    /**
     * Returns the destination state (last state in path).
     * 
     * @return destination state
     */
    public State getDestination() {
        return path.get(path.size() - 1);
    }
    
    @Override
    public String toString() {
        return String.format("DijkstraResult{pathLength=%d, perceivedCost=%ds, realDuration=%ds, accessWalk=%.0fm, egressWalk=%.0fm}", 
                           path.size(), perceivedCost, realDurationSec, accessWalkMeters, egressWalkMeters);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DijkstraResult that = (DijkstraResult) o;
        return perceivedCost == that.perceivedCost &&
               realDurationSec == that.realDurationSec &&
               path.equals(that.path);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path, perceivedCost, realDurationSec);
    }
}