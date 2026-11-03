package co.medellin.transit.routing;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareConfig;
import co.medellin.transit.fares.FareResult;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.geocoding.ParsedAddress;
import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Trip;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * Quick test to validate a single reference test case.
 */
class QuickReferenceTest {
    
    @Test
    void testSingleRoute() throws Exception {
        System.out.println("Loading graph...");
        String dataDir = "C:\\Users\\marti\\Downloads\\transporte-medellin\\kiro\\data";
        
        GraphBuilder builder = new GraphBuilder();
        TransitGraph graph = builder.build(Paths.get(dataDir));
        
        System.out.println("Graph loaded:");
        System.out.println("  States: " + graph.getStateCount());
        System.out.println("  Edges: " + graph.getEdgeCount());
        
        // Initialize components
        Parameters params = new Parameters(500.0, 120, 1.5);
        Router router = new Router(graph, params);
        StepBuilder stepBuilder = new StepBuilder(graph);
        FareCalculator fareCalculator = new FareCalculator(FareConfig.defaults(), graph);
        AddressParser addressParser = new AddressParser();
        AddressLocator addressLocator = new AddressLocator();
        
        // Test case from CSV: CL 50 53-48,CR 70 x CL 48,21,3800,0,bus:191
        String origin = "CL 50 53-48";
        String destination = "CR 70 x CL 48";
        int expectedMinutes = 21;
        int expectedCost = 3800;
        
        System.out.println("\n Testing route: " + origin + " -> " + destination);
        System.out.println("Expected: " + expectedMinutes + " min, $" + expectedCost + " COP");
        
        // Geocode
        Optional<ParsedAddress> parsedOrigin = addressParser.parse(origin);
        Optional<ParsedAddress> parsedDest = addressParser.parse(destination);
        
        if (parsedOrigin.isEmpty() || parsedDest.isEmpty()) {
            System.out.println("Failed to parse addresses");
            return;
        }
        
        Optional<Coordinate> originCoord = addressLocator.locate(parsedOrigin.get());
        Optional<Coordinate> destCoord = addressLocator.locate(parsedDest.get());
        
        if (originCoord.isEmpty() || destCoord.isEmpty()) {
            System.out.println("Failed to geocode addresses");
            return;
        }
        
        System.out.println("Origin: " + originCoord.get());
        System.out.println("Destination: " + destCoord.get());
        
        // Route
        Place originPlace = new Place("Origin", originCoord.get());
        Place destPlace = new Place("Destination", destCoord.get());
        
        Optional<DijkstraResult> result = router.findRoute(originPlace, destPlace);
        
        if (result.isEmpty()) {
            System.out.println("No route found!");
            return;
        }
        
        Trip trip = stepBuilder.buildTrip(result.get());
        int actualMinutes = (int) Math.ceil(trip.getTotalDurationSec() / 60.0);
        
        FareResult fare = fareCalculator.calculate(trip);
        int actualCost = fare.getTotalCOP();
        
        System.out.println("Actual: " + actualMinutes + " min, $" + actualCost + " COP");
        System.out.println("\nTrip steps:");
        for (int i = 0; i < trip.getSteps().size(); i++) {
            System.out.println("  " + (i + 1) + ". " + trip.getSteps().get(i).getDescription());
        }
        
        // Compare
        boolean minutesMatch = Math.abs(actualMinutes - expectedMinutes) <= 2;
        boolean costMatch = Math.abs(actualCost - expectedCost) <= 100;
        
        System.out.println("\nResult: " + (minutesMatch && costMatch ? "PASS" : "FAIL"));
        if (!minutesMatch) {
            System.out.println("  Duration mismatch: expected " + expectedMinutes + " min, got " + actualMinutes + " min");
        }
        if (!costMatch) {
            System.out.println("  Cost mismatch: expected $" + expectedCost + " COP, got $" + actualCost + " COP");
        }
    }
}
