# 01 · Data loading

## Introduction

Load the real transit files in `data/` into Java objects. This spec covers only reading files; building the graph is spec 02. The file formats, sample rows and expected counts are in `.kiro/steering/data.md`. The existing readers live in `co.medellin.transit.data`; fix or replace them as needed.

## Requirements

### 1. Bus segments
WHEN `data/bus_segments.csv` is read THEN the system SHALL return 9,550 segments with route id, direction, from/to stop ids, sequence numbers and `used_m`.

### 2. Transit lines
WHEN `data/transit_lines.csv` is read THEN the system SHALL return 98 station rows grouped into 11 lines (A, B, 1, 2, H, J, K, L, M, P, T-A) in `order`, keeping UTF-8 station names (e.g. `Niquía`) and treating the empty `dist_to_next_m`/`sec_to_next` of the last station as "no next station".

### 3. Walk links
WHEN `data/walk_links.csv` is read THEN the system SHALL return 25,865 links, parsing quoted names that contain commas, with place keys `STOP:<id>` or `STATION:<name>` and `kind` in {`stop_stop`, `stop_station`, `station_station`}.

### 4. Stops
WHEN the six `data/raw/stops_*.json` files are read (offsets 0, 2000, 4000, 6000, 8000, 10000) THEN the system SHALL read 10,043 features and produce 3,590 unique physical stops keyed by `id_paradero`, with coordinates from `latitud`/`longitud` and the address from `direccion`.

### 5. Routes
WHEN `data/raw/routes.json` is read THEN the system SHALL return 252 routes with `id_ruta` (as integer), `codigo`, `nombre` and `tipo`.

### 6. Stations
WHEN `data/raw/transit_stations.json` is read THEN the system SHALL return 81 stations with name, line and coordinates from `geometry.x`/`geometry.y`.

### 7. Bad rows
WHEN a row or feature is malformed THEN the system SHALL skip it, log a warning with the file and line, and keep reading.

### 8. Data directory
WHEN the app starts THEN the system SHALL use `data/` relative to the working directory (overridable by a command-line argument), never an absolute path.

## Acceptance criteria

- One test per file that reads the **real** file in `data/` and asserts the exact counts above (9,550 / 98 / 25,865 / 10,043 and 3,590 / 252 / 81).
- Small unit tests with sample text for malformed rows.
- `mvn test`: 0 failures, 0 errors (paste the line).
