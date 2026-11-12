# 05 · Fares: Design

## Problem

The current `FareCalculator` implementation has incorrect logic for Medellín's 2026 fare rules, resulting in 5 mismatches against the reference test cases:
- Case 6: Java produces 8515 COP, expected 11420 COP
- Case 8: Java produces 12335 COP, expected 11420 COP
- Case 9: Java produces 12335 COP, expected 11420 COP
- Case 12: Java produces 12335 COP, expected 8515 COP
- Case 14: Java produces 12355 COP, expected 4715 COP

The Python prototype `reference/prototype.py` (function `Network.fare()`) correctly implements the rules in `docs/fares.md` section 2 and produces all expected costs in `data/test_cases.csv`. It is the source of truth.

## Root Cause Analysis

Current implementation issues:

1. **Integration scope is too limited**: Current code only checks integration between exactly 2 boardings (`segments.size() == 1`). This prevents multi-leg mass-transit trips from integrating correctly.

2. **Missing route type lookup**: The code doesn't check if a bus route has `tipo_ruta = Integrada`. It needs to look up the bus route's actual type from the graph data.

3. **No integration surcharges**: The mass-transit integration rule requires adding +750 COP when the integration count reaches 5, 8, or 9. This is completely missing.

4. **Wrong integration logic flow**: The prototype has a different structure:
   - Mass-transit rides check if they should integrate with a *previous* Integrada bus
   - Bus rides check if they should integrate with *previous* mass-transit
   - The current code only checks forward integration on the second boarding

5. **No tracking of which segments were already integrated**: The prototype marks segments as `integrated` to prevent double-integration.

## Solution: Port `Network.fare()` 1:1

Replace the entire `FareCalculator.calculate()` method with a direct port of the Python prototype's algorithm from `reference/prototype.py` lines 163-177. The prototype's logic is:

```
cost = 0; transitStart = null; integrations = 0; last = None
for each ride step p:
  if p is mass transit:
     if transitStart != null and p.t0 − transitStart ≤ 90 min:      // transfer inside mass transit
         integrations += 1; if integrations in {5, 8, 9}: cost += 750
     else if last is an Integrada bus, ≤ 90 min, not yet integrated:  // integrated bus → transit
         cost += 4,715 − 3,800; transitStart = last.t0; integrations = 1; mark last integrated
     else:
         cost += 3,820; transitStart = p.t0; integrations = 1
  if p is a bus:
     if p is Integrada, last was transit and not integrated, and ≤ 90 min since transitStart:
         cost += 4,715 − 3,820; mark p integrated                  // transit → integrated bus
     else:
         cost += 3,800
  last = p
```

Key changes needed:

1. **Track `t0` for each step**: The algorithm needs the start time (seconds from trip start) for each ride step to compute the 90-minute window. Steps already have `getDurationSec()`, so we need to accumulate time as we iterate.

2. **Add route type lookup**: Query the `TransitGraph` for a bus route's `tipo_ruta` to distinguish `Integrada` from `Directa Centro`, `Circular`, etc.

3. **Track integration state**: Use a flag on each ride step (or in a parallel tracking structure) to mark which segments have been integrated.

4. **Handle mass-transit surcharges**: Add +750 when `integrations` reaches 5, 8, or 9.

5. **Distinguish mass-transit vs bus**: The prototype uses `p['mode'] != 'bus'` to identify mass-transit (metro, brt, cable_car, tram all use the same integration rules).

## Implementation Strategy

### Step 1: Add `getRouteType()` to TransitGraph
Add a method `String getRouteType(String busRouteId)` that returns the `tipo_ruta` value for a bus route (e.g., "Integrada", "Directa Centro"). This will require reading from the loaded bus stops data.

### Step 2: Rewrite `FareCalculator.calculate()`
Port the fare algorithm line-by-line from the Python prototype:
- Iterate through trip steps
- Track `t0` (cumulative time) for each step
- For each ride step, apply the logic from the pseudocode above
- Track `transitStart`, `integrations`, `last` step, and integration flags

### Step 3: Update FareSegment
Add a field `boolean integrated` to mark segments that have been integrated. Update constructors and getters.

### Step 4: Update ReferenceTest
Add fare assertions to `ReferenceTest.runTestCase()`:
```java
// Assert cost within ±100 COP
int costDiff = Math.abs(actualCost - testCase.expectedCost);
assertTrue(costDiff <= 100,
    String.format("Cost: expected %d COP, got %d COP (diff %d, tolerance ±100)",
        testCase.expectedCost, actualCost, costDiff));
```

## Acceptance Criteria

From requirements.md:
- All 15 reference cases now pass cost assertions within ±100 COP
- Unit tests for each fare rule still pass
- `mvn test`: 0 failures, 0 errors

## Data Structures

### Ride Step Information Needed
- `mode`: "bus" | "metro" | "brt" | "cable_car" | "tram"
- `routeType`: for buses, the `tipo_ruta` value (compare exactly with "Integrada")
- `t0`: start time in seconds when the BOARD step starts (before the wait). Must match the t0 column in docs/reference_trips.md
- `integrated`: flag tracking if this segment was integrated

### Critical Implementation Details
1. **t0 timing**: A ride's t0 is the cumulative trip time when its BOARD step starts. This is BEFORE the wait is added. For example, if the trip has walked for 35s, then boards, that ride's t0 = 35 (the BOARD happens at second 35, the wait adds to the ride duration). Verify against docs/reference_trips.md t0 column.

2. **Route type comparison**: Use exact string comparison `routeType.equals("Integrada")` against the `tipo_ruta` field from the stops JSON. The prototype does `p['route_type'] == 'Integrada'`.

### Variables Tracked During Iteration
- `cost`: total accumulated cost in COP
- `transitStart`: time (in seconds) when the current mass-transit integration window started, or null
- `integrations`: count of integrations within the current mass-transit window
- `last`: the previous ride step (for looking back at integration candidates)

## Testing Plan

1. Run `mvn test` and verify `ReferenceTest` shows all 15 cases with cost MATCH
2. Check the REF| lines printed by `ReferenceTest` show the correct costs
3. Run `powershell -ExecutionPolicy Bypass -File verify.ps1` and confirm 0 failures

## Notes

- **Don't change the test**: Per rules.md, never change a test's expected values to make it pass. The expected values come from `data/test_cases.csv`, which comes from the Python prototype.
- **Route type lookup location**: The route type information is in the raw stops JSON files (`data/raw/stops_*.json`), field `tipo_ruta`. The `GraphBuilder` already reads these files to build bus segments.
- **Integration tracking**: The prototype mutates the step dict with `last['integrated'] = True`. In Java, we can either add a field to `Step`, or maintain a separate `Set<Integer>` of integrated step indices.
