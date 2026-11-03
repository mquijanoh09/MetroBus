package co.medellin.transit.data;

import co.medellin.transit.model.Coordinate;
import org.apache.commons.csv.CSVRecord;

import java.util.Optional;

/**
 * CSV reader for transit line data (metro, metroplus, tranvía, metrocable).
 * Parses transit_lines.csv with schema:
 * line, mode, order, station, lat, lon, dist_to_next_m, sec_to_next
 */
public class TransitLineReader extends CsvReader<TransitSegment> {
    
    @Override
    protected Optional<TransitSegment> parseRecord(CSVRecord record, int lineNumber) throws Exception {
        try {
            String line = getString(record, "line");
            String mode = getString(record, "mode");
            int order = parseInt(record, "order");
            String station = getString(record, "station");
            double lat = parseDouble(record, "lat");
            double lon = parseDouble(record, "lon");
            
            // Handle optional dist_to_next_m and sec_to_next for last stations
            // Last station of each line has empty values (no next station)
            Optional<String> distStr = getOptionalString(record, "dist_to_next_m");
            double distToNextM = distStr.map(Double::parseDouble).orElse(0.0);
            
            Optional<String> secStr = getOptionalString(record, "sec_to_next");
            int secToNext = secStr.map(Integer::parseInt).orElse(0);
            
            // Map CSV mode values to expected values
            String normalizedMode = normalizeMode(mode);
            
            Coordinate coordinate = new Coordinate(lat, lon);
            
            // TransitSegment constructor will validate the data
            TransitSegment segment = new TransitSegment(
                line,
                normalizedMode,
                order,
                station,
                coordinate,
                distToNextM,
                secToNext
            );
            
            return Optional.of(segment);
            
        } catch (Exception e) {
            // Enhanced logging with actual row values
            throw new Exception(String.format(
                "Failed to parse row: %s. Error: %s",
                record.toString(),
                e.getMessage()
            ), e);
        }
    }
    
    /**
     * Maps CSV mode values to the expected format.
     * CSV uses lowercase: metro, brt, tram, cable_car
     * Expected: Metro, Metroplus, Tranvía, Metrocable
     */
    private String normalizeMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "metro" -> "Metro";
            case "brt" -> "Metroplus";
            case "tram" -> "Tranvía";
            case "cable_car" -> "Metrocable";
            default -> mode; // Return as-is for validation to catch
        };
    }
}
