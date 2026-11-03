# 03 · Address geocoding

## Introduction

Turn a Medellín address typed by the user into coordinates, as described in #[[file:docs/algorithm.md]] section 3 and `docs/graph-structure.md` section 5. The stops' own addresses (`direccion` in the stops JSON) are the map. The existing code is `co.medellin.transit.geocoding` (`AddressParser`, `AddressLocator`).

Depends on: spec 01 done (stops loaded).

## Requirements

### 1. Parsing
WHEN the user types an address in the formats used in `data/test_cases.csv` (`CL 50 53-48`, `CR 70 x CL 48`, `CL 48EE 99D-`, `CL 12 Sur x CR 50`, `CR 143A x CL 60C`) THEN the system SHALL parse it into a (calle, carrera) pair. Full words (`Calle`, `Carrera`) and `#` SHALL also be accepted.

### 2. Reference map
WHEN the stops are loaded THEN the system SHALL build its reference table from the stop addresses, parsing at least 3,481 of the 3,590 stops (97 %).

### 3. Locating
WHEN an address is parsed THEN the system SHALL locate it by averaging the coordinates of the 4 reference stops with the closest (calle, carrera), weighted by 1/distance (`algorithm.md` section 3, step 3).

### 4. Out of the city
IF the closest reference stop is more than 6 address units away (e.g. `Calle 200 # 50-10`) THEN the system SHALL return "not found" instead of a coordinate, and the app SHALL show «No reconozco esa dirección» with a valid example.

### 5. Invalid input
WHEN the input cannot be parsed THEN the system SHALL return "not found" without throwing, and the app SHALL ask again.

## Acceptance criteria

- A test that parses and locates all 30 origin and destination addresses in the **real** `data/test_cases.csv`.
- A test on 300 **real** stops: locate each stop's own address with that stop left out of the reference table, and assert a median error of at most 100 m (the prototype measured 75 m median, 327 m p90).
- A test that `Calle 200 # 50-10` is not located.
- `mvn test`: 0 failures, 0 errors (paste the line).
