# 04 · Routing, end to end in the console

## Introduction

The minimum working app: the user types an origin and a destination in the console, the system finds the best route and prints it. Follow #[[file:docs/algorithm.md]] sections 4 to 7 and the reference implementation `reference/prototype.py` (`find_route`, `to_steps`). Fares are spec 05; in this spec only minutes, transfers and vehicles must match.

Existing code: `co.medellin.transit.routing` (`Router`, `DijkstraResult`, `StepBuilder`) and `ui/Menu.java`.

Depends on: specs 01, 02 and 03 done.

## Requirements

### 1. Access and egress
WHEN a route is requested THEN every place within 500 m (straight line) of the origin SHALL be a Dijkstra source with initial perceived cost walk_sec × 1.5 + 30 s, and every place within 500 m of the destination a target, adding the egress walk the same way. walk_sec = distance × 1.3 ÷ 1.25 m/s.

### 2. Walking the whole way
WHEN a route is requested THEN walking directly to the destination SHALL always be a candidate (perceived walk_sec × 1.5 + 30 s on the direct distance). IF the best transit route's real duration is not shorter than walking THEN the system SHALL return a walk-only trip.

### 3. Same place / no stops
IF origin and destination are less than 50 m apart, or there is no stop within 500 m of either, THEN the system SHALL show a clear Spanish message instead of a route.

### 4. Real duration
WHEN a route is shown THEN its duration SHALL be the sum of real seconds: access walk + edge `realSec` (waits, rides, walks) + egress walk. Penalties (+120 s boarding, ×1.5 and +30 s walking) are for choosing the route only and SHALL NOT be added to the shown duration.

### 5. Steps
WHEN a route is shown THEN it SHALL include the access walk, each boarding and alighting, each ride, transfer walks and the egress walk, with walking meters added to the trip total. Consecutive walks are merged.

### 6. Robustness
WHEN any query is made THEN the app SHALL NOT crash; after showing the result it SHALL ask for the next query until the user types `salir`.

## Acceptance criteria

- `ReferenceCasesTest` (or similar, name ending in `Test`) runs all 15 rows of the **real** `data/test_cases.csv` through the same code the console uses, one assertion set per row: minutes within ±2, transfers equal, and the vehicle sequence equal to the `vehicles` column (`bus:<codigo>`, `metro:A`, `brt:2`, `cable_car:J`, `walk`). The test must fail if any row does not match.
- Show that Maven runs it, and paste the table of 15 rows (expected vs actual).
- Run the app from the console with two of the cases and paste the output.
- `mvn test`: 0 failures, 0 errors (paste the line).
