package co.medellin.transit.geocoding;

import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Stop;
import co.medellin.transit.data.StopArcGisReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class AddressLocator {
    private static final Logger LOGGER = Logger.getLogger(AddressLocator.class.getName());
    private static final double LAT_MIN = 6.15;
    private static final double LAT_MAX = 6.35;
    private static final double LON_MIN = -75.65;
    private static final double LON_MAX = -75.50;
    private final List<ReferencePoint> referenceTable;
    private final AddressParser parser;
    public AddressLocator() {
        this.parser = new AddressParser();
        this.referenceTable = buildDefaultReferenceTable();
        LOGGER.info(String.format("AddressLocator initialized with %d reference points", referenceTable.size()));
    }
    public AddressLocator(List<Stop> stops) {
        this.parser = new AddressParser();
        this.referenceTable = buildReferenceTable(stops);
    }
    public Optional<Coordinate> locate(ParsedAddress address) {
        if (address == null || referenceTable.isEmpty()) return Optional.empty();
        
        // Reject CIRCULAR addresses (algorithm.md line 136: "cannot be located")
        if (address.getStreetType() == StreetType.CIRCULAR) {
            return Optional.empty();
        }
        
        // Use prototype-transformed (calle, carrera) for locating
        double targetCalle = address.getCalleNumber();
        double targetCarrera = address.getCarreraNumber();
        
        List<WeightedPoint> closest = findClosestPoints(targetCalle, targetCarrera, 4);
        if (closest.isEmpty()) return Optional.empty();
        
        // Reject if closest reference point is > 6 address units away (algorithm.md section 3 step 4)
        WeightedPoint closestPoint = closest.get(0);
        if (closestPoint.distance > 6.0) {
            return Optional.empty();
        }
        
        double sumWeights = 0.0, weightedLat = 0.0, weightedLon = 0.0;
        for (WeightedPoint wp : closest) {
            sumWeights += wp.weight;
            weightedLat += wp.point.coordinate.getLat() * wp.weight;
            weightedLon += wp.point.coordinate.getLon() * wp.weight;
        }
        if (sumWeights == 0) return Optional.empty();
        double lat = weightedLat / sumWeights;
        double lon = weightedLon / sumWeights;
        if (!isInBounds(lat, lon)) return Optional.empty();
        try {
            return Optional.of(new Coordinate(lat, lon));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
    private List<WeightedPoint> findClosestPoints(double tCalle, double tCarrera, int n) {
        PriorityQueue<WeightedPoint> pq = new PriorityQueue<>(Comparator.comparingDouble(wp -> wp.distance));
        for (ReferencePoint ref : referenceTable) {
            double cDist = Math.abs(ref.calle - tCalle);
            double crDist = Math.abs(ref.carrera - tCarrera);
            double dist = Math.hypot(cDist, crDist);
            double weight = 1.0 / (0.001 + dist);
            pq.offer(new WeightedPoint(ref, dist, weight));
        }
        List<WeightedPoint> result = new ArrayList<>();
        for (int i = 0; i < n && !pq.isEmpty(); i++) result.add(pq.poll());
        return result;
    }
    public boolean isInBounds(Coordinate coord) {
        return coord != null && isInBounds(coord.getLat(), coord.getLon());
    }
    private boolean isInBounds(double lat, double lon) {
        return lat >= LAT_MIN && lat <= LAT_MAX && lon >= LON_MIN && lon <= LON_MAX;
    }
    private List<ReferencePoint> buildReferenceTable(List<Stop> stops) {
        List<ReferencePoint> table = new ArrayList<>();
        int skipped = 0;
        for (Stop stop : stops) {
            String addr = stop.getAddress();
            
            // Parse the address (parser handles all rejection logic)
            Optional<ParsedAddress> parsed = parser.parse(addr);
            if (parsed.isEmpty()) {
                skipped++;
                continue;
            }
            
            ParsedAddress pa = parsed.get();
            
            // Skip CIRCULAR addresses (algorithm.md: "cannot be located")
            if (pa.getStreetType() == StreetType.CIRCULAR) {
                skipped++;
                continue;
            }
            
            // Use prototype-transformed (calle, carrera) for reference table
            double calle = pa.getCalleNumber();
            double carrera = pa.getCarreraNumber();
            
            table.add(new ReferencePoint(calle, carrera, stop.getCoordinate()));
        }
        LOGGER.info(String.format("Built reference table: %d parseable addresses, %d skipped", table.size(), skipped));
        return table;
    }
    private List<ReferencePoint> buildDefaultReferenceTable() {
        try {
            String dataDir = System.getProperty("transit.data.dir", "data");
            StopArcGisReader reader = new StopArcGisReader();
            List<Stop> stops = reader.readPaginated(Paths.get(dataDir), "stops", ".json");
            return buildReferenceTable(stops);
        } catch (IOException e) {
            LOGGER.warning("Failed to load stop data for reference table: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    static class ReferencePoint {
        final double calle, carrera;
        final Coordinate coordinate;
        ReferencePoint(double calle, double carrera, Coordinate coordinate) {
            this.calle = calle;
            this.carrera = carrera;
            this.coordinate = coordinate;
        }
    }
    static class WeightedPoint {
        final ReferencePoint point;
        final double distance, weight;
        WeightedPoint(ReferencePoint point, double distance, double weight) {
            this.point = point;
            this.distance = distance;
            this.weight = weight;
        }
    }
}

