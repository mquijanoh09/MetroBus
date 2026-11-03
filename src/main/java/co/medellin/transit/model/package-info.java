/**
 * Core domain models and data structures.
 * 
 * Contains immutable value objects representing transit system entities:
 * - Coordinate: Geographic location (latitude, longitude)
 * - Place: Named location with coordinates
 * - Route: Transit route (bus line or metro line) with display information
 * - State: Node in the routing graph (walk location, bus stop, or metro station)
 * - Edge: Connection between states with cost and metadata
 * - Step: Single instruction in a trip (walk, board, ride, alight)
 * - Trip: Complete journey with steps, duration, and walking distance
 * 
 * These models are the foundation for the entire system.
 */
package co.medellin.transit.model;
