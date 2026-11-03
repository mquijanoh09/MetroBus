# Data Loading Bugfix Design

## Overview

The application fails to load real transit data due to multiple data loading issues: CSV schema mismatch, incorrect file path handling, broken JSON parsing regex, ignored configuration properties, and incomplete reference test suite execution. This design document formalizes the bug conditions and provides a comprehensive fix implementation that enables the system to construct a complete transit graph (~13,895 states, ~82,160 edges) from raw data files, run all 15 reference test cases, and display the home screen successfully.

## Glossary

- **Bug_Condition (C)**: The set of inputs (data files, configuration, test invocations) that trigger data loading failures
- **Property (P)**: The desired behavior for buggy inputs - successful graph construction with expected dimensions
- **Preservation**: Existing functionality that must remain unchanged by the fix
- **GraphCsvReader**: Current reader that expects pre-computed edge CSV format
- **Raw Transit Data**: Source CSV files with actual transit network structure (route segments, transit lines, walk links)
- **ArcGIS JSON**: Paginated JSON files from ArcGIS REST API containing stops and routes
- **State**: A graph node representing a location/transit position (WALK, BUS_STOP, or METRO_STATION type)
- **Edge**: A graph connection representing movement (WALK, BOARD, RIDE, or ALIGHT type)

## Bug Details

### Bug Condition

The data loading system fails when attempting to use real transit data files in their actual formats and locations. The current implementation expects pre-computed graph files that don't exist, uses incorrect file paths for JSON data, and contains a JSON parsing bug that drops geometry information.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type DataLoadingAttempt (dataDirectory, csvFiles, jsonFiles, configProps, testInvocation)
  OUTPUT: boolean
  
  RETURN (csvFilesMatchRawTransitSchema(csvFiles) AND NOT csvFilesMatchPrecomputedSchema(csvFiles))
         OR (jsonFilesInRawSubdirectory(jsonFiles) AND readerLooksInWrongLocation())
         OR (jsonHasNestedGeometry(jsonFiles) AND regexCutsAtFirstBrace())
         OR (configPropsHasDataDirectory(configProps) AND mainUsesHardcodedPath())
         OR (testInvocationTargetsReferenceTestSuite(testInvocation) AND classNameMismatchesMavenPattern())
END FUNCTION
```

### Examples

1. **CSV Schema Mismatch**:
   - **Actual**: `bus_segments.csv` contains `route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method`
   - **Expected by GraphCsvReader**: `source_id,target_id,edge_kind,real_sec,perceived_cost`
   - **Result**: GraphCsvReader throws IllegalArgumentException "Missing source ID field"

2. **JSON File Location**:
   - **Actual**: Files are `data/raw/stops_0.json, data/raw/stops_2000.json, ...`
   - **Expected by ArcGisReader.readPaginated**: `data/stops_1.json, data/stops_2.json, ...`
   - **Result**: "No paginated files found" warning, 0 stops loaded

3. **JSON Parsing Bug**:
   - **Actual**: Feature JSON contains `{"attributes":{...},"geometry":{"x":-75.567,"y":6.234}}`
   - **Regex Match**: `\{[^}]*"attributes"[^}]*\}` matches `{"attributes":{...}}` (cuts at first `}`)
   - **Result**: Geometry is excluded, coordinates = 0.0, stop is dropped

4. **Config Property Ignored**:
   - **Actual**: `config.properties` has `data.directory=C:\\Users\\marti\\Downloads\\transporte-medellin\\kiro\\data`
   - **Main.java behavior**: Uses hardcoded `DEFAULT_DATA_DIR = "data"`
   - **Result**: "No se encontró el directorio de datos" error

5. **Reference Test Suite Not Run**:
   - **Actual**: Class named `ReferenceTestSuite.java`
   - **Maven Surefire pattern**: `*Test.java`
   - **Result**: Test class is not discovered, 15 reference cases never run

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- GraphCsvReader's ability to read pre-computed edge files (if they exist) must continue to work
- ArcGisReader's parsing of simple JSON features without nested geometry must continue to work
- Main class accepting command-line data directory argument must continue to work
- Existing unit tests (ArcGisReaderTest, StopArcGisReaderTest) assertions must remain unchanged - only fix code to make them pass
- Router's Dijkstra algorithm and routing logic must remain completely unchanged
- test_cases.csv expected values must not be modified

**Scope:**
All inputs that do NOT involve raw transit data files should be completely unaffected by this fix. This includes:
- Pre-computed CSV files with edge schema (if provided)
- Simple JSON files without nested geometry
- Explicit command-line data directory arguments
- All routing queries after graph construction

## Hypothesized Root Cause

Based on the bug description, the implementation issues are:

1. **Abstraction Level Mismatch**: GraphCsvReader was designed for pre-computed graph edges, but the actual data files contain raw transit network structure that needs to be **transformed** into graph edges. The current code expects post-processing files that don't exist.

2. **Hardcoded File Discovery Logic**: ArcGisReader.readPaginated uses `String.format("%s_%d%s", baseFilename, pageNumber, extension)` starting at pageNumber=1, but the actual files use offset-based naming (stops_0.json = offset 0, stops_2000.json = offset 2000) and are located in a `data/raw/` subdirectory.

3. **Greedy Regex Limitation**: The feature pattern `\{[^}]*"attributes"[^}]*\}` uses `[^}]*` (match anything except `}`), which stops at the first closing brace. When geometry is nested like `{"attributes":{...},"geometry":{"x":...}}`, it cuts after the attributes object's closing brace, excluding geometry.

4. **Configuration Reading Not Implemented**: Main.java has a hardcoded DEFAULT_DATA_DIR constant but never reads config.properties, even though the file exists and contains a `data.directory` property.

5. **Test Naming Convention**: Maven Surefire's default pattern `**/*Test.java` doesn't match `ReferenceTestSuite.java`. Additionally, the test only processes 5 cases instead of all 15, and doesn't assert results.

## Correctness Properties

Property 1: Bug Condition - Data Loading From Raw Transit Files

_For any_ data loading attempt where CSV files contain raw transit schemas (bus_segments, transit_lines, walk_links), JSON files are in data/raw/ subdirectory with offset-based naming, and config.properties contains data.directory property, the fixed system SHALL construct a complete transit graph with approximately 13,895 states and 82,160 edges, run all 15 reference test cases successfully, and display the home screen.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7**

Property 2: Preservation - Pre-Computed Data Compatibility

_For any_ data loading attempt where CSV files already match the pre-computed edge schema (source_id, target_id, edge_kind, real_sec, perceived_cost), JSON files use simple geometry format, or command-line arguments specify data directory, the fixed system SHALL produce exactly the same behavior as the original system, preserving backward compatibility with pre-computed data formats.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

#### 1. Create Specialized CSV Readers for Raw Transit Data

**New Files**: 
- `src/main/java/co/medellin/transit/data/BusSegmentReader.java`
- `src/main/java/co/medellin/transit/data/TransitLineReader.java`
- `src/main/java/co/medellin/transit/data/WalkLinkReader.java`

**BusSegmentReader Implementation**:
- Extends `CsvReader<BusSegment>` 
- Parses schema: `route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method`
- Returns `BusSegment` record with: routeId (String), direction (int), fromStopId (int), toStopId (int), fromSeq (int), toSeq (int), straightM (double), roadM (double), usedM (double), method (String)
- Validates: routeId not empty, direction in {0,1}, stop IDs ≥ 0, sequence numbers ≥ 0, distances > 0

**TransitLineReader Implementation**:
- Extends `CsvReader<TransitSegment>`
- Parses schema: `line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next`
- Returns `TransitSegment` record with: line (String), mode (String), order (int), station (String), coordinate (Coordinate), distToNextM (double), secToNext (int)
- Validates: line not empty, mode in {Metro, Metroplus, Tranvía, Metrocable}, order ≥ 0, station not empty, coordinates in Medellín bounds (6.15-6.35N, -75.65--75.50W), distances ≥ 0, time ≥ 0

**WalkLinkReader Implementation**:
- Extends `CsvReader<WalkLink>`
- Parses schema: `place_a,name_a,place_b,name_b,kind,straight_m,walk_sec`
- Returns `WalkLink` record with: placeA (int), nameA (String), placeB (int), nameB (String), kind (String), straightM (double), walkSec (int)
- Validates: place IDs ≥ 0, names not empty, kind in {stop-to-stop, stop-to-station, station-to-station}, distance > 0, walkSec > 0

#### 2. Update GraphBuilder to Construct Edges From Raw Data

**File**: `src/main/java/co/medellin/transit/graph/GraphBuilder.java`

**Specific Changes**:

1. **Load Configuration Properties**:
   ```java
   // Load config at start of build() method
   Parameters params = new Parameters(); // Already loads config.properties
   double walkSpeedMps = params.getWalkingSpeedMps(); // 1.25 m/s
   double walkStreetFactor = params.getWalkStreetFactor(); // 1.3
   double walkPenaltyMultiplier = params.getWalkPenaltyMultiplier(); // 1.5
   int walkPenaltyConstant = params.getWalkPenaltyConstant(); // 30s
   int boardingPenaltySec = params.getBoardingPenaltySec(); // 120s
   ```

2. **Replace GraphCsvReader with Specialized Readers**:
   ```java
   // Remove: GraphCsvReader edgeReader = new GraphCsvReader();
   
   // Add: Read bus segments
   BusSegmentReader busSegmentReader = new BusSegmentReader();
   List<BusSegment> busSegments = busSegmentReader.read(dataDir.resolve("bus_segments.csv"));
   
   // Add: Read transit lines
   TransitLineReader transitLineReader = new TransitLineReader();
   List<TransitSegment> transitSegments = transitLineReader.read(dataDir.resolve("transit_lines.csv"));
   
   // Add: Read walk links
   WalkLinkReader walkLinkReader = new WalkLinkReader();
   List<WalkLink> walkLinks = walkLinkReader.read(dataDir.resolve("walk_links.csv"));
   ```

3. **Build State Lookup Maps**:
   ```java
   // Create maps for state ID assignment
   Map<String, Integer> stopIdToWalkStateId = new HashMap<>(); // stop_id -> WALK state
   Map<String, Integer> stopRouteToStateId = new HashMap<>(); // "stop_id:route_id:dir" -> BUS_STOP state
   Map<String, Integer> stationLineToStateId = new HashMap<>(); // "station:line:dir" -> METRO_STATION state
   
   int nextStateId = 0;
   
   // Assign WALK state IDs for all stops
   for (Stop stop : stops) {
       stopIdToWalkStateId.put(String.valueOf(stop.getId()), nextStateId);
       Place place = new Place(stop.getName(), stop.getCoordinate());
       State walkState = new State(nextStateId, place, StateType.WALK, null, null);
       graph.addState(walkState);
       nextStateId++;
   }
   ```

4. **Generate RIDE Edges from Bus Segments**:
   ```java
   for (BusSegment segment : busSegments) {
       // Get or create BUS_STOP states for from and to stops
       String fromKey = segment.fromStopId() + ":" + segment.routeId() + ":" + segment.direction();
       String toKey = segment.toStopId() + ":" + segment.routeId() + ":" + segment.direction();
       
       int fromStateId = stopRouteToStateId.computeIfAbsent(fromKey, k -> {
           // Create BUS_STOP state
           Integer stopWalkState = stopIdToWalkStateId.get(String.valueOf(segment.fromStopId()));
           Place place = graph.getState(stopWalkState).getPlace();
           State busStopState = new State(nextStateId, place, StateType.BUS_STOP, 
               segment.routeId(), segment.direction());
           graph.addState(busStopState);
           return nextStateId++;
       });
       
       int toStateId = stopRouteToStateId.computeIfAbsent(toKey, k -> {
           // Similar creation for to stop
           // ...
       });
       
       // Calculate travel time: usedM / (bus_speed_kmh / 3.6) + dwellTimeSec
       double busSpeedMps = getBusSpeed(segment.routeId(), params) / 3.6;
       int realSec = (int)(segment.usedM() / busSpeedMps) + params.getDwellTimeSec();
       int perceivedCost = realSec; // RIDE edges have no penalty
       
       Edge rideEdge = new Edge(fromStateId, toStateId, EdgeKind.RIDE, realSec, perceivedCost);
       graph.addEdge(rideEdge);
   }
   ```

5. **Generate RIDE Edges from Transit Lines**:
   ```java
   for (TransitSegment segment : transitSegments) {
       // Similar to bus segments but for Metro/Metroplus/Tranvía/Metrocable
       // Create METRO_STATION states
       // Use segment.secToNext() directly (already computed in source data)
       // perceivedCost = realSec (no penalty for RIDE edges)
   }
   ```

6. **Generate BOARD Edges**:
   ```java
   for (Map.Entry<String, Integer> entry : stopRouteToStateId.entrySet()) {
       String[] parts = entry.getKey().split(":");
       int stopId = Integer.parseInt(parts[0]);
       int walkStateId = stopIdToWalkStateId.get(String.valueOf(stopId));
       int busStopStateId = entry.getValue();
       
       // WALK -> BUS_STOP (boarding)
       int waitTimeSec = params.getWaitTimeBus(); // 300s from config
       int realSec = waitTimeSec;
       int perceivedCost = waitTimeSec + boardingPenaltySec; // +120s penalty
       
       Edge boardEdge = new Edge(walkStateId, busStopStateId, EdgeKind.BOARD, realSec, perceivedCost);
       graph.addEdge(boardEdge);
   }
   
   // Similar for METRO_STATION states with appropriate wait times
   ```

7. **Generate ALIGHT Edges**:
   ```java
   for (Map.Entry<String, Integer> entry : stopRouteToStateId.entrySet()) {
       String[] parts = entry.getKey().split(":");
       int stopId = Integer.parseInt(parts[0]);
       int walkStateId = stopIdToWalkStateId.get(String.valueOf(stopId));
       int busStopStateId = entry.getValue();
       
       // BUS_STOP -> WALK (alighting)
       // Get the actual travel time from the last RIDE edge to this state
       int realSec = getAlightTimeSec(busStopStateId, graph); // Extract from incoming RIDE edge
       int perceivedCost = 0; // ALIGHT edges have 0 perceived cost
       
       Edge alightEdge = new Edge(busStopStateId, walkStateId, EdgeKind.ALIGHT, realSec, perceivedCost);
       graph.addEdge(alightEdge);
   }
   ```

8. **Generate WALK Edges from Walk Links**:
   ```java
   for (WalkLink link : walkLinks) {
       int fromWalkState = stopIdToWalkStateId.get(String.valueOf(link.placeA()));
       int toWalkState = stopIdToWalkStateId.get(String.valueOf(link.placeB()));
       
       // Use walk_sec from file if available, otherwise calculate
       int realSec;
       if (link.walkSec() > 0) {
           realSec = link.walkSec();
       } else {
           // Calculate: (straight_m × walkStreetFactor) / walkSpeedMps
           realSec = (int)((link.straightM() * walkStreetFactor) / walkSpeedMps);
       }
       
       // Apply walk penalty: realSec × walkPenaltyMultiplier + walkPenaltyConstant
       int perceivedCost = (int)(realSec * walkPenaltyMultiplier) + walkPenaltyConstant;
       
       Edge walkEdge = new Edge(fromWalkState, toWalkState, EdgeKind.WALK, realSec, perceivedCost);
       graph.addEdge(walkEdge);
       
       // Add reverse edge
       Edge reverseWalkEdge = new Edge(toWalkState, fromWalkState, EdgeKind.WALK, realSec, perceivedCost);
       graph.addEdge(reverseWalkEdge);
   }
   ```

9. **Helper Method for Bus Speed Lookup**:
   ```java
   private double getBusSpeed(String routeId, Parameters params) {
       // Look up route type from routes list
       // Return appropriate speed from config:
       // - Directa Centro, Circular: 15 km/h
       // - Integrada, Directa No Centro: 18 km/h
   }
   ```

#### 3. Fix ArcGIS JSON Parsing

**File**: `src/main/java/co/medellin/transit/data/ArcGisReader.java`

**Specific Changes**:

1. **Replace Greedy Regex with Brace Counting**:
   ```java
   // Remove: private static final Pattern FEATURE_PATTERN = Pattern.compile("\\{[^}]*\"attributes\"[^}]*\\}");
   
   // Replace extractFeaturesArray() and feature parsing with proper brace counting:
   private List<String> extractFeatureObjects(String featuresSection) {
       List<String> features = new ArrayList<>();
       int pos = 0;
       
       while (pos < featuresSection.length()) {
           // Skip whitespace and commas
           while (pos < featuresSection.length() && 
                  (Character.isWhitespace(featuresSection.charAt(pos)) || 
                   featuresSection.charAt(pos) == ',')) {
               pos++;
           }
           
           if (pos >= featuresSection.length()) break;
           
           // Check for opening brace
           if (featuresSection.charAt(pos) == '{') {
               int start = pos;
               int braceCount = 1;
               pos++;
               
               // Count braces to find matching close
               while (pos < featuresSection.length() && braceCount > 0) {
                   char c = featuresSection.charAt(pos);
                   if (c == '{') {
                       braceCount++;
                   } else if (c == '}') {
                       braceCount--;
                   } else if (c == '"') {
                       // Skip string literals to avoid counting braces inside strings
                       pos++;
                       while (pos < featuresSection.length()) {
                           char sc = featuresSection.charAt(pos);
                           if (sc == '"' && featuresSection.charAt(pos - 1) != '\\') {
                               break;
                           }
                           pos++;
                       }
                   }
                   pos++;
               }
               
               if (braceCount == 0) {
                   features.add(featuresSection.substring(start, pos));
               }
           } else {
               pos++;
           }
       }
       
       return features;
   }
   ```

2. **Update parseFeature Loop**:
   ```java
   // In read() method, replace:
   // Matcher featureMatcher = FEATURE_PATTERN.matcher(featuresSection);
   // while (featureMatcher.find()) { ... }
   
   // With:
   List<String> featureObjects = extractFeatureObjects(featuresSection);
   int featureNumber = 0;
   
   for (String featureJson : featureObjects) {
       featureNumber++;
       try {
           Optional<T> parsed = parseFeature(featureJson, featureNumber);
           parsed.ifPresent(results::add);
       } catch (Exception e) {
           LOGGER.warning(String.format(
               "Skipping malformed feature in %s at feature %d: %s",
               jsonPath.getFileName(),
               featureNumber,
               e.getMessage()
           ));
       }
   }
   ```

#### 4. Fix JSON File Discovery for Paginated Files

**File**: `src/main/java/co/medellin/transit/data/ArcGisReader.java`

**Method**: `readPaginated()`

**Specific Changes**:

1. **Look in data/raw/ Subdirectory**:
   ```java
   public List<T> readPaginated(Path baseDir, String baseFilename, String extension) throws IOException {
       List<T> allResults = new ArrayList<>();
       
       // Check for raw subdirectory
       Path rawDir = baseDir.resolve("raw");
       Path searchDir = Files.exists(rawDir) ? rawDir : baseDir;
       
       LOGGER.info(String.format("Searching for %s files in: %s", baseFilename, searchDir));
   ```

2. **Support Offset-Based Naming Pattern**:
   ```java
       // Try offset-based naming first (stops_0.json, stops_2000.json, ...)
       List<Path> matchingFiles = Files.list(searchDir)
           .filter(p -> p.getFileName().toString().matches(
               Pattern.quote(baseFilename) + "_\\d+" + Pattern.quote(extension)))
           .sorted()
           .collect(Collectors.toList());
       
       if (!matchingFiles.isEmpty()) {
           LOGGER.info(String.format("Found %d %s files with offset-based naming", 
               matchingFiles.size(), baseFilename));
           
           for (Path filePath : matchingFiles) {
               List<T> pageResults = read(filePath);
               allResults.addAll(pageResults);
           }
           
           LOGGER.info(String.format("Loaded %d total features from %d files of %s",
               allResults.size(), matchingFiles.size(), baseFilename));
           
           return allResults;
       }
       
       // Fall back to old page-based naming (stops_1.json, stops_2.json, ...)
       int pageNumber = 1;
       while (true) {
           String filename = String.format("%s_%d%s", baseFilename, pageNumber, extension);
           Path filePath = searchDir.resolve(filename);
           
           if (!Files.exists(filePath)) {
               if (pageNumber == 1) {
                   LOGGER.warning(String.format(
                       "No paginated files found for %s in %s",
                       baseFilename,
                       searchDir
                   ));
               }
               break;
           }
           
           List<T> pageResults = read(filePath);
           allResults.addAll(pageResults);
           pageNumber++;
       }
   ```

#### 5. Read Configuration Properties in Main

**File**: `src/main/java/co/medellin/transit/Main.java`

**Specific Changes**:

1. **Load config.properties Early**:
   ```java
   public static void main(String[] args) {
       try {
           configureLogging();
           
           long startTime = System.nanoTime();
           // ... (existing startup tracking)
           
           LOGGER.info("Initializing Medellín Transit Routing System...");
           
           // NEW: Load configuration properties
           String dataDirPath;
           if (args.length > 0) {
               // Command-line argument takes precedence
               dataDirPath = args[0];
               LOGGER.info("Using data directory from command-line: " + dataDirPath);
           } else {
               // Load from config.properties
               try (InputStream input = Main.class.getClassLoader()
                       .getResourceAsStream("config.properties")) {
                   if (input != null) {
                       Properties props = new Properties();
                       props.load(input);
                       dataDirPath = props.getProperty("data.directory", DEFAULT_DATA_DIR);
                       LOGGER.info("Using data directory from config.properties: " + dataDirPath);
                   } else {
                       dataDirPath = DEFAULT_DATA_DIR;
                       LOGGER.info("config.properties not found, using default: " + dataDirPath);
                   }
               } catch (IOException e) {
                   LOGGER.log(Level.WARNING, "Could not load config.properties", e);
                   dataDirPath = DEFAULT_DATA_DIR;
               }
           }
           
           Path dataDir = Paths.get(dataDirPath);
   ```

2. **Add Import Statements**:
   ```java
   import java.io.InputStream;
   import java.util.Properties;
   ```

#### 6. Fix Reference Test Suite Execution

**File**: Rename `src/test/java/.../ReferenceTestSuite.java` to `ReferenceTest.java`

**Specific Changes**:

1. **Rename Class**:
   ```java
   // Change: public class ReferenceTestSuite {
   // To:     public class ReferenceTest {
   public class ReferenceTest {
   ```

2. **Process All Test Cases**:
   ```java
   @Test
   public void testReferenceRoutingScenarios() throws Exception {
       // Remove: int testCount = 0;
       // Remove: int maxTests = 5;
       
       List<TestCase> testCases = loadTestCases();
       
       // Process ALL test cases
       for (TestCase testCase : testCases) {
           // Remove: if (testCount++ >= maxTests) break;
           
           System.out.println("\nTest Case: " + testCase.description());
           // ... (existing processing)
       }
   }
   ```

3. **Add Assertions for All Fields**:
   ```java
   private void assertRouteMatches(Trip trip, TestCase expected) {
       // Assert travel time (±2 minutes tolerance)
       int actualMinutes = trip.getTotalTime() / 60;
       int expectedMinutes = expected.expectedMinutes();
       assertEquals(expectedMinutes, actualMinutes, 2, 
           "Travel time mismatch for: " + expected.description());
       
       // Assert cost (±100 COP tolerance)
       int actualCost = trip.getFare();
       int expectedCost = expected.expectedCost();
       assertEquals(expectedCost, actualCost, 100,
           "Cost mismatch for: " + expected.description());
       
       // Assert transfers (exact match)
       int actualTransfers = trip.getTransfers();
       int expectedTransfers = expected.expectedTransfers();
       assertEquals(expectedTransfers, actualTransfers,
           "Transfer count mismatch for: " + expected.description());
       
       // Assert vehicle sequence (exact match on comma-separated string)
       String actualVehicles = trip.getSteps().stream()
           .filter(s -> s.getType() == StepType.RIDE)
           .map(s -> s.getRouteName())
           .collect(Collectors.joining(","));
       String expectedVehicles = expected.expectedVehicles();
       assertEquals(expectedVehicles, actualVehicles,
           "Vehicle sequence mismatch for: " + expected.description());
   }
   ```

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bugs on unfixed code, then verify the fixes work correctly and preserve existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bugs BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Create minimal test data files that match the real data formats, then attempt to load them with the UNFIXED code to observe failures and understand the root causes.

**Test Cases**:
1. **CSV Schema Mismatch Test**: Create `bus_segments.csv` with raw transit schema, run GraphCsvReader.read() on unfixed code (will fail with "Missing source ID field")
2. **JSON Location Test**: Place stops_0.json in data/raw/ subdirectory, call readPaginated() on unfixed code (will fail with "No paginated files found")
3. **Nested Geometry Test**: Create JSON with nested geometry `{"attributes":{...},"geometry":{"x":-75.567,"y":6.234}}`, parse with unfixed ArcGisReader (will fail to extract coordinates, resulting in 0.0)
4. **Config Property Test**: Set data.directory in config.properties, run Main without arguments on unfixed code (will fail with "No se encontró el directorio de datos")
5. **Test Suite Naming Test**: Run `mvn test` on unfixed code (will skip ReferenceTestSuite because name doesn't match pattern)

**Expected Counterexamples**:
- GraphCsvReader throws IllegalArgumentException when reading raw transit CSV
- ArcGisReader returns empty list when files are in data/raw/ subdirectory
- StopArcGisReader creates stops with coordinate (0.0, 0.0) when geometry is nested
- Main uses hardcoded "data" path even when config.properties exists
- Maven reports N tests run but ReferenceTestSuite is not included

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := loadTransitData_fixed(input)
  ASSERT result.stateCount ≈ 13895
  ASSERT result.edgeCount ≈ 82160
  ASSERT result.homeScreenDisplayed = true
  ASSERT result.mavenTestFailures = 0
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT loadTransitData_original(input) = loadTransitData_fixed(input)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for all non-buggy inputs

**Test Plan**: Observe behavior on UNFIXED code first for pre-computed CSV files and simple JSON, then write property-based tests capturing that behavior.

**Test Cases**:
1. **Pre-Computed CSV Preservation**: Create CSV with edge schema (source_id, target_id, edge_kind, real_sec, perceived_cost), verify GraphCsvReader continues to parse it correctly after fix
2. **Simple JSON Preservation**: Create JSON with flat geometry (no nesting), verify ArcGisReader extracts coordinates correctly before and after fix
3. **Command-Line Arg Preservation**: Run Main with explicit data directory argument, verify it uses the argument path before and after fix
4. **Existing Unit Tests Preservation**: Run ArcGisReaderTest and StopArcGisReaderTest, verify that fixed code makes failing tests pass without changing test expectations
5. **Routing Logic Preservation**: Load graph, run routing queries, verify Dijkstra algorithm produces identical paths before and after fix (only graph construction changed, not routing)

### Unit Tests

- Test each specialized CSV reader (BusSegmentReader, TransitLineReader, WalkLinkReader) with valid and invalid data
- Test GraphBuilder edge generation for each edge type (WALK, BOARD, RIDE, ALIGHT)
- Test ArcGisReader.extractFeatureObjects() with nested JSON structures
- Test ArcGisReader.readPaginated() with offset-based file naming
- Test Main configuration loading with various combinations of config.properties and command-line args
- Test ReferenceTest execution with all 15 test cases

### Property-Based Tests

- Generate random bus segments with valid schemas, verify BusSegmentReader parses all successfully
- Generate random JSON features with various nesting levels, verify ArcGisReader extracts complete features
- Generate random stop/route combinations, verify GraphBuilder creates correct number of states
- Generate random edge configurations, verify GraphBuilder calculates costs according to formulas from config.properties

### Integration Tests

- Full graph loading test with real data files (bus_segments.csv, transit_lines.csv, walk_links.csv, stops_*.json, routes.json)
- Verify graph dimensions match expected values (~13,895 states, ~82,160 edges)
- Run complete routing query after loading real data
- Run `mvn test` and verify 0 failures, 0 errors
- Run Main with no arguments and verify home screen displays
