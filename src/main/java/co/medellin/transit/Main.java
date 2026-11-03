package co.medellin.transit;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareConfig;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.routing.Parameters;
import co.medellin.transit.routing.Router;
import co.medellin.transit.routing.StepBuilder;
import co.medellin.transit.ui.HomeScreen;
import co.medellin.transit.ui.Menu;
import co.medellin.transit.ui.TripPlanner;
import co.medellin.transit.ui.TripPrinter;
import co.medellin.transit.ui.gui.MainWindow;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the Medellín Transit Routing System.
 * 
 * <p>This application provides offline multi-modal transit routing for Medellín's
 * public transportation network, including buses and metro. It loads pre-computed
 * graph data from CSV files and uses Dijkstra's algorithm to find optimal routes.</p>
 * 
 * <p>Initialization sequence:</p>
 * <ol>
 *   <li>Configure logging and validate data directory</li>
 *   <li>Load transit graph from CSV files</li>
 *   <li>Initialize routing parameters from config.properties</li>
 *   <li>Create Router, FareCalculator, AddressParser components</li>
 *   <li>Display welcome screen and start interaction loop</li>
 * </ol>
 *
 * <p>By default the app opens a graphical window (Swing) with the home screen
 * as soon as it starts, and loads the graph in the meantime. With the
 * {@code --consola} flag, or on a machine without a display, it keeps the
 * text menu in the console.</p>
 * 
 * <p>Error handling (comprehensive as per Task 15.1):</p>
 * <ul>
 *   <li>CSV loading: Wrapped with try-catch, logs warnings for invalid rows (see CsvReader)</li>
 *   <li>User inputs: Validated before processing (see AddressParser, Menu)</li>
 *   <li>Recoverable errors: Return Optional.empty() (invalid address, no route)</li>
 *   <li>Fatal errors: File not found, OutOfMemoryError - exit with clear Spanish messages</li>
 *   <li>Logging: Uses java.util.logging for INFO/WARN/ERROR levels</li>
 * </ul>
 * 
 * <p>All user-facing text is in Spanish, while code and comments are in English.</p>
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String DEFAULT_DATA_DIR = "data";
    private static final String CONSOLE_FLAG = "--consola";

    /** The open window in graphical mode, or null in console mode. */
    private static MainWindow window;
    
    public static void main(String[] args) {
        try {
            // Configure logging level (can be overridden with system property)
            configureLogging();

            // Split flags from positional arguments (the data directory)
            boolean consoleMode = GraphicsEnvironment.isHeadless();
            List<String> positional = new ArrayList<>();
            for (String arg : args) {
                if (arg.equalsIgnoreCase(CONSOLE_FLAG)) {
                    consoleMode = true;
                } else {
                    positional.add(arg);
                }
            }
            args = positional.toArray(new String[0]);
            
            // Track startup time and memory for performance monitoring
            long startTime = System.nanoTime();
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            LOGGER.info("Initializing Medellín Transit Routing System...");
            
            // Determine data directory (from args, config.properties, or default)
            String dataDirPath;
            if (args.length > 0) {
                // Command-line argument takes precedence
                dataDirPath = args[0];
                LOGGER.info("Using data directory from command-line: " + dataDirPath);
            } else {
                // Try to load from config.properties
                dataDirPath = loadDataDirectoryFromConfig();
            }
            Path dataDir = Paths.get(dataDirPath);

            // In graphical mode, show the home screen right away while the graph loads
            if (!consoleMode) {
                window = MainWindow.open(dataDir);
            }
            
            // Validate data directory exists before attempting to load
            if (!Files.exists(dataDir)) {
                System.err.println("═══════════════════════════════════════════════════════════");
                System.err.println("Error fatal: No se encontró el directorio de datos");
                System.err.println("═══════════════════════════════════════════════════════════");
                System.err.println();
                System.err.println("Directorio esperado: " + dataDir.toAbsolutePath());
                System.err.println();
                System.err.println("El directorio debe contener los siguientes archivos:");
                System.err.println("  - bus_segments.csv");
                System.err.println("  - transit_lines.csv");
                System.err.println("  - walk_links.csv");
                System.err.println("  - stops_*.json (archivos paginados)");
                System.err.println("  - routes.json");
                System.err.println();
                System.err.println("Solución:");
                System.err.println("  1. Verifique que el directorio 'data' existe");
                System.err.println("  2. O especifique una ruta diferente: java -jar transit.jar /ruta/a/datos");
                System.err.println("═══════════════════════════════════════════════════════════");
                LOGGER.severe("Data directory not found: " + dataDir.toAbsolutePath());
                showFatal("No se encontró el directorio de datos",
                        "No se encontró la carpeta de datos:\n" + dataDir.toAbsolutePath()
                        + "\n\nVerifique que el directorio 'data' existe y ejecute la aplicación"
                        + "\ndesde la carpeta del proyecto (MetroBus).");
                System.exit(1);
            }
            
            if (!Files.isDirectory(dataDir)) {
                System.err.println("Error fatal: La ruta especificada no es un directorio.");
                System.err.println("Ruta: " + dataDir.toAbsolutePath());
                LOGGER.severe("Data path is not a directory: " + dataDir.toAbsolutePath());
                showFatal("La ruta de datos no es un directorio",
                        "La ruta especificada no es un directorio:\n" + dataDir.toAbsolutePath());
                System.exit(1);
            }
            
            // Phase 1: Load transit graph from CSV files
            LOGGER.info("Loading transit graph from data directory: " + dataDir.toAbsolutePath());
            
            TransitGraph graph;
            try {
                GraphBuilder graphBuilder = new GraphBuilder();
                graph = graphBuilder.build(dataDir);
                
                // Validate graph is not empty
                if (graph.getStateCount() == 0 || graph.getEdgeCount() == 0) {
                    System.err.println("═══════════════════════════════════════════════════════════");
                    System.err.println("Error fatal: El grafo de transporte está vacío");
                    System.err.println("═══════════════════════════════════════════════════════════");
                    System.err.println();
                    System.err.println("Los archivos CSV fueron cargados pero no contienen datos válidos.");
                    System.err.println();
                    System.err.println("Posibles causas:");
                    System.err.println("  - Archivos CSV vacíos o solo con encabezados");
                    System.err.println("  - Formato incorrecto de los archivos");
                    System.err.println("  - Datos corruptos o incompletos");
                    System.err.println("═══════════════════════════════════════════════════════════");
                    LOGGER.severe("Transit graph is empty after loading");
                    showFatal("El grafo de transporte está vacío",
                            "Los archivos de datos se cargaron pero no contienen datos válidos.");
                    System.exit(1);
                }
                
                LOGGER.log(Level.INFO, "Graph loaded successfully: {0} states, {1} edges", 
                          new Object[]{graph.getStateCount(), graph.getEdgeCount()});
                
            } catch (IOException e) {
                // Fatal error: cannot proceed without graph data
                System.err.println();
                System.err.println("═══════════════════════════════════════════════════════════");
                System.err.println("Error fatal: No se pudieron cargar los datos de transporte");
                System.err.println("═══════════════════════════════════════════════════════════");
                System.err.println();
                System.err.println("Detalle del error: " + e.getMessage());
                System.err.println();
                System.err.println("Posibles causas:");
                System.err.println("  - Archivos CSV faltantes o con formato incorrecto");
                System.err.println("  - Permisos insuficientes para leer los archivos");
                System.err.println("  - Archivos corruptos o datos inválidos");
                System.err.println("  - Codificación incorrecta (debe ser UTF-8)");
                System.err.println();
                System.err.println("Archivos requeridos:");
                System.err.println("  " + dataDir.toAbsolutePath() + "/bus_segments.csv");
                System.err.println("  " + dataDir.toAbsolutePath() + "/transit_lines.csv");
                System.err.println("  " + dataDir.toAbsolutePath() + "/walk_links.csv");
                System.err.println("═══════════════════════════════════════════════════════════");
                LOGGER.log(Level.SEVERE, "Failed to load transit graph", e);
                showFatal("No se pudieron cargar los datos de transporte",
                        "No se pudieron cargar los datos de transporte.\n\nDetalle: " + e.getMessage());
                System.exit(1);
                return; // Unreachable, but satisfies compiler
            }
            
            // Phase 2: Load configuration and initialize parameters
            LOGGER.info("Loading routing parameters...");
            Parameters params = new Parameters();
            
            LOGGER.info("Loading fare configuration...");
            FareConfig fareConfig;
            try {
                fareConfig = FareConfig.fromProperties();
                LOGGER.log(Level.INFO, "Fare config loaded: {0}", fareConfig);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not load fare config from properties, using defaults", e);
                fareConfig = FareConfig.defaults();
            }
            
            // Phase 3: Initialize all components
            LOGGER.info("Initializing components...");
            Router router = new Router(graph, params);
            FareCalculator fareCalculator = new FareCalculator(fareConfig, graph);
            AddressParser addressParser = new AddressParser();
            // AddressLocator reads its reference stops from this property; point it at our data
            if (System.getProperty("transit.data.dir") == null) {
                System.setProperty("transit.data.dir", dataDir.toString());
            }
            AddressLocator addressLocator = new AddressLocator();
            StepBuilder stepBuilder = new StepBuilder(graph);
            TripPrinter tripPrinter = new TripPrinter();
            
            // Calculate and display startup metrics
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            long usedMemoryMB = (currentMemory - initialMemory) / (1024 * 1024);
            
            LOGGER.log(Level.INFO, "Initialization complete in {0}ms, memory usage: {1}MB", 
                      new Object[]{elapsedMs, usedMemoryMB});
            
            // Warn if loading time exceeded target
            if (elapsedMs > 1000) {
                LOGGER.warning("WARNING: Graph loading time exceeded 1 second target");
            }
            
            // Phase 4a: Graphical mode - the window is already open; enable the planner
            if (window != null) {
                TripPlanner tripPlanner = new TripPlanner(router, addressParser, addressLocator,
                        fareCalculator, stepBuilder);
                window.showReady(tripPlanner, String.format(
                        "Red cargada: %,d nodos y %,d conexiones.", graph.getStateCount(), graph.getEdgeCount())
                        .replace(',', '.'));
                LOGGER.info("Graphical interface ready");
                return;
            }

            // Phase 4b: Console mode - display welcome screen and start menu loop
            HomeScreen homeScreen = new HomeScreen();
            homeScreen.display();
            
            Scanner scanner = new Scanner(System.in);
            Menu menu = new Menu(scanner, router, addressParser, addressLocator, fareCalculator, 
                               stepBuilder, tripPrinter);
            menu.run();
            
            scanner.close();
            LOGGER.info("Application terminated normally");
            
        } catch (OutOfMemoryError e) {
            // Handle out of memory error with helpful instructions
            System.err.println();
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("Error fatal: Memoria insuficiente");
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println();
            System.err.println("El sistema no tiene suficiente memoria para cargar el grafo");
            System.err.println("de transporte completo (13,895 estados, 82,160 aristas).");
            System.err.println();
            System.err.println("Solución: Aumente la memoria heap de Java con el parámetro -Xmx");
            System.err.println();
            System.err.println("Ejemplos:");
            System.err.println("  java -Xmx2G -jar transit-routing.jar");
            System.err.println("  java -Xmx1G -jar transit-routing.jar  (para sistemas limitados)");
            System.err.println();
            System.err.println("Memoria actual:");
            Runtime runtime = Runtime.getRuntime();
            System.err.println("  Max heap: " + (runtime.maxMemory() / (1024 * 1024)) + " MB");
            System.err.println("  Memoria recomendada: 2048 MB (-Xmx2G)");
            System.err.println("═══════════════════════════════════════════════════════════");
            LOGGER.log(Level.SEVERE, "Out of memory during initialization", e);
            showFatal("Memoria insuficiente",
                    "No hay memoria suficiente para cargar la red de transporte.\n"
                    + "Ejecute de nuevo con más memoria: java -Xmx2G -jar transit-routing-1.0.0.jar");
            System.exit(1);
            
        } catch (Exception e) {
            // Catch any unexpected errors
            System.err.println();
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println("Error inesperado durante la inicialización");
            System.err.println("═══════════════════════════════════════════════════════════");
            System.err.println();
            System.err.println("Tipo de error: " + e.getClass().getSimpleName());
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println();
            System.err.println("Por favor, reporte este error con el siguiente detalle técnico:");
            e.printStackTrace();
            System.err.println("═══════════════════════════════════════════════════════════");
            LOGGER.log(Level.SEVERE, "Unexpected error during initialization", e);
            showFatal("Error inesperado durante la inicialización",
                    "Error inesperado: " + e.getClass().getSimpleName() + "\n" + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * In graphical mode, shows a fatal error in a dialog before the app exits.
     * In console mode the message was already printed, so it does nothing.
     */
    private static void showFatal(String title, String detail) {
        if (window != null) {
            try {
                window.showFatalError(title, detail);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Could not show the error dialog", e);
            }
        }
    }

    /**
     * Configures logging for the application.
     * Default level is INFO, but can be overridden with system property: -Dlog.level=FINE
     * 
     * Valid levels: SEVERE, WARNING, INFO, FINE, FINER, FINEST
     */
    private static void configureLogging() {
        // Get root logger
        Logger rootLogger = Logger.getLogger("");
        
        // Set default level to INFO (can be overridden by system property)
        String logLevel = System.getProperty("log.level", "INFO");
        try {
            Level level = Level.parse(logLevel);
            rootLogger.setLevel(level);
            
            // Set level for all handlers
            for (var handler : rootLogger.getHandlers()) {
                handler.setLevel(level);
            }
            
            LOGGER.log(Level.FINE, "Logging configured at level: {0}", level);
        } catch (IllegalArgumentException e) {
            System.err.println("Advertencia: Nivel de log inválido '" + logLevel + "', usando INFO");
            rootLogger.setLevel(Level.INFO);
        }
    }
    
    /**
     * Loads the data directory path from config.properties.
     * Falls back to DEFAULT_DATA_DIR if the property is not found or an error occurs.
     * 
     * @return the data directory path from config or default
     */
    private static String loadDataDirectoryFromConfig() {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                LOGGER.info("config.properties not found, using default data directory: " + DEFAULT_DATA_DIR);
                return DEFAULT_DATA_DIR;
            }
            
            Properties props = new Properties();
            props.load(input);
            
            String dataDir = props.getProperty("data.directory");
            if (dataDir != null && !dataDir.isEmpty()) {
                LOGGER.info("Using data directory from config.properties: " + dataDir);
                return dataDir;
            } else {
                LOGGER.info("data.directory property not found in config.properties, using default: " + DEFAULT_DATA_DIR);
                return DEFAULT_DATA_DIR;
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading config.properties, using default data directory: " + DEFAULT_DATA_DIR, e);
            return DEFAULT_DATA_DIR;
        }
    }
}
