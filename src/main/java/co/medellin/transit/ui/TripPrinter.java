package co.medellin.transit.ui;

import co.medellin.transit.fares.FareResult;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.Trip;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats and displays trip instructions with fare information in Spanish.
 * 
 * <p>The printer generates a formatted output including:
 * <ul>
 *   <li>Trip summary (duration, walking distance, total fare)</li>
 *   <li>Step-by-step instructions with duration for each step</li>
 *   <li>Integration discount notification if applicable</li>
 * </ul>
 * 
 * <p>Output format example:
 * <pre>
 * ==== RUTA ENCONTRADA ====
 * 
 * Duración total: 25 minutos
 * Distancia a pie: 450 metros
 * Tarifa total: $3,000 COP
 * 
 * Instrucciones:
 * 1. Caminar a Parada Norte (2 min)
 * 2. Abordar Línea A en Parada Norte
 * 3. Viajar en Línea A por 15 minutos
 * 4. Bajar en Estación Central
 * 5. Caminar a destino (3 min)
 * 
 * ✓ Descuento por integración aplicado
 * 
 * =========================
 * </pre>
 * 
 * <p>All text is in Spanish with Colombian Peso formatting.
 */
public class TripPrinter {
    
    private static final String SEPARATOR = "============================";
    private static final String CHECK_MARK = "✓";
    
    private final NumberFormat currencyFormat;
    
    /**
     * Creates a new TripPrinter with Colombian locale formatting.
     */
    public TripPrinter() {
        // Use Colombian locale for number formatting (thousands separator)
        this.currencyFormat = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        this.currencyFormat.setGroupingUsed(true);
    }
    
    /**
     * Prints a complete trip with fare information to console.
     * 
     * @param trip the trip with steps and summary data
     * @param fare the fare breakdown for the trip
     */
    public void print(Trip trip, FareResult fare) {
        if (trip == null || fare == null) {
            throw new IllegalArgumentException("trip and fare cannot be null");
        }
        
        // Header
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println("   RUTA ENCONTRADA");
        System.out.println(SEPARATOR);
        System.out.println();
        
        // Trip summary
        printSummary(trip, fare);
        
        // Step-by-step instructions
        System.out.println("Instrucciones:");
        printSteps(trip);
        
        // Integration notice
        if (fare.isIntegrationApplied()) {
            System.out.println();
            System.out.println(CHECK_MARK + " Descuento por integración aplicado");
        }
        
        // Footer
        System.out.println();
        System.out.println(SEPARATOR);
    }
    
    /**
     * Prints the trip summary section.
     * 
     * @param trip the trip data
     * @param fare the fare data
     */
    private void printSummary(Trip trip, FareResult fare) {
        // Duration in minutes
        int totalMinutes = trip.getTotalDurationSec() / 60;
        System.out.printf("Duración total: %d minutos%n", totalMinutes);
        
        // Walking distance in meters
        int walkMeters = (int) Math.round(trip.getTotalWalkMeters());
        System.out.printf("Distancia a pie: %d metros%n", walkMeters);
        
        // Total fare in Colombian Pesos
        String formattedFare = currencyFormat.format(fare.getTotalCOP());
        System.out.printf("Tarifa total: $%s COP%n", formattedFare);
        System.out.println();
    }
    
    /**
     * Prints the step-by-step instructions.
     * 
     * @param trip the trip with steps
     */
    private void printSteps(Trip trip) {
        int stepNumber = 1;
        
        for (Step step : trip.getSteps()) {
            // Step number and description
            System.out.printf("%d. %s", stepNumber, step.getDescription());
            
            // Add duration if non-zero (omit for BOARD steps which have 0 duration)
            if (step.getDurationSec() > 0) {
                int minutes = (int) Math.ceil(step.getDurationSec() / 60.0);
                System.out.printf(" (%d min)", minutes);
            }
            
            System.out.println();
            stepNumber++;
        }
    }
}
