# Tasks: 01 · Data loading

## Task 1.1: Create Station data class and reader

**Goal:** Add support for reading transit stations from `data/raw/transit_stations.json`.

**Changes:**
1. Create `co.medellin.transit.data.Station` record:
   - Fields: `String name`, `String line`, `Coordinate coordinate`
   - Validation: non-null/non-empty name and line, coordinate within Medellín bounds

2. Create `co.medellin.transit.data.StationArcGisReader` extending `ArcGisReader<Station>`:
   - Parse `attributes.nombre` → name
   - Parse `attributes.linea` → line (keep original format like "Línea A")
   - Parse `geometry.x` → longitude, `geometry.y` → latitude

3. Create `co.medellin.transit.data.StationArcGisReaderTest`:
   - `testReadRealFile()`: load `data/raw/transit_stations.json` from relative path, assert 81 stations, FAIL if missing
   - `testGeometryParsing()`: verify x/y coordinates extracted correctly for a known station
   - `testMalformedFeature()`: sample JSON missing required fields, verify skip + warning

**Acceptance:**
- `mvn test` passes with 0 failures
- Real file test counts exactly 81 stations
- Real file test fails with clear error if file missing (no skip logic)
- Malformed feature test logs warning and continues

---

## Task 1.2: Fix Route model and reader

**Goal:** Update Route model to match data format (integer ID, codigo field) and verify RouteArcGisReader.

**Changes:**
1. Review `co.medellin.transit.model.Route`:
   - Verify fields match requirements: id (integer), codigo (string), nombre (string), tipo (string)
   - If `id` is currently String, change to `int` or keep as String but parse from integer in JSON
   - Add `codigo` field if missing (the user-visible route code like "260")
   - Add `tipo` field if missing (needed for fares: "Integrada", "Directa Centro", etc.)

2. Update `co.medellin.transit.data.RouteArcGisReader`:
   - Parse `id_ruta` from JSON string → integer
   - Extract `codigo` field (user-visible route code)
   - Extract `nombre` field (route name)
   - Extract `tipo` field (route type for fare calculation)
   - Keep existing color logic as fallback

3. Create `co.medellin.transit.data.RouteArcGisReaderTest`:
   - `testReadRealFile()`: load `data/raw/routes.json`, assert 252 routes
   - `testFieldExtraction()`: verify first route has correct id_ruta, codigo, nombre, tipo
   - `testMalformedFeature()`: sample JSON missing id_ruta, verify skip + warning

**Acceptance:**
- `mvn test` passes with 0 failures
- Real file test counts exactly 252 routes
- Route objects have integer ID, codigo, nombre, and tipo fields populated
- Malformed feature test logs warning and continues

---

## Task 1.3: Add real-file test to BusSegmentReaderTest

**Goal:** Ensure BusSegmentReader correctly loads all 9,550 bus segments.

**Changes:**
1. Add to existing `co.medellin.transit.data.BusSegmentReaderTest`:
   - `testReadRealFile()`: load `data/bus_segments.csv` from relative path, assert 9,550 segments, FAIL if missing
   - Verify first segment has correct route_id, direction, stops, distances

2. Fix any issues found in `BusSegmentReader` or `BusSegment` record.

**Acceptance:**
- `mvn test` passes with 0 failures
- Real file test counts exactly 9,550 segments
- Real file test fails with clear error if file missing (no skip logic)
- All existing tests continue to pass

---

## Task 1.4: Add real-file tests to TransitLineReaderTest

**Goal:** Ensure TransitLineReader correctly loads all 98 transit line segments with UTF-8 names.

**Changes:**
1. Add to existing `co.medellin.transit.data.TransitLineReaderTest`:
   - `testReadRealFile()`: load `data/transit_lines.csv` from relative path, assert 98 segments, FAIL if missing
   - `testUTF8StationNames()`: verify "Niquía" (first station) preserves accent
   - `testLastStationEmptyFields()`: verify last station of line A has distToNextM=0, secToNext=0

2. Fix any issues found in `TransitLineReader` or `TransitSegment` record.

**Acceptance:**
- `mvn test` passes with 0 failures
- Real file test counts exactly 98 segments
- UTF-8 test verifies "Niquía" appears correctly (not "NiquÃa" or mangled)
- Last station test verifies 0 means "no next station"
- Real file test fails with clear error if file missing (no skip logic)
- All existing tests continue to pass

---

## Task 1.5: Fix WalkLink and WalkLinkReader for String place keys

**Goal:** Update WalkLink to use String place keys and fix all existing tests.

**Changes:**
1. Update `co.medellin.transit.data.WalkLink` record:
   - Change `int placeA` → `String placeA`
   - Change `int placeB` → `String placeB`
   - Update validation: non-null/non-empty strings

2. Update `co.medellin.transit.data.WalkLinkReader`:
   - Read place_a and place_b as strings directly (no parsing, no hash generation)
   - Keep kind normalization: "stop_stop" → "stop-to-stop"

3. Fix ALL existing tests in `WalkLinkReaderTest`:
   - Change all integer place IDs to String format matching test data
   - Update assertions to expect String values

4. Add new tests to `WalkLinkReaderTest`:
   - `testReadRealFile()`: load `data/walk_links.csv` from relative path, assert 25,865 links, FAIL if missing
   - `testPrefixedPlaceKeys()`: verify `STOP:53191` and `STATION:Aguacatala` kept as-is
   - `testQuotedNamesWithCommas()`: verify comma handling in quoted names

**Acceptance:**
- `mvn test` passes with 0 failures for WalkLinkReaderTest
- Real file test counts exactly 25,865 links
- Place keys are kept as original strings (STOP:53191, STATION:Aguacatala)
- All existing tests pass with String place keys
- Real file test fails with clear error if file missing (no skip logic)

---

## Task 1.6: Fix StopArcGisReader and existing tests

**Goal:** Ensure StopArcGisReader correctly loads stops with direccion as name.

**Changes:**
1. Update `co.medellin.transit.data.StopArcGisReader`:
   - Use `direccion` field as Stop name (same value for both name and address)
   - If `direccion` is missing, use "Parada {id_paradero}" for both name and address

2. Fix ALL existing tests in `StopArcGisReaderTest`:
   - Replace invented "nombre_paradero" field with "direccion"
   - Update expected names to match direccion values
   - Keep all other assertions unchanged

3. Add new tests to `StopArcGisReaderTest`:
   - `testReadPaginatedRealFiles()`: load `data/raw/stops_*.json` from relative path, assert 10,043 features → 3,590 unique stops, FAIL if missing
   - `testMissingDireccion()`: verify stops without `direccion` get "Parada {id}" for name and address

**Acceptance:**
- `mvn test` passes with 0 failures for StopArcGisReaderTest
- Real file test counts exactly 3,590 unique stops (from 10,043 features)
- All existing tests pass with real field names (id_paradero, direccion, latitud, longitud)
- Real file test fails with clear error if file missing (no skip logic)

---

## Task 1.7: Integration test for all readers

**Goal:** Create a comprehensive integration test that loads all files and verifies counts.

**Changes:**
1. Create `co.medellin.transit.data.DataLoadingIntegrationTest`:
   - `testAllFileCounts()`: Load all 6 file types from relative paths, assert all counts in one test
     - Bus segments: 9,550
     - Transit lines: 98
     - Walk links: 25,865
     - Stops: 3,590 unique (from 10,043 features)
     - Routes: 252
     - Stations: 81
   - FAIL with clear error if any file missing (no skip logic)

**Acceptance:**
- `mvn test` passes with 0 failures
- Integration test verifies all 6 counts in requirements
- Test fails (not skips) if data directory or files not available

---

## Task 1.8: Update existing DataCountTest

**Goal:** Ensure the existing DataCountTest works with our updated readers.

**Changes:**
1. Review `co.medellin.transit.graph.DataCountTest`:
   - Verify it uses the same data counts as our new tests
   - If it references Station loading (requirement 6), ensure it works with new StationArcGisReader
   - No changes needed if it only tests graph building

**Acceptance:**
- `mvn test` passes with 0 failures
- DataCountTest continues to work (it tests graph counts, not just data loading)

---

## Order of execution

1. Task 1.1 (Station class and reader) — new component, no dependencies
2. Task 1.2 (Route model fix) — may affect other code, do early
3. Task 1.3 (BusSegmentReader) — independent verification
4. Task 1.4 (TransitLineReader) — independent verification
5. Task 1.5 (WalkLinkReader) — independent verification
6. Task 1.6 (StopArcGisReader) — independent verification
7. Task 1.7 (Integration test) — depends on all readers working
8. Task 1.8 (Update DataCountTest) — final verification

## Definition of Done (for entire spec)

All of these must be true before marking spec 01 complete:

- [ ] All 8 tasks completed
- [ ] `mvn test` shows: "Tests run: N, Failures: 0, Errors: 0"
- [ ] Each of 6 file types has a dedicated test with real file count assertion
- [ ] Each reader has a malformed-row/feature test
- [ ] Integration test passes with all counts matching requirements
- [ ] No absolute paths in code (all paths are relative or passed as parameters)
- [ ] UTF-8 handling verified (station names with accents)
- [ ] Quoted CSV fields verified (names with commas)
- [ ] Empty optional fields verified (last transit line station)
- [ ] DataCountTest still passes (doesn't break graph building tests)
