# Tasks: Transit Graph Construction

**Status:** ✅ COMPLETED — All tests passing, DataCountTest preserved with exact counts

## Task 1: Answer Design Questions (user action required)

**Status:** ✅ Completed — User confirmed all answers

**Questions:**

1. **testBuildGraphFromRawTransitData walk link format:** 
   - The test currently uses bare stop IDs in walk_links.csv: `100,Parada A,101,Parada B,...`
   - graph-structure.md section 2.3 specifies prefixed format: `STOP:100,Parada A,STOP:101,Parada B,...`
   - Should I change the test data to use the `STOP:` prefix?
   - **Impact:** This is why the test gets 8 edges instead of 12 (missing all 4 WALK edges)

2. **testBuildGraphWithTransitLines state count:**
   - Test expects: 3 states
   - Code produces: 9 states (6 METRO_STATION + 3 WALK)
   - graph-structure.md section 3.1 specifies both METRO_STATION states (with direction) and WALK states for each place
   - graph-structure.md section 2.2 says "each row gives two edges" (bidirectional)
   - Should I change the test expectation from 3 to 9 states?

3. **testBuildGraphWithTransitLines edge count:**
   - Test expects: 2 edges
   - Based on graph-structure.md section 3.2, should be: 16 edges (4 RIDE + 6 BOARD + 6 ALIGHT)
   - Should I change the test expectation from 2 to 16 edges?

4. **testBuildGraphWithTransitLines WALK states for stations:**
   - Should WALK states be created for transit stations even when there are no bus stops?
   - Current code creates them (which is why we get 9 states instead of 3)
   - graph-structure.md section 3.1 says "3,665 WALK states" includes both stops and stations

**Acceptance:** User responds to all 4 questions with clear decisions on expected values.

**User Answers:**
1. ✅ Require `STOP:/STATION:` prefixes and `outbound/inbound` directions per graph-structure.md
2. ✅ 9 states: 3 stations × 2 directions + 3 WALK states
3. ✅ 16 edges: 4 RIDE + 6 BOARD + 6 ALIGHT (every vehicle state gets board + alight)
4. ✅ Yes: every station gets a WALK state (3,665 = 3,590 stops + 75 stations)

---

## Task 2: Fix GraphBuilderTest - Rewrite testBuildCreatesGraphFromEdgeData

**Status:** ✅ Completed

**Prerequisites:** Task 1 completed

**Changes:**
- File: `src/test/java/co/medellin/transit/graph/GraphBuilderTest.java`
- Action: Rewrite the test to use real data format

**Steps:**

1. Rename test method from `testBuildCreatesGraphFromEdgeData` to `testBuildMinimalBusGraph`

2. Create proper directory structure:
   ```
   tempDir/
   ├── raw/
   │   ├── stops_0.json (ArcGIS format with 3 stops)
   │   └── routes.json (ArcGIS format with 1 route)
   ├── bus_segments.csv (2 segments)
   ├── transit_lines.csv (empty with header)
   └── walk_links.csv (empty with header)
   ```

3. Create `raw/stops_0.json` with proper ArcGIS format:
   ```json
   {
     "features": [
       {"attributes":{"OBJECTID":1,"id_paradero":100,"direccion":"Test 1"},"geometry":{"x":-75.57,"y":6.25}},
       {"attributes":{"OBJECTID":2,"id_paradero":101,"direccion":"Test 2"},"geometry":{"x":-75.56,"y":6.26}},
       {"attributes":{"OBJECTID":3,"id_paradero":102,"direccion":"Test 3"},"geometry":{"x":-75.55,"y":6.27}}
     ]
   }
   ```

4. Create `raw/routes.json`:
   ```json
   {
     "features": [
       {"attributes":{"id_ruta":"101","codigo":"T1","nombre":"Test Route 1","tipo":"Directa Centro"}}
     ]
   }
   ```

5. Create `bus_segments.csv`:
   ```
   route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
   101,outbound,100,101,1,2,1000,1200,1200,road
   101,outbound,101,102,2,3,1100,1300,1300,road
   ```

6. Create empty `transit_lines.csv` with header:
   ```
   line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
   ```

7. Create empty `walk_links.csv` with header:
   ```
   place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
   ```

8. Update assertions:
   - Expected states: 6 (3 WALK + 3 BUS_STOP)
   - Expected edges: 8 (2 RIDE + 3 BOARD + 3 ALIGHT)

9. Add detailed assertion comments explaining the counts

**Acceptance:**
- Test passes with real data format
- Assertions match calculated values from test data
- Test creates proper directory structure with raw/ subdirectory

---

## Task 3: Fix GraphBuilderTest - Delete testBuildHandlesAlternativeFieldNames

**Status:** ✅ Completed — Test deleted

**Prerequisites:** Task 1 completed

**Changes:**
- File: `src/test/java/co/medellin/transit/graph/GraphBuilderTest.java`
- Action: Delete entire test method

**Reason:** 
- This test was for the old pre-computed edge format's "alternative field names" feature
- The real data format has fixed column names per graph-structure.md
- No alternative names are supported in the documented format
- The test provides no value for the actual implementation

**Steps:**
1. Delete the `testBuildHandlesAlternativeFieldNames` method entirely (lines ~145-165)
2. Remove any imports only used by this test (none expected)

**Acceptance:**
- Method deleted
- File compiles successfully
- `mvn test -Dtest=GraphBuilderTest` runs 6 tests (down from 7)

---

## Task 4: Fix GraphBuilderTest - Rewrite testBuildHandlesMultipleCsvFiles

**Status:** ✅ Completed

**Prerequisites:** Task 1 completed

**Changes:**
- File: `src/test/java/co/medellin/transit/graph/GraphBuilderTest.java`
- Action: Rewrite test to use real data format for all three file types

**Steps:**

1. Create proper directory structure with raw/ subdirectory

2. Create `raw/stops_0.json` with 4 stops (IDs: 100, 101, 200, 300)

3. Create `raw/routes.json` with 1 bus route (ID: 101)

4. Create `bus_segments.csv` with 1 segment:
   ```
   route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
   101,outbound,100,101,1,2,1000,1200,1200,road
   ```

5. Create `transit_lines.csv` with 2 stations on line A:
   ```
   line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
   A,metro,1,Station 1,6.25,-75.57,1500,90
   A,metro,2,Station 2,6.26,-75.56,,
   ```

6. Create `walk_links.csv` with 1 bidirectional link:
   ```
   place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
   STOP:200,Test Stop,STOP:300,Test Stop 2,stop_stop,250,195
   ```

7. Calculate expected values:
   - States: 2 stops for bus (100, 101) + 2 isolated stops (200, 300) + 2 stations = 6 WALK states
            + 2 BUS_STOP states (100, 101 on route 101)
            + 4 METRO_STATION states (2 stations × 2 directions)
            = **12 states total**
   - Edges: 1 RIDE (bus) + 2 RIDE (transit, bidirectional) + 2 BOARD (bus) + 4 BOARD (metro) 
           + 2 ALIGHT (bus) + 4 ALIGHT (metro) + 2 WALK (bidirectional link)
           = **17 edges total**

8. Update assertions with calculated values and add comments

**Acceptance:**
- Test passes with all three data types
- Assertions match documented edge/state construction rules
- Test demonstrates graph combines data from all sources

---

## Task 5: Fix GraphBuilderTest - Rewrite testBuildContinuesWhenSomeFilesAreMissing

**Status:** ✅ Completed

**Prerequisites:** Task 1 completed

**Changes:**
- File: `src/test/java/co/medellin/transit/graph/GraphBuilderTest.java`
- Action: Fix to use real walk_links.csv format with prefixed place IDs

**Steps:**

1. Create proper directory structure with raw/ subdirectory

2. Create `raw/stops_0.json` with 2 stops (IDs: 100, 101)

3. Create `raw/routes.json` (empty features array)

4. Create only `walk_links.csv` (bus_segments and transit_lines missing):
   ```
   place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
   STOP:100,Test Stop 1,STOP:101,Test Stop 2,stop_stop,250,195
   ```
   **Note:** Use `STOP:100` format, not bare `100`

5. Update assertions:
   - Expected states: 2 (WALK states for stops 100 and 101)
   - Expected edges: 2 (bidirectional walk link)

6. Verify GraphBuilder logs warnings about missing files but continues

7. Update test comments to clarify it tests graceful handling of missing files

**Acceptance:**
- Test passes with only walk_links.csv present
- GraphBuilder logs warnings about missing bus_segments.csv and transit_lines.csv
- Graph is built successfully with available data only

---

## Task 6: Fix GraphBuilderIntegrationTest - Fix testBuildGraphFromRawTransitData

**Status:** ✅ Completed

**Prerequisites:** Task 1 completed, user confirms walk link format

**Changes:**
- File: `src/test/java/co/medellin/transit/graph/GraphBuilderIntegrationTest.java`
- Action: Fix bus segment direction and walk link place ID format

**Steps:**

1. Fix bus_segments.csv direction values:
   - Change: `C101,0,100,101,...` → `C101,outbound,100,101,...`
   - Change: `C101,0,101,102,...` → `C101,outbound,101,102,...`
   - **Reason:** graph-structure.md section 2.1 specifies direction as `outbound|inbound`, not numeric

2. Fix walk_links.csv place IDs (if user confirms prefix required):
   - Change: `100,Parada A,101,Parada B,...` → `STOP:100,Parada A,STOP:101,Parada B,...`
   - Change: `101,Parada B,102,Parada C,...` → `STOP:101,Parada B,STOP:102,Parada C,...`
   - **Reason:** graph-structure.md section 2.3 specifies place keys as `STOP:<id>` or `STATION:<name>`

3. Verify expected values:
   - States: 6 (3 WALK + 3 BUS_STOP) — should still be correct
   - Edges: 12 (2 RIDE + 3 BOARD + 3 ALIGHT + 4 WALK) — currently fails with 8, should pass after fix

4. Add comments documenting the expected count calculation

**Acceptance:**
- Test passes with 6 states and 12 edges
- All data uses documented format from graph-structure.md
- No CSV parsing warnings in test output

---

## Task 7: Fix GraphBuilderIntegrationTest - Fix testBuildGraphWithTransitLines

**Status:** ✅ Completed

**Prerequisites:** Task 1 completed, user confirms expected values

**Changes:**
- File: `src/test/java/co/medellin/transit/graph/GraphBuilderIntegrationTest.java`
- Action: Update expected state and edge counts to match bidirectional transit behavior

**Steps:**

1. **If user confirms bidirectional transit with WALK states:**
   - Update state count assertion: `assertEquals(3, ...)` → `assertEquals(9, ...)`
   - Update edge count assertion: `assertEquals(2, ...)` → `assertEquals(16, ...)`
   - Add detailed comment explaining:
     ```java
     // 9 states:
     //   - 3 WALK states (one per unique station)
     //   - 6 METRO_STATION states (3 stations × 2 directions)
     // 16 edges:
     //   - 4 RIDE edges (2 forward + 2 reverse for 3-station line)
     //   - 6 BOARD edges (3 stations × 2 directions, WALK → METRO_STATION)
     //   - 6 ALIGHT edges (3 stations × 2 directions, METRO_STATION → WALK)
     ```

2. **If user wants unidirectional transit (contradicts docs):**
   - Update state count: `assertEquals(3, ...)` → `assertEquals(6, ...)`  (3 WALK + 3 METRO_STATION)
   - Update edge count: `assertEquals(2, ...)` → `assertEquals(8, ...)` (2 RIDE + 3 BOARD + 3 ALIGHT)
   - **Note:** This would contradict graph-structure.md section 2.2's "each row gives two edges"

3. **If user wants no WALK states for stations (contradicts docs):**
   - Keep state count at 3 (only METRO_STATION in one direction)
   - Update edge count to 2 (only RIDE edges, no BOARD/ALIGHT)
   - **Note:** This would break the transfer model described in graph-structure.md section 3.1

4. Update test documentation to match the chosen behavior

**Acceptance:**
- Test passes with user-approved expected values
- Test documentation clearly explains the count calculation
- Behavior matches user's interpretation of graph-structure.md

---

## Task 8: Verify All Tests Pass

**Status:** ✅ Completed

**Prerequisites:** Tasks 2-7 completed

**Actions:**
1. Run full test suite: `mvn test`
2. Run specific test classes: `mvn test -Dtest=GraphBuilderTest,GraphBuilderIntegrationTest`
3. Verify DataCountTest still passes with exact counts

**Acceptance:**
- `mvn test` output shows:
  - `Tests run: N, Failures: 0, Errors: 0`
  - GraphBuilderTest: 6 tests passing (was 7, deleted 1)
  - GraphBuilderIntegrationTest: 2 tests passing
  - DataCountTest: 8 tests passing (unchanged)
- Paste the actual "Tests run" line from mvn output
- No CSV parsing warnings in test output
- App still starts and shows home screen

**Results:**
```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.876 s -- in co.medellin.transit.graph.DataCountTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.392 s -- in co.medellin.transit.graph.GraphBuilderIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.598 s -- in co.medellin.transit.graph.GraphBuilderTest

[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

App starts successfully and loads the graph (13,895 states, 82,160 edges).

---

## Task 9: Document Completion

**Status:** ✅ Completed

**Prerequisites:** Task 8 completed

**Actions:**
1. Update this tasks.md with actual test results
2. Create completion summary in spec directory
3. Mark spec 02 as complete in .kiro/specs/02-transit-graph/.config.kiro

**Deliverables:**
- Paste final `mvn test` output showing 0 failures, 0 errors
- Confirm app starts successfully
- Confirm DataCountTest exact counts: 13,895 states, 82,160 edges

**Acceptance:**
- All items in requirements.md acceptance criteria are met
- Test output proves correctness
- Ready to proceed to spec 03

**Final Summary:**
- ✅ GraphBuilder correctly implements graph-structure.md specification
- ✅ All graph tests pass: 16 tests, 0 failures, 0 errors
  - DataCountTest: 8 tests (validates exact counts on real data)
  - GraphBuilderIntegrationTest: 2 tests (validates correct expected values)
  - GraphBuilderTest: 6 tests (was 7, deleted obsolete test)
- ✅ DataCountTest confirms exact counts:
  - 13,895 states (3,665 WALK + 10,034 BUS_STOP + 196 METRO_STATION)
  - 82,160 edges (9,970 RIDE + 10,230 BOARD + 10,230 ALIGHT + 51,730 WALK)
- ✅ App starts and loads graph successfully
- ✅ No production code changes needed (GraphBuilder was already correct)
- ✅ All test data now uses documented real format from graph-structure.md

---

## Summary

**Total tasks:** 9
- **Task 1:** User action (answer 4 questions about expected values)
- **Tasks 2-5:** Fix GraphBuilderTest (4 test methods → 3 after deletion)
- **Tasks 6-7:** Fix GraphBuilderIntegrationTest (2 test methods)
- **Task 8:** Verification
- **Task 9:** Documentation

**Key decisions needed from user:**
1. Walk link place ID format (bare vs. prefixed)
2. Transit state count expectations (3, 6, or 9 states)
3. Transit edge count expectations (2, 8, or 16 edges)
4. WALK state creation policy for stations

**No production code changes needed** — GraphBuilder is correct per DataCountTest.
