package co.medellin.transit.routing;

import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.ui.gui.model.RouteCatalog;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Formats vehicle identifiers into user-friendly labels for trip instructions.
 * 
 * <p>Converts internal route IDs into Spanish labels that riders recognize:
 * <ul>
 *   <li>Buses: "bus {codigo} ({nombre})" (e.g., "bus 191 (Laureles Éxito)")</li>
 *   <li>Metro: "Metro Línea {line}" (e.g., "Metro Línea A")</li>
 *   <li>Metroplús: "Metroplús Línea {line}" (e.g., "Metroplús Línea 2")</li>
 *   <li>Metrocable: "Metrocable Línea {line}" (e.g., "Metrocable Línea J")</li>
 *   <li>Tranvía: "Tranvía de Ayacucho"</li>
 * </ul>
 * 
 * <p>Based on the prototype's LINE_LABEL and vehicle() function in reference/prototype.py.
 */
public class VehicleLabelFormatter {
    
    private static final Logger LOGGER = Logger.getLogger(VehicleLabelFormatter.class.getName());
    
    private final TransitGraph graph;
    private final RouteCatalog routeCatalog;
    
    /**
     * Creates a new formatter with access to graph and route metadata.
     * 
     * @param graph the transit graph containing route type information
     * @param dataDir the data directory containing routes.json
     */
    public VehicleLabelFormatter(TransitGraph graph, Path dataDir) {
        if (graph == null) {
            throw new NullPointerException("graph cannot be null");
        }
        if (dataDir == null) {
            throw new NullPointerException("dataDir cannot be null");
        }
        this.graph = graph;
        this.routeCatalog = new RouteCatalog(dataDir);
    }
    
    /**
     * Formats a vehicle identifier for display to users.
     * 
     * <p>Transit lines can be:
     * - Single letters ("A", "B", "J", "K", etc.)
     * - Numbers ("1", "2", etc.)
     * - With "L" prefix ("LA", "L2", "LJ") - legacy format
     * Bus routes are numeric identifiers (e.g., "90283").
     * 
     * @param routeId the internal route identifier
     * @return formatted label, or the routeId itself if formatting fails
     */
    public String formatVehicle(String routeId) {
        if (routeId == null || routeId.isEmpty()) {
            return routeId;
        }
        
        // Transit lines: single letter or digit, or starts with "L"
        if (isTransitLine(routeId)) {
            String line = routeId;
            
            // Remove "L" prefix if present (legacy format)
            if (line.startsWith("L") && line.length() > 1) {
                line = line.substring(1);
            }
            
            // Determine mode from graph
            String mode = determineTransitMode(routeId);
            
            return formatTransitLine(line, mode);
        }
        
        // Bus routes: look up in catalog
        return formatBusRoute(routeId);
    }
    
    /**
     * Checks if a route ID represents a transit line (metro, BRT, cable car, tram).
     * 
     * @param routeId the route identifier
     * @return true if it's a transit line
     */
    private boolean isTransitLine(String routeId) {
        // Single letter (A, B, J, K, etc.) or single digit (1, 2)
        if (routeId.length() == 1) {
            char c = routeId.charAt(0);
            if (Character.isLetter(c) || Character.isDigit(c)) {
                // Check if the graph has line mode for this
                String lineMode = graph.getLineMode(routeId);
                return lineMode != null;
            }
        }
        
        // "L" followed by letter or digit (LA, L2, LJ)
        if (routeId.startsWith("L") && routeId.length() == 2) {
            char c = routeId.charAt(1);
            if (Character.isLetter(c) || Character.isDigit(c)) {
                // Check if the graph has line mode (with or without L prefix)
                String lineMode = graph.getLineMode(routeId);
                if (lineMode == null) {
                    lineMode = graph.getLineMode(routeId.substring(1));
                }
                return lineMode != null;
            }
        }
        
        // Check if the graph has line mode info for this (it's marked as transit)
        String lineMode = graph.getLineMode(routeId);
        if (lineMode != null) {
            return true; // If it has a line mode, it's a transit line
        }
        
        return false;
    }
    
    /**
     * Formats a bus route using the route catalog.
     * 
     * @param routeId the bus route ID
     * @return formatted "bus {codigo} ({nombre})" or routeId if not found
     */
    private String formatBusRoute(String routeId) {
        Optional<RouteCatalog.RouteLabel> label = routeCatalog.find(routeId);
        
        if (label.isPresent()) {
            RouteCatalog.RouteLabel route = label.get();
            return String.format("bus %s (%s)", route.code(), route.name());
        }
        
        // Fallback: return the route ID itself
        LOGGER.fine("Route not found in catalog: " + routeId);
        return routeId;
    }
    
    /**
     * Formats a transit line name based on mode.
     * 
     * @param line the line identifier (e.g., "A", "2", "J")
     * @param mode the transit mode (metro, brt, cable_car, tram)
     * @return formatted line name in Spanish
     */
    private String formatTransitLine(String line, String mode) {
        switch (mode) {
            case "metro":
                return String.format("Metro Línea %s", line);
            case "brt":
                return String.format("Metroplús Línea %s", line);
            case "cable_car":
                return String.format("Metrocable Línea %s", line);
            case "tram":
                return "Tranvía de Ayacucho";
            default:
                // Unknown mode: return generic line label
                LOGGER.warning("Unknown transit mode: " + mode + " for line " + line);
                return "Línea " + line;
        }
    }
    
    /**
     * Determines the transit mode from a line identifier by checking the graph.
     * 
     * <p>Transit lines in the graph have a mode stored. This method queries
     * the line mode to determine its type.
     * 
     * @param routeId the line identifier (e.g., "A", "2", "LA", "L2")
     * @return mode string (metro, brt, cable_car, tram), or "unknown"
     */
    private String determineTransitMode(String routeId) {
        // Remove "L" prefix if present
        String line = routeId;
        if (line.startsWith("L") && line.length() > 1) {
            line = line.substring(1);
        }
        
        // Check both the original routeId and the stripped line
        String mode = graph.getLineMode(routeId);
        if (mode == null) {
            mode = graph.getLineMode(line);
        }
        
        if (mode != null) {
            // Normalize mode to standard names
            String modeLower = mode.toLowerCase();
            switch (modeLower) {
                case "metro":
                    return "metro";
                case "brt":
                case "metroplús":
                case "metroplus":
                    return "brt";
                case "cable_car":
                case "cable":
                case "metrocable":
                    return "cable_car";
                case "tram":
                case "tranvía":
                case "tranvia":
                    return "tram";
                default:
                    LOGGER.fine("Unrecognized line mode: " + mode);
                    return "unknown";
            }
        }
        
        // Fallback: try to infer from line letter
        if (line.matches("[AB]")) {
            return "metro"; // Lines A and B are Metro
        } else if (line.matches("[12]")) {
            return "brt"; // Lines 1 and 2 are Metroplús
        } else if (line.matches("[HJKLMPQ]")) {
            return "cable_car"; // Cable car lines
        } else if (line.equals("T") || line.equalsIgnoreCase("Ayacucho")) {
            return "tram";
        }
        
        LOGGER.warning("Could not determine mode for line: " + routeId);
        return "unknown";
    }
}
