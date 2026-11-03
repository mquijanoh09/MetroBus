# Requirements Document

## Introduction

The Medellín Transit Routing System is a Java console application that helps users find the fastest public transportation route between two street addresses in Medellín, Colombia. The system uses a pre-computed graph of transit network states (13,895 nodes, 82,160 edges) and applies Dijkstra's shortest path algorithm on perceived travel time cost to find optimal routes. The application operates entirely offline using pre-built CSV data files from October 2026, providing quick responses (query time < 100ms) with graceful error handling for edge cases.

## Glossary

- **Route_Planner**: The main system that coordinates address parsing, pathfinding, and route presentation
- **Address_Parser**: Component that converts Medellín street addresses (Calle/Carrera format) into coordinates
- **Graph_Loader**: Component that loads transit network data from CSV files into memory
- **Dijkstra_Engine**: Component that executes Dijkstra's algorithm on the transit graph
- **Stop_Finder**: Component that finds nearby transit stops/stations within a radius of a given location
- **Fare_Calculator**: Component that calculates total trip cost based on integration rules
- **Route_Formatter**: Component that formats routing results for display to users
- **Transit_Graph**: In-memory representation of the transit network with nodes (Dijkstra states) and edges (travel segments)
- **Dijkstra_State**: A node in the transit graph representing a specific location and transit mode combination
- **Perceived_Cost**: Travel time plus penalties used by pathfinding algorithm to guide optimal behavior
- **Real_Duration**: Actual travel time displayed to users (without penalties)
- **Transit_Mode**: Type of transportation: bus, Metro, Metroplús, tram, cable car, or walking
- **Integration_Window**: Time period allowing free transfers between certain transit modes
- **Physical_Stop**: A real-world bus stop or transit station where passengers can board/alight
- **Walk_Radius**: Maximum walking distance (500m) for accessing transit stops from addresses

## Requirements

### Requirement 1: Graph Data Loading

**User Story:** As a system administrator, I want the application to load transit network data efficiently, so that the application starts quickly and can handle routing queries.

#### Acceptance Criteria

1. THE Graph_Loader SHALL load all transit network data from CSV files in the data directory
2. THE Graph_Loader SHALL load bus_segments.csv containing bus route segments with travel times
3. THE Graph_Loader SHALL load transit_lines.csv containing Metro, Metroplús, tram, and cable car segments
4. THE Graph_Loader SHALL load walk_links.csv containing walking connections between stops
5. WHEN loading completes successfully, THE Graph_Loader SHALL construct a Transit_Graph with 13,895 Dijkstra_States and 82,160 edges
6. THE Graph_Loader SHALL complete loading in less than 1000 milliseconds
7. IF any required CSV file is missing, THEN THE Graph_Loader SHALL terminate with a descriptive error message in Spanish
8. IF any CSV file contains malformed data, THEN THE Graph_Loader SHALL terminate with a descriptive error message identifying the problematic file and line number
9. FOR ALL edges in the Transit_Graph, the perceived cost SHALL be greater than or equal to the real duration
10. FOR ALL loaded data, parsing and reconstructing SHALL produce structurally equivalent Transit_Graph (round-trip property)

### Requirement 2: Address Parsing

**User Story:** As a user, I want to enter street addresses in Medellín's standard format, so that I can find routes using familiar address conventions.

#### Acceptance Criteria

1. WHEN a user provides an address string, THE Address_Parser SHALL parse Medellín grid format addresses (e.g., "Calle 10 # 20-30", "Carrera 43A # 12-45")
2. THE Address_Parser SHALL extract street type (Calle, Carrera, Circular, Diagonal, Transversal)
3. THE Address_Parser SHALL extract primary street number with optional letter suffix (e.g., "43A")
4. THE Address_Parser SHALL extract cross street number
5. THE Address_Parser SHALL extract building number within the block
6. WHEN parsing succeeds, THE Address_Parser SHALL convert the address to geographic coordinates (latitude, longitude)
7. IF the address format is invalid, THEN THE Address_Parser SHALL return an error indicating the parsing failure
8. IF the address is outside Medellín's grid system, THEN THE Address_Parser SHALL return an error indicating the address is out of bounds
9. FOR ALL valid addresses, parsing and formatting SHALL produce an equivalent address string (round-trip property)
10. WHEN an address contains variations in spacing or capitalization, THE Address_Parser SHALL normalize and parse correctly

### Requirement 3: Stop and Station Discovery

**User Story:** As a user, I want the system to find nearby transit stops automatically, so that I don't need to know stop names or locations.

#### Acceptance Criteria

1. WHEN given a geographic coordinate, THE Stop_Finder SHALL identify all Physical_Stops within the Walk_Radius (500 meters)
2. THE Stop_Finder SHALL search all transit modes (bus stops, Metro stations, Metroplús stations, tram stations, cable car stations)
3. WHEN multiple stops are found, THE Stop_Finder SHALL return them ordered by distance from the coordinate
4. THE Stop_Finder SHALL calculate walking time to each stop based on a walking speed of 4 km/h
5. WHEN no stops are found within the Walk_Radius, THE Stop_Finder SHALL return an empty result set
6. FOR ALL coordinates within Medellín bounds, the distance calculation SHALL satisfy the triangle inequality
7. FOR ALL returned stops, the walking time SHALL be proportional to the calculated distance
8. WHEN the same coordinate is queried multiple times, THE Stop_Finder SHALL return identical results (idempotent property)

### Requirement 4: Shortest Path Finding

**User Story:** As a user, I want to find the fastest route between two addresses, so that I can minimize my travel time.

#### Acceptance Criteria

1. WHEN given a set of origin stops and destination stops, THE Dijkstra_Engine SHALL compute the shortest path using Dijkstra's algorithm
2. THE Dijkstra_Engine SHALL use Perceived_Cost as the edge weight for pathfinding
3. THE Dijkstra_Engine SHALL support multi-source pathfinding (starting from any origin stop)
4. THE Dijkstra_Engine SHALL support multi-target pathfinding (ending at any destination stop)
5. WHEN a path exists, THE Dijkstra_Engine SHALL return the sequence of Dijkstra_States from origin to destination
6. WHEN no path exists between any origin and destination pair, THE Dijkstra_Engine SHALL return an empty result
7. THE Dijkstra_Engine SHALL complete pathfinding in less than 100 milliseconds for any query
8. FOR ALL valid paths, the total Perceived_Cost SHALL equal the sum of individual edge costs
9. FOR ALL returned paths, the path SHALL be optimal (no other path has lower total Perceived_Cost)
10. WHEN adding zero-cost edges to a graph, THE Dijkstra_Engine SHALL return paths with cost less than or equal to the original (monotonicity property)

### Requirement 5: Route Formatting and Display

**User Story:** As a user, I want to see route instructions in Spanish with clear step-by-step directions, so that I can follow the route easily.

#### Acceptance Criteria

1. WHEN given a valid path, THE Route_Formatter SHALL convert the sequence of Dijkstra_States into human-readable instructions in Spanish
2. THE Route_Formatter SHALL display the starting address and destination address
3. THE Route_Formatter SHALL display walking directions to the first transit stop with distance and estimated time
4. THE Route_Formatter SHALL display each transit segment with route number/name, direction, boarding stop, and alighting stop
5. THE Route_Formatter SHALL display transfers between transit modes with walking directions
6. THE Route_Formatter SHALL display walking directions from the final transit stop to the destination address
7. THE Route_Formatter SHALL display total travel time using Real_Duration (excluding penalties)
8. THE Route_Formatter SHALL display the total fare calculated by Fare_Calculator
9. WHEN multiple routes exist, THE Route_Formatter SHALL display routes sorted by total Real_Duration
10. THE Route_Formatter SHALL use consistent terminology for transit modes (Metro, Metroplús, Tranvía, Metrocable, Bus)

### Requirement 6: Fare Calculation

**User Story:** As a user, I want to know the total cost of my trip, so that I can budget for my journey.

#### Acceptance Criteria

1. WHEN given a route with transit segments, THE Fare_Calculator SHALL calculate the total fare
2. THE Fare_Calculator SHALL apply base fare for the first boarding (Metro/Metroplús: 2950 COP, Bus: 2950 COP, Cable car: 2950 COP)
3. WHEN transferring between Metro and Metroplús within 120 minutes, THE Fare_Calculator SHALL apply integration discount (no additional fare)
4. WHEN transferring from Metro/Metroplús to bus within 120 minutes, THE Fare_Calculator SHALL apply integration discount (reduced fare)
5. WHEN transferring between buses without using Metro/Metroplús, THE Fare_Calculator SHALL charge full fare for each bus
6. THE Fare_Calculator SHALL display the fare in Colombian Pesos (COP)
7. FOR ALL routes using only one transit vehicle, the fare SHALL equal the base fare for that mode
8. FOR ALL routes with transfers, the total fare SHALL be less than or equal to the sum of individual base fares (integration discount property)
9. WHEN a route requires no transit (walking only), THE Fare_Calculator SHALL return zero fare

### Requirement 7: Error Handling and Fallback Options

**User Story:** As a user, I want the system to suggest alternatives when my request cannot be fulfilled, so that I can still find a way to reach my destination.

#### Acceptance Criteria

1. WHEN no transit stops are found near the origin address, THE Route_Planner SHALL inform the user in Spanish and suggest the nearest stop beyond the Walk_Radius
2. WHEN no transit stops are found near the destination address, THE Route_Planner SHALL inform the user in Spanish and suggest the nearest stop beyond the Walk_Radius
3. WHEN no route exists between the origin and destination, THE Route_Planner SHALL inform the user in Spanish
4. WHEN the origin and destination are within walking distance (less than 2 km), THE Route_Planner SHALL suggest walking as an alternative
5. WHEN a route exists but requires more than 3 transfers, THE Route_Planner SHALL warn the user and display the route
6. IF address parsing fails, THEN THE Route_Planner SHALL display the parsing error in Spanish and prompt for a corrected address
7. WHEN showing fallback options, THE Route_Planner SHALL display up to 3 alternative suggestions ranked by practicality
8. FOR ALL error messages, the text SHALL be in Spanish and provide actionable guidance

### Requirement 8: Test Case Validation

**User Story:** As a developer, I want the system to match validated test cases, so that I can ensure correctness against the reference implementation.

#### Acceptance Criteria

1. THE Route_Planner SHALL produce results matching data/test_cases.csv for all 300 test cases
2. WHEN running test cases, THE Route_Planner SHALL match the expected route sequence
3. WHEN running test cases, THE Route_Planner SHALL match the expected total Real_Duration within 5% tolerance
4. WHEN running test cases, THE Route_Planner SHALL match the expected total fare exactly
5. IF any test case fails, THEN THE Route_Planner SHALL report which test case failed and the difference between expected and actual results
6. THE Route_Planner SHALL complete all 300 test cases in less than 30 seconds total

### Requirement 9: Console User Interface

**User Story:** As a user, I want a simple console interface, so that I can quickly enter addresses and see results.

#### Acceptance Criteria

1. WHEN the application starts, THE Route_Planner SHALL display a welcome message in Spanish
2. THE Route_Planner SHALL prompt the user to enter the origin address in Spanish
3. WHEN the user enters an origin address, THE Route_Planner SHALL prompt for the destination address in Spanish
4. WHEN both addresses are entered, THE Route_Planner SHALL compute and display the route
5. WHEN a route is displayed, THE Route_Planner SHALL prompt the user to search for another route or exit
6. WHEN the user chooses to exit, THE Route_Planner SHALL terminate gracefully
7. THE Route_Planner SHALL handle Ctrl+C interruption gracefully with a Spanish farewell message
8. FOR ALL user prompts and system messages, the text SHALL be in Spanish
9. FOR ALL displayed routes, the formatting SHALL be clear and easy to read with proper spacing and indentation

### Requirement 10: Performance Requirements

**User Story:** As a user, I want fast responses, so that I can plan my route without waiting.

#### Acceptance Criteria

1. THE Graph_Loader SHALL complete initialization in less than 1000 milliseconds
2. THE Dijkstra_Engine SHALL compute any route in less than 100 milliseconds
3. WHEN processing a complete user request (parse addresses, find stops, compute route, format output), THE Route_Planner SHALL complete in less than 200 milliseconds
4. THE Route_Planner SHALL use less than 512 MB of RAM during normal operation
5. FOR ALL valid routing queries, increasing the number of nodes or edges by 10% SHALL increase computation time by less than 15% (scalability property)

### Requirement 11: Data Validation and Integrity

**User Story:** As a developer, I want to validate loaded data integrity, so that I can detect data corruption or inconsistencies early.

#### Acceptance Criteria

1. WHEN loading bus_segments.csv, THE Graph_Loader SHALL validate that all required columns are present (route_id, stop_from, stop_to, travel_time, perceived_cost)
2. WHEN loading transit_lines.csv, THE Graph_Loader SHALL validate that all required columns are present (line_id, station_from, station_to, travel_time, mode)
3. WHEN loading walk_links.csv, THE Graph_Loader SHALL validate that all required columns are present (stop_from, stop_to, walk_time)
4. THE Graph_Loader SHALL validate that all travel times are non-negative
5. THE Graph_Loader SHALL validate that all perceived costs are greater than or equal to real durations
6. THE Graph_Loader SHALL validate that all stop IDs reference known Physical_Stops
7. IF any validation fails, THEN THE Graph_Loader SHALL terminate with a descriptive error message identifying the specific validation failure
8. FOR ALL loaded edges, perceived_cost >= real_duration SHALL hold (invariant property)
9. FOR ALL stop references in edges, the stop SHALL exist in the Physical_Stops collection (referential integrity property)

### Requirement 12: Configuration and Extensibility

**User Story:** As a developer, I want configurable parameters, so that I can adjust system behavior without recompiling.

#### Acceptance Criteria

1. THE Route_Planner SHALL load configuration from a properties file or command-line arguments
2. THE Route_Planner SHALL support configuring the Walk_Radius (default: 500 meters)
3. THE Route_Planner SHALL support configuring the maximum number of transfers (default: unlimited)
4. THE Route_Planner SHALL support configuring the Integration_Window duration (default: 120 minutes)
5. THE Route_Planner SHALL support configuring fare amounts for each Transit_Mode
6. THE Route_Planner SHALL support configuring walking speed (default: 4 km/h)
7. THE Route_Planner SHALL support configuring the data directory path
8. WHEN configuration values are invalid or missing, THE Route_Planner SHALL use documented default values
9. WHEN configuration is updated and the application is restarted, THE Route_Planner SHALL use the new configuration values

