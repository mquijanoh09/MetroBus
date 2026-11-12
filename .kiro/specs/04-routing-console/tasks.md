# 04 · Routing Console - Tasks

## Task 1: Add Egress Walk Penalty to Router

**Goal:** Fix Router.dijkstra() to apply egress walk penalty when evaluating targets, matching the prototype behavior.

**Files:**
- `src/main/java/co/medellin/transit/routing/Router.java`

**Changes:**

1. In `dijkstra()` method, create a `Map<Integer, Double>` to store target distances:
   ```java
   Map<Integer, Double> targetDistances = new HashMap<>();
   for (State target : targets) {
       double dist = haversineDistance(target.getPlace().getCoord(), destination.getCoord());
       targetDistances.put(target.getId(), dist);
   }
   ```

2. Add field to track best egress walk:
   ```java
   int bestEgressWalkSec = 0;
   ```

3. Replace the target check logic (around line 195) with:
   ```java
   if (targetSet.contains(currentId)) {
       // Calculate egress walk penalty (matches prototype behavior)
       double egressDistMeters = targetDistances.get(currentId);
       double egressWalkMeters = egressDistMeters * params.getWalkStreetFactor();
       int egressWalkTimeSec = (int) Math.round(egressWalkMeters / params.getWalkingSpeedMps());
       int egressPerceivedCost = (int) Math.round(egressWalkTimeSec * params.getWalkPenaltyMultiplier()) 
           + params.getWalkPenaltyConstant();
       
       int totalCost = currentCost + egressPerceivedCost;
       
       if (totalCost < bestTargetCost) {
           bestTargetCost = totalCost;
           bestTargetId = currentId;
           bestEgressWalkSec = egressWalkTimeSec;
       }
       
       // Early termination check
       if (!pq.isEmpty() && pq.peek().cost + minEgressCost > bestTargetCost) {
           break;
       }
   }
   ```

4. Calculate minimum possible egress cost for early termination:
   ```java
   // Before main loop
   int minEgressCost = targetDistances.values().stream()
       .mapToInt(dist -> {
           double walkMeters = dist * params.getWalkStreetFactor();
           int walkSec = (int) Math.round(walkMeters / params.getWalkingSpeedMps());
           return (int) Math.round(walkSec * params.getWalkPenaltyMultiplier()) 
               + params.getWalkPenaltyConstant();
       })
       .min()
       .orElse(0);
   ```

5. Update return statement to include egress walk:
   ```java
   return Optional.of(new DijkstraResult(path, bestTargetCost, realDurationSec, bestEgressWalkSec));
   ```

**Verification:**
- Code compiles
- RouterTest still has failures but with DIFFERENT numbers

---

## Task 2: Update DijkstraResult to Store Egress Walk

**Goal:** Add egressWalkSec field to DijkstraResult so StepBuilder can use it.

**Files:**
- `src/main/java/co/medellin/transit/routing/DijkstraResult.java`

**Changes:**

1. Add field:
   ```java
   private final int egressWalkSec;
   ```

2. Update constructor:
   ```java
   public DijkstraResult(List<State> path, int perceivedCost, int realDurationSec, int egressWalkSec) {
       // ... existing validation ...
       this.egressWalkSec = egressWalkSec;
   }
   ```

3. Add getter:
   ```java
   public int getEgressWalkSec() {
       return egressWalkSec;
   }
   ```

4. Update any existing constructor calls in tests to pass `0` for egressWalkSec (temporary).

**Verification:**
- Code compiles
- All tests still run (may have same failures)

---

## Task 3: Update StepBuilder to Use Egress Walk

**Goal:** Ensure StepBuilder adds the final walk step using the egress walk from DijkstraResult.

**Files:**
- `src/main/java/co/medellin/transit/routing/StepBuilder.java`

**Changes:**

1. In `buildTrip()`, get egress walk seconds:
   ```java
   int egressWalkSec = result.getEgressWalkSec();
   ```

2. If the method currently calculates egress walk, replace that logic with the value from result.

3. Ensure the final walk step uses this value for both distance and time calculations:
   ```java
   double egressWalkMeters = egressWalkSec * params.getWalkingSpeedMps() / params.getWalkStreetFactor();
   // Add walk step with egressWalkMeters and egressWalkSec
   ```

**Verification:**
- Code compiles
- StepBuilder tests pass (if any)
- Total trip duration includes egress walk time

---

## Task 4: Calculate Correct Expected Values for RouterTest

**Goal:** Determine the correct expected perceived costs for the 4 failing RouterTest tests, then ask user for approval before changing.

**Process:**

For each failing test, analyze the test setup and calculate the expected perceived cost using the formula:
- Access walk: `walkSec × 1.5 + 30`
- Edges: sum of edge perceived costs
- Egress walk: `walkSec × 1.5 + 30`

### Test 1: testFindRouteSimpleWalkPath
**Current setup:**
- coord1 = (6.250, -75.560)
- coord2 = (6.255, -75.565) [~700m away]
- Walk edge: 60s real, perceived = 60×1.5+30 = 120
- Origin at coord1, destination at coord2

**Calculate:**
- Access: origin to state1 distance = ? meters → walkSec × 1.5 + 30 = ?
- Edge: 120 (given)
- Egress: state2 to dest distance = ? meters → walkSec × 1.5 + 30 = ?
- **Expected total: ?**
- **Current expected: 120**
- **Actual result: 150**

**Action:** Calculate exact distances, show calculation, ask user.

### Test 2: testFindRouteWithBoardAndRide
**Current setup:**
- coord1 = (6.250, -75.560)
- coord2 = (6.255, -75.565) [~750m]
- coord3 = (6.260, -75.570) [~750m from coord2]
- Access walk: 30s real, perceived = 30×1.5+30 = 75
- Board: 120
- Ride: 180
- Alight: 0

**Calculate:**
- Access: 75 (given)
- Edges: 120+180+0 = 300
- Egress: destination to walkState3 (coord3) distance = ? → ? × 1.5 + 30 = ?
- **Expected total: ?**
- **Current expected: 375**
- **Actual result: 405**

**Action:** Calculate egress, show calculation, ask user.

### Test 3: testFindRouteSelectsShorterPath
**Current setup:**
- Direct path: 100s walk
- Origin at coord1, destination at coord3

**Calculate:**
- Access: origin to state1 distance = ?
- Direct edge: 100s real → perceived ?
- Egress: state3 to dest distance = ?
- **Expected total: ?**
- **Current expected: 180**
- **Actual result: 210**

**Action:** Calculate full path, show calculation, ask user.

### Test 4: testBoardingPenaltyAffectsRouteChoice
**Current setup:**
- Two paths: walk (100s) vs bus (board 120 + ride 60)
- Expected: ≤180

**Calculate:**
- Walk path: access + 100×1.5+30 + egress
- Bus path: access + 120 + 60 + egress
- Compare final costs

**Action:** Show both calculations, ask user for correct assertion.

**Deliverable:**
- Write calculations in a comment or markdown
- Present to user: "These 4 tests need expected value changes. Here are the calculations showing why. Approve?"
- Wait for user approval
- Only then modify the test assertions

**Files:**
- `src/test/java/co/medellin/transit/routing/RouterTest.java`

**Verification:**
- All 14 RouterTest tests pass after changes

---

## Task 5: Merge Reference Tests

**Goal:** Merge ReferenceCasesTest into ReferenceTest, delete ReferenceCasesTest, ensure all 15 cases validated.

**Files:**
- `src/test/java/co/medellin/transit/routing/ReferenceTest.java` (modify)
- `src/test/java/co/medellin/transit/routing/ReferenceCasesTest.java` (delete)

**Changes:**

1. In ReferenceTest, keep the better structure:
   - Use ReferenceCasesTest's vehicle extraction logic (the `extr()` method)
   - Use ReferenceTest's clear assertion messages
   - Combine both approaches

2. Update the main test method:
   ```java
   @Test
   @Order(1)
   @DisplayName("Validate all 15 reference test cases")
   void validateAll15TestCases() {
       System.out.println("╔══════════════════════════════════════╗");
       System.out.println("║  Reference Test: 15 Test Cases      ║");
       System.out.println("╚══════════════════════════════════════╝\n");
       
       int passed = 0;
       int failed = 0;
       List<String> failures = new ArrayList<>();
       
       for (int i = 0; i < testCases.size(); i++) {
           TestCase tc = testCases.get(i);
           try {
               runTestCase(i + 1, tc);
               passed++;
               System.out.printf("  ✓ Case %2d: PASS\n", i + 1);
           } catch (AssertionError e) {
               failed++;
               failures.add(String.format("Case %d: %s", i + 1, e.getMessage()));
               System.out.printf("  ✗ Case %2d: FAIL - %s\n", i + 1, e.getMessage());
           }
       }
       
       System.out.printf("\n╔══════════════════════════════════════╗\n");
       System.out.printf("║  Results: %2d passed, %2d failed     ║\n", passed, failed);
       System.out.printf("╚══════════════════════════════════════╝\n");
       
       if (failed > 0) {
           fail(String.format("%d of 15 test cases failed:\n  - %s", 
               failed, String.join("\n  - ", failures)));
       }
   }
   ```

3. Ensure vehicle extraction handles all types correctly:
   ```java
   private String extractVehicleSequence(Trip trip) {
       List<String> vehicles = new ArrayList<>();
       String currentVehicle = null;
       
       for (Step step : trip.getSteps()) {
           if (step.getType() == StepType.BOARD) {
               currentVehicle = extractVehicleId(step.getDescription());
           } else if (step.getType() == StepType.ALIGHT && currentVehicle != null) {
               vehicles.add(currentVehicle);
               currentVehicle = null;
           }
       }
       
       return vehicles.isEmpty() ? "walk" : String.join(" > ", vehicles);
   }
   
   private String extractVehicleId(String description) {
       // Handle: "Abordar Ruta 191 en..."
       if (description.contains("Ruta ")) {
           int start = description.indexOf("Ruta ") + 5;
           int end = description.indexOf(" ", start);
           if (end == -1) end = description.indexOf(" en", start);
           if (end > start) {
               return "bus:" + description.substring(start, end).trim();
           }
       }
       
       // Handle: "Abordar Metro Línea A en..."
       if (description.contains("Metro Línea ")) {
           int start = description.indexOf("Línea ") + 6;
           int end = description.indexOf(" ", start);
           if (end == -1) end = description.indexOf(" en", start);
           if (end > start) {
               String line = description.substring(start, end).trim();
               return "metro:" + line;
           }
       }
       
       // Handle BRT: "Metroplús Línea 1"
       if (description.contains("Metroplús Línea ")) {
           int start = description.indexOf("Línea ") + 6;
           int end = description.indexOf(" ", start);
           if (end == -1) end = description.indexOf(" en", start);
           if (end > start) {
               return "brt:" + description.substring(start, end).trim();
           }
       }
       
       // Handle cable car: "Metrocable Línea J"
       if (description.contains("Metrocable Línea ")) {
           int start = description.indexOf("Línea ") + 6;
           int end = description.indexOf(" ", start);
           if (end == -1) end = description.indexOf(" en", start);
           if (end > start) {
               return "cable_car:" + description.substring(start, end).trim();
           }
       }
       
       // Handle tram: "Tranvía"
       if (description.contains("Tranvía")) {
           return "tram:T-A";
       }
       
       return "unknown";
   }
   ```

4. Ensure assertions use ±2 minute tolerance:
   ```java
   int minuteDiff = Math.abs(actualMinutes - expectedMinutes);
   assertTrue(minuteDiff <= 2,
       String.format("Time: expected %d min, got %d min (diff %d, tolerance ±2)",
           expectedMinutes, actualMinutes, minuteDiff));
   
   assertEquals(expectedTransfers, actualTransfers,
       String.format("Transfers: expected %d, got %d", expectedTransfers, actualTransfers));
   
   assertEquals(expectedVehicles, actualVehicles,
       String.format("Vehicles: expected '%s', got '%s'", expectedVehicles, actualVehicles));
   ```

5. Delete ReferenceCasesTest.java

**Verification:**
- `mvn test -Dtest=ReferenceTest` runs and shows all 15 cases
- ReferenceCasesTest no longer appears in Maven output
- Output shows clear table of pass/fail

---

## Task 6: Final Verification

**Goal:** Verify all tests pass and console app works correctly.

**Steps:**

1. Run full test suite:
   ```bash
   mvn test
   ```
   
2. Verify output shows:
   - `Tests run: N, Failures: 0, Errors: 0`
   - ReferenceTest shows all 15 cases validated
   - No ReferenceCasesTest in output

3. Test console app with 2 test cases:
   ```bash
   mvn exec:java -Dexec.mainClass="co.medellin.transit.Main"
   ```
   - Enter: `CL 50 53-48` → `CR 70 x CL 48`
   - Verify: ~21 min, $3,800, bus 191
   - Enter: `CL 10 43A-20` → `CL 77 65-30`
   - Verify: ~34 min, $3,820, Metro Línea A

4. Document results:
   - Paste `Tests run:` line from Maven
   - Paste ReferenceTest table showing all 15 cases
   - Paste console output for both test trips

**Files:**
- None (verification only)

**Completion criteria:**
- Zero test failures
- All 15 reference cases pass
- Console shows correct routes for test cases
- Minutes within ±2, transfers and vehicles exact match

---

## RouterTest Expected Values - Calculations Needed

**IMPORTANT:** Do NOT change any RouterTest expected values until the user approves the calculations below.

Based on the prototype's behavior (egress walk gets ×1.5 + 30 penalty), here are the cases that need review:

### Case 1: testFindRouteSimpleWalkPath
- **Issue:** Expected 120, actual 150 (difference: +30)
- **Analysis:** The +30 suggests one WALK_PENALTY_SEC is missing
- **Likely cause:** Test creates states at exact coordinates, but origin/destination may be offset
- **Calculation needed:** 
  - What is the distance from origin (6.250, -75.560) to state1 (6.250, -75.560)? If 0m, access = 30s
  - What is the distance from state2 (6.255, -75.565) to destination (6.255, -75.565)? If 0m, egress = 30s
  - If both are 0m: 30 (access) + 120 (edge) + 30 (egress) = 180, not 150
  - If one is 0m: either 120 + 30 = 150 ✓
- **Question for user:** Should test expect 150, or is the test setup wrong?

### Case 2: testFindRouteWithBoardAndRide
- **Issue:** Expected 375, actual 405 (difference: +30)
- **Analysis:** Missing one WALK_PENALTY_SEC for egress
- **Calculation:**
  - Access: 30s walk × 1.5 + 30 = 75
  - Board: 120
  - Ride: 180
  - Alight: 0
  - Egress: destination at coord3, so distance = 0? Then egress = 30
  - Total: 75 + 120 + 180 + 0 + 30 = 405 ✓
- **Question for user:** Should test expect 405?

### Case 3: testFindRouteSelectsShorterPath
- **Issue:** Expected 180, actual 210 (difference: +30)
- **Analysis:** Missing WALK_PENALTY_SEC for access or egress
- **Calculation:**
  - Direct path: 100s real time
  - Perceived: 100 × 1.5 + 30 = 180 (current expected assumes this is edge cost only)
  - But if origin != coord1 or destination != coord3:
    - Access: ? × 1.5 + 30
    - Edge: 180
    - Egress: ? × 1.5 + 30
  - If origin = coord1 and dest = coord3: just 0 + 180 + 0 = 180
  - If dest offset: 0 + 180 + 30 = 210 ✓
- **Question for user:** Is destination at coord3 exactly, or offset?

### Case 4: testBoardingPenaltyAffectsRouteChoice
- **Issue:** Expected ≤180, actual fails (likely both paths now >180)
- **Calculation:**
  - Walk path: access + (100×1.5+30) + egress = ? + 180 + ?
  - Bus path: access + 120 + 60 + egress = ? + 180 + ?
  - If origin = coord1 and dest = coord2 exactly: both = 180, tie
  - If any offset: both > 180
- **Question for user:** What should the assertion be? Maybe "walk cost ≤ bus cost" instead of absolute value?

**Action before Task 4:** Print these calculations and ask user which expected values to change.
