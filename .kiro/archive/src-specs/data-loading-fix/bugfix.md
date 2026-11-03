# Bugfix Requirements Document

## Introduction

The application cannot load real transit data from the configured data directory, resulting in an empty graph and preventing the home screen from appearing. When users attempt to run the application with actual transit data files (`bus_segments.csv`, `transit_lines.csv`, `walk_links.csv`, and JSON files in `data/raw/`), the system fails to construct the graph properly due to multiple data loading issues: CSV schema mismatch, incorrect file path handling, broken JSON parsing regex, ignored configuration properties, and incomplete reference test suite execution. This prevents all 15 reference test cases from running and causes Maven test failures (7 failures, 1 error out of 332 tests).

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN GraphCsvReader attempts to read `bus_segments.csv`, `transit_lines.csv`, or `walk_links.csv` THEN the system fails to parse them because it expects schema `source_id,target_id,edge_kind,real_sec,perceived_cost` but the actual files contain raw transit data with different schemas

1.2 WHEN GraphBuilder attempts to load JSON files from the data directory THEN the system looks for `stops_1.json, stops_2.json, ...` in `data/` but the actual files are named `stops_0.json, stops_2000.json, stops_4000.json, stops_6000.json, stops_8000.json, stops_10000.json` in `data/raw/` subdirectory

1.3 WHEN ArcGisReader parses JSON features with nested geometry objects THEN the system's regex pattern `\{[^}]*"attributes"[^}]*\}` cuts at the first `}` character, excluding geometry from the match and resulting in coordinates = 0.0, causing stops to be dropped

1.4 WHEN Main class is invoked without command-line arguments THEN the system uses hardcoded `DEFAULT_DATA_DIR = "data"` instead of reading `data.directory` from config.properties, resulting in "No se encontró el directorio de datos" error

1.5 WHEN Maven executes tests THEN the system does not run ReferenceTestSuite because its class name doesn't match the `*Test` pattern, and even if run manually it only processes the first 5 test cases without asserting results

1.6 WHEN the graph is loaded with these bugs present THEN the system reports "El grafo está vacío" (0 stops, 0 states, 0 edges) instead of the expected ~13,895 states and ~82,160 edges

### Expected Behavior (Correct)

2.1 WHEN GraphCsvReader and GraphBuilder process `bus_segments.csv`, `transit_lines.csv`, and `walk_links.csv` THEN the system SHALL construct graph edges following the specification in docs/graph-structure.md, creating ride, board, alight, and walk edges with speeds, waits, and penalties from config.properties

2.2 WHEN GraphBuilder loads stop and route data THEN the system SHALL correctly locate and parse JSON files named `stops_0.json, stops_2000.json, stops_4000.json, stops_6000.json, stops_8000.json, stops_10000.json` and `routes.json` in the `data/raw/` subdirectory

2.3 WHEN ArcGisReader parses JSON features THEN the system SHALL correctly extract both attributes and nested geometry objects, preserving coordinate data for all stops

2.4 WHEN Main class is invoked without command-line arguments THEN the system SHALL read the `data.directory` property from config.properties and use it as the data directory path

2.5 WHEN Maven executes tests THEN the system SHALL run ReferenceTestSuite as part of the test suite, process all 15 test cases from test_cases.csv, and assert that each result matches expected values (minutes ±2, cost ±100, transfers exact, vehicle sequence exact)

2.6 WHEN the graph is loaded with real data THEN the system SHALL report approximately 13,895 states and 82,160 edges as specified in docs/graph-structure.md, and display the home screen successfully

2.7 WHEN `mvn test` runs THEN it SHALL finish with 0 failures and 0 errors, and running Main with no arguments SHALL print the home screen

### Unchanged Behavior (Regression Prevention)

3.1 WHEN GraphCsvReader reads files that already match the expected schema `source_id,target_id,edge_kind,real_sec,perceived_cost` THEN the system SHALL CONTINUE TO parse them correctly

3.2 WHEN ArcGisReader parses simple JSON features without nested geometry objects THEN the system SHALL CONTINUE TO extract attributes correctly

3.3 WHEN Main class is invoked WITH a command-line argument specifying a data directory THEN the system SHALL CONTINUE TO use that argument as the data directory path

3.4 WHEN Maven executes existing unit tests (ArcGisReaderTest with 2 currently failing tests and StopArcGisReaderTest with 5 currently failing tests) THEN the system SHALL fix the ArcGisReader code to make these 7 tests pass without changing the tests' expected values

3.5 WHEN the Router processes queries after the graph is loaded THEN the system SHALL CONTINUE TO find paths using Dijkstra's algorithm with the same routing logic

3.6 WHEN test_cases.csv is read THEN the system SHALL CONTINUE TO respect the expected values without modification
