/**
 * Route computation using multi-source/multi-target Dijkstra's algorithm.
 * 
 * Implements optimal pathfinding for multi-modal transit:
 * - Router: Main routing engine with multi-source/multi-target Dijkstra
 * - Parameters: Configurable routing behavior (access radius, penalties)
 * - DijkstraResult: Path reconstruction and result representation
 * 
 * Target: query time <100ms, handles boarding penalties and walk penalties.
 */
package co.medellin.transit.routing;
