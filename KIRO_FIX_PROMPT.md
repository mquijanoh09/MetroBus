# Fix request: the app cannot load the real data (verified 2026-11-01)

Verified by running `mvn test` and `java co.medellin.transit.Main` on Windows (JDK 23, Maven 3.10).
Data folder: `C:\Users\marti\Downloads\transporte-medellin\kiro\data`.

## Observed results

- `mvn test`: 332 tests, 7 failures, 1 error. RouterTest now passes (14/14).
- Running `Main` with no arguments from the project root: fatal "No se encontró el directorio de datos" (looks for `.\data`). The home screen never appears.
- Running `Main` with the data folder as argument: the graph loads 0 stops, 0 states, 0 edges and the app exits with "El grafo está vacío". The home screen never appears.
- None of the 15 reference cases in `test_cases.csv` can run, because the graph is empty.

## Problems and root causes

1. **Edge CSVs are read with the wrong schema** (`GraphCsvReader`, `GraphBuilder` lines ~136-160).
   `GraphCsvReader` requires `source_id`/`from_id` and `target_id`/`to_id`, so every row of all three files is skipped. The real files are not pre-computed edges:
   - `bus_segments.csv`: `route_id,direction,from_stop_id,to_stop_id,from_seq,to_seq,straight_m,road_m,used_m,method`
   - `transit_lines.csv`: `line,mode,order,station,lat,lon,dist_to_next_m,sec_to_next` (a station list per line, not edges)
   - `walk_links.csv`: `place_a,name_a,place_b,name_b,kind,straight_m,walk_sec` (ids like `STOP:53191`)
   The graph must be built from these files following `docs/graph-structure.md` (ride, board, alight and walk edges, with the speeds, waits and penalties in `config.properties`).

2. **Stops and routes JSON are never found** (`GraphBuilder` line ~108, `ArcGisReader.readPaginated`).
   The files live in `data/raw/` and are named `stops_0.json, stops_2000.json, stops_4000.json, stops_6000.json, stops_8000.json, stops_10000.json` and `routes.json`. The reader looks in `data/` and expects `stops_1.json, stops_2.json, ...`, so it finds nothing.

3. **ArcGIS feature regex cuts each feature at the first `}`** (`ArcGisReader.FEATURE_PATTERN = \{[^}]*"attributes"[^}]*\}`).
   The match ends at the closing brace of `attributes`, so `geometry` is never part of the feature. Result: coordinates are 0.0 and stops without coordinates are dropped. This causes the 7 failing tests in `ArcGisReaderTest` (2) and `StopArcGisReaderTest` (5). Parse features with real brace matching (or a JSON parser) instead of this regex.

4. **Default data directory does not exist** (`Main.DEFAULT_DATA_DIR = "data"`).
   `config.properties` has `data.directory`, but `Main` ignores it. Running the app with no arguments must use that value (or a `data` folder that actually exists), so the home screen opens from the console.

5. **The reference suite does not really test anything.**
   `ReferenceTestSuite` is not run by Maven (the name does not match Surefire's `*Test` pattern), runs only the first 5 cases, and never asserts. Rename it so Surefire runs it, run all 15 cases, and fail the test when minutes (±2), cost (±100 COP), transfers or the vehicle sequence do not match `test_cases.csv`.

## Done when

- `mvn test` passes with 0 failures and 0 errors, including the reference suite with all 15 cases asserted.
- `java -cp target/classes;<commons-csv jar> co.medellin.transit.Main` (no arguments) prints the home screen.
- The graph loads about 13,895 states and 82,160 edges (as in `docs/graph-structure.md`).

Do not change the expected values in `test_cases.csv`.
