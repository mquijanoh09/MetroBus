# Routing fix, round 2 (verified run, 2026-11-03)

State after your last changes:
- `mvn test`: 399 run, 35 failures, 5 errors, 8 skipped (it was 28 failures and 6 errors before). It got worse.
- All 15 reference cases run without crashing, and the `routes.json` warning is gone. Only 6 of 15 match.
- Durations (min) per case: 11, 21, 19, 4, 13, 36, 9, 66, 65, 17, 44, 53, 35, 55, 8.
- Walking distance is still 0 m in cases 1, 3, 4, 5, 7, 13 and 15.
- **New regression:** case 4 (walk only) now costs 3800 COP and takes 4 min. Expected: 9 min and 0 COP. Case 15 gives 8 min / 3800, expected 12 / 3800.
- Fares of 12335, 12355 and 7600 still appear.

Your summary says access/egress walking and durations are "partially fixed" or "need further work". Finish them; do not stop at a partial fix:

1. **Access/egress walking must be part of the trip.** Store access and egress walk distance and time in `DijkstraResult`, create the WALK steps in `StepBuilder`, and add them to the trip's duration and walking distance. A trip from an address to a stop can never report 0 m walking unless the address is on the stop.
2. **Walk-only trips cost 0 COP.** If the best path has no BOARD step, the fare is 0 and no vehicle is shown (case 4).
3. **Durations.** The reported duration must include walk time, average wait (`wait.*`) at each boarding, ride time and bus dwell time, as in `graph-structure.md`. Penalties (boarding penalty, walk ×1.5 + 30 s) are for choosing the route only; they must not be added to the reported minutes.
4. **Fares.** Fix `FareCalculator` to follow the pseudocode in `fares.md` section 2 exactly: transfers inside mass transit (Metro, cable car, tram, Metroplús) within 90 minutes add no surcharge (only +750 at the 5th, 8th and 9th integration); an `Integrada` bus paired with the Metro costs 4715 for the pair; any other bus pays 3800 per boarding. Totals like 12335 and 12355 cannot come out of those rules.

Test failures to fix (fix the code; only change a test if the test contradicts the spec, and say which and why):
- `WalkLinkReaderTest` (10 failures): the reader returns 0 links for every test file.
- `StopArcGisReaderTest` (4) and `PreservationTest`/`PaginationTest` (5): the stop name now comes back as the address or "Parada N" instead of the name field.
- `RouterTest` (4): every walk cost is 30 s higher than expected. Check whether the +30 s constant is applied twice or applied where it should not be.
- `GraphBuilderTest` (4 errors), `GraphBuilderIntegrationTest` (2), `BusSegmentReaderTest` (1).
- `DataLoadingBugConditionTest` (5 failures, 1 error): these were the "bug exists" exploration tests. Now that the bugs are fixed, they fail as expected. Convert them to assert the fixed behavior or remove them; they also reference `ReferenceTestSuite.java`, which no longer exists.
- Out-of-bounds address tests (3): an address like Calle 200 must not be located.

Rules: edit files only with your built-in file tools, never PowerShell. Do not modify `data/test_cases.csv` and do not weaken test assertions.

Done when: `mvn test` ends with 0 failures and 0 errors, and running the 15 rows through the app gives minutes within ±2, cost within ±100 COP and the same transfers as `test_cases.csv`. Report the table of 15 cases again.
