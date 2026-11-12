# 04 · Routing Console - Design

## Overview

This spec completes the end-to-end routing pipeline by fixing the remaining issues in the Router's Dijkstra implementation and merging the two reference tests into a single comprehensive test that validates all 15 cases from `data/test_cases.csv`.

**Current state:** Spec 03 delivered the basic Router, but it has critical bugs in how it handles access and egress walk penalties. The prototype (`reference/prototype.py`) is the source of truth and shows that **both access and egress walks** must have the perceived cost penalty (×1.5 + 30 s) applied.

**Dependencies:** Specs 01 (data loading), 02 (graph), and 03 (geocoding) are complete.

## Problem Analysis

### Issue 1: Missing Egress Walk Penalty

**Location:** `Router.dijkstra()` method, lines ~190-200

The prototype applies the egress walk penalty when checking if a target is reached:
```python
if u[1] == 'WALK' and u[0] in targets:
    total = du + walk_sec(targets[u[0]]) * WALK_FACTOR + WALK_PENALTY_SEC
    if total < best: best, best_state = total, u
```

The Java Router currently does NOT apply this penalty - it just checks `targetSet.contains(currentId)` and uses `currentCost` directly. This causes:
- Wrong perceived costs in RouterTest (4 failures)
- Wrong route selection (faster routes appear slower)
- Mismatch with test_cases.csv expectations

**Algorithm.md section 4 confirmation:** "Sources: every place... with an initial cost equal to the walk from the point (perceived: × 1.5 + 30 s)" and "Targets: every place... kept in a map `place → meters`" - the egress walk must be added with the same penalty when evaluating targets.

### Issue 2: RouterTest Expected Values May Be Wrong

**Location:** `RouterTest.java` - 4 failing tests

The RouterTest creates synthetic graphs and asserts specific perceived costs. However, these expected values were written BEFORE algorithm.md section 4 was clarified. Since the prototype is the source of truth, we need to:

1. Check each failing RouterTest case
2. Recalculate the expected perceived cost using: access_walk × 1.5 + 30 + edges + egress_walk × 1.5 + 30
3. **List each discrepancy and ask the user before changing** any expected value

The 4 failing tests are:
- `testFindRouteSimpleWalkPath`: expects 120, gets 150
- `testFindRouteWithBoardAndRide`: expects 375, gets 405
- `testFindRouteSelectsShorterPath`: expects 180, gets 210
- `testBoardingPenaltyAffectsRouteChoice`: expects ≤180, fails

### Issue 3: Duplicate Reference Tests

**Location:** `ReferenceCasesTest.java` and `ReferenceTest.java`

Both tests validate against `data/test_cases.csv` but:
- Use different assertion logic
- Use different vehicle extraction code
- Both run all 15 cases but report differently

**Solution:** Merge into ONE test class that checks all 15 rows with consistent assertions:
- Minutes: ±2 tolerance
- Transfers: exact match
- Vehicles: exact match (format: `bus:191`, `metro:A`, `brt:2`, etc.)

## Design Decisions

### 1. Router Egress Walk Fix

**Change:** In `Router.dijkstra()`, when a target state is reached, calculate the total perceived cost including the egress walk penalty:

```java
if (targetSet.contains(currentId)) {
    // Calculate egress walk to final destination
    double egressDistMeters = haversineDistance(
        graph.getState(currentId).getPlace().getCoord(), 
        destination.getCoord()
    );
    double egressWalkMeters = egressDistMeters * params.getWalkStreetFactor();
    int egressWalkTimeSec = (int) Math.round(egressWalkMeters / params.getWalkingSpeedMps());
    int egressPerceivedCost = (int) Math.round(egressWalkTimeSec * params.getWalkPenaltyMultiplier()) 
        + params.getWalkPenaltyConstant();
    
    int totalCost = currentCost + egressPerceivedCost;
    
    if (totalCost < bestTargetCost) {
        bestTargetCost = totalCost;
        bestTargetId = currentId;
        bestEgressWalkSec = egressWalkTimeSec;  // Store for result
    }
}
```

**Impact:** This matches the prototype's behavior and fixes the perceived cost calculations.

### 2. Store Egress Walk in DijkstraResult

**Change:** Add `egressWalkSec` field to `DijkstraResult` so `StepBuilder` can add the final walk step with the correct duration.

**Rationale:** The egress walk is not in the graph edges - it's computed during routing and must be passed to the step builder.

### 3. RouterTest Expected Values

**Approach:** For each failing test:

1. **testFindRouteSimpleWalkPath**
   - Access: 0m (origin at state), perceived = 0
   - Walk edge: 60s × 1.5 + 30 = 120
   - Egress: 0m (dest at state), perceived = 0
   - **Expected total: 120** (current)
   - **Actual with egress fix:** If dest is 0m away, should stay 120
   - **Action:** Check if test setup has dest == coord2; if yes, expected is correct

2. **testFindRouteWithBoardAndRide**
   - Access: walkTime=30, perceived = 30×1.5+30 = 75
   - Board: 120
   - Ride: 180
   - Alight: 0
   - Egress: 0m?, perceived = ?
   - **Expected total: 375** (current)
   - **Actual: 405** → difference is 30 (one WALK_PENALTY_SEC)
   - **Action:** Check egress distance

3. **testFindRouteSelectsShorterPath**
   - Direct path: time=100, perceived = 100×1.5+30 = 180
   - **Expected: 180** (current)
   - **Actual: 210** → difference is 30
   - **Action:** Check if access or egress is missing

4. **testBoardingPenaltyAffectsRouteChoice**
   - Walk: 100, perceived = 150+30 = 180
   - Bus: 120+60 = 180
   - **Expected: ≤180** (current)
   - **Actual: fails** (likely now >180 with egress)
   - **Action:** Recalculate with egress

**IMPORTANT:** These will be listed in tasks.md with the instruction: "Calculate the correct expected value for each case and ask the user before changing."

### 4. Merged Reference Test

**New file:** Keep `ReferenceTest.java`, delete `ReferenceCasesTest.java`

**Structure:**
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReferenceTest {
    @Test @Order(1)
    void validateAll15TestCases() {
        // Load all 15 from test_cases.csv
        // For each: geocode, route, assert
        // Print summary table
        // Fail if any case fails
    }
}
```

**Assertions per row:**
- `Math.abs(actualMinutes - expectedMinutes) <= 2`
- `actualTransfers == expectedTransfers`
- `actualVehicles.equals(expectedVehicles)`

**Vehicle extraction:** Use consistent logic from ReferenceCasesTest's `extr()` method, which correctly handles `bus:`, `metro:`, `brt:`, `cable_car:`, `tram:` prefixes.

## Files to Modify

### 1. `Router.java` (main fix)
- Add egress walk penalty calculation in `dijkstra()`
- Store target distance map for egress calculation
- Track best egress walk seconds

### 2. `DijkstraResult.java`
- Add `egressWalkSec` field
- Add getter method
- Update constructor

### 3. `StepBuilder.java`
- Use `result.getEgressWalkSec()` to add final walk step
- Ensure egress walk distance is calculated correctly

### 4. `RouterTest.java`
- Fix expected values for 4 failing tests (after user confirmation)
- Keep all test structure intact
- Only change assertion values

### 5. `ReferenceTest.java`
- Merge functionality from ReferenceCasesTest
- Validate all 15 cases in one test method
- Use consistent vehicle extraction
- Print clear pass/fail table

### 6. Delete `ReferenceCasesTest.java`

## Test Strategy

### Phase 1: Fix Router and Update DijkstraResult
- Apply egress walk penalty in dijkstra()
- Update DijkstraResult to store egressWalkSec
- Verify RouterTest failures change (should still fail but with different numbers)

### Phase 2: Fix RouterTest Expected Values
- For each of 4 failing tests, calculate correct expected value
- Ask user to confirm each change
- Update test assertions
- Verify all 14 RouterTest tests pass

### Phase 3: Merge Reference Tests
- Merge ReferenceCasesTest into ReferenceTest
- Delete ReferenceCasesTest
- Verify merged test runs all 15 cases
- Print clear summary table

### Phase 4: Verify Complete
- `mvn test` → 0 failures
- All 15 test_cases.csv rows validated
- RouterTest all pass
- Run console app with 2 test cases

## Acceptance Criteria

1. **RouterTest:** All 14 tests pass with corrected expected values
2. **ReferenceTest:** Single test validates all 15 rows from test_cases.csv
3. **ReferenceCasesTest:** Deleted (functionality merged into ReferenceTest)
4. **Console:** App runs two test cases and shows correct output
5. **Maven:** `mvn test` → `Tests run: N, Failures: 0, Errors: 0`

## Notes

- The prototype is the source of truth - when Java disagrees, Java is wrong
- Algorithm.md section 4 specifies "× 1.5 + 30 s" for BOTH access and egress
- Never change expected values in RouterTest without user confirmation
- The merged ReferenceTest should print a table showing all 15 cases for easy debugging
