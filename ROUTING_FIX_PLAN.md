# KIRO_ROUTING_PROMPT Round 2 - Fix Plan

## Current State Analysis
- Tests: 399 run, 35 failures (↑ from 28), 5 errors (↓ from 6), 8 skipped
- Only 6 of 15 reference cases match expected values
- New regressions: walk-only trips now charge 3800 COP and have wrong durations
- Walking distance still shows 0 m in many cases
- My previous changes introduced +30s constant being applied incorrectly

## Root Cause Analysis

### Issue 1: Access/Egress Walking Not Tracked
**Current State**: Router initializes sources with perceived cost (walk time × 1.5 + 30s), but:
- DijkstraResult doesn't store access/egress distances or times
- StepBuilder doesn't create WALK steps for access/egress
- Trip.getTotalWalkMeters() only counts WALK edges in the graph

**Fix Required**: 
1. Track which source state was used and calculate access distance/time
2. Calculate egress distance/time to the best target state  
3. Store both in DijkstraResult (already updated)
4. Modify dijkstra() to calculate and pass these values when constructing DijkstraResult
5. Update StepBuilder to create access/egress WALK steps
6. Update Trip calculation to include access/egress walking

### Issue 2: Walk Penalty Applied to Reported Duration
**Current State**: The +30s constant and ×1.5 multiplier are for route OPTIMIZATION only, but they're being included in reported duration.

**Fix Required**:
- calculateRealDuration() should sum edge.getRealSec() WITHOUT penalties
- Access/egress walk time should be REAL time (distance/speed), not penalized time
- The perceived cost includes penalties, but realDurationSec should NOT

### Issue 3: Walk-Only Trips Charge Fare
**Current State**: Walk-only trips (no BOARD edges) are being charged 3800 COP

**Fix Required**:
- FareCalculator must check if trip has any BOARD steps
- If no BOARD steps, return fare = 0 COP

### Issue 4: RouterTest Failures - +30s Applied Twice
**Current State**: Every walk cost is 30s higher than expected

**Root Cause**: My changes initialize sources with `walkTime × 1.5 + 30`, but the walk penalty constant should only be applied to WALK EDGES in the graph, not to access/egress initialization.

**Fix Required**:
- Remove the `+ params.getWalkPenaltyConstant()` from dijkstra() source initialization
- Access/egress should only use the multiplier: `walkTime × 1.5`
- The constant is already in the graph's WALK edge costs

### Issue 5: WalkLinkReader Returns 0 Links
**Current State**: All WalkLinkReaderTest cases fail - reader returns 0 links

**Root Cause**: Likely the reader logic was broken by changes to CSV parsing or field mapping

**Fix Required**: Review WalkLinkReader.read() implementation

### Issue 6: StopArcGisReader Name Field Issue
**Current State**: Stop name comes back as address or "Parada N" instead of name field

**Fix Required**: Check field mapping in StopArcGisReader - may be using wrong JSON field

### Issue 7: Fare Calculation Not Following Integration Rules
**Current State**: Getting totals like 12335, 12355, 7600 that don't match fare rules

**Expected Rules** (from fares.md):
- Metro/tram/cable/Metroplús: 3820 COP
- Bus: 3800 COP  
- Bus + Metro integration (within 90 min): 4715 COP
- Transfers within mass transit (Metro/cable/tram/Metroplús): no extra charge
- Integration surcharge: +750 at 5th, 8th, 9th integration

**Fix Required**: Complete rewrite of FareCalculator.calculate() to follow pseudocode in fares.md section 2

### Issue 8: DataLoadingBugConditionTest Needs Update
**Current State**: These tests verified bugs existed, but bugs are now fixed

**Fix Required**: Either:
- Update tests to assert fixed behavior, OR  
- Remove them entirely (they were exploration tests)

### Issue 9: Out-of-Bounds Address Tests
**Current State**: Addresses like Calle 200 are being located when they shouldn't be

**Fix Required**: AddressLocator.locate() must return empty for addresses that would interpolate to coordinates outside bounds

## Implementation Priority

### CRITICAL (Blocks Everything):
1. Fix +30s double-application in Router
2. Fix walk-only fare (0 COP)
3. Fix calculateRealDuration to exclude penalties

### HIGH (Needed for Accuracy):
4. Complete access/egress tracking in dijkstra()
5. Update StepBuilder to create access/egress WALK steps
6. Rewrite FareCalculator to follow fares.md exactly

### MEDIUM (Test Fixes):
7. Fix WalkLinkReader
8. Fix StopArcGisReader name field
9. Fix out-of-bounds address location
10. Update/remove DataLoadingBugConditionTest

## Estimated Complexity
- Total changes needed: ~15 files
- Lines of code: ~500-800 LOC
- Time estimate: 3-4 hours for experienced developer
- Risk: High - core routing/fare logic

## Recommendation
This requires a systematic, file-by-file approach:
1. Start with Router fixes (remove +30s from source init)
2. Complete DijkstraResult access/egress tracking
3. Update StepBuilder for access/egress steps
4. Rewrite FareCalculator from spec
5. Fix data reader regressions
6. Run full test suite between each change

The current state shows my partial fixes created new issues. A complete, tested implementation of each component is needed rather than incremental patches.