package co.medellin.transit.data;

import co.medellin.transit.model.Coordinate;

import java.util.Optional;

/**
 * Reads station data from ArcGIS JSON format files.
 * Parses transit stations (metro, Metroplús, tranvía, metrocable) from transit_stations.json.
 * 
 * Expected fields in ArcGIS:
 * - attributes.nombre: station name
 * - attributes.linea: line identifier (e.g., "Línea A", "Línea 1")
 * - geometry.x: longitude
 * - geometry.y: latitude
 */
public class StationArcGisReader extends ArcGisReader<Station> {

    /**
     * Parses a single feature from ArcGIS JSON into a Station object.
     * 
     * @param featureJson the feature JSON string
     * @param featureNumber the feature number (for error reporting)
     * @return parsed Station, or empty to skip
     * @throws Exception if parsing fails
     */
    @Override
    protected Optional<Station> parseFeature(String featureJson, int featureNumber) throws Exception {
        String attributes = extractAttributes(featureJson);
        Optional<String> geometryOpt = extractGeometry(featureJson);
        
        // Extract station name
        Optional<String> nameOpt = getStringField(attributes, "nombre");
        if (nameOpt.isEmpty()) {
            throw new IllegalArgumentException("Missing station name (nombre field)");
        }
        String name = nameOpt.get();
        
        // Extract line
        Optional<String> lineOpt = getStringField(attributes, "linea");
        if (lineOpt.isEmpty()) {
            throw new IllegalArgumentException("Missing station line (linea field)");
        }
        String line = lineOpt.get();
        
        // Extract coordinates from geometry (x, y format)
        if (geometryOpt.isEmpty()) {
            throw new IllegalArgumentException("Missing geometry for station");
        }
        Coordinate coordinate = parseGeometryCoordinate(geometryOpt.get());
        
        return Optional.of(new Station(name, line, coordinate));
    }
}
