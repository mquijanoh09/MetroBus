# Design: Trip Display

## Overview

Spec 06 improves the user-facing trip output to match the Python prototype's formatting. The spec addresses four main issues: showing human-readable vehicle names instead of internal route IDs, displaying actual stop/station names instead of user-typed addresses, ensuring time consistency, completing the trip summary, and fixing UTF-8 encoding in the console.

## Components

### 1. Vehicle Label Formatter (`VehicleLabelFormatter`)

**Purpose**: Convert internal route identifiers to user-friendly labels.

**Design**:
- New class in `co.medellin.transit.routing` package
- Use `RouteCatalog` to load route metadata from `data/raw/routes.json`
- Use `TransitGraph.getRouteType()` to determine if a route is transit or bus
- Format according to prototype's `LINE_LABEL` and `vehicle()` function:
  - Buses: `"bus {codigo} ({nombre})"` (e.g., "bus 191 (Laureles Éxito)")
  - Metro: `"Metro Línea {line}"` (e.g., "Metro Línea A")
  - Metroplús: `"Metroplús Línea {line}"` (e.g., "Metroplús Línea 2")
  - Metrocable: `"Metrocable Línea {line}"` (e.g., "Metrocable Línea J")
  - Tranvía: `"Tranvía de Ayacucho"` (no line number)

**Key Methods**:
- `String formatVehicle(String routeId)`: Main formatting method
- `Optional<String> formatBusRoute(String routeId)`: Format bus routes using RouteCatalog
- `String formatTransitLine(String line, String mode)`: Format mass transit lines

### 2. StepBuilder Improvements

**Changes to `StepBuilder`**:
- Add `VehicleLabelFormatter` as a dependency
- Update `createBoardStep()` to use formatted vehicle labels instead of raw route IDs
- Update `createRideStep()` to use formatted vehicle labels
- Ensure place names come from graph data (stop addresses or station names), not user input

**Key Changes**:
- Board step: `"Abordar {formatted_vehicle} en {stop_address}"`
- Ride step: `"Viajar en {formatted_vehicle} por {minutes} minutos"`
- The places shown should be from `State.getPlace().getName()`, which contains actual stop/station data

### 3. TripPrinter Enhancements

**Changes to `TripPrinter`**:
- Add transfer count to summary
- Add data attribution notice: `"Datos de la Alcaldía de Medellín; pueden no estar al día"`
- Fix UTF-8 encoding for console output by creating a `PrintStream` with UTF-8 charset

**New Summary Fields**:
- `"Transferencias: {count}"` (count = number of vehicles - 1, minimum 0)
- Attribution notice appears after instructions

**Encoding Fix**:
- Wrap `System.out` in `PrintStream` with `StandardCharsets.UTF_8`
- Apply at TripPrinter construction time
- Document in README that PowerShell users may need `chcp 65001` before running

### 4. Time Consistency

**Verification**:
- `TripPrinter.printSummary()` uses `Trip.getTotalDurationSec()` for total time
- Each step's minutes are calculated from `Step.getDurationSec()`
- Both come from the same `DijkstraResult.getRealDurationSec()`
- The sum of step minutes should equal the total (within ±1 minute for rounding)
- Wait time is included in the BOARD step's duration (from `edge.getRealSec()`)

**Implementation Note**:
- The RIDE step already includes wait time because `StepBuilder.createBoardStep()` passes `edge.getRealSec()` (the wait) to the step, and `createRideStep()` accumulates `rideDurationSec` which includes the initial wait.
- Ensure wait time is visible: either as part of the ride duration or as a separate mention in the step description.

### 5. Config Properties File Cleanup

**Fix UTF-8 Corruption**:
- Rewrite 11 corrupted comment lines in `config.properties`
- Replace `EF BF BD` (U+FFFD) with correct Spanish characters
- Lines with corruption: 1 ("Medellín"), 12 ("Medellín"), 40 ("Metroplús"), 49 ("Metroplús"), etc.
- Save file as UTF-8 encoding
- Keep all property values unchanged (e.g., `data.directory=data`)

## Data Flow

1. **Router** finds optimal path and creates `DijkstraResult`
2. **StepBuilder** converts path to steps:
   - Uses `VehicleLabelFormatter` to format vehicle names
   - Uses `State.getPlace().getName()` for stop/station names
   - Calculates durations from edge real times
3. **FareCalculator** calculates trip fare
4. **TripPrinter** displays formatted output:
   - Summary includes: duration, walk meters, fare, transfers, attribution
   - Steps show: formatted vehicles, actual places, correct times

## Testing Strategy

### Unit Tests

1. **VehicleLabelFormatterTest**:
   - Test bus route formatting (e.g., route 90283 → "bus 191 (Laureles Éxito)")
   - Test metro line formatting (e.g., "LA" → "Metro Línea A")
   - Test Metroplús, Metrocable, Tranvía formatting
   - Test fallback for missing routes

2. **TripPrinterTest**:
   - Verify transfer count calculation
   - Verify attribution notice appears
   - Verify time sum consistency

### Integration Tests

1. **ReferenceTest** (existing):
   - Cases 1, 6, 14 should show proper vehicle labels
   - Case 1: should contain "bus 191 (Laureles Éxito)", not "90283"
   - Times should match REF| totals (±1 minute)

2. **Manual Console Test**:
   - Run `java -jar target/transit-routing-1.0.0.jar --consola` in plain PowerShell
   - Enter: origin `CL 50 53-48`, destination `CR 70 x CL 48`
   - Verify: accents display correctly, vehicle shows as "bus 191 (Laureles Éxito)"
   - Verify: times add up to total

## Edge Cases

1. **Missing Route Metadata**: If `routes.json` doesn't contain a bus route, fall back to showing the route ID
2. **Transit Line Parsing**: Line identifiers like "LA", "L2", "J" need proper extraction
3. **Encoding**: README should note that Windows console may need UTF-8 mode (`chcp 65001`)
4. **Zero Transfers**: Walking-only trip should show "Transferencias: 0"

## Files Changed

### New Files
- `src/main/java/co/medellin/transit/routing/VehicleLabelFormatter.java`
- `src/test/java/co/medellin/transit/routing/VehicleLabelFormatterTest.java`

### Modified Files
- `src/main/java/co/medellin/transit/routing/StepBuilder.java`: Use VehicleLabelFormatter
- `src/main/java/co/medellin/transit/ui/TripPrinter.java`: Add transfers, attribution, UTF-8 encoding
- `src/main/resources/config.properties`: Fix UTF-8 corruption
- `README.md`: Add note about UTF-8 console encoding

### No Changes
- Routing logic (specs 04, 05)
- Fare calculation
- Graph structure

## Acceptance Criteria

1. ✅ Vehicle names: Case 1 output contains "bus 191 (Laureles Éxito)", not "90283"
2. ✅ Places: Steps show actual stop addresses or station names from graph data
3. ✅ Time consistency: Sum of step minutes = total minutes (±1 for rounding)
4. ✅ Summary complete: Shows transfers count and attribution notice
5. ✅ Encoding: UTF-8 output works in Windows console
6. ✅ Config clean: No U+FFFD characters in config.properties
7. ✅ Tests pass: `mvn test` shows 0 failures, 0 errors
8. ✅ Reference cases: 15/15 REF| lines match test_cases.csv
