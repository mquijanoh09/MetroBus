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
        // Read place IDs as strings - keep original format like "STOP:53191" or "STATION:Aguacatala"
        String placeA = getString(record, "place_a");
        String nameA = getString(record, "name_a");
        String placeB = getString(record, "place_b");
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
