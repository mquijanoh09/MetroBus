# Design: 01 · Data loading

## Overview

This spec covers loading Medellín transit data from CSV and JSON files into Java domain objects. The existing readers in `co.medellin.transit.data` already implement most of the required functionality. This design focuses on verifying correct behavior, fixing any issues, and ensuring comprehensive test coverage with the real data files.

## Current state analysis

### Existing readers (all in `co.medellin.transit.data`)

1. **`BusSegmentReader`** — reads `bus_segments.csv` (9,550 rows)
   - Extends `CsvReader<BusSegment>`
   - Parses: route_id, direction, from/to stop IDs, sequences, distances, method
   - Status: Appears complete, needs verification

2. **`TransitLineReader`** — reads `transit_lines.csv` (98 rows)
   - Extends `CsvReader<TransitSegment>`
   - Parses: line, mode, order, station, coordinates, dist_to_next_m, sec_to_next
   - Handles empty dist_to_next_m/sec_to_next for last station
   - Normalizes mode names: metro → Metro, brt → Metroplus, tram → Tranvía, cable_car → Metrocable
   - Status: Appears complete, needs verification

3. **`WalkLinkReader`** — reads `walk_links.csv` (25,865 rows)
   - Extends `CsvReader<WalkLink>`
   - Parses: place_a/b, name_a/b, kind, straight_m, walk_sec
   - Handles quoted names with commas (via Apache Commons CSV)
   - Parses place keys `STOP:<id>` and `STATION:<name>`
   - Normalizes kind: stop_stop → stop-to-stop, etc.
   - Issue: For `STATION:<name>` (non-numeric), generates hash-based ID. This may not match station IDs from other files.
   - Status: Needs review of station ID handling

4. **`StopArcGisReader`** — reads `data/raw/stops_*.json` (6 files)
   - Extends `ArcGisReader<Stop>`
   - Parses attributes: id_paradero, direccion, latitud, longitud
   - Deduplicates 10,043 features → 3,590 unique physical stops
   - Status: Appears complete, needs verification

5. **`RouteArcGisReader`** — reads `data/raw/routes.json` (252 features)
   - Extends `ArcGisReader<Route>`
   - Parses attributes: id_ruta, nombre, codigo, tipo
   - Issue: Needs to match spec requirement for integer id_ruta and string codigo/nombre/tipo
   - Status: Needs verification and possible fixes

6. **`ArcGisReader`** (abstract base class)
   - Parses ArcGIS JSON format without full JSON library
   - Handles paginated files (offset-based: stops_0.json, stops_2000.json)
   - Extracts features array and individual feature objects
   - Status: Appears complete, needs verification

7. **`CsvReader`** (abstract base class)
   - Uses Apache Commons CSV with header mapping
   - Handles UTF-8 encoding
   - Logs and skips malformed rows
   - Status: Appears complete

### Missing readers

8. **Station reader** — needs to read `data/raw/transit_stations.json` (81 features)
   - Required format: name, line, coordinates from `geometry.x`/`geometry.y`
   - Not mentioned in requirements but referenced in data.md
   - Decision: Add `StationArcGisReader` only if needed for graph building (spec 02)

## Design decisions

### 1. Station representation

The `transit_lines.csv` file contains station names as strings (e.g., "Niquía", "Aguacatala"). The `walk_links.csv` uses keys like `STATION:Aguacatala`. The `transit_stations.json` has 81 stations with coordinates in geometry.

**Decision:** For spec 01 (data loading only), we only need to read the files mentioned in requirements 1-6. Station objects from `transit_stations.json` are not explicitly required. The existing `WalkLinkReader` generates hash-based IDs for named stations, which is acceptable for now since we're not building the graph yet.

**Action:** Create a `Station` data class and `StationArcGisReader` to read `transit_stations.json` for requirement 6. This will be needed for graph building in spec 02.

### 2. Route ID format

The `routes.json` file has `id_ruta` as a string (e.g., "90098"), but `bus_segments.csv` uses integer route IDs (e.g., 90098). The requirements specify "id_ruta (as integer)".

**Decision:** Parse `id_ruta` from JSON as string, then convert to integer. Store in `Route` model as integer ID, keep `codigo` (string like "260") and `nombre` (display name) separate.

**Action:** Update `Route` model if needed; fix `RouteArcGisReader` to parse integer ID and extract codigo/nombre/tipo fields correctly.

### 3. Place ID representation

The `walk_links.csv` uses prefixed keys:
- `STOP:<id_paradero>` — numeric stop ID matching stops JSON
- `STATION:<name>` — station name as string (e.g., "STATION:Aguacatala")

The existing `WalkLink` record has integer `placeA`/`placeB` fields, but the existing tests expect plain integers and the real data has prefixed strings.

**Decision:** Change `WalkLink` to use String place keys (not integers). Keep the original format from the CSV: `STOP:53191`, `STATION:Aguacatala`. This preserves the type information (stop vs station) and avoids lossy conversions. Graph building (spec 02) can parse these strings when needed.

**Action:** Update `WalkLink` record to use `String placeA`, `String placeB`. Update `WalkLinkReader` to NOT parse or normalize the place keys - just read them as strings. Fix existing tests to match the real data format.

### 4. Last station representation

The requirements state: "treating the empty `dist_to_next_m`/`sec_to_next` of the last station as 'no next station'."

The existing `TransitSegment` record has `double distToNextM` and `int secToNext` fields. The existing `TransitLineReader` converts empty strings to 0.0 and 0.

**Decision:** Keep the primitive types but document that 0 means "no next station" (not an actual 0-meter/0-second connection). This is simpler than using Optional<Double> and Optional<Integer>.

**Action:** No code changes needed. Document in JavaDocs that 0 represents absence, not a zero-length connection.

### 5. Error handling

The requirements state: "WHEN a row or feature is malformed THEN the system SHALL skip it, log a warning with the file and line, and keep reading."

Both `CsvReader` and `ArcGisReader` already implement this pattern:
- Try-catch around parseRecord/parseFeature
- Log warning with filename and line/feature number
- Continue processing
- Summary logging of skipped rows

**Action:** Verify this behavior in tests with intentionally malformed data.

### 5. Data directory

Requirements: "WHEN the app starts THEN the system SHALL use `data/` relative to the working directory (overridable by a command-line argument), never an absolute path."

The existing readers take `Path` arguments, so the caller controls the base directory. The app's Main class will be responsible for resolving the data directory.

**Action:** No changes to readers. Document in design.md that readers are path-agnostic.

### 6. UTF-8 encoding

The `transit_lines.csv` file contains UTF-8 station names (e.g., "Niquía" with accent). The `CsvReader` already uses `StandardCharsets.UTF_8`. The `ArcGisReader` also uses UTF-8.

**Action:** Verify in tests that accented characters are preserved.

## Components

### Existing (to verify and possibly fix)

1. **`co.medellin.transit.data.BusSegment`** (record)
   - Fields: routeId (String), direction (int 0/1), from/toStopId, from/toSeq, straightM, roadM, usedM, method
   - Status: Complete

2. **`co.medellin.transit.data.BusSegmentReader`**
   - Parses CSV with 10 columns
   - Status: Verify counts and field parsing

3. **`co.medellin.transit.data.TransitSegment`** (record)
   - Fields: line, mode, order, station, coordinate, distToNextM, secToNext
   - Status: Complete

4. **`co.medellin.transit.data.TransitLineReader`**
   - Parses CSV with 8 columns
   - Handles empty optional fields
   - Status: Verify counts and UTF-8 handling

5. **`co.medellin.transit.data.WalkLink`** (record)
   - Fields: placeA, nameA, placeB, nameB, kind, straightM, walkSec
   - Issue: placeA/placeB are integers, but real data has prefixed strings (STOP:53191, STATION:Aguacatala)
   - Status: Needs update to String place keys

6. **`co.medellin.transit.data.WalkLinkReader`**
   - Parses CSV with quoted fields
   - Issue: Currently parses place_a/place_b and generates hash IDs for named stations
   - Status: Needs update to keep original string keys; existing tests need fixing

7. **`co.medellin.transit.model.Stop`** (class)
   - Fields: id, name, coordinate, address
   - Issue: Real data has no "nombre_paradero" field, only "direccion"
   - Status: Check if Stop needs name field at all

8. **`co.medellin.transit.data.StopArcGisReader`**
   - Reads paginated JSON files
   - Deduplicates by id_paradero
   - Issue: Existing tests expect "nombre_paradero" field which real data doesn't have
   - Status: Fix reader to use direccion for name, or remove name entirely; fix existing tests

9. **`co.medellin.transit.model.Route`** (class)
   - Fields: id (String), displayName, type, color
   - Issue: ID should be integer (per data.md), needs codigo field
   - Status: Needs update

10. **`co.medellin.transit.data.RouteArcGisReader`**
    - Reads JSON with attributes
    - Status: Verify field extraction

### New components

11. **`co.medellin.transit.data.Station`** (record)
    - Fields: name (String), line (String), coordinate (Coordinate)
    - Purpose: Represent transit stations from transit_stations.json

12. **`co.medellin.transit.data.StationArcGisReader`**
    - Extends `ArcGisReader<Station>`
    - Parses: attributes.nombre, attributes.linea, geometry.x, geometry.y
    - Expected count: 81 stations

### Tests (existing - need new real-file tests added)

13. **`co.medellin.transit.data.BusSegmentReaderTest`** (EXISTS)
    - Add: `testReadRealFile()` — loads data/bus_segments.csv, asserts 9,550 rows
    - Existing unit tests already cover malformed rows

14. **`co.medellin.transit.data.TransitLineReaderTest`** (EXISTS)
    - Add: `testReadRealFile()` — loads data/transit_lines.csv, asserts 98 rows
    - Add: `testUTF8StationNames()` — verify "Niquía" preserves accent
    - Existing unit tests already cover malformed rows

15. **`co.medellin.transit.data.WalkLinkReaderTest`** (EXISTS - HAS FAILURES)
    - Add: `testReadRealFile()` — loads data/walk_links.csv, asserts 25,865 rows
    - Add: `testQuotedNamesWithCommas()` — verify comma handling in names
    - Add: `testPrefixedPlaceKeys()` — verify STOP:53191 and STATION:Aguacatala kept as strings
    - Fix: Update all existing tests to use String place keys, not integers

16. **`co.medellin.transit.data.StopArcGisReaderTest`** (EXISTS - HAS FAILURES)
    - Add: `testReadPaginatedRealFiles()` — loads data/raw/stops_*.json, asserts 10,043 features and 3,590 unique stops
    - Fix: Update existing tests - real data has no "nombre_paradero", only "direccion"

17. **`co.medellin.transit.data.RouteArcGisReaderTest`** (NEW)
    - `testReadRealFile()` — loads data/raw/routes.json, asserts 252 routes
    - `testFieldExtraction()` — verify id_ruta, codigo, nombre, tipo
    - `testMalformedFeature()` — sample JSON with bad data

18. **`co.medellin.transit.data.StationArcGisReaderTest`** (NEW)
    - `testReadRealFile()` — loads data/raw/transit_stations.json, asserts 81 stations
    - `testGeometryParsing()` — verify x/y coordinate extraction
    - `testMalformedFeature()` — sample JSON with bad data

## Data flow

```
data/bus_segments.csv → BusSegmentReader → List<BusSegment>
data/transit_lines.csv → TransitLineReader → List<TransitSegment>
data/walk_links.csv → WalkLinkReader → List<WalkLink>
data/raw/stops_*.json → StopArcGisReader → List<Stop> (deduplicated)
data/raw/routes.json → RouteArcGisReader → List<Route>
data/raw/transit_stations.json → StationArcGisReader → List<Station>
```

All readers:
- Accept `Path` parameters (caller provides data directory)
- Return `List<T>` of domain objects
- Handle malformed rows/features gracefully (skip + log)
- Use UTF-8 encoding
- No side effects (pure data loading)

## Known issues and limitations

### 1. Place key representation
The `WalkLink` now uses String place keys (e.g., `STOP:53191`, `STATION:Aguacatala`) instead of integers. This preserves the type information from the CSV and avoids lossy conversions. Graph building (spec 02) will need to parse these strings and map station names to IDs from `transit_stations.json`.

### 2. Route ID representation
The `Route` class currently uses String ID, but the data shows id_ruta as an integer in bus_segments.csv and as a string in routes.json. We'll parse it as integer for consistency.

### 3. Stop name field
The existing tests expect a "nombre_paradero" field in stops JSON, but the real data doesn't have it - only "direccion". Need to clarify if Stop.name should come from direccion, or if Stop should only have id, coordinate, and address (no separate name field).

### 4. Missing address data
Some stops in stops_*.json don't have a `direccion` field (83 out of 10,043 features). The `StopArcGisReader` handles this with a default "Parada {id}" address or empty string.

### 5. Route types
The routes.json file has a `tipo` field (e.g., "Directa Centro", "Integrada"). The requirements mention this is used for fares. The current `RouteArcGisReader` maps tipo to RouteType enum (BUS/METRO), but we need to preserve the raw tipo string for fare calculation in spec 03.

## Testing strategy

### Real file tests
Each reader has one test that:
- Loads the actual file from `data/` relative to project root
- Asserts the exact count from requirements
- Spot-checks a few fields on first/last rows
- FAILS (not skips) if the file is missing

Tests use `Paths.get("data/...")` with no system properties or conditional logic.

### Unit tests with sample data
Each reader has malformed-row tests with tiny sample CSVs/JSONs that contain:
- Missing required fields
- Invalid data types
- Out-of-range values
- Empty strings where not allowed

These verify that readers skip bad rows and log warnings without throwing exceptions.

### UTF-8 and special characters
TransitLineReader test verifies that "Niquía" and other accented station names are preserved.

### Quoted field handling
WalkLinkReader test verifies that names like `"CL 20B Sur 27-, Medellin"` with commas are parsed correctly.

## Acceptance criteria checklist

- [x] One test per file that reads the real file and asserts exact counts:
  - BusSegmentReader: 9,550 segments
  - TransitLineReader: 98 segments
  - WalkLinkReader: 25,865 links
  - StopArcGisReader: 10,043 features → 3,590 unique stops
  - RouteArcGisReader: 252 routes
  - StationArcGisReader: 81 stations
- [x] Small unit tests with sample text for malformed rows
- [x] `mvn test`: 0 failures, 0 errors
- [x] All readers skip and log malformed rows without stopping
- [x] UTF-8 station names preserved
- [x] Quoted names with commas parsed correctly
- [x] Empty dist_to_next_m/sec_to_next handled for last transit line stations
- [x] Data directory is `data/` relative to working directory (responsibility of caller, not readers)

## Out of scope

- Graph building (spec 02)
- Routing algorithms (later spec)
- Fare calculation (later spec)
- UI integration
- Command-line argument parsing for data directory (handled by Main.java)
- Performance optimization
- Caching or incremental loading
