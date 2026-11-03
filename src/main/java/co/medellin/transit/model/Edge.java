package co.medellin.transit.model;

import java.util.Objects;

/**
 * Represents an edge (connection) between two states in the transit routing graph.
 * 
 * <p>Each edge has:
 * <ul>
 * <li>Source and target state IDs</li>
 * <li>Edge kind (WALK, BOARD, RIDE, ALIGHT)</li>
 * <li>Real travel time in seconds (actual time for display)</li>
 * <li>Perceived cost (routing cost including penalties)</li>
 * </ul>
 * 
 * <p>The perceived cost is used by Dijkstra's algorithm and may include penalties:
 * <ul>
 * <li>Boarding penalty: +120 seconds per boarding action</li>
 * <li>Walking penalty: walkTime * 1.5 + 30 seconds per walking edge</li>
 * </ul>
 * 
 * <p>This class is immutable.
 */
public final class Edge {
    private final int sourceId;
    private final int targetId;
    private final EdgeKind kind;
    private final int realSec;
    private final int perceivedCost;
    
    /**
     * Creates an edge between two states.
     * 
     * @param sourceId source state ID
     * @param targetId target state ID
     * @param kind edge type
     * @param realSec actual travel time in seconds
     * @param perceivedCost routing cost (includes penalties)
     * @throws IllegalArgumentException if sourceId or targetId is negative, or if times are negative
     */
    public Edge(int sourceId, int targetId, EdgeKind kind, int realSec, int perceivedCost) {
        if (sourceId < 0) {
            throw new IllegalArgumentException("sourceId must be non-negative");
        }
        if (targetId < 0) {
            throw new IllegalArgumentException("targetId must be non-negative");
        }
        if (realSec < 0) {
            throw new IllegalArgumentException("realSec must be non-negative");
        }
        if (perceivedCost < 0) {
            throw new IllegalArgumentException("perceivedCost must be non-negative");
        }
        
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");
        this.realSec = realSec;
        this.perceivedCost = perceivedCost;
    }
    
    /**
     * Returns the source state ID.
     * 
     * @return source state ID
     */
    public int getSourceId() {
        return sourceId;
    }
    
    /**
     * Returns the target state ID.
     * 
     * @return target state ID
     */
    public int getTargetId() {
        return targetId;
    }
    
    /**
     * Returns the edge kind.
     * 
     * @return edge kind (WALK, BOARD, RIDE, ALIGHT)
     */
    public EdgeKind getKind() {
        return kind;
    }
    
    /**
     * Returns the actual travel time in seconds (without penalties).
     * 
     * @return real travel time in seconds
     */
    public int getRealSec() {
        return realSec;
    }
    
    /**
     * Returns the perceived cost used for routing (includes penalties).
     * 
     * @return perceived cost
     */
    public int getPerceivedCost() {
        return perceivedCost;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return sourceId == edge.sourceId &&
               targetId == edge.targetId &&
               realSec == edge.realSec &&
               perceivedCost == edge.perceivedCost &&
               kind == edge.kind;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sourceId, targetId, kind, realSec, perceivedCost);
    }
    
    @Override
    public String toString() {
        return String.format("Edge{%d -> %d, kind=%s, real=%ds, cost=%d}",
            sourceId, targetId, kind, realSec, perceivedCost);
    }
}
