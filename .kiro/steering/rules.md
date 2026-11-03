---
inclusion: always
---

# Working rules (apply to every task)

## Tests

- Never change a test's expected values or weaken assertions to make it pass; fix the code. If you think a test is wrong, stop and ask, explaining which spec line it contradicts.
- Every component has at least one test that uses the real files in `data/` (see `data.md`), not only invented sample data.
- Hand-calculated or reference results (like `data/test_cases.csv`) are JUnit tests with real assertions from day one. A test that only prints results is not a test.
- Test class names must end in `Test` so Maven Surefire runs them. After adding a test, show that `mvn test` lists it in the output.
- Do not keep "bug exists" exploration tests after the bug is fixed: turn them into tests of the correct behavior.

## Editing files

- Edit files only with your built-in file tools, never with shell commands (no PowerShell `Set-Content`, `Out-File`, `-replace`, here-strings or `[System.IO.File]::WriteAllText`). Shell edits have corrupted files in this project before.
- Use relative paths (`data/...`). Never hardcode `C:\Users\...`.
- Code and comments in English; text shown to the user in Spanish.
- Do not edit `Main.java`, `README.md`, `ui/TripPlanner.java`, `ui/PlanResult.java` or anything under `ui/gui/` unless the task says so. Another workstream owns them.
- Never edit `data/test_cases.csv` or anything under `data/`.

## Version control

- Before starting a task, make sure the work so far is committed in git (ask the user to commit if there are uncommitted changes), so a broken task can be rolled back.

## Finishing a task

A task is done only when all of these are true, and you show the real output as proof:

1. `mvn test` finishes with `Tests run: N, Failures: 0, Errors: 0`. Paste that line from the actual run.
2. The app still starts from the console and shows the home screen.
3. For tasks that touch loading, graph, routing or fares: the counts or reference cases in the spec's acceptance criteria match.

Do not report "all tests pass" without the command output. If something still fails, say so and list what fails. Do not move on to the next task while `mvn test` or the app is broken.

## Size

Work in small steps: one component at a time, following the numbered specs in `.kiro/specs/` in order. If a change needs more than about 5 files, stop and propose splitting it.
