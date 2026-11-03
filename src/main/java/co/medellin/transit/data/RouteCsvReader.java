package co.medellin.transit.data;

import co.medellin.transit.model.Route;
import co.medellin.transit.model.RouteType;
import org.apache.commons.csv.CSVRecord;

import java.util.Optional;

/**
 * Reads route metadata from CSV format files.
 * Parses bus routes and metro lines from CSV data.
 * 
 * Expected CSV fields:
 * - id_ruta (or route_id or codigo_ruta): unique route identifier
 * - nombre_ruta (or nombre or name): route display name
 * - tipo_ruta (or tipo or type): route type ("BUS" or "METRO")
 * - color (optional): hex color code for display
 */
public class RouteCsvReader extends CsvReader<Route> {

    private static final String DEFAULT_BUS_COLOR = "#0066CC";
    private static final String DEFAULT_METRO_COLOR = "#00AA00";

    /**
     * Parses a single CSV record into a Route object.
     * 
     * @param record the CSV record to parse
     * @param lineNumber the line number (for error reporting)
     * @return parsed Route, or empty to skip
     * @throws Exception if parsing fails
     */
    @Override
    protected Optional<Route> parseRecord(CSVRecord record, int lineNumber) throws Exception {
        // Extract route ID - try multiple field names
        String routeId;
        if (hasField(record, "id_ruta")) {
            routeId = getString(record, "id_ruta");
        } else if (hasField(record, "codigo_ruta")) {
            routeId = getString(record, "codigo_ruta");
        } else if (hasField(record, "route_id")) {
            routeId = getString(record, "route_id");
        } else if (hasField(record, "id")) {
            routeId = getString(record, "id");
        } else {
            throw new IllegalArgumentException("Missing route ID field (id_ruta, codigo_ruta, route_id, or id)");
        }
        
        if (routeId.isEmpty()) {
            throw new IllegalArgumentException("Route ID cannot be empty");
        }
        
        // Extract route name - try multiple field names
        String displayName;
        if (hasField(record, "nombre_ruta")) {
            displayName = getString(record, "nombre_ruta");
        } else if (hasField(record, "nombre")) {
            displayName = getString(record, "nombre");
        } else if (hasField(record, "name")) {
            displayName = getString(record, "name");
        } else {
            // Default to "Ruta {id}" if no name provided
            displayName = "Ruta " + routeId;
        }
        
        if (displayName.isEmpty()) {
            displayName = "Ruta " + routeId;
        }
        
        // Extract route type - try multiple field names
        RouteType routeType = RouteType.BUS;  // Default to BUS
        if (hasField(record, "tipo_ruta")) {
            String typeStr = getString(record, "tipo_ruta").toUpperCase();
            routeType = parseRouteType(typeStr);
        } else if (hasField(record, "tipo")) {
            String typeStr = getString(record, "tipo").toUpperCase();
            routeType = parseRouteType(typeStr);
        } else if (hasField(record, "type")) {
            String typeStr = getString(record, "type").toUpperCase();
            routeType = parseRouteType(typeStr);
        }
        
        // Extract color (optional)
        String color;
        if (hasField(record, "color")) {
            color = getOptionalString(record, "color")
                .orElse(routeType == RouteType.METRO ? DEFAULT_METRO_COLOR : DEFAULT_BUS_COLOR);
        } else {
            color = routeType == RouteType.METRO ? DEFAULT_METRO_COLOR : DEFAULT_BUS_COLOR;
        }
        
        // Ensure color has # prefix
        if (!color.isEmpty() && !color.startsWith("#")) {
            color = "#" + color;
        }
        
        return Optional.of(new Route(routeId, displayName, routeType, color));
    }
    
    /**
     * Parses a route type string to RouteType enum.
     * 
     * @param typeStr the type string (case-insensitive)
     * @return RouteType (defaults to BUS if unrecognized)
     */
    private RouteType parseRouteType(String typeStr) {
        if (typeStr.isEmpty()) {
            return RouteType.BUS;
        }
        
        if (typeStr.contains("METRO") || typeStr.equals("M")) {
            return RouteType.METRO;
        } else if (typeStr.contains("BUS") || typeStr.equals("B")) {
            return RouteType.BUS;
        }
        
        // Default to BUS for unrecognized types
        return RouteType.BUS;
    }
}
