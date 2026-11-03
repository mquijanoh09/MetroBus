package co.medellin.transit.geocoding;

import co.medellin.transit.data.StopArcGisReader;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Stop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete geocoding workflow with reference-table interpolation:
 * AddressParser -> AddressLocator -> Coordinate
 * 
 * Updated to reflect real Medellin geography:
 * - Calle numbers increase NORTHWARD (higher number = higher latitude)
 * - Carrera numbers increase WESTWARD (higher number = lower longitude)
 * - Coordinates are interpolated from real stop data, not fixed reference points
 */
class GeocodingIntegrationTest {
    
    private AddressParser parser;
    private AddressLocator locator;
    
    @BeforeEach
    void setUp() {
        parser = new AddressParser();
        locator = new AddressLocator();
    }
    
    @Test
    void testFullGeocodingWorkflow() {
        // Complete workflow: string -> parsed -> coordinate
        String addressStr = "Calle 50 # 50 - 48";
        
        // Step 1: Parse
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent(), "Address should parse successfully");
        
        // Step 2: Locate
        Optional<Coordinate> coord = locator.locate(parsed.get());
        assertTrue(coord.isPresent(), "Address should locate successfully");
        
        // Step 3: Validate bounds
        assertTrue(locator.isInBounds(coord.get()), "Coordinate should be in bounds");
        
        // Verify coordinate values are reasonable (interpolated, not exact)
        Coordinate c = coord.get();
        assertTrue(c.getLat() > 6.20 && c.getLat() < 6.30, "Latitude should be in reasonable range");
        assertTrue(c.getLon() > -75.60 && c.getLon() < -75.55, "Longitude should be in reasonable range");
    }
    
    @Test
    void testElPobladoAddress() {
        // Well-known area in Medellin: El Poblado
        String addressStr = "Calle 10 # 43A - 20";
        
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent());
        
        Optional<Coordinate> coord = locator.locate(parsed.get());
        assertTrue(coord.isPresent());
        assertTrue(locator.isInBounds(coord.get()));
        
        // Get center reference for comparison
        Coordinate center = geocode("Calle 50 # 50");
        
        // Calle 10 < Calle 50, so Calle 10 is SOUTH (lower latitude)
        assertTrue(coord.get().getLat() < center.getLat(), 
                  "Calle 10 should be south of Calle 50 (lower latitude)");
        
        // Carrera 43A < Carrera 50, so Carrera 43A is WEST (lower longitude)
        assertTrue(coord.get().getLon() < center.getLon(), 
                  "Carrera 43 should be west of Carrera 50 (lower longitude)");
    }
    
    @Test
    void testAddressWithSurCardinal() {
        // Address with SUR makes the Calle number negative
        String addressStr = "Calle 10 Sur # 50";
        
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent());
        
        ParsedAddress addr = parsed.get();
        // SUR makes the Calle number negative
        assertTrue(addr.getPrimaryNumber() < 0, "SUR should make Calle number negative");
        
        Optional<Coordinate> coord = locator.locate(addr);
        assertTrue(coord.isPresent());
        
        // Get center reference for comparison
        Coordinate center = geocode("Calle 50 # 50");
        
        // Negative Calle (Sur) is south of positive Calle (lower latitude)
        assertTrue(coord.get().getLat() < center.getLat(), 
                  "Calle Sur (negative) should be south of positive Calle (lower latitude)");
    }
    
    @Test
    void testCarreraAsMainStreet() {
        // Address starting with Carrera
        String addressStr = "Carrera 70 # 40";
        
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent());
        
        Optional<Coordinate> coord = locator.locate(parsed.get());
        assertTrue(coord.isPresent());
        assertTrue(locator.isInBounds(coord.get()));
        
        // Get center reference for comparison
        Coordinate center = geocode("Calle 50 # 50");
        
        // Carrera 70 > Carrera 50, so Carrera 70 is WEST (lower longitude)
        assertTrue(coord.get().getLon() < center.getLon(), 
                  "Carrera 70 should be west of Carrera 50 (lower longitude)");
        
        // Calle 40 < Calle 50, so Calle 40 is SOUTH (lower latitude)
        assertTrue(coord.get().getLat() < center.getLat(), 
                  "Calle 40 should be south of Calle 50 (lower latitude)");
    }
    
    @Test
    void testInvalidAddressFormat() {
        // Invalid format should fail to parse
        String addressStr = "This is not an address";
        
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isEmpty(), "Invalid format should not parse");
    }
    
    @Test
    void testAddressOutOfBounds() {
        // Create an address outside Medellin bounds
        String addressStr = "Calle 200 # 50";
        
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent(), "Should parse successfully");
        
        Optional<Coordinate> coord = locator.locate(parsed.get());
        // Addresses outside Medellin bounds must not locate
        assertTrue(coord.isEmpty(), "Out-of-bounds address should not locate");
    }
    
    @Test
    void testDiagonalAddress() {
        // Diagonal street type
        String addressStr = "Diagonal 50 # 50";
        
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent());
        
        Optional<Coordinate> coord = locator.locate(parsed.get());
        assertTrue(coord.isPresent(), "Diagonal addresses should be locatable");
        assertTrue(locator.isInBounds(coord.get()));
    }
    
    @Test
    void testTransversalAddress() {
        // Transversal street type
        String addressStr = "Transversal 45 # 52";
        
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent());
        
        Optional<Coordinate> coord = locator.locate(parsed.get());
        assertTrue(coord.isPresent(), "Transversal addresses should be locatable");
        assertTrue(locator.isInBounds(coord.get()));
    }
    
    @Test
    void testMultipleAddressesFormGrid() {
        // Verify that multiple addresses form a logical grid
        
        // Center
        Coordinate center = geocode("Calle 50 # 50");
        
        // North (higher Calle number = higher latitude)
        Coordinate north = geocode("Calle 60 # 50");
        assertTrue(north.getLat() > center.getLat(), 
                  "Higher Calle number should have higher latitude (north)");
        assertEquals(north.getLon(), center.getLon(), 0.01, 
                    "Same Carrera should have similar longitude");
        
        // South (lower Calle number = lower latitude)
        Coordinate south = geocode("Calle 40 # 50");
        assertTrue(south.getLat() < center.getLat(), 
                  "Lower Calle number should have lower latitude (south)");
        assertEquals(south.getLon(), center.getLon(), 0.01, 
                    "Same Carrera should have similar longitude");
        
        // East (lower Carrera number = higher longitude)
        Coordinate east = geocode("Calle 50 # 40");
        assertEquals(east.getLat(), center.getLat(), 0.01, 
                    "Same Calle should have similar latitude");
        assertTrue(east.getLon() > center.getLon(), 
                  "Lower Carrera number should have higher longitude (east)");
        
        // West (higher Carrera number = lower longitude)
        Coordinate west = geocode("Calle 50 # 60");
        assertEquals(west.getLat(), center.getLat(), 0.01, 
                    "Same Calle should have similar latitude");
        assertTrue(west.getLon() < center.getLon(), 
                  "Higher Carrera number should have lower longitude (west)");
    }
    
    @Test
    void testLocatorAccuracyOn300Stops() throws IOException {
        // Load all stops from real data
        Path dataDir = Paths.get("data");
        StopArcGisReader reader = new StopArcGisReader();
        List<Stop> allStops = reader.readPaginated(dataDir, "stops", ".json");
        
        assertEquals(3590, allStops.size(), "Should have 3,590 unique stops");
        
        // Select 300 test stops (deterministic: every 12th stop)
        List<Stop> testStops = new ArrayList<>();
        for (int i = 0; i < allStops.size(); i += 12) {
            testStops.add(allStops.get(i));
            if (testStops.size() >= 300) break;
        }
        
        assertEquals(300, testStops.size(), "Should have 300 test stops");
        
        // Test each stop
        List<Double> errors = new ArrayList<>();
        AddressParser parser = new AddressParser();
        
        for (Stop testStop : testStops) {
            // Build reference table WITHOUT this stop
            List<Stop> referenceStops = new ArrayList<>(allStops);
            referenceStops.remove(testStop);
            AddressLocator locator = new AddressLocator(referenceStops);
            
            // Parse the stop's address
            String address = testStop.getAddress();
            Optional<ParsedAddress> parsed = parser.parse(address);
            if (parsed.isEmpty()) continue; // Skip unparseable addresses
            
            // Locate it
            Optional<Coordinate> located = locator.locate(parsed.get());
            if (located.isEmpty()) continue; // Skip unlocatable addresses
            
            // Measure error (haversine distance)
            double errorMeters = haversineDistance(testStop.getCoordinate(), located.get());
            errors.add(errorMeters);
        }
        
        // With 6-unit rejection threshold, expect ~96% success rate (288+ of 300)
        assertTrue(errors.size() >= 285, 
            String.format("Should successfully locate at least 285 of 300 stops, got %d", errors.size()));
        
        // Calculate statistics
        Collections.sort(errors);
        double median = errors.get(errors.size() / 2);
        double p90 = errors.get((int)(errors.size() * 0.9));
        double max = errors.get(errors.size() - 1);
        
        System.out.println(String.format("300-stop accuracy test: %d stops located", errors.size()));
        System.out.println(String.format("  Median error: %.0f m", median));
        System.out.println(String.format("  P90 error: %.0f m", p90));
        System.out.println(String.format("  Max error: %.0f m", max));
        
        // Assert median threshold (per user answer to Task 1 question 3)
        assertTrue(median <= 100.0, 
            String.format("Median error should be ≤ 100 m, was %.0f m", median));
    }
    
    /**
     * Helper method to geocode an address string directly to a coordinate.
     * Throws assertion error if geocoding fails.
     */
    private Coordinate geocode(String addressStr) {
        Optional<ParsedAddress> parsed = parser.parse(addressStr);
        assertTrue(parsed.isPresent(), "Failed to parse: " + addressStr);
        
        Optional<Coordinate> coord = locator.locate(parsed.get());
        assertTrue(coord.isPresent(), "Failed to locate: " + addressStr);
        
        return coord.get();
    }
    
    /**
     * Haversine distance in meters between two coordinates.
     */
    private double haversineDistance(Coordinate c1, Coordinate c2) {
        double lat1 = Math.toRadians(c1.getLat());
        double lat2 = Math.toRadians(c2.getLat());
        double deltaLat = Math.toRadians(c2.getLat() - c1.getLat());
        double deltaLon = Math.toRadians(c2.getLon() - c1.getLon());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371000 * c; // Earth radius in meters
    }
}
