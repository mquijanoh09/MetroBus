package co.medellin.transit.model;

import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StepTest {

    @Test
    void testStepWithRouteInfo() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Parque Berrio", coord);
        
        Step step = new Step(
            StepType.BOARD,
            "Abordar Línea A en Parque Berrio",
            0,
            place,
            Optional.of("Línea A")
        );
        
        assertEquals(StepType.BOARD, step.getType());
        assertEquals("Abordar Línea A en Parque Berrio", step.getDescription());
        assertEquals(0, step.getDurationSec());
        assertEquals(place, step.getLocation());
        assertTrue(step.getRouteInfo().isPresent());
        assertEquals("Línea A", step.getRouteInfo().get());
    }

    @Test
    void testStepWithoutRouteInfo() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Parque Berrio", coord);
        
        Step step = new Step(
            StepType.WALK,
            "Caminar a Parque Berrio",
            120,
            place
        );
        
        assertEquals(StepType.WALK, step.getType());
        assertEquals("Caminar a Parque Berrio", step.getDescription());
        assertEquals(120, step.getDurationSec());
        assertEquals(place, step.getLocation());
        assertFalse(step.getRouteInfo().isPresent());
    }

    @Test
    void testWalkStep() {
        Coordinate coord = new Coordinate(6.2500, -75.5750);
        Place place = new Place("Estación San Antonio", coord);
        
        Step step = new Step(
            StepType.WALK,
            "Caminar 200 metros",
            150,
            place
        );
        
        assertEquals(StepType.WALK, step.getType());
        assertEquals(150, step.getDurationSec());
    }

    @Test
    void testBoardStep() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Parque Berrio", coord);
        
        Step step = new Step(
            StepType.BOARD,
            "Abordar bus 301",
            0,
            place,
            Optional.of("Ruta 301")
        );
        
        assertEquals(StepType.BOARD, step.getType());
        assertEquals(0, step.getDurationSec());
        assertTrue(step.getRouteInfo().isPresent());
        assertEquals("Ruta 301", step.getRouteInfo().get());
    }

    @Test
    void testRideStep() {
        Coordinate coord = new Coordinate(6.2500, -75.5750);
        Place place = new Place("San Antonio", coord);
        
        Step step = new Step(
            StepType.RIDE,
            "Viajar en Línea A por 5 minutos",
            300,
            place,
            Optional.of("Línea A")
        );
        
        assertEquals(StepType.RIDE, step.getType());
        assertEquals(300, step.getDurationSec());
        assertTrue(step.getRouteInfo().isPresent());
    }

    @Test
    void testAlightStep() {
        Coordinate coord = new Coordinate(6.2600, -75.5700);
        Place place = new Place("Estación Cisneros", coord);
        
        Step step = new Step(
            StepType.ALIGHT,
            "Bajar en Estación Cisneros",
            0,
            place,
            Optional.of("Línea A")
        );
        
        assertEquals(StepType.ALIGHT, step.getType());
        assertEquals(0, step.getDurationSec());
        assertTrue(step.getRouteInfo().isPresent());
    }

    @Test
    void testStepToString() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Test Place", coord);
        
        Step step = new Step(
            StepType.WALK,
            "Test description",
            60,
            place,
            Optional.of("Test Route")
        );
        
        String str = step.toString();
        assertTrue(str.contains("Step"));
        assertTrue(str.contains("WALK"));
        assertTrue(str.contains("Test description"));
        assertTrue(str.contains("60"));
    }

    @Test
    void testStepWithSpanishDescription() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Parque Berrio", coord);
        
        Step step = new Step(
            StepType.WALK,
            "Caminar a la estación de metro",
            90,
            place
        );
        
        assertEquals("Caminar a la estación de metro", step.getDescription());
    }

    @Test
    void testStepWithLongDuration() {
        Coordinate coord = new Coordinate(6.2442, -75.5812);
        Place place = new Place("Test", coord);
        
        // 30 minutes = 1800 seconds
        Step step = new Step(StepType.RIDE, "Long ride", 1800, place);
        
        assertEquals(1800, step.getDurationSec());
        assertEquals(30, step.getDurationSec() / 60); // 30 minutes
    }
}
