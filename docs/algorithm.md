# Routing Algorithm: How the App Finds the Fastest Trip

**Date:** October 29, 2026 · **Audience:** the Kiro spec (requirements → design → tasks)
**Tested reference:** `reference/prototype.py` implements everything in this document. It ran 300 random trips with no failures (results in section 9).
**See also:** `graph-structure.md` (graph and parameters) and `fares.md` (prices).

Tags: **[TESTED]** = verified with the prototype on the real data. **[ESTIMATED]** = parameter with no official source (speeds and waits).

User-facing text stays **in Spanish**: the app is for Medellín riders. Code, identifiers and comments are in English.

---

## 1. Query flow

```
1. The user types an origin and a destination (Medellín street addresses)
2. Locate each address → (lat, lon)                                        §3
3. If origin and destination are < 50 m apart: «Ya estás en tu destino»     §7
4. Find every place within 500 m of the origin and of the destination      §4
5. Multi-source / multi-target Dijkstra on PERCEIVED cost                   §5
6. Rebuild the path and turn it into readable STEPS                         §6
7. Compute the REAL duration (no penalties) and the COST                    §6, §8
8. If walking all the way is as fast or faster in real time → walk only     §7
9. Show the steps, the total duration and the total cost
```

## 2. Startup (once)

- Read `data/raw/stops_*.json`, `data/bus_segments.csv`, `data/transit_lines.csv` and `data/walk_links.csv`, and build the graph in memory: **13,895 nodes and 82,160 edges**. The prototype loads in about **0.4 s** in Python [TESTED]; the Java app must load in under 1 s, while the home screen is shown.
- Build the **address reference table**: the 3,481 stop addresses that can be parsed, each with its (calle, carrera) and coordinates.
- **The graph is read-only after loading.** Each query uses its own `dist`, `prev` and `real` maps, so several queries can run on different threads without locks.

## 3. Locating an address (offline) [TESTED]

1. **Normalize:** upper case; drop everything after the first comma; treat `#`, `No.` and `N°` as separators; collapse spaces. Synonyms: `Calle`/`CL`; `Carrera`/`CR`/`KR`/`CRA`; `Diagonal`/`DG` counts as a calle; `Transversal`/`TV` counts as a carrera.
2. **Parse** with the pattern `STREET NUMBER[letters] [SUR] [X|CON|#] [STREET] NUMBER[letters]`:
   - «CL 50 53-48» → calle 50, carrera 53.
   - «CR 70 x CL 48» → calle 48, carrera 70.
   - A letter adds a fraction: 43A ≈ 43.075.
   - **SUR** makes the number negative (CL 12 Sur → calle −12).
3. **Interpolate:** take the 4 reference stops with the closest (calle, carrera) and average their coordinates, weighted by 1/distance.
4. **Reject** the address if it cannot be parsed, or if the closest reference stop is more than 6 address units away (about 6 blocks). The app then says «No reconozco esa dirección» and shows a valid example.
5. **Measured accuracy** on 300 stops: median error 75 m, p90 327 m.

Tested inputs: `cl 50 # 53-48` ✔, `Calle 10 # 43A - 20` ✔, `CL 12 Sur x CR 50` ✔, `hola mundo` ✘, `VDA EL YOLOMBO` ✘, `CL 900 x CR 900` ✘.

## 4. Access from the origin and to the destination

- **Sources:** every place (stop or station) within 500 m in a straight line of the origin point. Each one enters the queue in state `(place, WALK)`, with an initial cost equal to the walk from the point (perceived: × 1.5 + 30 s).
- **Targets:** every place within 500 m of the destination point, kept in a map `place → meters`.
- Walking seconds = meters × 1.3 ÷ 1.25 m/s.
- If there is no place within 500 m of either point: «No hay paraderos ni estaciones cerca de esa dirección».

## 5. Multi-source / multi-target Dijkstra [TESTED]

```
priority queue ordered by perceivedCost (ties broken by insertion order, with a counter)
best = perceived cost of walking all the way          // walking is always an option
while the queue is not empty:
    pop (d, u)
    if d ≥ best: stop                                  // nothing left can improve the best total
    if d > dist[u]: continue                           // stale queue entry
    if u.mode == WALK and u.place in targets:
        total = d + perceived walk from u.place to the destination
        if total < best: best = total; bestState = u
    for each edge (u → v):
        nd = d + edge.perceivedCost
        if nd < dist[v]:
            dist[v] = nd; real[v] = real[u] + edge.realSec; prev[v] = (u, edge)
            push (nd, v)
```

- **Trips only end in `WALK` states:** the rider must get off to arrive.
- The 120 s penalty on **every** boarding, including the first, prevents needless transfers without changing which direct route wins.
- The walking penalties (× 1.5 and +30 s per edge) stop the route from chaining long walks.
- Query time in Python: median **34 ms**, p95 86 ms [TESTED].

## 6. From path to instructions

Follow `prev` back from `bestState`, reverse the result, and group it:

- **Consecutive walks** merge into one step: «Camina N m (~M min) hasta X».
- **board + rides + alight** form one step: «En X toma bus 191 (Laureles Éxito); bájate en Y (8 paradas, ~19 min con espera)». Transit labels: «Metro Línea A», «Metroplús Línea 2», «Metrocable Línea J», «Tranvía de Ayacucho».
- Append the final walk to the destination.
- Each step stores `t0`, its start time since departure, which the 90-minute fare window uses.
- **Total duration = sum of `realSec`**, never the perceived cost.

Real output of the prototype:

```
CL 50 53-48 → CR 70 x CL 48
  1. Camina 35 m (~1 min) hasta CL 50 53-48
  2. En CL 50 53-48 toma bus 191 (Laureles Éxito); bájate en CR 70 x CL 48 (8 paradas, ~19 min con espera)
  3. Camina 67 m (~1 min) hasta tu destino
  Total: 21 min · $3.800
```

## 7. Rules for natural behavior [TESTED]

| Rule | Why |
|---|---|
| Origin and destination < 50 m apart → «Ya estás en tu destino» | Avoids an absurd 0 m route. |
| If walking all the way is **as fast or faster in real time** than the best route, recommend walking | Without this rule, 7 of 300 trips were slower than walking (because of the penalties). With it: 0. |
| Never get off and back on the same vehicle | Measured: 0 cases in 300; the boarding penalty guarantees it. |
| Merge consecutive walks into one step | «Camina 40 m, camina 60 m…» is useless to the rider. |
| Round to whole minutes, and show walking in meters | Per-second precision would be fake: the times are estimates. |
| Show «Tiempos estimados · Datos de la Alcaldía de Medellín» | Honesty with the user. |

## 8. Trip cost

See `fares.md`, section 3. Costs seen in 300 random trips [TESTED]: 3,800 (bus), 3,820 (transit), 4,715 (integrated pair), 7,600 (two buses), 7,620 (non-integrated bus + Metro), 8,515 (bus + Metro + integrated bus), 11,420 (three fares).

## 9. Prototype results on 300 random trips [TESTED]

| Measure | Result |
|---|---|
| Failed queries | 0 of 300 |
| Walk-only answers | 15 |
| Transfers | median 1, p95 3, max 4 |
| Walking per trip | median 405 m, p95 940 m |
| Trips slower than walking | 0 |
| Getting off and back on the same vehicle | 0 |

## 10. Acceptance tests for the Java version

- `data/test_cases.csv` has 15 fixed trips with the expected `minutes, cost_cop, transfers, vehicles`. Vehicle ids look like `bus:191`, `metro:A`, `brt:2`, `cable_car:J`, `tram:T-A`, or `walk`. **The Java version must return the same cost, transfers and vehicles, and the duration within ±1 minute.** A mismatch means a bug in one of the two implementations.
- Edge cases must show a message, never crash: empty address, «hola mundo», «CL 900 x CR 900», a rural address («VDA …»), origin equal to destination.
- Performance: load in under 1 s; each query in under 100 ms.
- Regenerate the expected cases after a parameter change with `python3 -I reference/prototype.py data --make-cases`.

## 11. What is still estimated

- **Speeds and waits** for buses and mass transit: Medellín publishes no bus schedules. The values come from the calibration (`graph-structure.md`, section 4).
- Metroplús line 2 skips Manrique and Prado toward U. de M.; the data assumes it stops there (a difference of a few seconds).
- Double integration with two integrated buses (`fares.md`).
- Circular streets (CQ) and rural addresses cannot be located; the app shows a message.
