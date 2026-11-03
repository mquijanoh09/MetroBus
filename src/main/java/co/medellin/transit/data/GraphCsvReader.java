package co.medellin.transit.data;

import co.medellin.transit.model.Edge;
import co.medellin.transit.model.EdgeKind;
import org.apache.commons.csv.CSVRecord;

import java.util.Optional;

/**
 * Reads pre-computed graph edges from CSV files.
 * Can read edges from various CSV formats including bus_segments.csv, 
 * transit_lines.csv, and walk_links.csv.
 * 
 * Expected CSV fields:
 * - source_id (or from_id): source state ID
 * - target_id (or to_id): target state ID
 * - edge_kind (or kind or type): edge type (WALK, BOARD, RIDE, ALIGHT)
 * - real_sec (or duration or time_sec): actual travel time in seconds
 * - perceived_cost (or cost): routing cost including penalties
 */
public class GraphCsvReader extends CsvReader<Edge> {

    /**
     * Parses a single CSV record into an Edge object.
     * 
     * @param record the CSV record to parse
     * @param lineNumber the line number (for error reporting)
     * @return parsed Edge, or empty to skip
     * @throws Exception if parsing fails
     */
    @Override
    protected Optional<Edge> parseRecord(CSVRecord record, int lineNumber) throws Exception {
        // Extract source ID - try multiple field names
        int sourceId;
        if (hasField(record, "source_id")) {
            sourceId = parseInt(record, "source_id");
        } else if (hasField(record, "from_id")) {
            sourceId = parseInt(record, "from_id");
        } else {
            throw new IllegalArgumentException("Missing source ID field (source_id or from_id)");
        }
        
        // Extract target ID - try multiple field names
        int targetId;
        if (hasField(record, "target_id")) {
            targetId = parseInt(record, "target_id");
        } else if (hasField(record, "to_id")) {
            targetId = parseInt(record, "to_id");
        } else {
            throw new IllegalArgumentException("Missing target ID field (target_id or to_id)");
        }
        
        // Extract edge kind - try multiple field names
        EdgeKind kind;
        String kindStr;
        if (hasField(record, "edge_kind")) {
            kindStr = getString(record, "edge_kind");
        } else if (hasField(record, "kind")) {
            kindStr = getString(record, "kind");
        } else if (hasField(record, "type")) {
            kindStr = getString(record, "type");
        } else {
            throw new IllegalArgumentException("Missing edge kind field (edge_kind, kind, or type)");
        }
        
        // Parse edge kind string to enum
        try {
            kind = EdgeKind.valueOf(kindStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid edge kind: " + kindStr + 
                " (expected: WALK, BOARD, RIDE, or ALIGHT)");
        }
        
        // Extract real travel time - try multiple field names
        int realSec;
        if (hasField(record, "real_sec")) {
            realSec = parseInt(record, "real_sec");
        } else if (hasField(record, "duration")) {
            realSec = parseInt(record, "duration");
        } else if (hasField(record, "time_sec")) {
            realSec = parseInt(record, "time_sec");
        } else {
            throw new IllegalArgumentException("Missing travel time field (real_sec, duration, or time_sec)");
        }
        
        // Extract perceived cost - try multiple field names
        int perceivedCost;
        if (hasField(record, "perceived_cost")) {
            perceivedCost = parseInt(record, "perceived_cost");
        } else if (hasField(record, "cost")) {
            perceivedCost = parseInt(record, "cost");
        } else {
            // If no perceived cost provided, use real time
            perceivedCost = realSec;
        }
        
        return Optional.of(new Edge(sourceId, targetId, kind, realSec, perceivedCost));
    }
}
