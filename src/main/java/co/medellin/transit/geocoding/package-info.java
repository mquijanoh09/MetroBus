/**
 * Geocoding module for converting Medellín addresses to geographic coordinates.
 * 
 * <p>This package provides functionality to parse and locate Medellín street addresses:
 * <ul>
 *   <li>{@link co.medellin.transit.geocoding.AddressParser} - Parses address strings into structured components</li>
 *   <li>{@link co.medellin.transit.geocoding.ParsedAddress} - Represents a parsed address</li>
 *   <li>{@link co.medellin.transit.geocoding.StreetType} - Enum for street types (CALLE, CARRERA, etc.)</li>
 *   <li>{@link co.medellin.transit.geocoding.Cardinal} - Enum for cardinal directions (NORTE, SUR, etc.)</li>
 * </ul>
 * 
 * <p>The geocoding process follows Medellín's standard address format:
 * {@code (StreetType) (Number)[Letter] [Cardinal] # (Number)[Letter] [Cardinal] - (BuildingNumber)}
 * 
 * <p>Example usage:
 * <pre>{@code
 * AddressParser parser = new AddressParser();
 * Optional<ParsedAddress> address = parser.parse("Calle 50 # 53-48");
 * if (address.isPresent()) {
 *     // Use parsed address for coordinate lookup
 * }
 * }</pre>
 */
package co.medellin.transit.geocoding;
