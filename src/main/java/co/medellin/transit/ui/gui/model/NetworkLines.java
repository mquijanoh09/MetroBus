package co.medellin.transit.ui.gui.model;

import co.medellin.transit.model.Coordinate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The mass transit lines (Metro, Metroplús, cable, tram) with their stations
 * in order, read from data/transit_lines.csv. It is small (about 5 KB), so the
 * map can be drawn before the full graph finishes loading.
 */
public final class NetworkLines {

    /** One station of a line. */
    public record Station(String name, Coordinate coord) {
    }

    /** One line with its stations in travel order. */
    public record Line(String code, TransitMode mode, List<Station> stations) {
        public Station first() {
            return stations.get(0);
        }

        public Station last() {
            return stations.get(stations.size() - 1);
        }
    }

    private final List<Line> lines;

    public NetworkLines(List<Line> lines) {
        this.lines = List.copyOf(lines);
    }

    public static NetworkLines empty() {
        return new NetworkLines(List.of());
    }

    public static NetworkLines load(Path dataDir) throws IOException {
        Path file = dataDir.resolve("transit_lines.csv");
        Map<String, List<Station>> stations = new LinkedHashMap<>();
        Map<String, TransitMode> modes = new LinkedHashMap<>();
        Map<String, List<Integer>> orders = new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true).setTrim(true).build().parse(reader);
            for (CSVRecord r : records) {
                String code = r.get("line");
                modes.putIfAbsent(code, TransitMode.ofCsvMode(r.get("mode")));
                stations.computeIfAbsent(code, k -> new ArrayList<>()).add(new Station(r.get("station"),
                        new Coordinate(Double.parseDouble(r.get("lat")), Double.parseDouble(r.get("lon")))));
                orders.computeIfAbsent(code, k -> new ArrayList<>()).add(Integer.parseInt(r.get("order")));
            }
        }
        List<Line> lines = new ArrayList<>();
        for (Map.Entry<String, List<Station>> e : stations.entrySet()) {
            List<Station> list = e.getValue();
            List<Integer> order = orders.get(e.getKey());
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                idx.add(i);
            }
            idx.sort((a, b) -> Integer.compare(order.get(a), order.get(b)));
            List<Station> sorted = new ArrayList<>();
            for (int i : idx) {
                sorted.add(list.get(i));
            }
            lines.add(new Line(e.getKey(), modes.get(e.getKey()), Collections.unmodifiableList(sorted)));
        }
        return new NetworkLines(lines);
    }

    public List<Line> lines() {
        return lines;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public Optional<Line> line(String code) {
        return lines.stream().filter(l -> l.code().equalsIgnoreCase(code)).findFirst();
    }

    /**
     * Stations of a line between the ones closest to two points, inclusive, in
     * the direction of travel. Used to draw a ride along the real line.
     */
    public List<Station> stretch(String lineCode, Coordinate from, Coordinate to) {
        Optional<Line> line = line(lineCode);
        if (line.isEmpty() || from == null || to == null) {
            return List.of();
        }
        List<Station> s = line.get().stations();
        int a = nearest(s, from);
        int b = nearest(s, to);
        List<Station> out = new ArrayList<>();
        int step = a <= b ? 1 : -1;
        for (int i = a; i != b + step; i += step) {
            out.add(s.get(i));
        }
        return out;
    }

    private static int nearest(List<Station> stations, Coordinate c) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < stations.size(); i++) {
            Coordinate s = stations.get(i).coord();
            double dLat = s.getLat() - c.getLat();
            double dLon = (s.getLon() - c.getLon()) * Math.cos(Math.toRadians(c.getLat()));
            double d = dLat * dLat + dLon * dLon;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }
}
