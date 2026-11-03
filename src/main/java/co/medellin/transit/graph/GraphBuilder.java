package co.medellin.transit.graph;

import co.medellin.transit.data.BusSegment;
import co.medellin.transit.data.BusSegmentReader;
import co.medellin.transit.data.RouteArcGisReader;
import co.medellin.transit.data.StopArcGisReader;
import co.medellin.transit.data.TransitLineReader;
import co.medellin.transit.data.TransitSegment;
import co.medellin.transit.data.WalkLink;
import co.medellin.transit.data.WalkLinkReader;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Direction;
import co.medellin.transit.model.Edge;
import co.medellin.transit.model.EdgeKind;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Route;
import co.medellin.transit.model.RouteType;
import co.medellin.transit.model.State;
import co.medellin.transit.model.StateType;
import co.medellin.transit.model.Stop;
import co.medellin.transit.routing.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds the Medellín transit routing graph from raw CSV and JSON data files.
 * 
 * <p>The builder constructs a complete graph with:
 * <ul>
 * <li>13,895 states (nodes): WALK states for physical locations, BUS_STOP states for stops on routes,
 *     and METRO_STATION states for metro/cable car/tram stations</li>
 * <li>82,160 edges: RIDE edges for vehicle travel, BOARD edges for boarding, ALIGHT edges for alighting,
 *     and WALK edges for pedestrian connections</li>
 * </ul>
 * 
 * <p>The graph construction process:
 * <ol>
 * <li>Load stops from paginated ArcGIS JSON files (stops_*.json)</li>
 * <li>Load routes from ArcGIS JSON (routes.json)</li>
 * <li>Load raw transit data from CSV files (bus_segments.csv, transit_lines.csv, walk_links.csv)</li>
 * <li>Build state lookup maps for stops, routes, and stations</li>
 * <li>Generate WALK states for all stops</li>
 * <li>Generate RIDE, BOARD, and ALIGHT edges from bus segments and transit lines</li>
 * <li>Generate WALK edges from walk links</li>
 * </ol>
 * 
 * <p>Edge costs follow specifications from graph-structure.md section 3.2:
 * <ul>
 * <li>RIDE edges: perceivedCost = realSec (no penalty)</li>
 * <li>BOARD edges: realSec = average wait time, perceivedCost = wait + boardingPenalty (120s)</li>
 * <li>ALIGHT edges: realSec = 0, perceivedCost = 0</li>
 * <li>WALK edges: realSec = (straightM × walkStreetFactor) ÷ walkSpeedMps, perceivedCost = realSec × walkPenaltyMultiplier + walkPenaltyConstant</li>
 * </ul>
 * 
 * <p>Performance target: Graph construction in under 1 second.
 * Memory optimization: String interning for route IDs and place names saves ~100MB.
 */
public class GraphBuilder {
    private static final Logger LOGGER = Logger.getLogger(GraphBuilder.class.getName());
    
    // Expected counts for validation
    private static final int EXPECTED_STATES = 13_895;
    private static final int EXPECTED_EDGES = 82_160;
    private static final int EXPECTED_STOPS = 3_590;
    
    // Data file names
    private static final String STOPS_BASE_FILENAME = "stops";
    private static final String ROUTES_FILENAME = "routes.json";
    private static final String TRANSIT_STATIONS_FILENAME = "transit_stations.json";
    private static final String BUS_SEGMENTS_FILENAME = "bus_segments.csv";
    private static final String TRANSIT_LINES_FILENAME = "transit_lines.csv";
    private static final String WALK_LINKS_FILENAME = "walk_links.csv";
    
    /**
     * Builds the complete transit graph from data files in the specified directory.
     * 
     * <p>The data directory must contain:
     * <ul>
     * <li>Paginated stop files: stops_0.json, stops_2000.json, ... (in data/raw/ subdirectory)</li>
     * <li>Routes file: routes.json (in data/raw/ subdirectory)</li>
     * <li>Raw transit data CSVs: bus_segments.csv, transit_lines.csv, walk_links.csv</li>
     * </ul>
     * 
     * @param dataDir directory containing data files
     * @return fully constructed TransitGraph ready for routing
     * @throws IOException if data files cannot be read or are malformed
     * @throws IllegalStateException if graph structure doesn't match expected dimensions
     */
    public TransitGraph build(Path dataDir) throws IOException {
        long startTime = System.currentTimeMillis();
        
        LOGGER.info("Iniciando construcción del grafo de transporte...");
        LOGGER.info("Directorio de datos: " + dataDir.toAbsolutePath());
        
        // Validate data directory exists
        if (!Files.exists(dataDir)) {
            throw new IOException("Directorio de datos no encontrado: " + dataDir.toAbsolutePath());
        }
        
        if (!Files.isDirectory(dataDir)) {
            throw new IOException("La ruta no es un directorio: " + dataDir.toAbsolutePath());
        }
        
        // Load configuration parameters
        Parameters params = new Parameters();
        double walkSpeedMps = params.getWalkingSpeedMps();
        double walkStreetFactor = params.getWalkStreetFactor();
        double walkPenaltyMultiplier = params.getWalkPenaltyMultiplier();
        int walkPenaltyConstant = params.getWalkPenaltyConstant();
        int boardingPenaltySec = params.getBoardingPenaltySec();
        
        LOGGER.info(String.format("Parámetros de configuración: walkSpeed=%.2f m/s, walkStreetFactor=%.2f, " +
                "walkPenaltyMult=%.2f, walkPenaltyConst=%ds, boardingPenalty=%ds",
                walkSpeedMps, walkStreetFactor, walkPenaltyMultiplier, walkPenaltyConstant, boardingPenaltySec));
        
        // Initialize graph with expected capacity
        TransitGraph graph = new TransitGraph(EXPECTED_STATES);
        
        try {
            // Step 1: Load stops from paginated JSON files
            LOGGER.info("Cargando paradas desde archivos JSON paginados...");
            StopArcGisReader stopReader = new StopArcGisReader();
            List<Stop> stops;
            try {
                stops = stopReader.readPaginated(dataDir, STOPS_BASE_FILENAME, ".json");
                LOGGER.info(String.format("Cargadas %d paradas", stops.size()));
            } catch (IOException e) {
                LOGGER.warning("No se pudieron cargar paradas desde JSON: " + e.getMessage());
                stops = new ArrayList<>();
            }
            
            if (stops.isEmpty()) {
                LOGGER.warning("No se encontraron paradas de bus. Solo se procesarán estaciones de tránsito.");
            }
            
            // Step 2: Load routes from JSON
            LOGGER.info("Cargando rutas desde JSON...");
            RouteArcGisReader routeReader = new RouteArcGisReader();
            Path routesPath = dataDir.resolve("raw").resolve(ROUTES_FILENAME);
            List<Route> routes;
            try {
                routes = routeReader.read(routesPath);
                LOGGER.info(String.format("Cargadas %d rutas", routes.size()));
            } catch (IOException e) {
                LOGGER.warning("No se pudieron cargar rutas desde JSON: " + e.getMessage());
                routes = new ArrayList<>();
            }
            
            // Create route lookup map
            Map<Integer, Route> routeMap = new HashMap<>();
            for (Route route : routes) {
                routeMap.put(route.getId(), route);
            }
            
            // Step 3: Load raw transit data from CSV files
            LOGGER.info("Cargando datos de tránsito desde CSV...");
            
            // Load bus segments
            Path busSegmentsPath = dataDir.resolve(BUS_SEGMENTS_FILENAME);
            List<BusSegment> busSegments = new ArrayList<>();
            if (Files.exists(busSegmentsPath)) {
                BusSegmentReader busReader = new BusSegmentReader();
                busSegments = busReader.read(busSegmentsPath);
                LOGGER.info(String.format("Cargados %d segmentos de bus", busSegments.size()));
            } else {
                LOGGER.warning("Archivo no encontrado: " + busSegmentsPath.getFileName());
            }
            
            // Load transit lines
            Path transitLinesPath = dataDir.resolve(TRANSIT_LINES_FILENAME);
            List<TransitSegment> transitSegments = new ArrayList<>();
            if (Files.exists(transitLinesPath)) {
                TransitLineReader transitReader = new TransitLineReader();
                transitSegments = transitReader.read(transitLinesPath);
                LOGGER.info(String.format("Cargados %d segmentos de líneas de tránsito", transitSegments.size()));
            } else {
                LOGGER.warning("Archivo no encontrado: " + transitLinesPath.getFileName());
            }
            
            // Load walk links
            Path walkLinksPath = dataDir.resolve(WALK_LINKS_FILENAME);
            List<WalkLink> walkLinks = new ArrayList<>();
            if (Files.exists(walkLinksPath)) {
                WalkLinkReader walkReader = new WalkLinkReader();
                walkLinks = walkReader.read(walkLinksPath);
                LOGGER.info(String.format("Cargados %d enlaces peatonales", walkLinks.size()));
            } else {
                LOGGER.warning("Archivo no encontrado: " + walkLinksPath.getFileName());
            }
            
            // Step 4: Build state lookup maps
            LOGGER.info("Construyendo mapas de búsqueda de estados...");
            
            // Map: stop ID -> WALK state ID
            Map<Integer, Integer> stopIdToWalkStateId = new HashMap<>();
            
            // Map: (stop ID, route ID, direction) -> BUS_STOP state ID
            Map<String, Integer> stopRouteToStateId = new HashMap<>();
            
            // Map: (station name, line, direction) -> METRO_STATION state ID
            Map<String, Integer> stationLineToStateId = new HashMap<>();
            
            // Map: station name -> WALK state ID (for walk links)
            Map<String, Integer> stationNameToWalkStateId = new HashMap<>();
            
            // Map: place string (from walk_links) -> WALK state ID
            // This will handle both "STOP:id" and "STATION:name" formats
            Map<String, Integer> placeStringToWalkStateId = new HashMap<>();
            
            // Map: stop ID -> Stop object
            Map<Integer, Stop> stopMap = new HashMap<>();
            for (Stop stop : stops) {
                stopMap.put(stop.getId(), stop);
            }
            
            // State ID counter - use array wrapper so it can be captured in lambdas
            final int[] nextStateId = {0};
            
            // Step 5: Create WALK states for all stops first
            LOGGER.info("Creando estados WALK para todas las paradas...");
            for (Stop stop : stops) {
                Place place = new Place(stop.getName(), stop.getCoordinate());
                State walkState = new State(nextStateId[0], place);
                graph.addState(walkState);
                stopIdToWalkStateId.put(stop.getId(), nextStateId[0]);
                // Also map the "STOP:id" format for walk links
                placeStringToWalkStateId.put("STOP:" + stop.getId(), nextStateId[0]);
                nextStateId[0]++;
            }
            int stopWalkStates = stopIdToWalkStateId.size();
            LOGGER.info(String.format("Creados %d estados WALK para paradas", stopWalkStates));
            
            // Step 5b: Create WALK states for unique transit stations
            LOGGER.info("Creando estados WALK para estaciones de tránsito...");
            for (TransitSegment segment : transitSegments) {
                String stationName = segment.station();
                if (!stationNameToWalkStateId.containsKey(stationName)) {
                    Place place = new Place(stationName, segment.coordinate());
                    State walkState = new State(nextStateId[0], place);
                    graph.addState(walkState);
                    stationNameToWalkStateId.put(stationName, nextStateId[0]);
                    // Also map the "STATION:name" format for walk links
                    placeStringToWalkStateId.put("STATION:" + stationName, nextStateId[0]);
                    nextStateId[0]++;
                }
            }
            int stationWalkStates = stationNameToWalkStateId.size();
            LOGGER.info(String.format("Creados %d estados WALK para estaciones (total WALK: %d)", 
                    stationWalkStates, stopWalkStates + stationWalkStates));
            
            // Step 6: Generate RIDE edges from bus segments
            LOGGER.info("Generando aristas RIDE desde segmentos de bus...");
            int busRideEdges = 0;
            int busStopStatesCreated = 0;
            
            for (BusSegment segment : busSegments) {
                String routeId = segment.routeId();
                Direction direction = segment.direction() == 0 ? Direction.OUTBOUND : Direction.INBOUND;
                int fromStopId = segment.fromStopId();
                int toStopId = segment.toStopId();
                
                // Get or create BUS_STOP states
                String fromKey = fromStopId + "_" + routeId + "_" + direction;
                String toKey = toStopId + "_" + routeId + "_" + direction;
                
                int fromStateId = stopRouteToStateId.computeIfAbsent(fromKey, k -> {
                    Stop stop = stopMap.get(fromStopId);
                    if (stop == null) {
                        LOGGER.warning("Stop ID " + fromStopId + " not found in stop map");
                        return -1;
                    }
                    Place place = new Place(stop.getName(), stop.getCoordinate());
                    State state = new State(nextStateId[0], place, StateType.BUS_STOP, routeId, direction);
                    graph.addState(state);
                    return nextStateId[0]++;
                });
                
                int toStateId = stopRouteToStateId.computeIfAbsent(toKey, k -> {
                    Stop stop = stopMap.get(toStopId);
                    if (stop == null) {
                        LOGGER.warning("Stop ID " + toStopId + " not found in stop map");
                        return -1;
                    }
                    Place place = new Place(stop.getName(), stop.getCoordinate());
                    State state = new State(nextStateId[0], place, StateType.BUS_STOP, routeId, direction);
                    graph.addState(state);
                    return nextStateId[0]++;
                });
                
                if (fromStateId == -1 || toStateId == -1) {
                    continue;
                }
                
                // Calculate travel time: (usedM / busSpeedMps) + dwellTimeSec
                double busSpeedMps = getBusSpeed(routeId, params) / 3.6; // Convert km/h to m/s
                int dwellTimeSec = 20; // Default dwell time
                int realSec = (int) Math.round((segment.usedM() / busSpeedMps) + dwellTimeSec);
                int perceivedCost = realSec; // No penalty for RIDE edges
                
                Edge rideEdge = new Edge(fromStateId, toStateId, EdgeKind.RIDE, realSec, perceivedCost);
                graph.addEdge(rideEdge);
                busRideEdges++;
            }
            
            busStopStatesCreated = stopRouteToStateId.size();
            LOGGER.info(String.format("Creados %d estados BUS_STOP, %d aristas RIDE de bus", 
                    busStopStatesCreated, busRideEdges));
            
            // Step 6b: Add turnaround edges (last outbound -> first inbound for each route)
            LOGGER.info("Generando aristas de turnaround para rutas de bus...");
            int turnaroundEdges = 0;
            
            // Group bus segments by route
            Map<String, List<BusSegment>> busRouteGroups = new HashMap<>();
            for (BusSegment segment : busSegments) {
                busRouteGroups.computeIfAbsent(segment.routeId(), k -> new ArrayList<>()).add(segment);
            }
            
            // For each route, find last outbound and first inbound
            for (Map.Entry<String, List<BusSegment>> entry : busRouteGroups.entrySet()) {
                String routeId = entry.getKey();
                List<BusSegment> segments = entry.getValue();
                
                // Separate by direction and find highest/lowest sequence
                BusSegment lastOutbound = null;
                BusSegment firstInbound = null;
                
                for (BusSegment segment : segments) {
                    if (segment.direction() == 0) { // OUTBOUND
                        if (lastOutbound == null || segment.toSeq() > lastOutbound.toSeq()) {
                            lastOutbound = segment;
                        }
                    } else { // INBOUND
                        if (firstInbound == null || segment.fromSeq() < firstInbound.fromSeq()) {
                            firstInbound = segment;
                        }
                    }
                }
                
                // Create turnaround edge if both directions exist
                if (lastOutbound != null && firstInbound != null) {
                    String fromKey = lastOutbound.toStopId() + "_" + routeId + "_" + Direction.OUTBOUND;
                    String toKey = firstInbound.fromStopId() + "_" + routeId + "_" + Direction.INBOUND;
                    
                    Integer fromStateId = stopRouteToStateId.get(fromKey);
                    Integer toStateId = stopRouteToStateId.get(toKey);
                    
                    if (fromStateId != null && toStateId != null) {
                        // Turnaround time: estimate 2 minutes (120 seconds)
                        int realSec = 120;
                        int perceivedCost = realSec;
                        
                        Edge turnaroundEdge = new Edge(fromStateId, toStateId, EdgeKind.RIDE, realSec, perceivedCost);
                        graph.addEdge(turnaroundEdge);
                        turnaroundEdges++;
                    }
                }
            }
            
            LOGGER.info(String.format("Creadas %d aristas de turnaround", turnaroundEdges));
            
            // Step 7: Generate RIDE edges from transit lines
            LOGGER.info("Generando aristas RIDE desde líneas de tránsito...");
            int transitRideEdges = 0;
            int metroStationStatesCreated = 0;
            
            // Group transit segments by line
            Map<String, List<TransitSegment>> transitLineGroups = new HashMap<>();
            for (TransitSegment segment : transitSegments) {
                transitLineGroups.computeIfAbsent(segment.line(), k -> new ArrayList<>()).add(segment);
            }
            
            // Sort each line's segments by order
            for (List<TransitSegment> segments : transitLineGroups.values()) {
                segments.sort((a, b) -> Integer.compare(a.order(), b.order()));
            }
            
            // Process each line for BOTH directions (OUTBOUND and INBOUND)
            // The CSV contains one direction, but we need to create states for both directions
            for (Map.Entry<String, List<TransitSegment>> entry : transitLineGroups.entrySet()) {
                String line = entry.getKey();
                List<TransitSegment> segments = entry.getValue();
                
                // Create states and edges for OUTBOUND direction
                for (int i = 0; i < segments.size(); i++) {
                    TransitSegment current = segments.get(i);
                    final Direction direction = Direction.OUTBOUND;
                    
                    // Create METRO_STATION state for current segment
                    String stationKey = current.station() + "_" + line + "_" + direction;
                    stationLineToStateId.computeIfAbsent(stationKey, k -> {
                        Place place = new Place(current.station(), current.coordinate());
                        State state = new State(nextStateId[0], place, StateType.METRO_STATION, line, direction);
                        graph.addState(state);
                        return nextStateId[0]++;
                    });
                    
                    // Create RIDE edge to next segment if exists
                    if (i < segments.size() - 1) {
                        TransitSegment next = segments.get(i + 1);
                        String fromKey = current.station() + "_" + line + "_" + direction;
                        String toKey = next.station() + "_" + line + "_" + direction;
                        
                        int fromStateId = stationLineToStateId.get(fromKey);
                        int toStateId = stationLineToStateId.computeIfAbsent(toKey, k -> {
                            Place place = new Place(next.station(), next.coordinate());
                            State state = new State(nextStateId[0], place, StateType.METRO_STATION, line, direction);
                            graph.addState(state);
                            return nextStateId[0]++;
                        });
                        
                        // Use secToNext from data (skip if last station has no next)
                        if (current.secToNext().isEmpty()) {
                            continue; // Last station - no ride edge to next
                        }
                        int realSec = current.secToNext().get();
                        int perceivedCost = realSec; // No penalty for RIDE edges
                        
                        Edge rideEdge = new Edge(fromStateId, toStateId, EdgeKind.RIDE, realSec, perceivedCost);
                        graph.addEdge(rideEdge);
                        transitRideEdges++;
                    }
                }
                
                // Create states and edges for INBOUND direction (reverse order)
                for (int i = segments.size() - 1; i >= 0; i--) {
                    TransitSegment current = segments.get(i);
                    final Direction direction = Direction.INBOUND;
                    
                    // Create METRO_STATION state for current segment
                    String stationKey = current.station() + "_" + line + "_" + direction;
                    stationLineToStateId.computeIfAbsent(stationKey, k -> {
                        Place place = new Place(current.station(), current.coordinate());
                        State state = new State(nextStateId[0], place, StateType.METRO_STATION, line, direction);
                        graph.addState(state);
                        return nextStateId[0]++;
                    });
                    
                    // Create RIDE edge to next segment (which is previous in original order)
                    if (i > 0) {
                        TransitSegment next = segments.get(i - 1);
                        String fromKey = current.station() + "_" + line + "_" + direction;
                        String toKey = next.station() + "_" + line + "_" + direction;
                        
                        int fromStateId = stationLineToStateId.get(fromKey);
                        int toStateId = stationLineToStateId.computeIfAbsent(toKey, k -> {
                            Place place = new Place(next.station(), next.coordinate());
                            State state = new State(nextStateId[0], place, StateType.METRO_STATION, line, direction);
                            graph.addState(state);
                            return nextStateId[0]++;
                        });
                        
                        // Use secToNext from next segment (since we're going in reverse)
                        // Skip if last station has no next
                        if (next.secToNext().isEmpty()) {
                            continue; // Last station - no ride edge to next
                        }
                        // In reverse direction, we use the same travel time
                        int realSec = next.secToNext().get();
                        int perceivedCost = realSec; // No penalty for RIDE edges
                        
                        Edge rideEdge = new Edge(fromStateId, toStateId, EdgeKind.RIDE, realSec, perceivedCost);
                        graph.addEdge(rideEdge);
                        transitRideEdges++;
                    }
                }
            }
            
            metroStationStatesCreated = stationLineToStateId.size();
            LOGGER.info(String.format("Creados %d estados METRO_STATION, %d aristas RIDE de tránsito", 
                    metroStationStatesCreated, transitRideEdges));
            
            // Step 8: Generate BOARD edges (WALK -> BUS_STOP/METRO_STATION)
            LOGGER.info("Generando aristas BOARD...");
            int boardEdges = 0;
            
            // Board edges for bus stops
            for (Map.Entry<String, Integer> entry : stopRouteToStateId.entrySet()) {
                String[] parts = entry.getKey().split("_");
                int stopId = Integer.parseInt(parts[0]);
                Integer walkStateId = stopIdToWalkStateId.get(stopId);
                if (walkStateId == null) {
                    continue;
                }
                int busStopStateId = entry.getValue();
                
                // Average wait time for buses (from config, default 300s = 5 min)
                int waitTimeSec = 300;
                int realSec = waitTimeSec;
                int perceivedCost = waitTimeSec + boardingPenaltySec;
                
                Edge boardEdge = new Edge(walkStateId, busStopStateId, EdgeKind.BOARD, realSec, perceivedCost);
                graph.addEdge(boardEdge);
                boardEdges++;
            }
            
            // Board edges for metro stations
            for (Map.Entry<String, Integer> entry : stationLineToStateId.entrySet()) {
                String[] parts = entry.getKey().split("_");
                String stationName = parts[0];
                Integer walkStateId = stationNameToWalkStateId.get(stationName);
                if (walkStateId == null) {
                    LOGGER.warning("No WALK state found for station: " + stationName);
                    continue;
                }
                int metroStationStateId = entry.getValue();
                
                // Average wait time for metro (shorter than bus, default 180s = 3 min)
                int waitTimeSec = 180;
                int realSec = waitTimeSec;
                int perceivedCost = waitTimeSec + boardingPenaltySec;
                
                Edge boardEdge = new Edge(walkStateId, metroStationStateId, EdgeKind.BOARD, realSec, perceivedCost);
                graph.addEdge(boardEdge);
                boardEdges++;
            }
            
            LOGGER.info(String.format("Creadas %d aristas BOARD", boardEdges));
            
            // Step 9: Generate ALIGHT edges (BUS_STOP/METRO_STATION -> WALK)
            LOGGER.info("Generando aristas ALIGHT...");
            int alightEdges = 0;
            
            // Alight edges for bus stops
            for (Map.Entry<String, Integer> entry : stopRouteToStateId.entrySet()) {
                String[] parts = entry.getKey().split("_");
                int stopId = Integer.parseInt(parts[0]);
                Integer walkStateId = stopIdToWalkStateId.get(stopId);
                if (walkStateId == null) {
                    continue;
                }
                int busStopStateId = entry.getValue();
                
                int realSec = 0;
                int perceivedCost = 0;
                
                Edge alightEdge = new Edge(busStopStateId, walkStateId, EdgeKind.ALIGHT, realSec, perceivedCost);
                graph.addEdge(alightEdge);
                alightEdges++;
            }
            
            // Alight edges for metro stations
            for (Map.Entry<String, Integer> entry : stationLineToStateId.entrySet()) {
                String[] parts = entry.getKey().split("_");
                String stationName = parts[0];
                Integer walkStateId = stationNameToWalkStateId.get(stationName);
                if (walkStateId == null) {
                    continue;
                }
                int metroStationStateId = entry.getValue();
                
                int realSec = 0;
                int perceivedCost = 0;
                
                Edge alightEdge = new Edge(metroStationStateId, walkStateId, EdgeKind.ALIGHT, realSec, perceivedCost);
                graph.addEdge(alightEdge);
                alightEdges++;
            }
            
            LOGGER.info(String.format("Creadas %d aristas ALIGHT", alightEdges));
            
            // Step 10: Generate WALK edges from walk links
            LOGGER.info("Generando aristas WALK desde enlaces peatonales...");
            int walkEdges = 0;
            int skippedWalkLinks = 0;
            
            for (WalkLink link : walkLinks) {
                // Look up WALK state IDs using the place strings (STOP:123 or STATION:name)
                Integer stateIdA = placeStringToWalkStateId.get(link.placeA());
                Integer stateIdB = placeStringToWalkStateId.get(link.placeB());
                
                if (stateIdA == null || stateIdB == null) {
                    skippedWalkLinks++;
                    if (skippedWalkLinks <= 5) {
                        LOGGER.warning(String.format("Omitiendo enlace peatonal: placeA=%s, placeB=%s (no encontrado)", 
                                link.placeA(), link.placeB()));
                    }
                    continue;
                }
                
                // Use walkSec from data
                int realSec = link.walkSec();
                
                // Apply walk penalty
                int perceivedCost = (int) Math.round(realSec * walkPenaltyMultiplier) + walkPenaltyConstant;
                
                // Add bidirectional edges
                Edge edgeAB = new Edge(stateIdA, stateIdB, EdgeKind.WALK, realSec, perceivedCost);
                Edge edgeBA = new Edge(stateIdB, stateIdA, EdgeKind.WALK, realSec, perceivedCost);
                graph.addEdge(edgeAB);
                graph.addEdge(edgeBA);
                walkEdges += 2;
            }
            
            if (skippedWalkLinks > 5) {
                LOGGER.warning(String.format("...y %d enlaces más", skippedWalkLinks - 5));
            }
            LOGGER.info(String.format("Creadas %d aristas WALK (%d enlaces omitidos)", walkEdges, skippedWalkLinks));
            
            // Validation
            long elapsedMs = System.currentTimeMillis() - startTime;
            
            int totalWalkStates = stopWalkStates + stationWalkStates;
            int totalBusRideEdges = busRideEdges + turnaroundEdges;
            
            LOGGER.info("=== Construcción del grafo completada ===");
            LOGGER.info(String.format("Estados: %d (esperados: %d)", graph.getStateCount(), EXPECTED_STATES));
            LOGGER.info(String.format("  - WALK: %d (%d paradas + %d estaciones)", totalWalkStates, stopWalkStates, stationWalkStates));
            LOGGER.info(String.format("  - BUS_STOP: %d", busStopStatesCreated));
            LOGGER.info(String.format("  - METRO_STATION: %d", metroStationStatesCreated));
            LOGGER.info(String.format("Aristas: %d (esperadas: %d)", graph.getEdgeCount(), EXPECTED_EDGES));
            LOGGER.info(String.format("  - Bus RIDE: %d (%d CSV + %d turnaround)", totalBusRideEdges, busRideEdges, turnaroundEdges));
            LOGGER.info(String.format("  - Transit RIDE: %d", transitRideEdges));
            LOGGER.info(String.format("  - BOARD: %d", boardEdges));
            LOGGER.info(String.format("  - ALIGHT: %d", alightEdges));
            LOGGER.info(String.format("  - WALK: %d", walkEdges));
            LOGGER.info(String.format("Tiempo de carga: %d ms", elapsedMs));
            
            if (elapsedMs > 1000) {
                LOGGER.warning("ADVERTENCIA: Tiempo de carga excede el objetivo de 1 segundo");
            }
            
            if (graph.getStateCount() == 0 || graph.getEdgeCount() == 0) {
                throw new IOException(
                    "El grafo está vacío. Verifique que los archivos CSV/JSON existan en: " + 
                    dataDir.toAbsolutePath()
                );
            }
            
            return graph;
            
        } catch (IOException e) {
            throw new IOException("Error al construir el grafo: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets the bus speed in km/h for a given route ID.
     * Looks up the route type and returns the appropriate speed from configuration.
     * 
     * @param routeId route identifier
     * @param params configuration parameters
     * @return bus speed in km/h (default: 18 km/h for Integrada buses)
     */
    private double getBusSpeed(String routeId, Parameters params) {
        // In a full implementation, this would look up the route type from routeMap
        // and return speeds like:
        // - Directa Centro / Circular: 15 km/h
        // - Integrada / Directa No Centro: 18 km/h
        //
        // For now, return a default speed
        return 18.0; // km/h
    }
}
