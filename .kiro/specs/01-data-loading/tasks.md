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

## Task 1.3: Verify BusSegmentReader

**Goal:** Ensure BusSegmentReader correctly loads all 9,550 bus segments.

**Changes:**
1. Create `co.medellin.transit.data.BusSegmentReaderTest`:
   - `testReadRealFile()`: load `data/bus_segments.csv`, assert 9,550 segments
   - `testFieldParsing()`: verify first segment has correct route_id, direction, stops, distances
   - `testDirectionParsing()`: verify both "outbound"/0 and "inbound"/1 are parsed correctly
   - `testMalformedRow()`: sample CSV with invalid direction, verify skip + warning

2. Fix any issues found in `BusSegmentReader` or `BusSegment` record.

**Acceptance:**
- `mvn test` passes with 0 failures
- Real file test counts exactly 9,550 segments
- Direction parsing handles both string and numeric formats
- Malformed row test logs warning and continues

---

## Task 1.4: Verify TransitLineReader

**Goal:** Ensure TransitLineReader correctly loads all 98 transit line segments with UTF-8 names.

**Changes:**
1. Create `co.medellin.transit.data.TransitLineReaderTest`:
   - `testReadRealFile()`: load `data/transit_lines.csv`, assert 98 segments
   - `testUTF8StationNames()`: verify "Niquía" (first station) preserves accent
   - `testEmptyDistanceForLastStation()`: verify last station of line A has distToNextM=0, secToNext=0
   - `testModeNormalization()`: verify "metro" → "Metro", "brt" → "Metroplus", etc.
   - `testMalformedRow()`: sample CSV with invalid mode, verify skip + warning

2. Fix any issues found in `TransitLineReader` or `TransitSegment` record.

**Acceptance:**
- `mvn test` passes with 0 failures
- Real file test counts exactly 98 segments
- UTF-8 test verifies "Niquía" appears correctly (not "NiquÃa" or mangled)
- Last station has empty optional fields handled correctly
- Malformed row test logs warning and continues

---

## Task 1.5: Verify WalkLinkReader

**Goal:** Ensure WalkLinkReader correctly loads all 25,865 walk links with quoted names.

**Changes:**
1. Create `co.medellin.transit.data.WalkLinkReaderTest`:
   - `testReadRealFile()`: load `data/walk_links.csv`, assert 25,865 links
   - `testQuotedNamesWithCommas()`: verify a link with `"CL 20B Sur 27-, Medellin"` parses correctly
   - `testPlaceKeyParsing()`: verify `STOP:53191` → placeA=53191, `STATION:Aguacatala` → hash-based ID
   - `testKindNormalization()`: verify "stop_stop" → "stop-to-stop" conversion
   - `testMalformedRow()`: sample CSV with invalid kind, verify skip + warning

2. Fix any issues found in `WalkLinkReader` or `WalkLink` record.

**Acceptance:**
- `mvn test` passes with 0 failures
- Real file test counts exactly 25,865 links
- Quoted names with commas are parsed without splitting on internal comma
- Place keys are parsed correctly (numeric IDs for stops, hash for named stations)
- Malformed row test logs warning and continues

---

## Task 1.6: Verify StopArcGisReader

**Goal:** Ensure StopArcGisReader correctly loads and deduplicates stops.

**Changes:**
1. Create `co.medellin.transit.data.StopArcGisReaderTest`:
   - `testReadPaginatedRealFiles()`: load `data/raw/stops_*.json`, assert 3,590 unique stops (from 10,043 features)
   - `testDeduplication()`: verify that the reader reports correct deduplication counts in logs
   - `testCoordinateExtraction()`: verify a known stop has correct latitud/longitud
   - `testMissingAddress()`: verify stops without `direccion` get default "Parada {id}" address
   - `testMalformedFeature()`: sample JSON missing id_paradero, verify skip + warning

2. Fix any issues found in `StopArcGisReader`.

**Acceptance:**
- `mvn test` passes with 0 failures
- Real file test counts exactly 3,590 unique stops (not 10,043)
- Deduplication logs show "10043 total features → 3590 unique physical stops"
- Missing address handling works correctly
- Malformed feature test logs warning and continues

---

## Task 1.7: Integration test for all readers

**Goal:** Create a comprehensive integration test that loads all files and verifies counts.

**Changes:**
1. Create `co.medellin.transit.data.DataLoadingIntegrationTest`:
   - `testAllFileCounts()`: Load all 6 file types, assert all counts in one test
     - Bus segments: 9,550
     - Transit lines: 98
     - Walk links: 25,865
     - Stops: 3,590 unique (from 10,043 features)
     - Routes: 252
     - Stations: 81
   - Use a helper method to get data directory from system property or skip test if not set

**Acceptance:**
- `mvn test` passes with 0 failures
- Integration test verifies all 6 counts in requirements
- Test is skipped with clear message if data directory not available (not a failure)

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
