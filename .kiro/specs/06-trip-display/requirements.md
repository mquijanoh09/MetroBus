# 06 · Trip display

## Introduction

Show the route to the user in clear Spanish, like the prototype's `to_steps` and `vehicle` output. The GUI under `ui/gui/` is owned by another workstream; this spec covers the shared step text and the console (`TripPrinter`, `StepBuilder`).

Depends on: specs 04 and 05 done.

## Requirements

### 1. Vehicle names
WHEN a step names a vehicle THEN it SHALL show `bus <codigo> (<nombre>)` for buses (from `routes.json`, e.g. `bus 191 (Laureles Éxito)`), `Metro Línea A`, `Metroplús Línea 2`, `Metrocable Línea J` or `Tranvía de Ayacucho` (the prototype's `LINE_LABEL`), never an internal id like `90007`.

### 2. Places
WHEN a step names a place THEN it SHALL show the station name or the stop address.

### 3. Summary
WHEN a trip is shown THEN it SHALL show the total minutes, total walking meters, cost formatted as `$3.800 COP`, the number of transfers and the notice «Datos de la Alcaldía de Medellín; pueden no estar al día».

### 4. Encoding
WHEN the app prints to the Windows console THEN accents and box characters SHALL display correctly (UTF-8 output).

## Acceptance criteria

- A test on the **real** data that the trip for `CL 50 53-48` → `CR 70 x CL 48` contains `bus 191 (Laureles Éxito)` and no 5-digit route id.
- Paste the console output for three of the reference cases.
- `mvn test`: 0 failures, 0 errors (paste the line).
