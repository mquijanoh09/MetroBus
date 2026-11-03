package co.medellin.transit.data;

import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Stop;
import org.apache.commons.csv.CSVRecord;

import java.util.Optional;

/**
 * Reads stop data from CSV format files.
 * Parses bus stops and metro stations from CSV data.
 * 
 * Expected CSV fields:
 * - id_paradero (or id or stop_id): unique stop identifier
 * - nombre (or nombre_paradero or name): stop name
 * - latitud (or lat): latitude coordinate
 * - longitud (or lon or lng): longitude coordinate
 * - direccion (or address): street address (optional)
 */
public class StopCsvReader extends CsvReader<Stop> {

    /**
     * Parses a single CSV record into a Stop object.
     * 
     * @param record the CSV record to parse
     * @param lineNumber the line number (for error reporting)
     * @return parsed Stop, or empty to skip
     * @throws Exception if parsing fails
     */
    @Override
    protected Optional<Stop> parseRecord(CSVRecord record, int lineNumber) throws Exception {
        // Extract stop ID - try multiple field names
        int stopId;
        if (hasField(record, "id_paradero")) {
            stopId = parseInt(record, "id_paradero");
        } else if (hasField(record, "stop_id")) {
            stopId = parseInt(record, "stop_id");
        } else if (hasField(record, "id")) {
            stopId = parseInt(record, "id");
        } else {
            throw new IllegalArgumentException("Missing stop ID field (id_paradero, stop_id, or id)");
        }
        
        // Extract stop name - try multiple field names
        String name;
        if (hasField(record, "nombre_paradero")) {
            name = getString(record, "nombre_paradero");
        } else if (hasField(record, "nombre")) {
            name = getString(record, "nombre");
        } else if (hasField(record, "name")) {
            name = getString(record, "name");
        } else {
            throw new IllegalArgumentException("Missing stop name field (nombre_paradero, nombre, or name)");
        }
        
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Stop name cannot be empty");
        }
        
        // Extract latitude - try multiple field names
        double lat;
        if (hasField(record, "latitud")) {
            lat = parseDouble(record, "latitud");
        } else if (hasField(record, "lat")) {
            lat = parseDouble(record, "lat");
        } else if (hasField(record, "latitude")) {
            lat = parseDouble(record, "latitude");
        } else {
            throw new IllegalArgumentException("Missing latitude field (latitud, lat, or latitude)");
        }
        
        // Extract longitude - try multiple field names
        double lon;
        if (hasField(record, "longitud")) {
            lon = parseDouble(record, "longitud");
        } else if (hasField(record, "lon")) {
            lon = parseDouble(record, "lon");
        } else if (hasField(record, "lng")) {
            lon = parseDouble(record, "lng");
        } else if (hasField(record, "longitude")) {
            lon = parseDouble(record, "longitude");
        } else {
            throw new IllegalArgumentException("Missing longitude field (longitud, lon, lng, or longitude)");
        }
        
        Coordinate coordinate = new Coordinate(lat, lon);
        
        // Extract address (optional, default to empty string)
        String address = "";
        if (hasField(record, "direccion")) {
            address = getOptionalString(record, "direccion").orElse("");
        } else if (hasField(record, "address")) {
            address = getOptionalString(record, "address").orElse("");
        }
        
        return Optional.of(new Stop(stopId, name, coordinate, address));
    }
}
