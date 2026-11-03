# Tasks: Address Geocoding

**Status:** Not started — awaiting user approval of design.md questions before proceeding

## Overview

Fix 3 failing geocoding tests by implementing the missing 6-unit rejection rule from algorithm.md section 3 step 4. Add acceptance tests for test_cases.csv addresses and 300-stop accuracy validation.

**Total tasks:** 6
- Task 1: User answers design questions (BLOCKING)
- Task 2: Implement 6-unit rejection rule in AddressLocator
- Task 3: Add test for test_cases.csv addresses (30 addresses)
- Task 4: Add 300-stop accuracy test
- Task 5: Add reference table size test
- Task 6: Verification and documentation

---

## Task 1: Answer Design Questions (user action required)

**Status:** Blocked — needs user input

**Questions:**

1. **6-unit threshold precision:**
   - Algorithm.md says "more than 6 address units away"
   - Should I use exactly `> 6.0` or add a small margin (e.g., `> 6.5`)?
   - **Impact:** Affects which borderline addresses get rejected

2. **Distance metric confirmation:**
   - Current code uses Euclidean distance: `sqrt((calle_diff)² + (carrera_diff)²)`
   - Is this correct per algorithm.md section 3 step 3 ("closest (calle, carrera)")?
   - **Impact:** None if confirmed; needs recalculation if wrong

3. **300-stop accuracy thresholds:**
   - Prototype measured: median 75 m, p90 327 m
   - Should I assert: median ≤ 100 m, p90 ≤ 350 m (adding margin)?
   - Or use tighter thresholds: median ≤ 80 m, p90 ≤ 330 m?
   - **Impact:** Test may be too strict or too lenient

4. **test_cases.csv expectations:**
   - File has 15 rows × 2 addresses = 30 addresses total
   - Should ALL 30 parse and locate successfully?
   - Or are some expected to fail (e.g., walk-only trips)?
   - **Impact:** Test assertions

**Acceptance:** User responds to all 4 questions with clear decisions.

---

## Task 2: Implement 6-Unit Rejection Rule

**Status:** Not started

**Prerequisites:** Task 1 completed

**Changes:**
- File: `src/main/java/co/medellin/transit/geocoding/AddressLocator.java`
- Action: Add distance check in `locate()` method

**Steps:**

1. Open `AddressLocator.java` and locate the `locate(ParsedAddress address)` method

2. Find the section after `findClosestPoints()` call (around line 35-36):
   ```java
   List<WeightedPoint> closest = findClosestPoints(targetCalle, targetCarrera, 4);
   if (closest.isEmpty()) return Optional.empty();
   ```

3. Insert new check immediately after:
   ```java
   // Reject if closest reference point is > 6 address units away (algorithm.md section 3 step 4)
   WeightedPoint closestPoint = closest.get(0);
   if (closestPoint.distance > 6.0) {  // Use threshold from Task 1 answer
       LOGGER.fine(String.format("Address (%.1f, %.1f) rejected: closest reference is %.1f units away (> 6)",
           targetCalle, targetCarrera, closestPoint.distance));
       return Optional.empty();
   }
   ```

4. Verify the check is BEFORE the interpolation loop that starts with:
   ```java
   double sumWeights = 0.0, weightedLat = 0.0, weightedLon = 0.0;
   ```

5. Compile and run the 3 failing tests:
   ```bash
   mvn test -Dtest=AddressLocatorTest#testOutOfBoundsNorth,AddressLocatorTest#testOutOfBoundsSouth,GeocodingIntegrationTest#testAddressOutOfBounds
   ```

**Expected behavior:**
- Calle 200 # 50: closest reference probably at Calle ~100, distance ~100 units → rejected ✓
- Calle -50 # 50: closest reference probably at Calle ~-20, distance ~30 units → rejected ✓
- All normal addresses (Calle 10-100, Carrera 10-150) continue to locate ✓

**Acceptance:**
- `testOutOfBoundsNorth` passes
- `testOutOfBoundsSouth` passes
- `testAddressOutOfBounds` passes
- All other existing AddressLocatorTest and GeocodingIntegrationTest tests continue to pass
- Code compiles without warnings

---

## Task 3: Add Test for test_cases.csv Addresses

**Status:** Not started

**Prerequisites:** Task 1 completed

**Changes:**
- New file: `src/test/java/co/medellin/transit/geocoding/TestCasesAddressTest.java`
- Action: Create test that validates all 30 addresses from test_cases.csv

**Steps:**

1. Create new test class:
   ```java
   package co.medellin.transit.geocoding;
   
   import co.medellin.transit.model.Coordinate;
   import org.junit.jupiter.api.BeforeEach;
   import org.junit.jupiter.api.Test;
   
   import java.io.BufferedReader;
   import java.io.FileReader;
   import java.io.IOException;
   import java.nio.file.Path;
   import java.nio.file.Paths;
   import java.util.*;
   
   import static org.junit.jupiter.api.Assertions.*;
   
   /**
    * Tests that all addresses in data/test_cases.csv can be parsed and located.
    * Per requirements.md acceptance criteria section 1.
    */
   class TestCasesAddressTest {
       private static final Path TEST_CASES_FILE = Paths.get("data/test_cases.csv");
       private AddressParser parser;
       private AddressLocator locator;
       
       @BeforeEach
       void setUp() {
           parser = new AddressParser();
           locator = new AddressLocator();
       }
       
       @Test
       void testAllTestCaseAddressesLocate() throws IOException {
           // Read test_cases.csv and extract all addresses
           List<String> addresses = new ArrayList<>();
           
           try (BufferedReader reader = new BufferedReader(new FileReader(TEST_CASES_FILE.toFile()))) {
               String line;
               boolean firstLine = true;
               while ((line = reader.readLine()) != null) {
                   if (firstLine) {
                       firstLine = false;
                       continue; // Skip header
                   }
                   String[] parts = line.split(",");
                   if (parts.length >= 2) {
                       addresses.add(parts[0].trim()); // origin
                       addresses.add(parts[1].trim()); // destination
                   }
               }
           }
           
           assertEquals(30, addresses.size(), "Should have 30 addresses (15 origins + 15 destinations)");
           
           // Test each address
           List<String> failedToParse = new ArrayList<>();
           List<String> failedToLocate = new ArrayList<>();
           
           for (String addressStr : addresses) {
               Optional<ParsedAddress> parsed = parser.parse(addressStr);
               if (parsed.isEmpty()) {
                   failedToParse.add(addressStr);
                   continue;
               }
               
               Optional<Coordinate> coord = locator.locate(parsed.get());
               if (coord.isEmpty()) {
                   failedToLocate.add(addressStr);
               }
           }
           
           // Assert all addresses parse and locate
           if (!failedToParse.isEmpty()) {
               fail(String.format("Failed to parse %d addresses: %s", 
                   failedToParse.size(), String.join(", ", failedToParse)));
           }
           
           if (!failedToLocate.isEmpty()) {
               fail(String.format("Failed to locate %d addresses: %s", 
                   failedToLocate.size(), String.join(", ", failedToLocate)));
           }
           
           // If we got here, all 30 addresses parsed and located successfully
           System.out.println("✓ All 30 addresses from test_cases.csv parsed and located successfully");
       }
   }
   ```

2. Run the test:
   ```bash
   mvn test -Dtest=TestCasesAddressTest
   ```

3. If any addresses fail, investigate:
   - Check if address format is unusual (e.g., "CL 48EE 99D-")
   - Check if parser handles all variations
   - Check if reference table has coverage in that area

**Expected behavior:**
- All 30 addresses should parse (parser handles all formats)
- All 30 addresses should locate (reference table covers all areas)
- Test passes with green output

**Acceptance:**
- Test passes: all 30 addresses parse and locate
- Test output shows "✓ All 30 addresses from test_cases.csv parsed and located successfully"
- If any fail, report which addresses and why before proceeding

---

## Task 4: Add 300-Stop Accuracy Test

**Status:** Not started

**Prerequisites:** Task 1 completed (need accuracy thresholds)

**Changes:**
- File: `src/test/java/co/medellin/transit/geocoding/GeocodingIntegrationTest.java`
- Action: Add new test method `testLocatorAccuracyOn300Stops()`

**Steps:**

1. Add new test method to GeocodingIntegrationTest:
   ```java
   @Test
   void testLocatorAccuracyOn300Stops() throws IOException {
       // Load all stops from real data
       Path dataDir = Paths.get("data");
       StopArcGisReader reader = new StopArcGisReader();
       List<Stop> allStops = reader.readPaginated(dataDir, "stops", ".json");
       
       assertEquals(3590, allStops.size(), "Should have 3,590 unique stops");
       
       // Select 300 test stops (deterministic: every 12th stop)
       List<Stop> testStops = new ArrayList<>();
       for (int i = 0; i < allStops.size(); i += 12) {
           testStops.add(allStops.get(i));
           if (testStops.size() >= 300) break;
       }
       
       assertEquals(300, testStops.size(), "Should have 300 test stops");
       
       // Test each stop
       List<Double> errors = new ArrayList<>();
       AddressParser parser = new AddressParser();
       
       for (Stop testStop : testStops) {
           // Build reference table WITHOUT this stop
           List<Stop> referenceStops = new ArrayList<>(allStops);
           referenceStops.remove(testStop);
           AddressLocator locator = new AddressLocator(referenceStops);
           
           // Parse the stop's address
           String address = testStop.getAddress();
           Optional<ParsedAddress> parsed = parser.parse(address);
           if (parsed.isEmpty()) continue; // Skip unparseable addresses
           
           // Locate it
           Optional<Coordinate> located = locator.locate(parsed.get());
           if (located.isEmpty()) continue; // Skip unlocatable addresses
           
           // Measure error (haversine distance)
           double errorMeters = haversineDistance(testStop.getCoordinate(), located.get());
           errors.add(errorMeters);
       }
       
       // Calculate statistics
       Collections.sort(errors);
       double median = errors.get(errors.size() / 2);
       double p90 = errors.get((int)(errors.size() * 0.9));
       double max = errors.get(errors.size() - 1);
       
       System.out.println(String.format("300-stop accuracy: median %.0f m, p90 %.0f m, max %.0f m",
           median, p90, max));
       
       // Assert thresholds (from Task 1 answer)
       assertTrue(median <= 100.0, 
           String.format("Median error should be ≤ 100 m, was %.0f m", median));
       assertTrue(p90 <= 350.0, 
           String.format("P90 error should be ≤ 350 m, was %.0f m", p90));
   }
   
   /**
    * Haversine distance in meters between two coordinates.
    */
   private double haversineDistance(Coordinate c1, Coordinate c2) {
       double lat1 = Math.toRadians(c1.getLat());
       double lat2 = Math.toRadians(c2.getLat());
       double deltaLat = Math.toRadians(c2.getLat() - c1.getLat());
       double deltaLon = Math.toRadians(c2.getLon() - c1.getLon());
       
       double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                  Math.cos(lat1) * Math.cos(lat2) *
                  Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
       double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
       
       return 6371000 * c; // Earth radius in meters
   }
   ```

2. Add required imports:
   ```java
   import co.medellin.transit.data.StopArcGisReader;
   import co.medellin.transit.model.Stop;
   import java.io.IOException;
   import java.nio.file.Path;
   import java.nio.file.Paths;
   import java.util.*;
   ```

3. Run the test:
   ```bash
   mvn test -Dtest=GeocodingIntegrationTest#testLocatorAccuracyOn300Stops
   ```

4. Verify results match prototype expectations:
   - Median should be ~75 m (assert ≤ 100 m)
   - P90 should be ~327 m (assert ≤ 350 m)

**Expected behavior:**
- Test measures accuracy on 300 real stops
- Results should be close to prototype: median ~75 m, p90 ~327 m
- Test passes with actual measurements logged

**Acceptance:**
- Test passes: median ≤ 100 m, p90 ≤ 350 m
- Console output shows actual statistics
- If results are significantly different from prototype, investigate before proceeding

---

## Task 5: Add Reference Table Size Test

**Status:** Not started

**Prerequisites:** None (can run independently)

**Changes:**
- File: `src/test/java/co/medellin/transit/geocoding/AddressLocatorTest.java`
- Action: Add new test method `testReferenceTableSize()`

**Steps:**

1. Add new test method to AddressLocatorTest:
   ```java
   @Test
   void testReferenceTableSize() throws IOException {
       // Load stops from real data
       Path dataDir = Paths.get("data");
       StopArcGisReader reader = new StopArcGisReader();
       List<Stop> stops = reader.readPaginated(dataDir, "stops", ".json");
       
       assertEquals(3590, stops.size(), "Should have 3,590 unique stops");
       
       // Build reference table
       AddressLocator locator = new AddressLocator(stops);
       
       // Access reference table size via reflection (since it's private)
       // Alternative: add a public method getReferenceTableSize() to AddressLocator
       // For now, test indirectly by counting locatable addresses
       
       AddressParser parser = new AddressParser();
       int parseableCount = 0;
       
       for (Stop stop : stops) {
           String address = stop.getAddress();
           Optional<ParsedAddress> parsed = parser.parse(address);
           if (parsed.isPresent()) {
               parseableCount++;
           }
       }
       
       // Per requirements.md section 2: at least 3,481 stops (97% of 3,590)
       assertTrue(parseableCount >= 3481, 
           String.format("Reference table should have ≥ 3,481 parseable addresses, found %d (%.1f%%)",
               parseableCount, 100.0 * parseableCount / stops.size()));
       
       System.out.println(String.format("Reference table: %d / %d stops parseable (%.1f%%)",
           parseableCount, stops.size(), 100.0 * parseableCount / stops.size()));
   }
   ```

2. Add required imports:
   ```java
   import co.medellin.transit.data.StopArcGisReader;
   import co.medellin.transit.model.Stop;
   import java.io.IOException;
   import java.nio.file.Path;
   import java.nio.file.Paths;
   import java.util.List;
   ```

3. Run the test:
   ```bash
   mvn test -Dtest=AddressLocatorTest#testReferenceTableSize
   ```

**Expected behavior:**
- Test loads 3,590 stops
- Counts parseable addresses
- Should find ≥ 3,481 (97%)
- Test passes and logs actual count

**Acceptance:**
- Test passes: ≥ 3,481 parseable addresses
- Console output shows actual count and percentage
- Confirms requirement 2 from requirements.md

---

## Task 6: Verification and Documentation

**Status:** Not started

**Prerequisites:** Tasks 2-5 completed

**Actions:**
1. Run full test suite to verify no regressions
2. Run specific geocoding tests to confirm all pass
3. Update COMPLETION.md with results
4. Verify acceptance criteria met

**Steps:**

1. Run all geocoding tests:
   ```bash
   mvn test -Dtest=*geocoding*Test,*Geocoding*Test
   ```

2. Verify specific test counts:
   - AddressLocatorTest: expect ~24 tests, 0 failures
   - GeocodingIntegrationTest: expect ~11 tests, 0 failures  
   - TestCasesAddressTest: expect 1 test, 0 failures

3. Run full test suite:
   ```bash
   mvn test
   ```

4. Verify acceptance criteria from requirements.md:
   - ✅ All 30 addresses from test_cases.csv locate (Task 3)
   - ✅ 300-stop accuracy: median ≤ 100 m (Task 4)
   - ✅ Calle 200 # 50 not located (Task 2)
   - ✅ mvn test: 0 failures, 0 errors

5. Create COMPLETION.md documenting:
   - What was fixed (6-unit rejection rule)
   - Test results (all passing)
   - Accuracy measurements (median, p90)
   - Reference table size (actual count)

**Acceptance:**
- `mvn test` output shows: `Tests run: N, Failures: 0, Errors: 0`
- All geocoding tests pass
- Paste actual "Tests run" line from mvn output
- Accuracy measurements logged and within thresholds

---

## Summary

**Implementation sequence:**
1. Task 1: Get user answers (BLOCKING for all other tasks)
2. Task 2: Fix AddressLocator (3-4 lines of code)
3. Task 3: Add test_cases.csv test (new file, ~70 lines)
4. Task 4: Add 300-stop accuracy test (~60 lines added to existing file)
5. Task 5: Add reference table size test (~30 lines added to existing file)
6. Task 6: Verification and documentation

**Files to modify:**
- Production: 1 file (AddressLocator.java)
- Tests: 3 files (2 existing + 1 new)
- Total lines changed: ~150 lines

**Key decisions needed from user (Task 1):**
- 6-unit threshold (exactly 6.0 or with margin?)
- Distance metric confirmation (Euclidean correct?)
- Accuracy test thresholds (100m/350m or tighter?)
- test_cases.csv expectations (all 30 should locate?)
