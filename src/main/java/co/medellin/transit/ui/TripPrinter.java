package co.medellin.transit.ui;

import co.medellin.transit.fares.FareResult;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;
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
    private final PrintStream out;
    
    /**
     * Creates a new TripPrinter with Colombian locale formatting and UTF-8 output.
     */
    public TripPrinter() {
        // Use Colombian locale for number formatting (thousands separator)
        this.currencyFormat = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        this.currencyFormat.setGroupingUsed(true);
        
        // Create UTF-8 output stream to ensure accents display correctly
        this.out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
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
        out.println();
        out.println(SEPARATOR);
        out.println("   RUTA ENCONTRADA");
        out.println(SEPARATOR);
        out.println();
        
        // Trip summary
        printSummary(trip, fare);
        
        // Step-by-step instructions
        out.println("Instrucciones:");
        printSteps(trip);
        
        // Data attribution notice
        out.println();
        out.println("Datos de la Alcaldía de Medellín; pueden no estar al día");
        
        // Integration notice
        if (fare.isIntegrationApplied()) {
            out.println();
            out.println(CHECK_MARK + " Descuento por integración aplicado");
        }
        
        // Footer
        out.println();
        out.println(SEPARATOR);
    }
    
    /**
     * Prints the trip summary section.
     * 
     * @param trip the trip data
     * @param fare the fare data
     */
    private void printSummary(Trip trip, FareResult fare) {
        // Duration in minutes - use rounding to match reference output
        int totalMinutes = (int) Math.round(trip.getTotalDurationSec() / 60.0);
        out.printf("Duración total: %d minutos%n", totalMinutes);
        
        // Walking distance in meters
        int walkMeters = (int) Math.round(trip.getTotalWalkMeters());
        out.printf("Distancia a pie: %d metros%n", walkMeters);
        
        // Total fare in Colombian Pesos
        String formattedFare = currencyFormat.format(fare.getTotalCOP());
        out.printf("Tarifa total: $%s COP%n", formattedFare);
        
        // Number of transfers
        int transfers = calculateTransfers(trip);
        out.printf("Transferencias: %d%n", transfers);
        
        out.println();
    }
    
    /**
     * Prints the step-by-step instructions.
     * 
     * @param trip the trip with steps
     */
    private void printSteps(Trip trip) {
        int stepNumber = 1;
        
        // Calculate step minutes using largest-remainder method so they sum to the total
        int totalMinutes = (int) Math.round(trip.getTotalDurationSec() / 60.0);
        int[] stepMinutes = allocateMinutesLargestRemainder(trip, totalMinutes);
        
        int stepIndex = 0;
        for (Step step : trip.getSteps()) {
            // Step number and base description
            String description = step.getDescription();
            
            // For WAIT steps, replace the "(X min)" part with the allocated minutes
            if (step.getType() == StepType.WAIT && step.getDurationSec() > 0) {
                int minutes = stepMinutes[stepIndex];
                // Replace the old "(X min)" with the new allocated minutes
                description = description.replaceFirst("\\(\\d+ min\\)", "(" + minutes + " min)");
                out.printf("%d. %s", stepNumber, description);
            } else {
                out.printf("%d. %s", stepNumber, description);
                
                // Add duration if non-zero (omit for BOARD/ALIGHT steps which have 0 duration)
                if (step.getDurationSec() > 0) {
                    int minutes = stepMinutes[stepIndex];
                    out.printf(" (%d min)", minutes);
                }
            }
            
            out.println();
            stepNumber++;
            stepIndex++;
        }
    }
    
    /**
     * Allocates minutes to steps using the largest-remainder method.
     * This ensures that the sum of displayed step minutes equals the total.
     * 
     * @param trip the trip with steps
     * @param totalMinutes the total minutes to allocate
     * @return array of minutes for each step
     */
    private int[] allocateMinutesLargestRemainder(Trip trip, int totalMinutes) {
        List<Step> steps = trip.getSteps();
        int[] minutes = new int[steps.size()];
        double[] fractional = new double[steps.size()];
        int allocatedSum = 0;
        
        // First pass: allocate floor values and track remainders
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (step.getDurationSec() > 0) {
                double exactMinutes = step.getDurationSec() / 60.0;
                int floor = (int) exactMinutes;
                minutes[i] = floor;
                fractional[i] = exactMinutes - floor;
                allocatedSum += floor;
            } else {
                minutes[i] = 0;
                fractional[i] = 0.0;
            }
        }
        
        // Second pass: distribute remaining minutes to steps with largest remainders
        int remaining = totalMinutes - allocatedSum;
        while (remaining > 0) {
            // Find step with largest fractional part that hasn't been maxed out
            int maxIndex = -1;
            double maxFractional = -1.0;
            for (int i = 0; i < steps.size(); i++) {
                if (fractional[i] > maxFractional && steps.get(i).getDurationSec() > 0) {
                    maxFractional = fractional[i];
                    maxIndex = i;
                }
            }
            
            if (maxIndex == -1) {
                break; // No more candidates
            }
            
            minutes[maxIndex]++;
            fractional[maxIndex] = -1.0; // Mark as used
            allocatedSum++;
            remaining--;
        }
        
        // Ensure minimum of 1 minute for visible steps (non-zero duration, not BOARD/ALIGHT)
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (step.getDurationSec() > 0 && 
                step.getType() != StepType.BOARD && 
                step.getType() != StepType.ALIGHT && 
                minutes[i] == 0) {
                minutes[i] = 1;
            }
        }
        
        return minutes;
    }
    
    /**
     * Calculates the number of transfers in a trip.
     * 
     * @param trip the trip to analyze
     * @return number of transfers (number of vehicles - 1, minimum 0)
     */
    private int calculateTransfers(Trip trip) {
        int vehicleCount = 0;
        
        for (Step step : trip.getSteps()) {
            // Count RIDE steps (steps that involve a vehicle)
            if (step.getType() == StepType.RIDE) {
                vehicleCount++;
            }
        }
        
        // Transfers = number of vehicles - 1 (minimum 0)
        return Math.max(0, vehicleCount - 1);
    }
}
