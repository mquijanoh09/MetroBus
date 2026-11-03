package co.medellin.transit.data;

import co.medellin.transit.model.Route;

import java.util.Optional;

/**
 * Reads route metadata from ArcGIS JSON format files.
 * Parses bus routes from routes.json.
 * 
 * Expected fields in ArcGIS attributes:
 * - id_ruta: unique internal route identifier (parsed as integer)
 * - codigo: user-visible route code (e.g., "263")
 * - nombre: route display name
 * - tipo: route type for fares (e.g., "Directa Centro", "Integrada")
 */
public class RouteArcGisReader extends ArcGisReader<Route> {

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
        
        // Extract id_ruta as integer
        int id;
        Optional<String> idRutaOpt = getStringField(attributes, "id_ruta");
        if (idRutaOpt.isEmpty()) {
            throw new IllegalArgumentException("Missing id_ruta field");
        }
        try {
            id = Integer.parseInt(idRutaOpt.get());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id_ruta must be numeric: " + idRutaOpt.get());
        }
        
        // Extract codigo (user-visible route code)
        Optional<String> codigoOpt = getStringField(attributes, "codigo");
        if (codigoOpt.isEmpty()) {
            throw new IllegalArgumentException("Missing codigo field");
        }
        String codigo = codigoOpt.get();
        
        // Extract nombre (route name)
        Optional<String> nombreOpt = getStringField(attributes, "nombre");
        if (nombreOpt.isEmpty()) {
            throw new IllegalArgumentException("Missing nombre field");
        }
        String nombre = nombreOpt.get();
        
        // Extract tipo (route type for fares)
        Optional<String> tipoOpt = getStringField(attributes, "tipo");
        if (tipoOpt.isEmpty()) {
            throw new IllegalArgumentException("Missing tipo field");
        }
        String tipo = tipoOpt.get();
        
        return Optional.of(new Route(id, codigo, nombre, tipo));
    }
}
