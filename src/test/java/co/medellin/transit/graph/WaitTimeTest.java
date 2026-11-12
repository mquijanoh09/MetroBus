package co.medellin.transit.graph;

import co.medellin.transit.model.Edge;
import co.medellin.transit.model.EdgeKind;
import co.medellin.transit.model.State;
import co.medellin.transit.model.StateType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that BOARD edges have correct wait times based on transit mode.
 * Per graph-structure.md section 4:
 * - Metro: 240s
 * - Metroplús (BRT): 300s
 * - Tram: 300s
 * - Cable car: 120s
 * - Bus: 300s
 */
class WaitTimeTest {
    
    private static final String DATA_DIR = "data";
    
    private static TransitGraph graph;
    
    @BeforeAll
    static void setUp() throws IOException {
        graph = new GraphBuilder().build(Paths.get(DATA_DIR));
    }
    
    @Test
    @DisplayName("Metro stations should have 240s wait time")
    void testMetroWaitTime() {
        // Find a metro station state (line A, B, etc.)
        State metroState = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.METRO_STATION)
            .filter(s -> s.getRouteId().isPresent() && s.getRouteId().get().equals("A"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No metro station found for line A"));
        
        // Find the BOARD edge leading to this state
        Edge boardEdge = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.WALK)
            .flatMap(s -> graph.getOutgoingEdges(s.getId()).stream())
            .filter(e -> e.getKind() == EdgeKind.BOARD)
            .filter(e -> e.getTargetId() == metroState.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("No BOARD edge found for metro station"));
        
        assertEquals(240, boardEdge.getRealSec(), 
            "Metro BOARD edge should have 240s wait time");
        
        System.out.println("✓ Metro wait time: " + boardEdge.getRealSec() + "s (expected 240s)");
    }
    
    @Test
    @DisplayName("Cable car stations should have 120s wait time")
    void testCableCarWaitTime() {
        // Find a cable car station (line J, K, L, M, P)
        State cableCarState = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.METRO_STATION)
            .filter(s -> s.getRouteId().isPresent())
            .filter(s -> {
                String line = s.getRouteId().get();
                return line.equals("J") || line.equals("K") || line.equals("L") || 
                       line.equals("M") || line.equals("P");
            })
            .findFirst()
            .orElseThrow(() -> new AssertionError("No cable car station found"));
        
        // Find the BOARD edge leading to this state
        Edge boardEdge = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.WALK)
            .flatMap(s -> graph.getOutgoingEdges(s.getId()).stream())
            .filter(e -> e.getKind() == EdgeKind.BOARD)
            .filter(e -> e.getTargetId() == cableCarState.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("No BOARD edge found for cable car station"));
        
        assertEquals(120, boardEdge.getRealSec(), 
            "Cable car BOARD edge should have 120s wait time");
        
        System.out.println("✓ Cable car wait time: " + boardEdge.getRealSec() + "s (expected 120s)");
    }
    
    @Test
    @DisplayName("Metroplús stations should have 300s wait time")
    void testMetroplusWaitTime() {
        // Find a Metroplús station (line 1 or 2)
        State metroplusState = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.METRO_STATION)
            .filter(s -> s.getRouteId().isPresent())
            .filter(s -> {
                String line = s.getRouteId().get();
                return line.equals("1") || line.equals("2");
            })
            .findFirst()
            .orElseThrow(() -> new AssertionError("No Metroplús station found"));
        
        // Find the BOARD edge leading to this state
        Edge boardEdge = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.WALK)
            .flatMap(s -> graph.getOutgoingEdges(s.getId()).stream())
            .filter(e -> e.getKind() == EdgeKind.BOARD)
            .filter(e -> e.getTargetId() == metroplusState.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("No BOARD edge found for Metroplús station"));
        
        assertEquals(300, boardEdge.getRealSec(), 
            "Metroplús BOARD edge should have 300s wait time");
        
        System.out.println("✓ Metroplús wait time: " + boardEdge.getRealSec() + "s (expected 300s)");
    }
    
    @Test
    @DisplayName("Bus stops should have 300s wait time")
    void testBusWaitTime() {
        // Find any bus stop
        State busState = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.BUS_STOP)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No bus stop found"));
        
        // Find the BOARD edge leading to this state
        Edge boardEdge = graph.getStates().stream()
            .filter(s -> s.getType() == StateType.WALK)
            .flatMap(s -> graph.getOutgoingEdges(s.getId()).stream())
            .filter(e -> e.getKind() == EdgeKind.BOARD)
            .filter(e -> e.getTargetId() == busState.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("No BOARD edge found for bus stop"));
        
        assertEquals(300, boardEdge.getRealSec(), 
            "Bus BOARD edge should have 300s wait time");
        
        System.out.println("✓ Bus wait time: " + boardEdge.getRealSec() + "s (expected 300s)");
    }
}
