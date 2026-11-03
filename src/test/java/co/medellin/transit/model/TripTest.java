package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    @Test
    void testEmptyTrip() {
        Trip trip = new Trip();
        
        assertEquals(0, trip.getStepCount());
        assertEquals(0, trip.getTotalDurationSec());
        assertEquals(0.0, trip.getTotalWalkMeters());
        assertEquals(0, trip.getTotalDurationMinutes());
        assertTrue(trip.getSteps().isEmpty());
    }

    @Test
    void testTripWithSteps() {
        Coordinate coord1 = new Coordinate(6.2442, -75.5812);
        Coordinate coord2 = new Coordinate(6.2500, -75.5750);
        Place place1 = new Place("Parque Berrio", coord1);
        Place place2 = new Place("Estación San Antonio", coord2);
        
        Step step1 = new Step(StepType.WALK, "Caminar a Parque Berrio", 120, place1);
        Step step2 = new Step(StepType.BOARD, "Abordar Línea A", 0, place1, Optional.of("Línea A"));
        Step step3 = new Step(StepType.RIDE, "Viajar en Línea A", 180, place2, Optional.of("Línea A"));
        
        List<Step> steps = new ArrayList<>();
        steps.add(step1);
        steps.add(step2);
        steps.add(step3);
        
        Trip trip = new Trip(steps, 300, 150.0);
        
        assertEquals(3, trip.getStepCount());
        assertEquals(300, trip.getTotalDurationSec());
        assertEquals(5, trip.getTotalDurationMinutes());
        assertEquals(150.0, trip.getTotalWalkMeters());
        assertEquals(3, trip.getSteps().size());
    }

    @Test
    void testTripImmutability() {
        List<Step> steps = new ArrayList<>();
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Test", coord);
        Step step = new Step(StepType.WALK, "Walk", 60, place);
        steps.add(step);
        
        Trip trip = new Trip(steps, 60, 50.0);
        
        // Modify original list
        steps.clear();
        
        // Trip should still have the step
        assertEquals(1, trip.getStepCount());
        
        // Returned list should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            trip.getSteps().clear();
        });
    }

    @Test
    void testTripDurationMinutesRounding() {
        List<Step> steps = new ArrayList<>();
        
        // 61 seconds = 2 minutes (rounded up)
        Trip trip1 = new Trip(steps, 61, 0.0);
        assertEquals(2, trip1.getTotalDurationMinutes());
        
        // 60 seconds = 1 minute (exact)
        Trip trip2 = new Trip(steps, 60, 0.0);
        assertEquals(1, trip2.getTotalDurationMinutes());
        
        // 59 seconds = 1 minute (rounded up)
        Trip trip3 = new Trip(steps, 59, 0.0);
        assertEquals(1, trip3.getTotalDurationMinutes());
        
        // 120 seconds = 2 minutes (exact)
        Trip trip4 = new Trip(steps, 120, 0.0);
        assertEquals(2, trip4.getTotalDurationMinutes());
    }

    @Test
    void testTripBuilder() {
        Coordinate coord1 = new Coordinate(6.2442, -75.5812);
        Coordinate coord2 = new Coordinate(6.2500, -75.5750);
        Place place1 = new Place("Parque Berrio", coord1);
        Place place2 = new Place("San Antonio", coord2);
        
        Step step1 = new Step(StepType.WALK, "Caminar", 60, place1);
        Step step2 = new Step(StepType.BOARD, "Abordar", 0, place1, Optional.of("Línea A"));
        Step step3 = new Step(StepType.RIDE, "Viajar", 120, place2, Optional.of("Línea A"));
        
        Trip trip = new Trip.Builder()
                .addStep(step1)
                .addStep(step2)
                .addStep(step3)
                .setTotalWalkMeters(75.0)
                .build();
        
        assertEquals(3, trip.getStepCount());
        assertEquals(180, trip.getTotalDurationSec());
        assertEquals(75.0, trip.getTotalWalkMeters());
    }

    @Test
    void testTripBuilderAccumulatesDuration() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Test", coord);
        
        Step step1 = new Step(StepType.WALK, "Walk 1", 60, place);
        Step step2 = new Step(StepType.WALK, "Walk 2", 90, place);
        Step step3 = new Step(StepType.RIDE, "Ride", 120, place);
        
        Trip trip = new Trip.Builder()
                .addStep(step1)
                .addStep(step2)
                .addStep(step3)
                .build();
        
        // Builder should accumulate duration from steps
        assertEquals(270, trip.getTotalDurationSec());
    }

    @Test
    void testTripBuilderManualDurationOverride() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Test", coord);
        
        Step step1 = new Step(StepType.WALK, "Walk", 60, place);
        
        Trip trip = new Trip.Builder()
                .addStep(step1)
                .setTotalDurationSec(300)  // Override accumulated duration
                .setTotalWalkMeters(100.0)
                .build();
        
        assertEquals(300, trip.getTotalDurationSec());
        assertEquals(100.0, trip.getTotalWalkMeters());
    }

    @Test
    void testTripToString() {
        List<Step> steps = new ArrayList<>();
        Trip trip = new Trip(steps, 180, 100.0);
        
        String str = trip.toString();
        assertTrue(str.contains("Trip"));
        assertTrue(str.contains("180"));
        assertTrue(str.contains("100.0"));
    }
}
