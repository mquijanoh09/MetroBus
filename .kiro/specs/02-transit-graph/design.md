# Design: Transit Graph Construction

## Problem Analysis

The current `GraphBuilder` implementation was built for a **pre-computed edge CSV format** where edges were already computed with `source_id`, `target_id`, `edge_kind`, `real_sec`, and `perceived_cost` columns. However, the real data uses a different format:

- `bus_segments.csv`: Contains raw bus route segments with `route_id`, `direction`, `from_stop_id`, `to_stop_id`, and distance metrics
- `transit_lines.csv`: Contains ordered station lists with `line`, `mode`, `order`, `station`, coordinates, and travel times
- `walk_links.csv`: Contains place-to-place connections with `place_a`, `name_a`, `place_b`, `name_b`, `kind`, `straight_m`, and `walk_sec`

The current code **does** build the graph correctly from the real data format (as proven by `DataCountTest` passing with 13,895 states and 82,160 edges), but the unit tests fail because they were written for the old pre-computed edge format.

## Test Failures Analysis

### GraphBuilderTest (4 errors)

**Root cause:** These tests create CSV files with the **old pre-computed edge format** (`source_id`, `target_id`, `edge_kind`, `real_sec`, `perceived_cost`), but `GraphBuilder` now expects the **real data format** described in #[[file:docs/graph-structure.md]].

**Contradiction:** The test format contradicts the documented format in `graph-structure.md` sections 2.1, 2.2, and 2.3, which specify:
- `bus_segments.csv` with columns: `route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method`
- `transit_lines.csv` with columns: `line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next`
- `walk_links.csv` with columns: `place_a,name_a,place_b,name_b,kind,straight_m,walk_sec`

**Evidence:** The test failures show CSV parsing warnings:
```
WARNING: Skipped 1 malformed rows in walk_links.csv. First 1 examples: 
[line 2: Mapping for place_a not found, expected one of [source_id, target_id, edge_kind, real_sec, perceived_cost]]
```

This confirms the tests are using the wrong format.

**Errors:**
1. `testBuildCreatesGraphFromEdgeData`: Creates walk_links.csv with `source_id,target_id,edge_kind,real_sec,perceived_cost` → fails because no raw/ subdirectory with stops_*.json exists
2. `testBuildHandlesMultipleCsvFiles`: Creates all three CSVs with old format → same issue
3. `testBuildHandlesAlternativeFieldNames`: Creates CSV with `from_id,to_id,kind,duration,cost` → wrong format
4. `testBuildContinuesWhenSomeFilesAreMissing`: Creates walk_links.csv with old format → fails

**Solution:** Rewrite these 4 tests to use the real data format as documented.

### GraphBuilderIntegrationTest (2 failures)

**Root cause:** These tests create CSV files in the **real data format** but with minimal test data. The failures are due to incorrect expected values that don't match what the code actually produces.

#### Failure 1: `testBuildGraphFromRawTransitData`

**Expected:** 6 states, 12 edges
**Actual:** States unknown (test passed state count), 8 edges

**Analysis:**
The test creates:
- 3 stops (100, 101, 102)
- 1 route (C101) with direction "0" (should be "outbound" per graph-structure.md section 2.1)
- 2 bus segments (100→101, 101→102)
- 2 bidirectional walk links (100↔101, 101↔102)

**What the code produces:**
According to graph-structure.md section 3.2:
1. **States:** 3 WALK states (one per stop) + 3 BUS_STOP states (one per stop on route) = **6 states** ✓
2. **Edges:**
   - 2 RIDE edges (from bus segments) ✓
   - 3 BOARD edges (WALK → BUS_STOP for each stop) ✓
   - 3 ALIGHT edges (BUS_STOP → WALK for each stop) ✓
   - **4 WALK edges** (2 walk links × 2 directions) ✓
   - **Total: 12 edges**

**But test gets 8 edges.** Missing: 4 WALK edges (all of them).

**Issue:** The test's `walk_links.csv` uses column headers `place_a,name_a,place_b,name_b,kind,straight_m,walk_sec`, which matches the documented format. However, the values are:
```
100,Parada A,101,Parada B,stop-to-stop,1000,800
```

According to graph-structure.md section 2.3, place identifiers should be **prefixed**: `STOP:<id>` or `STATION:<name>`. The test uses bare numbers `100`, `101`, `102`.

**Question for user:** Should `walk_links.csv` place_a/place_b accept bare stop IDs (like `100`) or require the prefix format (`STOP:100`)? The doc says "Place keys: `STOP:<id_paradero>` and `STATION:<station name>`" but I need to check if the code handles both.

#### Failure 2: `testBuildGraphWithTransitLines`

**Expected:** 3 states, 2 edges
**Actual:** 9 states, edges unknown

**Analysis:**
The test creates:
- 0 stops
- 0 routes
- 3 transit stations in `transit_lines.csv` (Niquía, Bello, Madera on line "A", mode "Metro")

**What the code produces:**
According to graph-structure.md section 3.1, for each transit line the code creates states for **BOTH directions** (OUTBOUND and INBOUND):
1. **3 stations × 2 directions = 6 METRO_STATION states**
2. **3 WALK states** (one per unique station name)
3. **Total: 9 states**

The test expects only 3 states, but the code correctly implements bidirectional transit lines as specified in graph-structure.md section 2.2: "Trains and cable cars run both ways with the same time, so **each row gives two edges**."

**Edges:**
- 2 RIDE edges in OUTBOUND direction (Niquía→Bello, Bello→Madera)
- 2 RIDE edges in INBOUND direction (Madera→Bello, Bello→Niquía)
- 6 BOARD edges (3 stations × 2 directions, from WALK state)
- 6 ALIGHT edges (3 stations × 2 directions, to WALK state)
- **Total: 16 edges**

**Contradiction:** The test's expected value of 3 states contradicts graph-structure.md section 3.1, which specifies that transit states include both direction and have the format `(STATION:name, L<line>, +/-)`. The test ignores:
1. The WALK states requirement (section 3.1: "3,665 WALK states for all places")
2. The bidirectional requirement (section 3.2: "each row gives two edges" means bidirectional)

**Question for user:** The test expects 3 states and 2 edges, but graph-structure.md clearly specifies bidirectional transit with WALK states. Should I change the test expectations to match the documented behavior (9 states: 6 METRO_STATION + 3 WALK)?

## Design Decisions

### 1. GraphBuilder Correctness

**Current state:** `GraphBuilder` correctly implements the graph structure as specified in #[[file:docs/graph-structure.md]]. Evidence:
- `DataCountTest` passes with exact counts: 13,895 states, 82,160 edges
- Edge breakdown matches spec: 9,970 RIDE + 10,230 BOARD + 10,230 ALIGHT + 51,730 WALK
- State breakdown matches spec: 3,665 WALK + 10,034 BUS_STOP + 196 METRO_STATION

**Conclusion:** The production code is correct. The tests need fixing.

### 2. Test Data Format

**Decision:** All tests must use the **real data format** as documented:
- `bus_segments.csv`: Full format with route_id, direction, stop IDs, sequences, distances, method
- `transit_lines.csv`: Full format with line, mode, order, station, coordinates, distances, times
- `walk_links.csv`: Full format with place_a/b (prefixed), names, kind, distances, walk_sec
- `raw/stops_*.json`: ArcGIS JSON format with features array
- `raw/routes.json`: ArcGIS JSON format with route attributes

### 3. Expected Values Strategy

**Decision:** For each integration test:
1. Calculate expected values manually based on the test data
2. Verify against graph-structure.md section 3.2 edge rules
3. Document the calculation in test comments
4. Ask user before changing any expected value

## Implementation Strategy

### Phase 1: Fix GraphBuilderTest (4 test methods)

**Approach:** Rewrite all 4 tests to use the real data format:

1. **testBuildCreatesGraphFromEdgeData** → Rename to `testBuildMinimalBusGraph`
   - Create proper directory structure with `raw/` subdirectory
   - Create `raw/stops_0.json` with 3 stops in ArcGIS format
   - Create `raw/routes.json` with 1 route
   - Create `bus_segments.csv` with 2 segments
   - Create empty `transit_lines.csv` and `walk_links.csv`
   - Calculate expected: 3 WALK + 3 BUS_STOP = 6 states, 2 RIDE + 3 BOARD + 3 ALIGHT = 8 edges

2. **testBuildHandlesMultipleCsvFiles** → Keep name, fix format
   - Create complete minimal dataset with all three data types
   - Use proper formats for all files
   - Calculate expected based on actual data

3. **testBuildHandlesAlternativeFieldNames** → Delete this test
   - Reason: The "alternative field names" concept was for the old pre-computed edge format
   - The real format has fixed column names per documentation
   - No alternative names are supported or needed

4. **testBuildContinuesWhenSomeFilesAreMissing** → Fix format
   - Use proper walk_links.csv format with prefixed place IDs
   - Verify builder handles missing bus_segments.csv and transit_lines.csv gracefully

### Phase 2: Fix GraphBuilderIntegrationTest (2 test methods)

**Approach:** Fix expected values and data format issues:

1. **testBuildGraphFromRawTransitData**
   - **Issue 1:** Fix `direction` value: change `"0"` to `"outbound"` in bus_segments.csv
   - **Issue 2:** Fix `walk_links.csv` place IDs: change `100` to `STOP:100`, etc.
   - **Recalculate edges:** Should be 2 RIDE + 3 BOARD + 3 ALIGHT + 4 WALK = 12 edges ✓
   - **Ask user before changing:** Expected state count (currently 6, may be correct)

2. **testBuildGraphWithTransitLines**
   - **Ask user:** Should expected be changed from 3 states to 9 states (6 METRO_STATION + 3 WALK)?
   - **Ask user:** Should expected edges be changed from 2 to 16 (4 RIDE + 6 BOARD + 6 ALIGHT)?
   - **Reason:** Current expectations contradict graph-structure.md section 3.1-3.2

## Questions for User

Before proceeding with implementation:

1. **testBuildGraphFromRawTransitData:** The test expects 6 states and currently passes the state count assertion. After fixing walk links (adding `STOP:` prefix), will it still be 6 states, or should it change? The code should create 3 WALK + 3 BUS_STOP = 6 states according to the documented logic.

2. **testBuildGraphFromRawTransitData:** Should walk_links.csv accept bare stop IDs (like `100`) or require prefixed format (`STOP:100`)? The doc specifies prefixes, but I need to verify the code's behavior.

3. **testBuildGraphWithTransitLines:** Should I change expected values from (3 states, 2 edges) to (9 states, 16 edges) to match the bidirectional transit behavior documented in graph-structure.md? The current expectations seem to assume unidirectional transit, which contradicts the spec.

4. **testBuildGraphWithTransitLines:** Should WALK states be created for transit stations even when there are no bus stops? The current code creates them (hence 9 states), but the test expects only 3 states.

## File Changes

### Tests to Modify
1. `src/test/java/co/medellin/transit/graph/GraphBuilderTest.java`
   - Rewrite 3 tests: testBuildCreatesGraphFromEdgeData, testBuildHandlesMultipleCsvFiles, testBuildContinuesWhenSomeFilesAreMissing
   - Delete 1 test: testBuildHandlesAlternativeFieldNames

2. `src/test/java/co/medellin/transit/graph/GraphBuilderIntegrationTest.java`
   - Fix testBuildGraphFromRawTransitData: direction value and walk link place IDs
   - Fix testBuildGraphWithTransitLines: expected values (pending user confirmation)

### Production Code
**No changes needed.** `GraphBuilder` is correct per `DataCountTest` results.

## Success Criteria

- `mvn test -Dtest=GraphBuilderTest`: 0 failures, 0 errors (4 tests passing after deleting 1)
- `mvn test -Dtest=GraphBuilderIntegrationTest`: 0 failures, 0 errors (2 tests passing)
- `DataCountTest` continues to pass with exact counts: 13,895 states, 82,160 edges
- All test data uses the documented real data format from graph-structure.md
