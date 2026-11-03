package co.medellin.transit.data;

import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Stop;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

/**
 * Reads stop data from ArcGIS JSON format files.
 * Handles paginated stop files (stops_0.json, stops_2000.json, etc.).
 * 
 * Expected fields in ArcGIS attributes:
 * - id_paradero: unique stop identifier (physical stop location)
 * - direccion: street address (optional - some stops don't have it)
 * - latitud/longitud: coordinates
 * 
 * Note: The JSON files contain 10,043 total features (one per route-stop combination),
 * but only 3,590 unique physical stops (unique id_paradero values).
 * This reader deduplicates by id_paradero to return only physical stops.
 */
public class StopArcGisReader extends ArcGisReader<Stop> {
    
    private static final Logger LOGGER = Logger.getLogger(StopArcGisReader.class.getName());
    
    /**
     * Reads paginated stop files and deduplicates by id_paradero.
     * Overrides the parent method to add deduplication logic.
     * 
     * @param baseDir directory containing the JSON files
     * @param baseFilename base filename without page number
     * @param extension file extension
     * @return list of unique stops (one per physical location)
     * @throws IOException if file read fails
     */
    @Override
    public List<Stop> readPaginated(Path baseDir, String baseFilename, String extension) throws IOException {
        // Read all features from paginated files
        List<Stop> allStops = super.readPaginated(baseDir, baseFilename, extension);
        
        // Deduplicate by id_paradero
        Map<Integer, Stop> uniqueStops = new LinkedHashMap<>();
        int duplicatesSkipped = 0;
        
        for (Stop stop : allStops) {
            if (!uniqueStops.containsKey(stop.getId())) {
                uniqueStops.put(stop.getId(), stop);
            } else {
                duplicatesSkipped++;
            }
        }
        
        LOGGER.info(String.format(
            "Deduplicated stops: %d total features → %d unique physical stops (%d duplicates skipped)",
            allStops.size(),
            uniqueStops.size(),
            duplicatesSkipped
        ));
        
        return new ArrayList<>(uniqueStops.values());
    }

    /**
     * Parses a single feature from ArcGIS JSON into a Stop object.
     * 
     * @param featureJson the feature JSON string
     * @param featureNumber the feature number (for error reporting)
     * @return parsed Stop, or empty to skip
     * @throws Exception if parsing fails
     */
    @Override
    protected Optional<Stop> parseFeature(String featureJson, int featureNumber) throws Exception {
        String attributes = extractAttributes(featureJson);
        Optional<String> geometryOpt = extractGeometry(featureJson);
        
        // Extract stop ID - try multiple field names
        int stopId;
        try {
            stopId = getIntField(attributes, "id_paradero");
        } catch (IllegalArgumentException e) {
            try {
                stopId = getIntField(attributes, "OBJECTID");
            } catch (IllegalArgumentException e2) {
                throw new IllegalArgumentException("Missing stop ID field (id_paradero or OBJECTID)");
            }
        }
        
        // Extract address (direccion field) - OPTIONAL
        // Some stops (83 out of 10,043) don't have direccion
        // Use a default address for those stops
        String address = getStringField(attributes, "direccion")
            .orElse("Parada " + stopId);
        
        // Use direccion as name, or default if missing
        String name = address;
        
        // Extract coordinates from attributes (latitud/longitud fields)
        // The JSON files have NO geometry object - coordinates are in attributes
        Coordinate coordinate;
        try {
            double lat = getDoubleField(attributes, "latitud");
            double lon = getDoubleField(attributes, "longitud");
            coordinate = new Coordinate(lat, lon);
        } catch (IllegalArgumentException e) {
            // If attributes don't have coordinates, try geometry as fallback
            if (geometryOpt.isPresent()) {
                coordinate = parseGeometryCoordinate(geometryOpt.get());
            } else {
                throw new IllegalArgumentException("Missing coordinate data (latitud/longitud in attributes or geometry object)");
            }
        }
        
        return Optional.of(new Stop(stopId, name, coordinate, address));
    }
}
