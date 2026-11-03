# Implementation Plan: Medellín Transit Routing System

## Overview

This implementation plan breaks down the Medellín Transit Routing System into discrete, testable tasks. The system is a Java console application that provides offline multi-modal transit routing using a pre-computed graph and Dijkstra's algorithm. All tasks build incrementally toward a complete system that loads in <1s and routes in <100ms.

## Tasks

- [x] 1. Set up project structure and core dependencies
  - Create Maven project with Java 17
  - Add dependencies: Apache Commons CSV 1.10.0, JUnit Jupiter 5.10.0
  - Configure pom.xml with main class: `co.medellin.transit.Main`
  - Create package structure: ui, data, model, graph, geocoding, routing, fares
  - Add config.properties for routing and fare parameters
  - _Requirements: Project foundation for offline Java console application_

- [x] 2. Implement core domain models
  - [x] 2.1 Create fundamental data structures
    - Implement `Coordinate` class (lat, lon with validation)
    - Implement `Place` class (name, coordinate)
    - Implement `RouteType` enum (BUS, METRO)
    - Implement `Route` class (id, displayName, type, color)
    - Implement `Direction` enum (OUTBOUND, INBOUND)
    - _Requirements: Geographic and transit data representation_
  
  - [x] 2.2 Create graph model classes
    - Implement `StateType` enum (WALK, BUS_STOP, METRO_STATION)
    - Implement `State` class (id, place, type, routeId, direction)
    - Implement `EdgeKind` enum (WALK, BOARD, RIDE, ALIGHT)
    - Implement `Edge` class (sourceId, targetId, kind, realSec, perceivedCost)
    - _Requirements: Graph representation for routing algorithm_
  
  - [x] 2.3 Create trip output models
    - Implement `StepType` enum (WALK, BOARD, RIDE, ALIGHT)
    - Implement `Step` class (type, description, durationSec, location, routeInfo)
    - Implement `Trip` class (steps list, totalDurationSec, totalWalkMeters)
    - _Requirements: Structured trip instructions for display_

- [ ] 3. Implement geographic utilities
  - [x] 3.1 Create Geo utility class
    - Implement Haversine distance formula (lat/lon pairs to meters)
    - Add bounds checking for Medellín (6.15°-6.35° lat, -75.65°--75.50° lon)
    - Write unit tests for known distance pairs
    - _Requirements: Accurate geographic calculations for 500m access radius_
  
  - [ ]* 3.2 Write unit tests for Geo class
    - Test distance calculation accuracy (known point pairs)
    - Test edge cases (same point, antipodes, equator crossing)
    - Test bounds validation (in/out of Medellín)
    - _Requirements: Verify geographic calculation correctness_

- [x] 4. Checkpoint - Verify foundation
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement CSV data readers
  - [x] 5.1 Create CSV reading infrastructure
    - Implement `CsvReader` base class with buffered reading
    - Implement `ArcGisReader` for ArcGIS geometry format
    - Handle malformed rows gracefully (log warning, skip, continue)
    - _Requirements: Load transit data from CSV files for offline operation_
  
  - [x] 5.2 Implement specialized CSV readers
    - Implement `StopCsvReader` (parse stops.csv into Stop objects)
    - Implement `RouteCsvReader` (parse routes.csv into Route objects)
    - Implement `GraphCsvReader` (parse pre-computed edges)
    - Pre-size collections for known dataset size (~15,000 stops)
    - _Requirements: Parse stops, routes, and graph topology from CSV_
  
  - [ ]* 5.3 Write unit tests for CSV readers
    - Test valid CSV parsing with sample data
    - Test malformed data handling (invalid numbers, missing fields)
    - Test empty file and missing file scenarios
    - _Requirements: Robust CSV parsing with error handling_

- [x] 6. Implement graph construction
  - [x] 6.1 Create TransitGraph data structure
    - Implement `TransitGraph` with adjacency list representation
    - Use `List<Edge>[]` for efficient edge storage (13,895 states)
    - Add `placeIndex` map for quick origin/destination lookup
    - Implement `getOutgoingEdges(stateId)` method
    - Implement `getStatesNearPlace(place, radiusMeters)` with spatial index
    - _Requirements: In-memory graph structure optimized for Dijkstra_
  
  - [x] 6.2 Implement GraphBuilder
    - Create `GraphBuilder.build(dataDir)` that loads all CSVs
    - Build state nodes: (place, WALK), (STOP, route, dir), (STATION, line, dir)
    - Add ride edges within routes based on travel times
    - Add board edges: (place, WALK) → (STOP/STATION, route/line, dir) cost=0
    - Add alight edges: (STOP/STATION, route, dir) → (place, WALK) cost=travelTime
    - Add walking edges between states within 500m radius
    - Use string interning for route IDs and place names (save ~100MB memory)
    - Target: graph construction in <1 second
    - _Requirements: Build 13,895-node graph with 82,160 edges from CSV data_
  
  - [ ]* 6.3 Write integration tests for GraphBuilder
    - Test graph construction with sample CSV data (50-100 stops)
    - Verify state count and edge count match expected values
    - Test performance: loading time <1 second for full dataset
    - Verify spatial index returns correct nearby states
    - _Requirements: Validate graph construction correctness and performance_

- [x] 7. Checkpoint - Verify graph loading
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement address geocoding
  - [x] 8.1 Create AddressParser
    - Parse Medellín address format: "(Calle|Carrera|Diagonal|Circular|Transversal) <num>[A-Z]? # <num> (Sur|Norte)? - <num>"
    - Implement `StreetType` enum (CALLE, CARRERA, DIAGONAL, CIRCULAR, TRANSVERSAL)
    - Implement `Cardinal` enum (NORTE, SUR, ESTE, OESTE)
    - Implement `ParsedAddress` class (type, primary, secondary, number, cardinal)
    - Return `Optional<ParsedAddress>` for invalid formats
    - Handle case-insensitive input and extra whitespace
    - _Requirements: Parse Medellín street naming convention_
  
  - [x] 8.2 Create AddressLocator
    - Implement grid interpolation for Medellín addresses
    - Use reference points for major streets (from CSV or hardcoded)
    - Validate coordinates are within Medellín bounds
    - Return `Optional<Coordinate>` for unreliable extrapolations
    - _Requirements: Convert parsed addresses to geographic coordinates_
  
  - [ ]* 8.3 Write unit tests for address geocoding
    - Test valid address formats (Carrera, Calle, with/without cardinal directions)
    - Test invalid formats (missing #, malformed numbers)
    - Test known addresses map to expected coordinates
    - Test bounds checking (in-bounds vs out-of-bounds addresses)
    - _Requirements: Accurate address parsing and localization_

- [x] 9. Implement routing engine
  - [x] 9.1 Create Parameters configuration class
    - Load routing parameters from config.properties
    - Defaults: accessRadiusMeters=500, boardingPenalty=120s, walkPenaltyMultiplier=1.5
    - Implement getters with fallback to hardcoded defaults
    - _Requirements: Configurable routing behavior_
  
  - [x] 9.2 Implement Dijkstra's algorithm
    - Create `Router` class with `findRoute(origin, destination, params)`
    - Implement multi-source initialization (all states within access radius of origin)
    - Implement multi-target termination (first target reached optimally)
    - Use `PriorityQueue<Node>` for min-heap (binary heap)
    - Use primitive int[] arrays for distances and previous pointers
    - Cost function: perceivedCost = realSec + boardingPenalty + walkPenalty
    - Early termination when pq.min > bestTargetCost
    - Target: query time <100ms per route
    - _Requirements: Optimal multi-modal route computation_
  
  - [x] 9.3 Implement path reconstruction
    - Create `DijkstraResult` class (path, perceivedCost, realDurationSec)
    - Reconstruct path from previous[] array (trace from target to source)
    - Handle no-path scenario (return Optional.empty())
    - Handle origin=destination scenario (distance <50m)
    - _Requirements: Extract complete path from Dijkstra state_
  
  - [ ]* 9.4 Write unit tests for Router
    - Test simple routes (walk → board → ride → alight → walk)
    - Test multi-transfer routes (bus → walk → metro)
    - Test edge cases (no path exists, same origin/destination)
    - Test that boarding/walking penalties affect route selection
    - _Requirements: Verify routing algorithm correctness_
  
  - [ ]* 9.5 Write integration tests for routing
    - Test with real Medellín data (13,895 states, 82,160 edges)
    - Use known origin/destination pairs from data/test_cases.csv
    - Verify routes match Python prototype behavior
    - Verify performance: queries complete in <100ms
    - _Requirements: End-to-end routing validation with real data_

- [x] 10. Checkpoint - Verify routing core
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Implement fare calculation
  - [x] 11.1 Create FareConfig and models
    - Implement `FareConfig` (metroFare=3000, busFare=2950, integrationDiscount=0.5, integrationWindow=90min)
    - Implement `FareSegment` (mode, baseFare, appliedFare, integrationApplied)
    - Implement `FareResult` (totalCOP, segments, integrationApplied boolean)
    - Load fare values from config.properties
    - _Requirements: Configurable fare rules_
  
  - [x] 11.2 Implement FareCalculator
    - Create `FareCalculator.calculate(trip)` method
    - Track last boarding time and mode for integration detection
    - Apply integration discount when: (metro→bus or bus→metro) within 90 minutes
    - Walking steps have zero fare
    - Return detailed FareResult with per-segment breakdown
    - _Requirements: Accurate fare calculation with 90-minute integration window_
  
  - [ ]* 11.3 Write unit tests for FareCalculator
    - Test single-mode trips (bus only, metro only)
    - Test integration scenarios (metro→bus within 90min gets discount)
    - Test no integration (same mode, >90min gap)
    - Test walking-only trips (zero fare)
    - _Requirements: Validate fare rules and integration logic_

- [x] 12. Implement step builder
  - [x] 12.1 Create StepBuilder
    - Implement `StepBuilder.buildTrip(dijkstraResult, graph)`
    - Convert WALK edges to "Caminar a {place}" steps in Spanish
    - Convert BOARD edges to "Abordar {route} en {place}" steps
    - Consolidate consecutive RIDE edges on same route into single "Viajar en {route} por {duration} minutos"
    - Convert ALIGHT edges to "Bajar en {place}" steps
    - Omit walks <50m between stops of same route
    - Calculate totalDurationSec and totalWalkMeters
    - _Requirements: Human-readable Spanish trip instructions_
  
  - [ ]* 12.2 Write unit tests for StepBuilder
    - Test simple paths (walk → board → ride → alight → walk)
    - Test multi-transfer paths with step consolidation
    - Verify Spanish text formatting is correct
    - Test that consecutive rides are merged into single step
    - _Requirements: Accurate trip instruction generation_

- [x] 13. Implement console user interface
  - [x] 13.1 Create HomeScreen
    - Display welcome banner: "Sistema de Rutas Medellín Metro"
    - Use ASCII box drawing for visual appeal
    - _Requirements: User-friendly console entry point_
  
  - [x] 13.2 Create TripPrinter
    - Implement `print(trip, fare)` with Spanish formatting
    - Display: duration, walking distance, total fare, integration status
    - Format instructions as numbered list with durations
    - Add checkmark "✓ Descuento por integración aplicado" when applicable
    - Use Colombian Peso formatting: $X,XXX COP
    - _Requirements: Clear, formatted Spanish trip output_
  
  - [x] 13.3 Create Menu with interaction loop
    - Implement main loop: prompt for origin, destination, compute route, display result
    - Integrate AddressParser and AddressLocator for geocoding
    - Handle invalid addresses with Spanish error messages and examples
    - Handle out-of-bounds addresses: "La dirección está fuera de los límites de Medellín"
    - Handle no route found: "No se encontró una ruta"
    - Handle origin=destination (< 50m): "El origen y destino están muy cerca"
    - Exit on "salir" command
    - _Requirements: Interactive Spanish-language console interface_

- [x] 14. Implement Main application entry point
  - [x] 14.1 Create Main class with initialization
    - Load graph with GraphBuilder in try-catch (fatal error if fails)
    - Handle OutOfMemoryError with instructions: "java -Xmx2G -jar transit.jar"
    - Handle IOException with Spanish message: "No se pudieron cargar los datos de transporte"
    - Initialize all components (Router, FareCalculator, AddressParser, etc.)
    - Display HomeScreen and start Menu loop
    - Log startup time and memory usage (optional)
    - _Requirements: Robust application initialization with error handling_
  
  - [ ]* 14.2 Write integration test for Main
    - Test full end-to-end workflow with sample data
    - Verify Spanish output for all error scenarios
    - Test memory usage stays under 512MB
    - _Requirements: Complete application integration validation_

- [x] 15. Add error handling and logging
  - [x] 15.1 Implement comprehensive error handling
    - Wrap CSV loading with try-catch, log warnings for invalid rows
    - Validate all user inputs before processing
    - Return Optional.empty() for recoverable errors (invalid address, no route)
    - Fatal errors (file not found, OOM) exit with clear Spanish messages
    - Use java.util.logging for INFO/WARN/ERROR levels
    - _Requirements: Graceful error handling with Spanish user feedback_

- [x] 16. Optimize performance
  - [x] 16.1 Apply memory optimizations
    - Verify string interning is working (check memory usage)
    - Use primitive int[] arrays in Dijkstra (not Integer[])
    - Verify spatial index reduces getStatesNearPlace to O(k) not O(n)
    - Target: total heap usage <512MB
    - _Requirements: Meet memory footprint constraint_
  
  - [x] 16.2 Profile and optimize hotspots
    - Run performance tests: 100 random route queries
    - Use VisualVM or JProfiler to identify bottlenecks
    - Optimize top 3 hotspots if query time >100ms
    - Verify graph loading time <1 second
    - _Requirements: Meet performance targets (<1s load, <100ms query)_

- [x] 17. Checkpoint - Verify complete system
  - Ensure all tests pass, ask the user if questions arise.

- [x] 18. Validate against reference test cases
  - [x] 18.1 Run reference test suite
    - Load test cases from data/test_cases.csv
    - Each test case: origin, destination, expected route, expected fare
    - Compare computed routes with Python prototype results
    - All test cases must produce identical results
    - _Requirements: Match Python prototype behavior exactly_
  
  - [ ]* 18.2 Create automated test harness
    - Implement JUnit test that reads test_cases.csv
    - Assert each route matches expected values
    - Report differences clearly (expected vs actual)
    - _Requirements: Automated validation of system correctness_

- [x] 19. Final integration and polish
  - [x] 19.1 Create executable JAR
    - Configure maven-jar-plugin with main class manifest
    - Build with: mvn clean package
    - Test JAR execution: java -jar transit-routing-1.0.0.jar
    - _Requirements: Deployable application artifact_
  
  - [x] 19.2 Write README documentation
    - Installation instructions (Java 17 required)
    - Data setup instructions (CSV file locations)
    - Usage examples (sample addresses)
    - Configuration guide (config.properties parameters)
    - Troubleshooting section (OOM, missing files, etc.)
    - _Requirements: User documentation for deployment_

- [x] 20. Final checkpoint - Complete validation
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional test tasks and can be skipped for faster MVP
- Each task references the specific design components it implements
- Integration tests validate end-to-end workflows and performance
- All user-facing strings must be in Spanish (prompts, errors, instructions)
- Code, comments, and logs should be in English for maintainability
- The implementation must match the Python prototype behavior for all test cases
- Performance targets are strict requirements: <1s load, <100ms query, <512MB memory

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["5.1"] },
    { "id": 4, "tasks": ["5.2", "5.3"] },
    { "id": 5, "tasks": ["6.1"] },
    { "id": 6, "tasks": ["6.2", "6.3"] },
    { "id": 7, "tasks": ["8.1", "8.2", "8.3"] },
    { "id": 8, "tasks": ["9.1"] },
    { "id": 9, "tasks": ["9.2"] },
    { "id": 10, "tasks": ["9.3", "9.4"] },
    { "id": 11, "tasks": ["9.5", "11.1"] },
    { "id": 12, "tasks": ["11.2", "11.3", "12.1", "12.2"] },
    { "id": 13, "tasks": ["13.1", "13.2"] },
    { "id": 14, "tasks": ["13.3"] },
    { "id": 15, "tasks": ["14.1", "14.2", "15.1"] },
    { "id": 16, "tasks": ["16.1", "16.2"] },
    { "id": 17, "tasks": ["18.1", "18.2"] },
    { "id": 18, "tasks": ["19.1", "19.2"] }
  ]
}
```
