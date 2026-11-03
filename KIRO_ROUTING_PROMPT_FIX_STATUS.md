# KIRO_ROUTING_PROMPT.md - Fix Status

## Completed Fixes

### 1. ✅ routes.json Path Fixed
**Issue**: Warning "No se pudieron cargar rutas desde JSON: data\routes.json"
**Root Cause**: GraphBuilder was looking for routes.json in data/ instead of data/raw/
**Fix**: Changed `dataDir.resolve(ROUTES_FILENAME)` to `dataDir.resolve("raw").resolve(ROUTES_FILENAME)` in GraphBuilder.java line 148

### 2. ⚠️ Access/Egress Walking - Partially Implemented
**Issue**: 8 of 15 trips report "walk 0 m" - missing walking from origin to first stop and from last stop to destination
**Root Cause**: Router initializes Dijkstra sources with cost 0, assuming user starts AT the stop
**Fix Attempted**: 
- Modified Router.dijkstra() to accept origin/destination Place objects
- Calculate walking distance and time using haversineDistance, walkStreetFactor (1.3), walkingSpeedMps (1.25)
- Initialize source states with perceived walking cost (time * walkPenaltyMultiplier + walkPenaltyConstant)
- Added haversineDistance() method to Router

**Limitation**: DijkstraResult and Trip classes don't currently track access/egress walking distance separately. The walking distance shown in Trip comes only from WALK edges in the graph, not from the initial/final walks.

## Remaining Issues

### 3. ❌ Walking Distance Tracking
**Problem**: Trip.getTotalWalkMeters() doesn't include access/egress walking
**Solution Needed**:
1. Modify DijkstraResult to store:
   - accessWalkMeters: distance from origin to first stop
   - egressWalkMeters: distance from last stop to destination
2. Modify Router.dijkstra() to calculate and store these distances
3. Modify StepBuilder.buildTrip() to include access/egress in total walk distance
4. Add initial WALK step from origin to first stop
5. Add final WALK step from last stop to destination

### 4. ❌ Duration Calculation
**Problem**: Times too short - case 4 (pure walk) shows 2 min instead of 9 min
**Potential Causes**:
- Real duration may not include all components (wait times, dwell times, boarding penalty)
- Walking time calculation may be incorrect
- Trip duration may only sum edge.getRealSec() without including wait/boarding times

**Solution Needed**: Audit how Trip.getTotalDurationSec() is calculated:
1. Check that BOARD edges include wait time + boarding penalty in realSec
2. Check that RIDE edges include dwell time at intermediate stops
3. Verify walk time calculation: distance * streetFactor / walkSpeed

### 5. ❌ Fare Calculation
**Problem**: Totals like 12335, 12355, 7600 don't follow integration rules
**Expected**: Bus+Metro within 90 min = 4715 COP
**Actual**: Getting sums like 3820+3820=7640 or 3800+3800+3820=11420 instead of applying integration

**Solution Needed**: Review FareCalculator.calculate() against fares.md:
1. Check integration window logic (90 minutes)
2. Verify integrated fare application (4715 for bus+metro)
3. Check fare segment grouping and transfer detection

## Files Modified

1. `src/main/java/co/medellin/transit/graph/GraphBuilder.java`
   - Line 148: Fixed routes.json path to use raw subdirectory

2. `src/main/java/co/medellin/transit/routing/Router.java`
   - Line 99: Updated findRoute() to pass origin/destination to dijkstra()
   - Line 152-166: Modified dijkstra() to calculate and apply access walking costs
   - Added haversineDistance() method for distance calculations
   - Added Coordinate import

## Testing Status

**Compile**: ✅ Success
**Runtime**: Not tested yet - requires comprehensive changes to track walking distance properly

## Recommended Next Steps

1. **Immediate**: Test with current changes to see if routes.json fix and access walking costs improve results
2. **High Priority**: Implement walking distance tracking in DijkstraResult/Trip
3. **High Priority**: Audit duration calculation to include all wait/boarding/dwell times
4. **Medium Priority**: Fix fare calculation integration logic
5. **Validation**: Run all 15 test cases and compare against expected values

## Architecture Notes

The current architecture has a fundamental limitation: the routing graph only contains fixed WALK edges between stops, not dynamic walk edges from arbitrary user locations. The Router's multi-source Dijkstra approach correctly finds nearby stops, but the Trip/StepBuilder classes weren't designed to handle "virtual" access/egress walking that happens outside the graph.

A complete fix requires:
- Extending DijkstraResult to track access/egress metadata
- Modifying StepBuilder to synthesize WALK steps for access/egress
- Updating Trip to aggregate all walking components