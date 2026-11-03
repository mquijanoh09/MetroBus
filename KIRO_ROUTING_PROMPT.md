# Routing accuracy fix (verified run, 2026-11-03)

The crash is fixed: all 15 rows of `data/test_cases.csv` now run through the app without crashing or hanging. But the results are far from the expected values. Actual vs expected (minutes / COP / walk distance):

| # | Expected | Actual |
|---|----------|--------|
| 1 | 21 / 3800 / bus:191 | 6 / 3820 / walk 0 m (metro B San Antonio -> Estadio) |
| 2 | 34 / 3820 / metro:A | 21 / 3820 / walk 320 m |
| 3 | 31 / 3820 / brt:2 | 19 / 3800 / walk 0 m |
| 4 | 9 / 0 / walk | 2 / 0 / walk 144 m |
| 5 | 24 / 3800 / bus:195 | 11 / 3800 / walk 0 m |
| 6 | 46 / 11420 | 32 / 3800 / walk 0 m |
| 7 | 18 / 3820 / cable_car:J | 9 / 3820 / walk 0 m |
| 8 | 76 / 11420 | 64 / 12335 / walk 118 m |
| 9 | 76 / 11420 | 65 / 11400 / walk 79 m |
| 10 | 28 / 4715 | 17 / 7600 / walk 112 m |
| 11 | 54 / 4715 | 45 / 3800 / walk 0 m |
| 12 | 66 / 8515 | 45 / 11440 / walk 519 m |
| 13 | 48 / 7600 | 33 / 7600 / walk 0 m |
| 14 | 61 / 4715 | 52 / 12355 / walk 264 m |
| 15 | 12 / 3800 / bus:94 | 2 / 0 / walk 145 m |

Fix these root causes, in this order:

1. **Missing access/egress walking.** 8 of 15 trips report "walk 0 m", e.g. case 1 boards metro B at San Antonio directly from CL 50 53-48 (~500 m away). The origin and destination must connect to nearby stops through WALK edges whose time uses `walkStreetFactor` (1.3), `walkingSpeedMps` (1.25) and the walk penalty from `config.properties`, and that distance must be added to the trip.
2. **Times too short across the board.** Check that wait times (`wait.*`), bus dwell time and the boarding penalty are included in the reported duration where the spec says so (see `.kiro/specs` and `graph-structure.md`). Case 4 (pure walk) gives 2 min where 9 is expected.
3. **Fares.** Totals like 12335, 12355 and 7600 for case 10 don't follow the integration rules (90-minute window, integrated bus+metro = 4715). Verify `FareCalculator` against `fares.md`.
4. Warning at startup: `No se pudieron cargar rutas desde JSON: data\routes.json`. The file is at `data/raw/routes.json`.

Rules: edit files only with your built-in file tools, never PowerShell. Do not modify `data/test_cases.csv` and do not weaken test assertions. When done, run all 15 rows through the app and report the same table again.
