package co.medellin.transit.data;

import co.medellin.transit.model.Route;
import co.medellin.transit.model.Stop;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for all data readers loading real files.
 */
class DataLoadingIntegrationTest {

    @Test
    void testAllFileCounts() throws IOException {
        // Load all 6 file types from relative paths and verify counts
        
        // 1. Bus segments: 9,550
        Path busSegmentsPath = Paths.get("data/bus_segments.csv");
        BusSegmentReader busReader = new BusSegmentReader();
        List<BusSegment> busSegments = busReader.read(busSegmentsPath);
        assertEquals(9550, busSegments.size(), 
            "Expected 9,550 bus segments");
        
        // 2. Transit lines: 98
        Path transitLinesPath = Paths.get("data/transit_lines.csv");
        TransitLineReader transitReader = new TransitLineReader();
        List<TransitSegment> transitSegments = transitReader.read(transitLinesPath);
        assertEquals(98, transitSegments.size(), 
            "Expected 98 transit line segments");
        
        // 3. Walk links: 25,865
        Path walkLinksPath = Paths.get("data/walk_links.csv");
        WalkLinkReader walkReader = new WalkLinkReader();
        List<WalkLink> walkLinks = walkReader.read(walkLinksPath);
        assertEquals(25865, walkLinks.size(), 
            "Expected 25,865 walk links");
        
        // 4. Stops: 3,590 unique (from 10,043 features)
        Path stopsDir = Paths.get("data/raw");
        StopArcGisReader stopReader = new StopArcGisReader();
        List<Stop> stops = stopReader.readPaginated(stopsDir, "stops", ".json");
        assertEquals(3590, stops.size(), 
            "Expected 3,590 unique stops from 10,043 features");
        
        // 5. Routes: 252
        Path routesPath = Paths.get("data/raw/routes.json");
        RouteArcGisReader routeReader = new RouteArcGisReader();
        List<Route> routes = routeReader.read(routesPath);
        assertEquals(252, routes.size(), 
            "Expected 252 routes");
        
        // 6. Stations: 81
        Path stationsPath = Paths.get("data/raw/transit_stations.json");
        StationArcGisReader stationReader = new StationArcGisReader();
        List<Station> stations = stationReader.read(stationsPath);
        assertEquals(81, stations.size(), 
            "Expected 81 stations");
        
        // Log summary
        System.out.println("=== Data Loading Integration Test Summary ===");
        System.out.println("Bus segments: " + busSegments.size());
        System.out.println("Transit lines: " + transitSegments.size());
        System.out.println("Walk links: " + walkLinks.size());
        System.out.println("Stops: " + stops.size());
        System.out.println("Routes: " + routes.size());
        System.out.println("Stations: " + stations.size());
    }
}
