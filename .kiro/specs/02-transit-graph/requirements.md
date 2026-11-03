# 02 · Transit graph

## Introduction

Build the Dijkstra graph from the objects loaded in spec 01, exactly as described in #[[file:docs/graph-structure.md]] sections 3 and 4. Parameters come from `src/main/resources/config.properties`. The existing code is `co.medellin.transit.graph.GraphBuilder` and `TransitGraph`.

Depends on: spec 01 done.

## Requirements

### 1. States
WHEN the graph is built from the real data THEN it SHALL have 13,895 states: 3,665 `(place, WALK)`, about 10,035 bus states `(STOP:id, route, direction)` and about 196 transit states `(STATION:name, line, +/-)`. Stations with the same name are one place.

### 2. Edges
WHEN the graph is built from the real data THEN it SHALL have 82,160 edges, each with `realSec` and `perceivedCost`:
- bus ride: 9,550 from `bus_segments.csv` plus 246 links from the last outbound stop to the first inbound stop (straight line × 1.3); realSec = used_m ÷ bus speed (15 or 18 km/h by route `tipo`) + 20 s dwell; perceived = real.
- transit ride: 174 (each `transit_lines.csv` row with a next station gives one edge per direction); realSec = `sec_to_next`; perceived = real.
- board: 10,230; realSec = average wait of the mode; perceived = wait + 120 s.
- alight: 10,230; realSec = 0; perceived = 0.
- walk: 51,730 (each `walk_links.csv` row in both directions); realSec = `walk_sec`; perceived = walk_sec × 1.5 + 30 s.

### 3. Connectivity
WHEN the graph is built THEN every `(place, WALK)` state SHALL be reachable from `(STATION:Parque Berrío, WALK)` and SHALL reach it.

### 4. Startup
WHEN the app starts with the real data THEN the graph SHALL load without errors and the home screen SHALL appear.

## Acceptance criteria

- A test that builds the graph from the **real** `data/` folder and asserts 13,895 states, 82,160 edges and the per-kind edge counts above.
- A test for the Parque Berrío connectivity check on the real graph.
- `mvn test`: 0 failures, 0 errors (paste the line), and the app shows the home screen.
