package co.medellin.transit.data;

import org.apache.commons.csv.CSVRecord;

import java.util.Optional;

/**
 * CSV reader for walk link data.
 * Parses walk_links.csv with schema:
 * place_a, name_a, place_b, name_b, kind, straight_m, walk_sec
 */
public class WalkLinkReader extends CsvReader<WalkLink> {
    
    @Override
    protected Optional<WalkLink> parseRecord(CSVRecord record, int lineNumber) throws Exception {
        // Parse place IDs - they are prefixed strings like "STOP:53191" or "STATION:5"
        int placeA = parsePlaceId(record, "place_a");
        String nameA = getString(record, "name_a");
        int placeB = parsePlaceId(record, "place_b");
        String nameB = getString(record, "name_b");
        
        // Normalize kind format - CSV has "stop_stop", we need "stop-to-stop"
        String kind = normalizeKind(getString(record, "kind"));
        
        double straightM = parseDouble(record, "straight_m");
        int walkSec = parseInt(record, "walk_sec");
        
        // WalkLink constructor will validate the data
        WalkLink link = new WalkLink(
            placeA,
            nameA,
            placeB,
            nameB,
            kind,
            straightM,
            walkSec
        );
        
        return Optional.of(link);
    }
    
    /**
     * Parses a place ID from a prefixed string like "STOP:53191" or "STATION:5".
     * For named stations like "STATION:Aguacatala", generates a hash-based ID.
     *
     * @param record the CSV record
     * @param fieldName the field name
     * @return the numeric place ID
     * @throws IllegalArgumentException if the format is invalid
     */
    private int parsePlaceId(CSVRecord record, String fieldName) {
        String value = getString(record, fieldName);
        int colonIndex = value.indexOf(':');
        
        if (colonIndex < 0) {
            throw new IllegalArgumentException(
                "Place ID must be in format 'STOP:123' or 'STATION:5', got: " + value
            );
        }
        
        String idPart = value.substring(colonIndex + 1);
        try {
            return Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            // Handle named stations like "STATION:Aguacatala" by generating a consistent hash
            // Use absolute value to ensure positive, and keep in safe integer range
            return Math.abs(value.hashCode() % 100_000_000);
        }
    }
    
    /**
     * Normalizes kind format from CSV (e.g., "stop_stop") to canonical format ("stop-to-stop").
     *
     * @param kind the kind string from CSV
     * @return normalized kind string
     */
    private String normalizeKind(String kind) {
        return switch (kind) {
            case "stop_stop" -> "stop-to-stop";
            case "stop_station" -> "stop-to-station";
            case "station_station" -> "station-to-station";
            default -> kind; // Return as-is if already in correct format
        };
    }
}
