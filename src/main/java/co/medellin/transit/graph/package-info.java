/**
 * Graph construction and data structures for routing.
 * 
 * Builds and maintains the in-memory transit graph:
 * - TransitGraph: Adjacency list representation optimized for Dijkstra's algorithm
 * - GraphBuilder: Constructs graph from CSV data, adds walking edges
 * - Spatial index for efficient nearest-neighbor queries
 * 
 * Target: 13,895 states, 82,160 edges, construction time <1 second.
 */
package co.medellin.transit.graph;
