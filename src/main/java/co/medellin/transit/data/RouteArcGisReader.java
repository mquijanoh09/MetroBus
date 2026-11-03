package co.medellin.transit.data;

import co.medellin.transit.model.Route;
import co.medellin.transit.model.RouteType;

import java.util.Optional;

/**
 * Reads route metadata from ArcGIS JSON format files.
 * Parses bus routes and metro lines from ArcGIS feature data.
 * 
 * Expected fields in ArcGIS attributes:
 * - id_ruta (or codigo_ruta or OBJECTID): unique route identifier
 * - nombre_ruta (or nombre): route display name
 * - tipo_ruta (or tipo): route type ("BUS" or "METRO")
 * - color (optional): hex color code for display
 */
public class RouteArcGisReader extends ArcGisReader<Route> {

    private static final String DEFAULT_BUS_COLOR = "#0066CC";
    private static final String DEFAULT_METRO_COLOR = "#00AA00";

    /**
     * Parses a single feature from ArcGIS JSON into a Route object.
     * 
     * @param featureJson the feature JSON string
     * @param featureNumber the feature number (for error reporting)
     * @return parsed Route, or empty to skip
     * @throws Exception if parsing fails
     */
    @Override
    protected Optional<Route> parseFeature(String featureJson, int featureNumber) throws Exception {
        String attributes = extractAttributes(featureJson);
        
        // Extract route ID - try multiple field names
        String routeId;
        Optional<String> routeIdOpt = getStringField(attributes, "id_ruta");
        if (routeIdOpt.isEmpty()) {
            routeIdOpt = getStringField(attributes, "codigo_ruta");
        }
        if (routeIdOpt.isEmpty()) {
            // Try as integer OBJECTID and convert to string
            try {
                int objectId = getIntField(attributes, "OBJECTID");
                routeId = String.valueOf(objectId);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Missing route ID field (id_ruta, codigo_ruta, or OBJECTID)");
            }
        } else {
            routeId = routeIdOpt.get();
        }
        
        // Extract route name - try multiple field names
        Optional<String> nameOpt = getStringField(attributes, "nombre_ruta");
        if (nameOpt.isEmpty()) {
            nameOpt = getStringField(attributes, "nombre");
        }
        if (nameOpt.isEmpty()) {
            // Default to route ID if no name provided
            nameOpt = Optional.of("Ruta " + routeId);
        }
        String displayName = nameOpt.get();
        
        // Extract route type - try multiple field names
        RouteType routeType = RouteType.BUS;  // Default to BUS
        Optional<String> typeOpt = getStringField(attributes, "tipo_ruta");
        if (typeOpt.isEmpty()) {
            typeOpt = getStringField(attributes, "tipo");
        }
        if (typeOpt.isPresent()) {
            String typeStr = typeOpt.get().toUpperCase();
            if (typeStr.contains("METRO") || typeStr.equals("M")) {
                routeType = RouteType.METRO;
            } else if (typeStr.contains("BUS") || typeStr.equals("B")) {
                routeType = RouteType.BUS;
            }
        }
        
        // Extract color (optional)
        String color = getStringField(attributes, "color")
            .orElse(routeType == RouteType.METRO ? DEFAULT_METRO_COLOR : DEFAULT_BUS_COLOR);
        
        // Ensure color has # prefix
        if (!color.startsWith("#")) {
            color = "#" + color;
        }
        
        return Optional.of(new Route(routeId, displayName, routeType, color));
    }
}
