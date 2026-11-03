package co.medellin.transit;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Main class initialization.
 * 
 * <p>Tests verify:</p>
 * <ul>
 *   <li>Main class structure and imports are correct</li>
 *   <li>Spanish error messages are present</li>
 *   <li>All required components are initialized</li>
 * </ul>
 * 
 * <p>Note: We don't actually run Main.main() in tests because it calls System.exit(),
 * which would crash the test harness. Instead, we verify the structure and content
 * of the Main class through source code analysis.</p>
 */
class MainTest {
    
    /**
     * Test that Main class compiles and has proper structure.
     * This is a basic smoke test to ensure no compilation errors.
     */
    @Test
    void testMainClassExists() {
        // Verify Main class can be loaded
        assertNotNull(Main.class);
        
        // Verify main method exists with correct signature
        try {
            Main.class.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            fail("Main.main(String[]) method should exist");
        }
    }
    
    /**
     * Test that required components are imported correctly.
     * Verifies Task 14.1 requirement: initialize all components.
     */
    @Test
    void testMainImportsAllRequiredComponents() {
        // This test verifies the Main class structure through source code analysis
        String mainSource;
        try {
            Path mainFile = Path.of("src/main/java/co/medellin/transit/Main.java");
            mainSource = Files.readString(mainFile);
        } catch (Exception e) {
            fail("Could not read Main.java source file: " + e.getMessage());
            return;
        }
        
        // Verify imports for all required components (Task 14.1 requirements)
        assertTrue(mainSource.contains("import co.medellin.transit.graph.GraphBuilder"),
                  "Should import GraphBuilder");
        assertTrue(mainSource.contains("import co.medellin.transit.routing.Router"),
                  "Should import Router");
        assertTrue(mainSource.contains("import co.medellin.transit.fares.FareCalculator"),
                  "Should import FareCalculator");
        assertTrue(mainSource.contains("import co.medellin.transit.fares.FareConfig"),
                  "Should import FareConfig");
        assertTrue(mainSource.contains("import co.medellin.transit.geocoding.AddressParser"),
                  "Should import AddressParser");
        assertTrue(mainSource.contains("import co.medellin.transit.geocoding.AddressLocator"),
                  "Should import AddressLocator");
        assertTrue(mainSource.contains("import co.medellin.transit.routing.StepBuilder"),
                  "Should import StepBuilder");
        assertTrue(mainSource.contains("import co.medellin.transit.routing.Parameters"),
                  "Should import Parameters");
        assertTrue(mainSource.contains("import co.medellin.transit.ui.HomeScreen"),
                  "Should import HomeScreen");
        assertTrue(mainSource.contains("import co.medellin.transit.ui.Menu"),
                  "Should import Menu");
        assertTrue(mainSource.contains("import co.medellin.transit.ui.TripPrinter"),
                  "Should import TripPrinter");
        assertTrue(mainSource.contains("import java.util.Scanner"),
                  "Should import Scanner for user input");
    }
    
    /**
     * Test that all components are initialized in main().
     * Verifies Task 14.1 requirement: initialize all components.
     */
    @Test
    void testAllComponentsAreInitialized() {
        String mainSource;
        try {
            Path mainFile = Path.of("src/main/java/co/medellin/transit/Main.java");
            mainSource = Files.readString(mainFile);
        } catch (Exception e) {
            fail("Could not read Main.java source file: " + e.getMessage());
            return;
        }
        
        // Verify all components are instantiated
        assertTrue(mainSource.contains("new GraphBuilder()"),
                  "Should create GraphBuilder instance");
        assertTrue(mainSource.contains("graphBuilder.build("),
                  "Should call GraphBuilder.build()");
        assertTrue(mainSource.contains("new Parameters()"),
                  "Should create Parameters instance");
        assertTrue(mainSource.contains("FareConfig.fromProperties()"),
                  "Should load FareConfig from properties");
        assertTrue(mainSource.contains("new Router(graph, params)"),
                  "Should create Router with graph and params");
        assertTrue(mainSource.contains("new FareCalculator(fareConfig, graph)"),
                  "Should create FareCalculator with config and graph");
        assertTrue(mainSource.contains("new AddressParser()"),
                  "Should create AddressParser instance");
        assertTrue(mainSource.contains("new AddressLocator()"),
                  "Should create AddressLocator instance");
        assertTrue(mainSource.contains("new StepBuilder(graph)"),
                  "Should create StepBuilder with graph");
        assertTrue(mainSource.contains("new TripPrinter()"),
                  "Should create TripPrinter instance");
        assertTrue(mainSource.contains("new HomeScreen()"),
                  "Should create HomeScreen instance");
        assertTrue(mainSource.contains("homeScreen.display()"),
                  "Should display HomeScreen");
        assertTrue(mainSource.contains("new Scanner(System.in)"),
                  "Should create Scanner for input");
        assertTrue(mainSource.contains("new Menu("),
                  "Should create Menu instance");
        assertTrue(mainSource.contains("menu.run()"),
                  "Should start Menu loop");
    }
    
    /**
     * Test that error handling is properly implemented.
     * Verifies Task 14.1 requirements: OutOfMemoryError and IOException handling.
     */
    @Test
    void testErrorHandlingIsImplemented() {
        String mainSource;
        try {
            Path mainFile = Path.of("src/main/java/co/medellin/transit/Main.java");
            mainSource = Files.readString(mainFile);
        } catch (Exception e) {
            fail("Could not read Main.java source file: " + e.getMessage());
            return;
        }
        
        // Verify error handling setup (Task 14.1 requirement)
        assertTrue(mainSource.contains("catch (OutOfMemoryError"),
                  "Should catch OutOfMemoryError");
        assertTrue(mainSource.contains("catch (IOException"),
                  "Should catch IOException during graph loading");
        assertTrue(mainSource.contains("System.exit(1)"),
                  "Should exit with error code on fatal errors");
    }
    
    /**
     * Test that Spanish error messages are present for all error scenarios.
     * Verifies Task 14.1 requirement: proper error messages in Spanish.
     */
    @Test
    void testSpanishErrorMessages() {
        String mainSource;
        try {
            Path mainFile = Path.of("src/main/java/co/medellin/transit/Main.java");
            mainSource = Files.readString(mainFile);
        } catch (Exception e) {
            fail("Could not read Main.java source file: " + e.getMessage());
            return;
        }
        
        // Verify Spanish error messages for all scenarios
        assertTrue(mainSource.contains("No se pudieron cargar los datos de transporte"),
                  "Should have Spanish message for data loading failure");
        assertTrue(mainSource.contains("Memoria insuficiente"),
                  "Should have Spanish message for out of memory error");
        assertTrue(mainSource.contains("-Xmx2G"),
                  "Should provide memory increase instructions");
        assertTrue(mainSource.contains("Verifique que el directorio"),
                  "Should provide instructions about data directory");
    }
    
    /**
     * Test that startup metrics are logged.
     * Verifies Task 14.1 optional requirement: log startup time and memory usage.
     */
    @Test
    void testStartupMetricsAreLogged() {
        String mainSource;
        try {
            Path mainFile = Path.of("src/main/java/co/medellin/transit/Main.java");
            mainSource = Files.readString(mainFile);
        } catch (Exception e) {
            fail("Could not read Main.java source file: " + e.getMessage());
            return;
        }
        
        // Verify startup metrics tracking
        assertTrue(mainSource.contains("System.nanoTime()"),
                  "Should track startup time");
        assertTrue(mainSource.contains("Runtime.getRuntime()"),
                  "Should access Runtime for memory tracking");
        assertTrue(mainSource.contains("Initialization complete"),
                  "Should log initialization completion");
    }
    
    /**
     * Test that data directory path is configurable.
     * Verifies Task 14.1 requirement: load graph from data directory.
     */
    @Test
    void testDataDirectoryConfiguration() {
        String mainSource;
        try {
            Path mainFile = Path.of("src/main/java/co/medellin/transit/Main.java");
            mainSource = Files.readString(mainFile);
        } catch (Exception e) {
            fail("Could not read Main.java source file: " + e.getMessage());
            return;
        }
        
        // Verify data directory is used (either DATA_DIR or DEFAULT_DATA_DIR)
        assertTrue(mainSource.contains("DATA_DIR") || mainSource.contains("DEFAULT_DATA_DIR"),
                  "Should define data directory constant");
        assertTrue(mainSource.contains("Paths.get(") && mainSource.contains("dataDir"),
                  "Should use data directory path for graph loading");
    }
}
