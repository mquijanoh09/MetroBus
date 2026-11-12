# 05 · Fares: Tasks

## Task 1: Add route type lookup to TransitGraph

**Goal**: Provide a way to query a bus route's `tipo_ruta` (e.g., "Integrada", "Directa Centro") from the graph.

### Subtasks

1.1. Add a field `Map<String, String> routeTypes` to `TransitGraph`
   - Key: route ID (as string)
   - Value: tipo_ruta ("Integrada" | "Directa Centro" | "Circular" | "Directa No Centro")

1.2. Add public method `String getRouteType(String routeId)` to `TransitGraph`
   - Returns the route type for the given route ID, or null if not found

1.3. Update `GraphBuilder.build()` to populate the route types map
   - When processing `data/raw/stops_*.json`, extract `id_ruta` and `tipo_ruta` pairs
   - Store them in the `TransitGraph` via a new setter or builder method

1.4. Add a getter method to expose route count: `int getRouteTypeCount()` for verification

### Verification
- Add a unit test that checks route type lookup for known routes
- Verify in main that a few known routes return the correct type

**Estimated effort**: 30 minutes

---

## Task 2: Rewrite FareCalculator to match prototype fare() 1:1

**Goal**: Replace the current fare calculation logic with a direct port of `reference/prototype.py` `Network.fare()` (lines 163-177).

### Subtasks

2.1. Update `FareCalculator.calculate()` to track ride step timing
   - Initialize `int cumulativeTime = 0`
   - For each step:
     - If step is BOARD: record `int t0 = cumulativeTime` (BEFORE the wait)
     - After processing the step: `cumulativeTime += step.getDurationSec()`
   - The t0 value must match the t0 column in docs/reference_trips.md for verification

2.2. Implement the prototype's algorithm structure:
   - Variables to track: `cost`, `transitStart`, `integrations`, `last`
   - Iterate through `trip.getSteps()`, processing only ride steps
   - For each ride step:
     - Determine if it's mass transit (not bus) or bus
     - Apply the corresponding fare logic from design.md pseudocode

2.3. Add helper method `boolean isMassTransit(Step step)`:
   - Returns true if mode is metro, brt, cable_car, or tram
   - Returns false if mode is bus

2.4. Add helper method `boolean isIntegradaBus(Step step)`:
   - Get the route ID from `step.getRouteInfo()`
   - Query `graph.getRouteType(routeId)`
   - Return true if route type equals "Integrada" (exact string comparison)

2.5. Track integration state:
   - Use a `Set<Integer>` to track indices of integrated ride steps
   - Mark steps as integrated when the integrated bus→transit or transit→bus logic applies

2.6. Implement mass-transit integration logic:
   - If `transitStart != null` and within 90 minutes: increment `integrations`, add +750 if count is 5, 8, or 9
   - Else if last was Integrada bus and not yet integrated: integrate backward
   - Else: start new transit window

2.7. Implement bus fare logic:
   - If Integrada bus, last was transit, not integrated, within 90 min: integrate forward
   - Else: pay full bus fare (3800)

2.8. Update `FareSegment` to track integrated state:
   - Add `boolean integrated` field
   - Update constructor and getters
   - Update `FareCalculator` to populate this field correctly

2.9. Remove old integration logic:
   - Delete the current implementation that only checks first 2 boardings
   - Remove the cache-based route type lookup (will use TransitGraph instead)

2.10. Add unit tests for each fare rule (per requirements.md):
   - Test: Walk-only trip costs 0 COP
   - Test: Single bus boarding costs 3800 COP
   - Test: Single metro boarding costs 3820 COP
   - Test: Metro → cable car within 90 min costs 3820 COP (integration, no surcharge)
   - Test: Integrada bus → metro costs 4715 COP (bus+transit integration)
   - Test: Directa Centro bus → metro costs 7620 COP (3800 + 3820, no integration)
   - Test: Bus → bus costs 7600 COP (3800 + 3800, no integration)
   - Test: 5 mass-transit integrations costs 3820 + 750 = 4570 COP (surcharge at 5th)
   - Test: Mass-transit transfer after 90 minutes pays again (3820 + 3820)
   - Create `FareCalculatorTest.java` with hand-crafted trip objects for each case

### Verification
- Run unit tests for fare calculation rules (all pass)
- Run `ReferenceTest` and check that all 15 cases produce costs matching expected values

**Estimated effort**: 90 minutes

---

## Task 3: Update ReferenceTest to assert fares within ±100 COP

**Goal**: Make the reference test also verify fare costs, not just duration, transfers, and vehicles.

### Subtasks

3.1. Add fare assertion in `ReferenceTest.runTestCase()`:
   ```java
   int costDiff = Math.abs(actualCost - testCase.expectedCost);
   assertTrue(costDiff <= 100,
       String.format("Cost: expected %d COP, got %d COP (diff %d, tolerance ±100)",
           testCase.expectedCost, actualCost, costDiff));
   ```

3.2. Update the REF| output line to mark MISMATCH if cost differs by >100 COP:
   - Update the `status` calculation to include cost check
   - Current check: `actualCost == testCase.expectedCost`
   - New check: `Math.abs(actualCost - testCase.expectedCost) <= 100`

3.3. Update console output to show cost mismatches clearly

### Verification
- Run `mvn test` and verify ReferenceTest shows cost assertions
- Run `verify.ps1` and check that REF| lines report OK for all 15 cases

**Estimated effort**: 15 minutes

---

## Task 4: Final verification and cleanup

**Goal**: Confirm all acceptance criteria are met.

### Subtasks

4.1. Run full test suite: `mvn test`
   - Verify 0 failures, 0 errors
   - Paste the test summary output

4.2. Run `verify.ps1` and verify all REF| lines show OK

4.3. Print the REF| table showing all 15 cases with expected and actual costs

4.4. Verify the app still starts:
   - Run `mvn exec:java`
   - Verify the home screen appears

4.5. Clean up any debug output or commented code

4.6. Verify no hardcoded paths remain (use relative `data/...` paths only)

### Verification
- All 15 reference cases pass with cost within ±100 COP
- `mvn test` shows 0 failures
- App still starts and shows home screen

**Estimated effort**: 15 minutes

---

## Total Estimated Effort: ~2.5 hours

## Dependencies
- All tasks are sequential: 1 → 2 → 3 → 4
- Task 2 depends on Task 1 (route type lookup)
- Task 3 depends on Task 2 (correct fare calculation)
- Task 4 is verification only
