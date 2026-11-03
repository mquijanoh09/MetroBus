package co.medellin.transit.data;

import org.apache.commons.csv.CSVRecord;

import java.util.Optional;

/**
 * CSV reader for bus segment data.
 * Parses bus_segments.csv with schema:
 * route_id, direction, from_stop_id, to_stop_id, from_seq, to_seq, straight_m, road_m, used_m, method
 */
public class BusSegmentReader extends CsvReader<BusSegment> {
    
    @Override
    protected Optional<BusSegment> parseRecord(CSVRecord record, int lineNumber) throws Exception {
        String routeId = getString(record, "route_id");
        
        // Parse direction - can be "outbound"/"inbound" or "0"/"1"
        int direction;
        String directionStr = getString(record, "direction").toLowerCase();
        if (directionStr.equals("outbound") || directionStr.equals("0")) {
            direction = 0;
        } else if (directionStr.equals("inbound") || directionStr.equals("1")) {
            direction = 1;
        } else {
            throw new IllegalArgumentException("Invalid direction: " + directionStr + " (expected 'outbound', 'inbound', '0', or '1')");
        }
        
        int fromStopId = parseInt(record, "from_stop_id");
        int toStopId = parseInt(record, "to_stop_id");
        int fromSeq = parseInt(record, "from_seq");
        int toSeq = parseInt(record, "to_seq");
        double straightM = parseDouble(record, "straight_m");
        double usedM = parseDouble(record, "used_m");
        
        // road_m is optional - use used_m as fallback if empty
        String roadMStr = record.get("road_m");
        double roadM = (roadMStr == null || roadMStr.trim().isEmpty()) ? usedM : Double.parseDouble(roadMStr);
        
        String method = getString(record, "method");
        
        // BusSegment constructor will validate the data
        BusSegment segment = new BusSegment(
            routeId,
            direction,
            fromStopId,
            toStopId,
            fromSeq,
            toSeq,
            straightM,
            roadM,
            usedM,
            method
        );
        
        return Optional.of(segment);
    }
}
