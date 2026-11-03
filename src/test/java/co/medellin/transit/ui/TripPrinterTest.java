package co.medellin.transit.ui;

import co.medellin.transit.fares.FareResult;
import co.medellin.transit.fares.FareSegment;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.RouteType;
import co.medellin.transit.model.Step;
import co.medellin.transit.model.StepType;
import co.medellin.transit.model.Trip;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the TripPrinter class.
 */
class TripPrinterTest {
    
    @Test
    void testPrintSimpleTrip() {
        // Create a simple trip with one walk step
        Place origin = new Place("Origen", new Coordinate(6.25, -75.57));
        Place destination = new Place("Destino", new Coordinate(6.26, -75.58));
        
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(StepType.WALK, "Caminar a Destino", 300, destination));
        
        Trip trip = new Trip(steps, 300, 450.0);
        FareResult fare = FareResult.zero();
        
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            TripPrinter printer = new TripPrinter();
            printer.print(trip, fare);
            
            String output = outputStream.toString();
            
            // Verify output contains expected elements
            assertTrue(output.contains("RUTA ENCONTRADA"));
            assertTrue(output.contains("Duración total: 5 minutos")); // 300 seconds = 5 minutes
            assertTrue(output.contains("Distancia a pie: 450 metros"));
            assertTrue(output.contains("Tarifa total: $0 COP"));
            assertTrue(output.contains("Instrucciones:"));
            assertTrue(output.contains("1. Caminar a Destino"));
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    void testPrintTripWithIntegration() {
        // Create a trip with integration discount
        Place origin = new Place("Origen", new Coordinate(6.25, -75.57));
        Place destination = new Place("Destino", new Coordinate(6.26, -75.58));
        
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(StepType.WALK, "Caminar a Estación", 120, origin));
        steps.add(new Step(StepType.BOARD, "Abordar Línea A", 0, origin));
        steps.add(new Step(StepType.RIDE, "Viajar en Línea A", 600, destination));
        steps.add(new Step(StepType.ALIGHT, "Bajar en Estación", 30, destination));
        
        Trip trip = new Trip(steps, 750, 150.0);
        
        // Create fare with integration
        List<FareSegment> segments = new ArrayList<>();
        segments.add(new FareSegment(RouteType.METRO, 3000, 3000, false));
        FareResult fare = new FareResult(3000, segments, true);
        
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            TripPrinter printer = new TripPrinter();
            printer.print(trip, fare);
            
            String output = outputStream.toString();
            
            // Verify integration message is shown
            assertTrue(output.contains("✓ Descuento por integración aplicado"));
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    void testPrintFormatsStepsCorrectly() {
        // Create a multi-step trip
        Place stop1 = new Place("Parada 1", new Coordinate(6.25, -75.57));
        Place stop2 = new Place("Parada 2", new Coordinate(6.26, -75.58));
        Place stop3 = new Place("Parada 3", new Coordinate(6.27, -75.59));
        
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(StepType.WALK, "Caminar a Parada 1", 180, stop1));
        steps.add(new Step(StepType.BOARD, "Abordar Ruta 301", 0, stop1));
        steps.add(new Step(StepType.RIDE, "Viajar en Ruta 301", 900, stop2));
        steps.add(new Step(StepType.ALIGHT, "Bajar en Parada 2", 30, stop2));
        steps.add(new Step(StepType.WALK, "Caminar a Parada 3", 120, stop3));
        
        Trip trip = new Trip(steps, 1230, 400.0);
        FareResult fare = new FareResult(2950, new ArrayList<>(), false);
        
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            TripPrinter printer = new TripPrinter();
            printer.print(trip, fare);
            
            String output = outputStream.toString();
            
            // Verify all steps are numbered
            assertTrue(output.contains("1. Caminar a Parada 1"));
            assertTrue(output.contains("2. Abordar Ruta 301"));
            assertTrue(output.contains("3. Viajar en Ruta 301"));
            assertTrue(output.contains("4. Bajar en Parada 2"));
            assertTrue(output.contains("5. Caminar a Parada 3"));
            
            // Verify duration shown for steps with non-zero duration
            assertTrue(output.contains("(3 min)")); // 180 seconds
            assertTrue(output.contains("(15 min)")); // 900 seconds
            assertTrue(output.contains("(2 min)")); // 120 seconds
        } finally {
            System.setOut(originalOut);
        }
    }
}
