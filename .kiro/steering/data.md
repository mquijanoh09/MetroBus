---
inclusion: always
---

# Real data: files, columns and sample rows

All data lives in `data/` at the project root (relative path, never an absolute `C:\Users\...` path). Read these files; do not invent other formats. Every number below was counted from the real files on 2026-11-03.

Specs that describe how the data becomes a graph:
- #[[file:docs/graph-structure.md]]
- #[[file:docs/algorithm.md]]
- #[[file:docs/fares.md]]
- Reference implementation (Python, the source of truth for expected results): `reference/prototype.py`

## Expected totals after loading

| What | Expected |
|------|----------|
| Physical bus stops (unique `id_paradero`) | 3,590 |
| Bus routes in `routes.json` | 252 (247 have stops; the other 5 are ignored) |
| Graph states (nodes) | 13,895 |
| Graph edges | 82,160 |

A loader or graph that does not reach these numbers with the real files is wrong, even if its unit tests pass.

## `data/bus_segments.csv` (9,550 rows)

One row per consecutive pair of stops on a bus route.

```
route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method
90098,outbound,51929,51832,1,2,245,287,287,road
90098,outbound,51832,51877,2,3,254,419,419,road
90098,outbound,51877,51973,3,4,223,425,425,road
```

- `route_id`: matches `id_ruta` in `raw/routes.json` (246 distinct routes in this file).
- `direction`: `outbound` (4,990 rows) or `inbound` (4,560 rows).
- `from_stop_id`, `to_stop_id`: matches `id_paradero` in `raw/stops_*.json` and the number after `STOP:` in `walk_links.csv`.
- `used_m`: distance to use for ride time. `method` is `road` (9,370) or `straight_x1.3` (180).

## `data/transit_lines.csv` (98 rows)

One row per station, in order, per mass-transit line. This is a station list, not an edge list.

```
line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next
A,metro,1,Niquía,6.337817,-75.544312,1288,125
A,metro,2,Bello,6.330538,-75.553381,1705,166
A,metro,3,Madera,6.31532,-75.555282,1706,166
```

- Lines and modes: `A`, `B` metro; `1`, `2` brt (Metroplús); `H`, `J`, `K`, `L`, `M`, `P` cable_car; `T-A` tram.
- The last station of each line has empty `dist_to_next_m` and `sec_to_next`, e.g. `A,metro,21,La Estrella,6.152747,-75.626353,,`.
- `station` matches the name after `STATION:` in `walk_links.csv`.
- The file is UTF-8 (station names have accents).

## `data/walk_links.csv` (25,865 rows)

Walking connections between places. Ids are prefixed with `STOP:` (bus stop) or `STATION:` (station name).

```
place_a,name_a,place_b,name_b,kind,straight_m,walk_sec
STOP:53191,"CL 20B Sur 27-, Medellin",STOP:53076,"CR 34 16A Sur-271, Medellin",stop_stop,265,276
STATION:Aguacatala,Aguacatala,STOP:52292,"CL 12 Sur x CR 50, Medellin",stop_station,219,228
STOP:52673,"CL 10 48-85, Medellin",STATION:Poblado,Poblado,stop_station,181,188
```

- `kind`: `stop_stop` (25,032), `stop_station` (830), `station_station` (3).
- Names are quoted and contain commas: use a real CSV parser (Apache Commons CSV), never `split(",")`.
- `walk_sec` is real walking time. The perceived cost (×1.5 + 30 s) is computed by the graph, not stored here.

## `data/test_cases.csv` (15 rows)

Expected results from the reference prototype. Never edit this file.

```
origin,destination,minutes,cost_cop,transfers,vehicles
CL 50 53-48,CR 70 x CL 48,21,3800,0,bus:191
CL 10 43A-20,CL 77 65-30,34,3820,0,metro:A
CL 30 65-10,CL 30 66-20,9,0,0,walk
```

- `vehicles` uses `mode:line` with ` > ` between legs, e.g. `bus:243 > metro:B > bus:91`. `walk` means a walk-only trip.

## `data/raw/stops_*.json` (6 files, 10,043 features in total)

Files: `stops_0.json`, `stops_2000.json`, `stops_4000.json`, `stops_6000.json`, `stops_8000.json`, `stops_10000.json`. The number is the record offset, not a page number. One feature per (route, stop) pair, so the same physical stop appears many times; deduplicate by `id_paradero` to get 3,590 stops.

Coordinates are in the attributes (`latitud`, `longitud`); there is no usable geometry. Sample `attributes`:

```json
{"objectid":1,"id_paradero":52588,"id_parada":"90098014","id_ruta":90098,"nro_parada":14.0,
 "direccion":"CR 68A x CL 78B, Medellin","tipo_parada":"Reglamentaria","recorrido":"Origen-Destino",
 "codigo_ruta":"260T","nombre_ruta":"Torres","sistema_ruta":"2A","tipo_ruta":"Directa Centro",
 "empresa":"Palenque Robledal","longitud":-75.57797867,"latitud":6.27579455, ...}
```

## `data/raw/routes.json` (252 features)

```json
{"objectid":1,"nombre":"12 de Octubre - Calle 90","id_ruta":"90111","codigo":"263",
 "recorrido":"Origen Destino_Destino Origen","sistema":"2B","tipo":"Directa Centro", ...}
```

- `id_ruta` is a string here but an integer in `bus_segments.csv`.
- Show users `codigo` and `nombre` (e.g. `bus 263 (12 de Octubre - Calle 90)`), never the internal `id_ruta`.
- `tipo` decides fares: only `Integrada` routes get the bus + Metro integrated fare.

## `data/raw/transit_stations.json` (81 features)

```json
attributes: {"objectid":488,"nombre":"Universidad de Medellín","linea":"Línea 1","tipo_est":"Estación","estado":1,"tipo":3}
geometry:   {"x":-75.609308089921356,"y":6.2307992758030712}
```

`route_geometry.json` (11 MB) holds route shapes and is not needed for routing.
