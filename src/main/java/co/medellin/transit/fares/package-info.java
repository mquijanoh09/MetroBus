/**
 * Fare calculation with integration discount rules.
 * 
 * Computes transit fares based on Medellín's fare structure:
 * - FareCalculator: Main calculation engine with 90-minute integration window
 * - FareConfig: Configurable fare rules (metro fare, bus fare, integration discount)
 * - FareResult: Detailed fare breakdown with per-segment information
 * 
 * Implements integration discount: 50% off second leg when transferring between
 * bus and metro within 90 minutes.
 */
package co.medellin.transit.fares;
