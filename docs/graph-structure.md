# Data and Graph Structure: Medellín Public Transit App

**Date:** October 29, 2026 · **Algorithm:** Dijkstra (decided Oct 26)
**Raw data:** `data/raw/`, downloaded on Oct 26, 2026 from the public ArcGIS REST service of the Alcaldía de Medellín
**Derived data:** `data/bus_segments.csv`, `data/transit_lines.csv`, `data/walk_links.csv`

Tags: **[VERIFIED]** = measured on the files. **[ESTIMATED]** = parameter with no official source.

---

## 1. Raw data [VERIFIED]

| File | Records | Content |
|---|---|---|
| `raw/stops_*.json` (6 pages) | 10,043 | One row per **(physical stop, route, direction)**. No duplicates. |
| `raw/routes.json` | 252 | Bus routes. Key: `id_ruta`. |
| `raw/transit_stations.json` | 81 | Metro, Metroplús (BRT), tram and cable-car stations, lat/lon (EPSG:4326). |
| `raw/route_geometry.json` | 504 | **Actual street path** of each route, one per direction (252 × 2), in meters (EPSG:9377). 294,645 points. Downloaded from layer 2, because layer 1 returns error 400 when geometry is requested. |

The field names inside the JSON files are the official Spanish names and must not be changed. The ones the app uses:

| JSON field | Meaning |
|---|---|
| `id_paradero` | Physical stop id (integer). Always has the same coordinates. |
| `id_ruta` | Route id. The unique route key; `codigo_ruta` (the code riders see) is **not** unique. |
| `nro_parada` | Stop sequence number, continuous across the whole route: outbound 1…22, then inbound 23…43. |
| `recorrido` | Direction: `Origen-Destino` = outbound, `Destino-Origen` = inbound. |
| `direccion` | Street address of the stop, for example `CL 50 53-48, Medellin`. |
| `codigo_ruta`, `nombre_ruta` | Route code and name shown to the user. |
| `tipo_ruta` | `Directa Centro`, `Integrada` (feeds the Metro, integrated fare), `Circular`, `Directa No Centro`. |
| `latitud`, `longitud`, `x_origen_nacional`, `y_origen_nacional` | WGS84 coordinates, and EPSG:9377 coordinates in meters. |

**Key figures**

- **3,590 physical stops** and 252 routes. 247 of the routes have stops; the 5 without stops (255iD, 255i, 087 special schedule, and two rural 255V routes) are ignored.
- Each stop is served by 1 to **31 routes**; 1,624 stops have only one.
- Straight-line distance between consecutive stops: median 277 m, p90 548 m.
- Mass-transit stations by line: A 21, B 6, Metroplús 1 20, Metroplús 2 10, tram T-A 8, cable cars P 4, K 3, J 3, H 2, L 2, M 2.

## 2. Data issues and how they are handled

| # | Issue | Handling |
|---|---|---|
| 1 | 69 % of the stop records were last updated in August 2018. | Use them anyway (official source), and show the notice «Datos de la Alcaldía de Medellín; pueden no estar al día». |
| 2 | Stations come with no order and no travel times. | `transit_lines.csv`, built by hand (section 2.2). |
| 3 | Shared stations appear under one line only. | In `transit_lines.csv` each station is listed on every line it serves. Stations with the same name are one place. |
| 4 | 10 stations have no bus stop within 300 m (Niquía, Bello, Madera, Ayurá, Envigado, Itagüí, Sabaneta, La Estrella, Arví, Barrio Colombia). | Expected: the bus data covers only Medellín. Those stations are reached by train or on foot. |
| 5 | 11 sequences have gaps in `nro_parada`, and one has a repeated number (route 310R, stop 5). | Sort by `nro_parada`, break ties by `objectid`, and accept the gaps. |

## 2.1 Bus segments: `bus_segments.csv` [VERIFIED]

9,550 rows, one per **directed edge** between consecutive stops of a route in its direction.
Columns: `route_id, direction (outbound|inbound), from_stop_id, to_stop_id, from_seq, to_seq, straight_m, road_m, used_m, method (road|straight_x1.3)`.

- Each stop is projected onto the street path of its route and direction, and `road_m` is the distance **along the street** between the two projections.
- Stops lie a median of **6 m** from the path (p99 = 17 m). **98 % of the segments (9,370) have a road distance.** The other 180 use the straight line × 1.3.
- 171 of the 493 paths were drawn in the opposite order to `nro_parada`; the build script detects and reverses them.
- Road distance is on average **1.28 ×** the straight line.
- **Not included in the CSV:** the link from the last outbound stop to the first inbound stop (median 343 m apart). The app adds it as one more ride edge: straight line × 1.3.

## 2.2 Mass transit: `transit_lines.csv`

98 rows. Columns: `line, mode (metro|brt|tram|cable_car), order, station, lat, lon, dist_to_next_m, sec_to_next`. Trains and cable cars run both ways with the same time, so **each row gives two edges**.

- Station order was written by hand and checked against the coordinates.
- `sec_to_next` [ESTIMATED] = straight distance ÷ commercial speed, dwell included: metro 37 km/h, Metroplús 20, tram 15, cable car 18. End-to-end totals: line A 39 min, Metroplús 1 35 min.
- **Metroplús line 2** (confirmed with Metro de Medellín and Wikipedia): Universidad de Medellín → Industriales (shared with line 1) → Barrio Colombia → San Diego → Colón → San José → La Playa → Catedral → Prado → Palos Verdes → Gardel → Berlín (shared with line 1) → Parque Aranjuez. Known gap: toward U. de M. it does not stop at Manrique or Prado; the data assumes it does.

## 2.3 Walking links: `walk_links.csv` [VERIFIED; parameters ESTIMATED]

25,865 pairs of places within 300 m in a straight line. **Each row gives two edges.** Columns: `place_a, name_a, place_b, name_b, kind (stop_stop|stop_station|station_station), straight_m, walk_sec`.
Place keys: `STOP:<id_paradero>` and `STATION:<station name>`.

- **stop ↔ station: 830 links.** 65 of the 75 stations have at least one bus stop nearby (median 11).
- **station ↔ station: 3** (Cisneros ↔ Cisneros-Moda 200 m, San José ↔ La Playa 272 m, Palos Verdes ↔ Palos Verdes B 142 m).
- **stop ↔ stop: 25,032.**
- `walk_sec` = straight_m × 1.3 ÷ 4.5 km/h.

## 3. The graph

### 3.1 Nodes: Dijkstra states (13,895 in total) [VERIFIED]

| State | Meaning | Count |
|---|---|---|
| `(place, WALK)` | Standing at a stop or station, not in a vehicle. | 3,665 |
| `(STOP:id, route_id, direction)` | On that bus route, at that stop. | ~10,035 |
| `(STATION:name, L<line>, +/-)` | On that line, at that station, in that direction. | ~196 |

The `WALK` state is what keeps transfers cheap to model. Without it, a stop with 31 routes would need 930 transfer edges; with it, each route needs only one *board* edge and one *alight* edge.

### 3.2 Edges (82,160 in total) [VERIFIED]

Each edge stores **`realSec`** (shown to the user) and **`perceivedCost`** (minimized by Dijkstra).

| Kind | From → to | realSec | perceivedCost | Count |
|---|---|---|---|---|
| ride (bus) | `(s, route, dir)` → `(next s, route, dir)` | used_m ÷ bus speed + 20 s | = realSec | 9,550 + 246 links |
| ride (transit) | `(st, L, ±)` → `(next st, L, ±)` | sec_to_next | = realSec | 174 |
| board | `(p, WALK)` → `(p, vehicle)` | average wait | wait + 120 s | 10,230 |
| alight | `(p, vehicle)` → `(p, WALK)` | 0 | 0 | 10,230 |
| walk | `(p, WALK)` → `(q, WALK)` | walk_sec | walk_sec × 1.5 + 30 s | 51,730 |

Connectivity check: **every place can reach every other place** (tested from and to Parque Berrío in both directions).

## 4. Parameters (calibrated on Oct 29 with 300 random trips; speeds and waits are ESTIMATED)

| Parameter | Value |
|---|---|
| Bus speed: Directa Centro, Circular | 15 km/h |
| Bus speed: Integrada, Directa No Centro | 18 km/h |
| Dwell per bus stop | 20 s |
| Metro / Metroplús / tram / cable car | 37 / 20 / 15 / 18 km/h (already in `transit_lines.csv`) |
| Average wait: bus / metro / Metroplús / tram / cable car | 300 / 240 / 300 / 300 / 120 s |
| Boarding penalty (perceived only) | +120 s on every boarding |
| Walking penalty (perceived only) | walk time × 1.5, plus 30 s per walking edge |
| Walking speed, street factor, link radius | 4.5 km/h, × 1.3, 300 m |
| Access radius around the origin and destination | 500 m |

**Why these values.** With an 8-minute bus wait and no walking penalty, the best route chained up to 6 walks (median 688 m on foot). With the values above, the median walk is about 400 m (p95 940 m), there is 1 transfer on median and 3 at p95, and no trip is slower than walking.

**Rule:** Dijkstra minimizes the **perceived cost**, but the app always shows the **real duration** (the sum of `realSec` along the chosen path).

## 5. Origin and destination from an address [VERIFIED]

Medellín addresses form a grid: «CL 50 53-48» means Calle 50 at Carrera 53. Parsing an address gives a numeric pair (calle, carrera). The stops' own addresses serve as the map: **97 % of them (3,481 of 3,590) can be parsed.** A new address is located by interpolating the 4 stops with the closest (calle, carrera). The measured error is a median of **75 m** (p90 = 327 m). See `algorithm.md`, section 3.

## 6. Java project layout (Maven)

```
medellin-transit/
├── pom.xml
├── data/                         ← copy of kiro/data (raw JSON + derived CSV)
└── src/
    ├── main/java/co/medellin/transit/
    │   ├── Main.java             ← shows the home screen in the console
    │   ├── ui/                   ← HomeScreen, Menu, TripPrinter (user-facing text in Spanish)
    │   ├── data/                 ← ArcGisReader (JSON), CsvReader
    │   ├── model/                ← Place, Route, State, Edge, EdgeKind, Trip, Step
    │   ├── graph/                ← GraphBuilder, Parameters, Geo (distances)
    │   ├── geocoding/            ← AddressParser, AddressLocator
    │   ├── routing/              ← Router (multi-source / multi-target Dijkstra), StepBuilder
    │   └── fares/                ← FareCalculator
    └── test/java/co/medellin/transit/
        └── ReferenceCasesTest.java   ← must reproduce data/test_cases.csv
```

The data is read from the files in `data/` at startup. There is no network access at runtime.
