# Tasks: Trip Display

## Task 1: Create VehicleLabelFormatter

**Objective**: Implement vehicle label formatting to convert internal route IDs to user-friendly names.

**Steps**:
1. Create `VehicleLabelFormatter` class in `co.medellin.transit.routing`
2. Add dependency on `RouteCatalog` (from ui.gui.model package)
3. Implement `formatVehicle(String routeId)` method:
   - Check if routeId starts with "L" (transit line)
   - If transit: parse mode and line from graph, call `formatTransitLine()`
   - If bus: look up in RouteCatalog, call `formatBusRoute()`
   - Return formatted label or fallback to routeId
4. Implement `formatBusRoute(String routeId)`:
   - Use RouteCatalog to find route metadata
   - Return `"bus {codigo} ({nombre})"` format
   - Handle missing routes gracefully (return routeId as fallback)
5. Implement `formatTransitLine(String line, String mode)`:
   - Metro: `"Metro Línea {line}"`
   - BRT (Metroplús): `"Metroplús Línea {line}"`
   - Cable car: `"Metrocable Línea {line}"`
   - Tram: `"Tranvía de Ayacucho"`
6. Add constructor accepting `TransitGraph` and data directory Path

**Files**:
- New: `src/main/java/co/medellin/transit/routing/VehicleLabelFormatter.java`

**Acceptance**:
- Route "90283" formats to "bus 191 (Laureles Éxito)"
- Line "LA" formats to "Metro Línea A"
- Line "L2" with mode "brt" formats to "Metroplús Línea 2"
- Missing routes return the routeId unchanged

---

## Task 2: Add Unit Tests for VehicleLabelFormatter

**Objective**: Verify vehicle label formatting logic.

**Steps**:
1. Create `VehicleLabelFormatterTest` in test/routing/
2. Test bus route formatting:
   - Mock RouteCatalog with test data
   - Verify format matches "bus {code} ({name})" pattern
3. Test transit line formatting:
   - Test Metro line (mode "metro")
   - Test Metroplús (mode "brt")
   - Test Metrocable (mode "cable_car")
   - Test Tranvía (mode "tram")
4. Test fallback behavior for missing routes

**Files**:
- New: `src/test/java/co/medellin/transit/routing/VehicleLabelFormatterTest.java`

**Acceptance**:
- All tests pass
- Coverage includes all transit modes and bus routes

---

## Task 3: Update StepBuilder to Use VehicleLabelFormatter

**Objective**: Replace raw route IDs with formatted vehicle labels in trip steps.

**Steps**:
1. Add `VehicleLabelFormatter` field to `StepBuilder`
2. Update constructor to accept `VehicleLabelFormatter` parameter
3. Modify `createBoardStep()`:
   - Call `formatter.formatVehicle(routeId)` to get label
   - Use formatted label in description: `"Abordar {label} en {location}"`
4. Modify `createRideStep()`:
   - Call `formatter.formatVehicle(routeId)` to get label
   - Use formatted label in description: `"Viajar en {label} por {minutes} minutos"`
5. Verify that place names come from `State.getPlace().getName()` (already correct)

**Files**:
- Modified: `src/main/java/co/medellin/transit/routing/StepBuilder.java`

**Acceptance**:
- Steps show formatted vehicle labels instead of route IDs
- Places show actual stop addresses or station names
- No compilation errors

---

## Task 4: Update Main.java to Pass VehicleLabelFormatter to StepBuilder

**Objective**: Wire up the new dependency in the application initialization.

**Steps**:
1. In `Main.main()`, after creating `TransitGraph`:
   - Create `VehicleLabelFormatter` with graph and dataDir
2. Pass formatter to `StepBuilder` constructor:
   - Change `new StepBuilder(graph)` to `new StepBuilder(graph, formatter)`
3. Update `TripPlanner` initialization in GUI mode similarly

**Files**:
- Modified: `src/main/java/co/medellin/transit/Main.java`

**Acceptance**:
- Application compiles and starts
- StepBuilder has access to formatter

---

## Task 5: Enhance TripPrinter with Transfers and Attribution

**Objective**: Add transfer count and data attribution notice to trip output.

**Steps**:
1. In `TripPrinter.printSummary()`:
   - Calculate transfer count from trip: `trip.getTransferCount()` or count ride steps - 1
   - Add line: `"Transferencias: {count}"`
   - Add after instructions: blank line + `"Datos de la Alcaldía de Medellín; pueden no estar al día"`
2. Add method to calculate transfers:
   - Count steps of type RIDE or with routeId present
   - Transfers = vehicle count - 1 (minimum 0)

**Files**:
- Modified: `src/main/java/co/medellin/transit/ui/TripPrinter.java`

**Acceptance**:
- Summary shows transfer count
- Attribution notice appears after steps
- Walk-only trips show "Transferencias: 0"

---

## Task 6: Fix UTF-8 Encoding in Console Output

**Objective**: Ensure accents and special characters display correctly in Windows console.

**Steps**:
1. In `TripPrinter` constructor:
   - Create UTF-8 PrintStream: `new PrintStream(System.out, true, StandardCharsets.UTF_8)`
   - Store as instance field
2. Replace all `System.out.println()` calls with `out.println()`
3. Replace all `System.out.printf()` calls with `out.printf()`
4. Import `java.nio.charset.StandardCharsets`

**Files**:
- Modified: `src/main/java/co/medellin/transit/ui/TripPrinter.java`

**Acceptance**:
- TripPrinter uses UTF-8 output stream
- Accents (á, é, í, ó, ú, ñ) display correctly
- Box characters (═, ✓) display correctly

---

## Task 7: Add Transfer Count to Trip Model

**Objective**: Store transfer count in Trip for easy access.

**Steps**:
1. Check if `Trip` class has a method to get transfer count
2. If not, add `getTransferCount()` method:
   - Count steps where `getRouteId()` is present (BOARD/RIDE steps)
   - Return count - 1 (minimum 0)
3. Alternative: Calculate in TripPrinter directly from steps list

**Files**:
- Modified (if needed): `src/main/java/co/medellin/transit/model/Trip.java`

**Acceptance**:
- Transfer count is accessible from Trip or calculable from steps
- Calculation is correct (number of vehicles - 1)

---

## Task 8: Clean Up config.properties UTF-8 Corruption

**Objective**: Fix corrupted characters in config.properties comments.

**Steps**:
1. Open `src/main/resources/config.properties` in a UTF-8 capable editor
2. Find all instances of `�` (U+FFFD replacement character)
3. Replace with correct Spanish characters:
   - Line 1: `"Medell�n"` → `"Medellín"`
   - Line 12: `"Medell�n"` → `"Medellín"`
   - Lines with `Metropl�s` → `Metroplús`
   - Any other corrupted characters (í, ú, ×, °)
4. Save file with UTF-8 encoding
5. Verify `data.directory=data` remains unchanged

**Files**:
- Modified: `src/main/resources/config.properties`

**Acceptance**:
- No U+FFFD characters remain
- All Spanish accents are correct
- File is saved as UTF-8
- All properties values unchanged

---

## Task 9: Update README with UTF-8 Console Note

**Objective**: Document the need for UTF-8 console encoding on Windows.

**Steps**:
1. Add section to README about console encoding
2. Mention that Windows PowerShell may need: `chcp 65001` before running
3. Explain that this ensures accents display correctly
4. Note that Git Bash and modern terminals handle UTF-8 by default

**Files**:
- Modified: `README.md`

**Acceptance**:
- README mentions UTF-8 encoding requirement
- Provides `chcp 65001` command for PowerShell
- Clear and concise instructions

---

## Task 10: Verify Time Consistency

**Objective**: Ensure step times sum to total time.

**Steps**:
1. Review `TripPrinter.printSummary()`:
   - Verify it uses `trip.getTotalDurationSec()` converted to minutes
2. Review `TripPrinter.printSteps()`:
   - Verify each step shows `step.getDurationSec()` converted to minutes with ceiling
3. Add internal verification (optional):
   - Calculate sum of all step durations
   - Compare to total duration
   - Log warning if difference > 60 seconds
4. Check `StepBuilder`:
   - Ensure BOARD step duration includes wait time
   - Ensure RIDE steps accumulate all segment times

**Files**:
- Review: `src/main/java/co/medellin/transit/ui/TripPrinter.java`
- Review: `src/main/java/co/medellin/transit/routing/StepBuilder.java`

**Acceptance**:
- Total time in summary equals sum of step times (±1 min for rounding)
- All times come from `DijkstraResult.getRealDurationSec()`

---

## Task 11: Integration Test with Real Data

**Objective**: Verify output matches reference for test cases.

**Steps**:
1. Build project: `mvn clean package`
2. Run console mode: `java -jar target/transit-routing-1.0.0.jar --consola`
3. Test case 1: origin `CL 50 53-48`, destination `CR 70 x CL 48`
   - Verify output contains "bus 191 (Laureles Éxito)"
   - Verify no "90283" appears
   - Verify stop names are shown, not user addresses
   - Verify time total is 21 minutes
   - Verify times add up
4. Test case 6: origin `CR 82 x CL 51B`, destination `CL 57A 30-87`
   - Verify shows: bus 243, Metro Línea B, bus 91
   - Verify transfer count is 2
5. Test case 14: origin `CR 97 x CL 67`, destination `CL 12 Sur x CR 50`
   - Verify shows cable car and metro lines formatted correctly
6. Copy console output for all three cases

**Files**:
- None (manual testing)

**Acceptance**:
- Case 1 output matches expectations
- All vehicle labels are formatted
- Stop/station names are shown
- Times are consistent
- Encoding works (accents display)

---

## Task 12: Run Full Test Suite and Verify

**Objective**: Ensure all tests pass including reference cases.

**Steps**:
1. Run: `mvn clean test`
2. Verify: 0 failures, 0 errors, 0 skipped
3. Check ReferenceTest output:
   - All 15 REF| lines should match test_cases.csv
   - Total duration should match within ±1 minute
   - Vehicle strings should match (e.g., "bus:191" not "bus:90283")
4. Run verify script: `.\verify.ps1`
5. Capture full output

**Files**:
- None (testing)

**Acceptance**:
- `mvn test`: 0 failures
- All 15 reference cases pass
- `verify.ps1` shows success

---

## Task 13: Document Changes and Capture Output

**Objective**: Finalize spec implementation with verification output.

**Steps**:
1. Run `.\verify.ps1` and save full output
2. Run console app and capture output for cases 1, 6, 14
3. Verify all acceptance criteria from requirements.md:
   - ✅ Vehicle names formatted correctly
   - ✅ Places show actual stop addresses
   - ✅ Time consistency
   - ✅ Summary complete with transfers and attribution
   - ✅ UTF-8 encoding works
   - ✅ config.properties clean
   - ✅ All tests pass
4. Paste outputs as requested

**Files**:
- None (documentation)

**Acceptance**:
- All outputs captured
- All requirements met
- Spec 06 complete

---

## Summary

**Total Tasks**: 13  
**Estimated Effort**: 4-6 hours  
**Critical Path**: Task 1 → 3 → 4 → 5 → 6 → 11 → 12  
**Dependencies**: 
- Task 3 depends on Task 1
- Task 4 depends on Task 3
- Task 11 depends on Tasks 1-10
- Task 12 depends on all implementation tasks
