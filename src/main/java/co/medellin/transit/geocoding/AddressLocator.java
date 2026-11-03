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
        double targetCalle = address.getPrimaryNumber();
        double targetCarrera = address.getSecondaryNumber();
        if (address.getStreetType() == StreetType.CARRERA) {
            double temp = targetCalle;
            targetCalle = targetCarrera;
            targetCarrera = temp;
        }
        List<WeightedPoint> closest = findClosestPoints(targetCalle, targetCarrera, 4);
        if (closest.isEmpty()) return Optional.empty();
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
            double dist = Math.sqrt(cDist * cDist + crDist * crDist);
            double weight = dist < 0.01 ? 1000.0 : 1.0 / dist;
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
            if (addr.toUpperCase().startsWith("VDA ") || addr.toUpperCase().startsWith("VEREDA ") || 
                addr.trim().isEmpty() || addr.matches("Parada \\d+")) {
                skipped++;
                continue;
            }
            Optional<ParsedAddress> parsed = parser.parse(addr);
            if (parsed.isEmpty()) {
                skipped++;
                continue;
            }
            ParsedAddress pa = parsed.get();
            double calle = pa.getPrimaryNumber();
            double carrera = pa.getSecondaryNumber();
            if (pa.getStreetType() == StreetType.CARRERA) {
                double temp = calle;
                calle = carrera;
                carrera = temp;
            }
            table.add(new ReferencePoint(calle, carrera, stop.getCoordinate()));
        }
        LOGGER.info(String.format("Built reference table: %d parseable addresses, %d skipped", table.size(), skipped));
        return table;
    }
    private List<ReferencePoint> buildDefaultReferenceTable() {
        try {
            String dataDir = System.getProperty("transit.data.dir", 
                "C:\\Users\\marti\\Downloads\\transporte-medellin\\kiro\\data");
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

