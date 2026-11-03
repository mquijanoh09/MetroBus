/**
 * Data loading and CSV parsing components.
 * 
 * Handles reading transit data from CSV files:
 * - CsvReader: Base infrastructure for buffered CSV reading
 * - ArcGisReader: Parser for ArcGIS geometry formats
 * - StopCsvReader: Loads bus stops and metro stations
 * - RouteCsvReader: Loads route information (bus lines, metro lines)
 * - GraphCsvReader: Loads pre-computed graph edges
 * 
 * Implements robust error handling for malformed data.
 */
package co.medellin.transit.data;
