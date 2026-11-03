package co.medellin.transit.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HomeScreen.
 */
class HomeScreenTest {
    
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }
    
    @Test
    void testDisplayShowsWelcomeBanner() {
        HomeScreen homeScreen = new HomeScreen();
        homeScreen.display();
        
        String output = outputStream.toString();
        
        // Verify the welcome message is present
        assertTrue(output.contains("Sistema de Rutas Medellín Metro"),
                "Welcome banner should contain the application name");
    }
    
    @Test
    void testDisplayUsesBoxDrawing() {
        HomeScreen homeScreen = new HomeScreen();
        homeScreen.display();
        
        String output = outputStream.toString();
        
        // Verify ASCII box drawing characters are used
        assertTrue(output.contains("╔") || output.contains("═") || output.contains("╗"),
                "Should use ASCII box drawing characters for visual appeal");
    }
    
    @Test
    void testDisplayEndsWithNewline() {
        HomeScreen homeScreen = new HomeScreen();
        homeScreen.display();
        
        String output = outputStream.toString();
        
        // Verify output ends with newline for proper spacing
        assertTrue(output.endsWith("\n"),
                "Display should end with newline for proper spacing");
    }
}
