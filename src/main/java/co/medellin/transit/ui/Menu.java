package co.medellin.transit.ui;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareResult;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.geocoding.ParsedAddress;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Trip;
import co.medellin.transit.routing.DijkstraResult;
import co.medellin.transit.routing.Router;
import co.medellin.transit.routing.StepBuilder;

import java.util.Optional;
import java.util.Scanner;

/**
 * Interactive console menu for the Medellín Transit Routing System.
 * 
 * <p>This class provides the main interaction loop for the application:
 * <ol>
 *   <li>Prompt user for origin address in Spanish</li>
 *   <li>Parse and geocode the origin address</li>
 *   <li>Prompt user for destination address</li>
 *   <li>Parse and geocode the destination address</li>
 *   <li>Check if origin and destination are too close (<50m)</li>
 *   <li>Compute the optimal route</li>
 *   <li>Display the trip instructions and fare</li>
 *   <li>Repeat until user enters "salir"</li>
 * </ol>
 * 
 * <p>Error handling includes:
 * <ul>
 *   <li>Invalid address format: Show error with example</li>
 *   <li>Address out of bounds: Show bounds error message</li>
 *   <li>No route found: Show no route message</li>
 *   <li>Origin and destination too close: Show proximity message</li>
 * </ul>
 * 
 * <p>All user-facing text is in Spanish.
 */
public class Menu {
    
    private static final double MIN_DISTANCE_METERS = 50.0;
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    
    private final Scanner scanner;
    private final Router router;
    private final AddressParser addressParser;
    private final AddressLocator addressLocator;
    private final FareCalculator fareCalculator;
    private final StepBuilder stepBuilder;
    private final TripPrinter tripPrinter;
    
    /**
     * Creates a new Menu with all required dependencies.
     * 
     * @param scanner input scanner for reading user input
     * @param router router for computing optimal routes
     * @param addressParser parser for Medellín address format
     * @param addressLocator locator for converting addresses to coordinates
     * @param fareCalculator calculator for transit fares
     * @param stepBuilder builder for trip instructions
     * @param tripPrinter printer for displaying trip results
     */
    public Menu(
            Scanner scanner,
            Router router,
            AddressParser addressParser,
            AddressLocator addressLocator,
            FareCalculator fareCalculator,
            StepBuilder stepBuilder,
            TripPrinter tripPrinter) {
        this.scanner = scanner;
        this.router = router;
        this.addressParser = addressParser;
        this.addressLocator = addressLocator;
        this.fareCalculator = fareCalculator;
        this.stepBuilder = stepBuilder;
        this.tripPrinter = tripPrinter;
    }
    
    /**
     * Runs the main interaction loop.
     * 
     * <p>Continues prompting for origin and destination until the user
     * enters "salir" to exit the application.
     */
    public void run() {
        while (true) {
            System.out.println();
            System.out.println("Ingrese la dirección de origen (o 'salir' para terminar):");
            String originStr = scanner.nextLine().trim();
            
            // Check for exit command
            if (originStr.equalsIgnoreCase("salir")) {
                System.out.println("¡Hasta luego!");
                break;
            }
            
            // Skip empty input
            if (originStr.isEmpty()) {
                continue;
            }
            
            // Geocode origin
            Optional<Place> origin = geocode(originStr);
            if (origin.isEmpty()) {
                continue;
            }
            
            System.out.println("Ingrese la dirección de destino:");
            String destStr = scanner.nextLine().trim();
            
            // Skip empty input
            if (destStr.isEmpty()) {
                continue;
            }
            
            // Geocode destination
            Optional<Place> dest = geocode(destStr);
            if (dest.isEmpty()) {
                continue;
            }
            
            // Check if origin and destination are too close
            double distance = calculateDistance(
                origin.get().getCoord(), 
                dest.get().getCoord()
            );
            
            if (distance < MIN_DISTANCE_METERS) {
                System.out.println("El origen y destino están muy cerca (menos de 50 metros).");
                System.out.println("No es necesario usar transporte público.");
                continue;
            }
            
            try {
                // Find route
                Optional<DijkstraResult> result = router.findRoute(
                    origin.get(), 
                    dest.get()
                );
                
                if (result.isEmpty()) {
                    System.out.println("No se encontró una ruta entre las direcciones indicadas.");
                    System.out.println("Intente con direcciones diferentes o más cercanas al transporte público.");
                    continue;
                }
                
                // Build trip instructions
                Trip trip = stepBuilder.buildTrip(result.get());
                
                // Calculate fare
                FareResult fare = fareCalculator.calculate(trip);
                
                // Display the trip
                tripPrinter.print(trip, fare);
                
            } catch (Exception e) {
                System.out.println("Error al calcular la ruta: " + e.getMessage());
                System.out.println("Por favor, intente con otras direcciones.");
                // Continue to next iteration instead of crashing
            }
        }
    }
    
    /**
     * Geocodes an address string to a Place.
     * 
     * <p>This method handles:
     * <ul>
     *   <li>Address parsing with format validation</li>
     *   <li>Coordinate lookup</li>
     *   <li>Bounds checking</li>
     *   <li>User-friendly error messages in Spanish</li>
     * </ul>
     * 
     * @param addressStr the address string to geocode
     * @return Place if successful, empty if geocoding failed
     */
    private Optional<Place> geocode(String addressStr) {
        // Parse the address
        Optional<ParsedAddress> parsed = addressParser.parse(addressStr);
        
        if (parsed.isEmpty()) {
            System.out.println("Error: Formato de dirección inválido.");
            System.out.println("Ejemplo válido: Carrera 43A # 1 Sur - 100");
            System.out.println("Ejemplo válido: Calle 10 # 50 - 30");
            return Optional.empty();
        }
        
        // Locate the address
        Optional<Coordinate> coord = addressLocator.locate(parsed.get());
        
        if (coord.isEmpty()) {
            System.out.println("Error: No se pudo localizar la dirección.");
            System.out.println("La dirección puede estar fuera del área de cobertura.");
            return Optional.empty();
        }
        
        // Check bounds
        if (!addressLocator.isInBounds(coord.get())) {
            System.out.println("Error: La dirección está fuera de los límites de Medellín.");
            System.out.println("Por favor, ingrese una dirección dentro de la ciudad.");
            return Optional.empty();
        }
        
        // Create place with the original address string as the name
        return Optional.of(new Place(addressStr, coord.get()));
    }
    
    /**
     * Calculates the Haversine distance between two coordinates.
     * 
     * @param c1 first coordinate
     * @param c2 second coordinate
     * @return distance in meters
     */
    private double calculateDistance(Coordinate c1, Coordinate c2) {
        double lat1 = Math.toRadians(c1.getLat());
        double lat2 = Math.toRadians(c2.getLat());
        double deltaLat = Math.toRadians(c2.getLat() - c1.getLat());
        double deltaLon = Math.toRadians(c2.getLon() - c1.getLon());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.asin(Math.sqrt(a));
        
        return EARTH_RADIUS_METERS * c;
    }
}
