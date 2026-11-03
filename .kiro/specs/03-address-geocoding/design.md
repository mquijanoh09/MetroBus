# Design: Address Geocoding

## Problem Analysis

The geocoding system (AddressParser + AddressLocator) successfully parses addresses and interpolates coordinates from reference stops, but **does NOT implement the rejection rule** from algorithm.md section 3 step 4:

> **Reject** the address if it cannot be parsed, or if the closest reference stop is more than 6 address units away (about 6 blocks). The app then says «No reconozco esa dirección» and shows a valid example.

### Current Behavior

`AddressLocator.locate()` currently:
1. ✅ Finds the 4 closest reference points by address distance
2. ✅ Computes inverse-distance-weighted average of their coordinates
3. ✅ Rejects if interpolated coordinate is out of bounds (lat/lon limits)
4. ❌ **MISSING:** Does NOT check if closest reference point is > 6 address units away

This causes 3 test failures:
- `AddressLocatorTest.testOutOfBoundsNorth` - Calle 200 # 50 should NOT locate
- `AddressLocatorTest.testOutOfBoundsSouth` - Calle -50 # 50 should NOT locate
- `GeocodingIntegrationTest.testAddressOutOfBounds` - Calle 200 # 50 should NOT locate

### Why the Rule Matters

Without the 6-unit rejection rule:
- **Calle 200 # 50** (way outside Medellín) finds some reference stop at Calle 100 and interpolates north
- The interpolated coordinate might still be within lat/lon bounds by chance
- But the result is **unreliable extrapolation** from stops 100+ blocks away
- Algorithm.md says: "the closest reference stop is more than 6 address units away (e.g. `Calle 200 # 50`)" — this exact example should be rejected

## Test Failure Analysis

### testOutOfBoundsNorth (AddressLocatorTest line 230)

```java
ParsedAddress address = new ParsedAddress(StreetType.CALLE, 200.0, 50.0, Optional.empty(), Optional.empty());
Optional<Coordinate> result = locator.locate(address);
assertTrue(result.isEmpty(), "Out-of-bounds address should not locate");
```

**Expected:** `result.isEmpty() == true`
**Actual:** `result.isEmpty() == false` (address locates successfully)

**Why it fails:** Calle 200 is ~100+ units from the highest calle in reference table (probably around Calle 100-ish). Without the 6-unit check, the locator interpolates from distant stops and returns a coordinate.

**Question for user:** What is the actual highest calle number in the reference table? I need to verify the 6-unit distance calculation.

### testOutOfBoundsSouth (AddressLocatorTest line 246)

```java
ParsedAddress address = new ParsedAddress(StreetType.CALLE, -50.0, 50.0, Optional.empty(), Optional.empty());
Optional<Coordinate> result = locator.locate(address);
assertTrue(result.isEmpty(), "Out-of-bounds address should not locate");
```

**Expected:** `result.isEmpty() == true`
**Actual:** `result.isEmpty() == false`

**Why it fails:** Calle -50 (50 Sur) is way south of the city. The lowest calle in the reference table is probably around Calle -20 to -30. The distance is > 6 units but not checked.

### testAddressOutOfBounds (GeocodingIntegrationTest line 143)

Same as testOutOfBoundsNorth — tests the exact example from algorithm.md: "Calle 200 # 50".

## Design Decisions

### 1. Implement the 6-Unit Rejection Rule

Add a check in `AddressLocator.locate()` BEFORE interpolation:

```java
// After finding closest points, before interpolation
if (closest.isEmpty()) return Optional.empty();

// NEW: Check if closest point is > 6 address units away
WeightedPoint closestPoint = closest.get(0); // Already sorted by distance
if (closestPoint.distance > 6.0) {
    LOGGER.fine(String.format("Address (%.1f, %.1f) rejected: closest reference is %.1f units away (> 6)",
        targetCalle, targetCarrera, closestPoint.distance));
    return Optional.empty();
}

// Continue with interpolation...
```

**Rationale:**
- Algorithm.md section 3 step 4 is explicit: "more than 6 address units away"
- The `distance` in `WeightedPoint` is already the Euclidean distance in address space: `sqrt((calle_diff)² + (carrera_diff)²)`
- This matches the prototype behavior (verified in algorithm.md section 3 step 5: "median error 75 m, p90 327 m" on 300 stops)

### 2. Keep Bounds Check as Additional Safety

The existing `isInBounds(lat, lon)` check remains AFTER interpolation as a safety net for:
- Edge cases where 6-unit rule doesn't catch everything
- Protecting against invalid coordinates (NaN, infinity, etc.)

Both checks are needed:
1. **6-unit check:** Rejects addresses too far from known data (algorithm.md requirement)
2. **Bounds check:** Ensures final coordinate is within Medellín metro area (safety)

### 3. No Changes to AddressParser

AddressParser is working correctly:
- Parses all formats in requirements.md section 1
- Handles letters (43A → 43.075)
- Handles SUR (makes calle negative)
- Returns `Optional.empty()` for unparseable input

No changes needed.

### 4. Test Coverage Strategy

**Existing tests that will pass after fix:**
- `testOutOfBoundsNorth` — Calle 200 is > 6 units from any reference
- `testOutOfBoundsSouth` — Calle -50 is > 6 units from any reference
- `testAddressOutOfBounds` — Same as testOutOfBoundsNorth

**New tests needed for requirements.md:**
1. **Test all 30 addresses from test_cases.csv** — Per acceptance criteria section 1
   - Parse and locate all origin/destination addresses
   - Verify all parse and locate successfully
   - This validates the reference table has good coverage

2. **Test 300-stop accuracy** — Per acceptance criteria section 2
   - For 300 real stops: locate each stop's own address with that stop excluded from reference table
   - Measure error distance (haversine between interpolated and actual)
   - Assert median error ≤ 100 m (prototype measured 75 m)
   - Assert p90 error ≤ 350 m (giving some margin over prototype's 327 m)

3. **Test reference table size** — Per requirements.md section 2
   - Assert reference table has ≥ 3,481 points (97% of 3,590 stops)
   - Currently: ~3,481 per logs, so should pass

## Questions for User

Before implementing, I need clarification:

1. **6-unit threshold:** Algorithm.md says "more than 6 address units away". Should this be exactly 6.0, or should I use a slightly higher threshold (e.g., 6.5) to give some margin?

2. **Distance metric:** The current code uses Euclidean distance `sqrt((calle_diff)² + (carrera_diff)²)`. This is correct per algorithm.md section 3 step 3 ("closest (calle, carrera)"), right?

3. **Test expectations:** For the 300-stop accuracy test, what thresholds should I use?
   - Median error ≤ 100 m (prototype: 75 m)?
   - P90 error ≤ 350 m (prototype: 327 m)?

4. **test_cases.csv addresses:** I see 15 rows in test_cases.csv with origin and destination columns. That's 30 addresses total. Should all 30 parse and locate, or are some expected to fail?

## Implementation Plan

### Phase 1: Fix AddressLocator (6-unit rejection rule)

**File:** `src/main/java/co/medellin/transit/geocoding/AddressLocator.java`

**Change:** Add distance check in `locate()` method:

```java
public Optional<Coordinate> locate(ParsedAddress address) {
    if (address == null || referenceTable.isEmpty()) return Optional.empty();
    
    double targetCalle = address.getPrimaryNumber();
    double targetCarrera = address.getSecondaryNumber();
    
    if (address.getStreetType() == StreetType.CARRERA) {
        double temp = targetCalle;
        targetCalle = targetCarrera;
        targetCarrera = temp;
    }
    
    List<WeightedPoint> closest = findClosestPoints(targetCalle, targetCarrera, 4);
    if (closest.isEmpty()) return Optional.empty();
    
    // NEW: Reject if closest reference point is > 6 address units away
    WeightedPoint closestPoint = closest.get(0);
    if (closestPoint.distance > 6.0) {
        return Optional.empty();
    }
    
    // Continue with existing interpolation logic...
    double sumWeights = 0.0, weightedLat = 0.0, weightedLon = 0.0;
    // ... rest unchanged
}
```

**Lines to change:** Insert new check around line 36 (after `if (closest.isEmpty())`, before interpolation loop)

**Expected impact:**
- `testOutOfBoundsNorth` passes (Calle 200 rejected)
- `testOutOfBoundsSouth` passes (Calle -50 rejected)  
- `testAddressOutOfBounds` passes (Calle 200 rejected)
- All other existing tests continue to pass (addresses within 6 units)

### Phase 2: Add Test for test_cases.csv Addresses

**New file:** `src/test/java/co/medellin/transit/geocoding/TestCasesAddressTest.java`

**Purpose:** Validate requirement "all 30 addresses in data/test_cases.csv locate"

**Test logic:**
1. Read test_cases.csv (15 rows)
2. Extract all origin and destination addresses (30 total)
3. For each address: parse and locate
4. Assert all parse successfully
5. Assert all locate successfully
6. Report any failures with address text

**Acceptance:** All 30 addresses parse and locate

### Phase 3: Add 300-Stop Accuracy Test

**New test method in:** `src/test/java/co/medellin/transit/geocoding/GeocodingIntegrationTest.java`

**Method:** `testLocatorAccuracyOn300Stops()`

**Test logic:**
1. Load all 3,590 stops from real data
2. Build reference table from all stops
3. Select 300 stops (deterministic: every 12th stop to cover range)
4. For each test stop:
   - Remove it from reference table
   - Parse and locate its address
   - Measure haversine distance to actual coordinate
   - Store error distances
5. Calculate median and p90
6. Assert median ≤ 100 m
7. Assert p90 ≤ 350 m (margin over prototype's 327 m)

**Acceptance:** Median ≤ 100 m, p90 ≤ 350 m

### Phase 4: Add Reference Table Size Test

**New test method in:** `src/test/java/co/medellin/transit/geocoding/AddressLocatorTest.java`

**Method:** `testReferenceTableSize()`

**Test logic:**
1. Load stops from real data
2. Build reference table
3. Assert size ≥ 3,481 (97% of 3,590)

**Acceptance:** Reference table has ≥ 3,481 points

## File Changes Summary

### Production Code (1 file)
- `src/main/java/co/medellin/transit/geocoding/AddressLocator.java`
  - Add 6-unit distance check in `locate()` method (3-4 lines)

### Test Code (3 files)
- `src/test/java/co/medellin/transit/geocoding/AddressLocatorTest.java`
  - Add `testReferenceTableSize()` method
  - Existing `testOutOfBoundsNorth`, `testOutOfBoundsSouth` will pass after fix

- `src/test/java/co/medellin/transit/geocoding/GeocodingIntegrationTest.java`
  - Add `testLocatorAccuracyOn300Stops()` method
  - Existing `testAddressOutOfBounds` will pass after fix

- `src/test/java/co/medellin/transit/geocoding/TestCasesAddressTest.java` (NEW)
  - Add test to validate all 30 addresses from test_cases.csv

## Success Criteria

Per requirements.md acceptance criteria:

✅ **Test all 30 addresses from test_cases.csv:** New TestCasesAddressTest validates all parse and locate

✅ **300-stop accuracy test:** New testLocatorAccuracyOn300Stops validates median ≤ 100 m

✅ **Calle 200 # 50 not located:** After fix, testOutOfBoundsNorth / testAddressOutOfBounds pass

✅ **mvn test: 0 failures, 0 errors:** All existing tests pass + new tests pass

## Risk Assessment

**Low risk changes:**
- Adding 6-unit check is a pure addition (no existing logic changed)
- Only affects addresses >6 units from reference data (edge cases)
- All normal addresses (within the city) continue to work

**Testing strategy:**
- Run existing geocoding tests first to verify no regression
- Add new tests incrementally
- Verify against prototype results (algorithm.md section 3 step 5)

## Alternative Considered and Rejected

**Alternative:** Use lat/lon bounds only (current behavior)

**Rejected because:**
- Doesn't match algorithm.md specification
- Allows unreliable extrapolation from distant stops
- Fails the exact example in the docs ("Calle 200 # 50")
- Tests explicitly require this behavior
