# 05 · Fares

## Introduction

Compute the trip's cost in COP following #[[file:docs/fares.md]] (2026 Cívica fares and the integration pseudocode in section 2). The existing code is `co.medellin.transit.fares` (`FareCalculator`, `FareConfig`).

Depends on: spec 04 done (routes match in minutes, transfers and vehicles).

## Requirements

### 1. Base fares
WHEN a trip is priced THEN Metro, Metroplús, tram and cable car SHALL cost 3,820 and a bus boarding 3,800.

### 2. Walk only
IF the trip has no boarding THEN its cost SHALL be 0.

### 3. Mass-transit integration
WHEN the trip transfers between Metro, Metroplús, tram and cable car within 90 minutes of the first transit payment THEN no extra fare SHALL be charged, except +750 when the integration count reaches 5, 8 and 9.

### 4. Bus + Metro integration
WHEN a bus whose route `tipo` is `Integrada` is used right before or right after mass transit, within 90 minutes, THEN that bus and the transit pair SHALL cost 4,715 in total. Only one bus per trip is integrated (the prototype's rule); the other pays 3,800.

### 5. No integration
WHEN the bus is `Directa Centro`, `Directa No Centro` or `Circular`, or the trip is bus → bus, THEN each boarding SHALL pay full fare.

## Acceptance criteria

- Unit tests for each rule above with small hand-made trips.
- `ReferenceCasesTest` now also asserts cost within ±100 COP for all 15 rows of the **real** `data/test_cases.csv`, and all 15 pass. Paste the table.
- `mvn test`: 0 failures, 0 errors (paste the line).
