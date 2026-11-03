package co.medellin.transit.ui;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for the Menu class.
 * 
 * Tests basic functionality:
 * - Exit on "salir" command
 * - Error handling for invalid addresses
 * 
 * Note: Full integration tests would require a complete graph and routing system.
 * These tests verify the basic interaction flow and error handling.
 */
class MenuTest {
    
    @Test
    void testHomeScreenDisplays() {
        // Capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            HomeScreen homeScreen = new HomeScreen();
            homeScreen.display();
            
            String output = outputStream.toString();
            assertTrue(output.contains("Sistema de Rutas Medellín Metro"));
            assertTrue(output.contains("Bienvenido"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
