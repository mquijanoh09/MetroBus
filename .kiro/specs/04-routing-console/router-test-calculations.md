# RouterTest Expected Values - Correct Calculations

## Parameters
- Walk penalty multiplier: 1.5
- Walk penalty constant: 30 s
- Boarding penalty: 120 s
- Walk street factor: 1.3
- Walking speed: 1.25 m/s (4.5 km/h)

## Haversine Distance Formula
```
For two points (lat1, lon1) and (lat2, lon2):
Δlat = lat2 - lat1
Δlon = lon2 - lon1
a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)
distance_m = 2 × 6371000 × asin(√a)
```

## Test 1: testFindRouteSimpleWalkPath

**Setup:**
- coord1 = (6.250, -75.560)
- coord2 = (6.255, -75.565)
- Origin at coord1
- Destination at coord2
- State1 at coord1 (WALK)
- State2 at coord2 (WALK)
- Edge: State1→State2, real=60s, perceived=120s

**Distance coord1 to coord2:**
- Δlat = 0.005, Δlon = -0.005
- ≈ 700m

**Path via graph:**
- Access: origin (coord1) to state1 (coord1) = 0m
  - walk_sec = 0m × 1.3 / 1.25 = 0s
  - perceived = 0 × 1.5 + 30 = **30s**
- Edge: state1 to state2 = **120s** (given perceived)
- Egress: state2 (coord2) to destination (coord2) = 0m
  - walk_sec = 0m × 1.3 / 1.25 = 0s
  - perceived = 0 × 1.5 + 30 = **30s**
- **Total perceived via graph: 30 + 120 + 30 = 180s**

**Walk-only candidate:**
- Direct: coord1 to coord2 = ~700m
- walk_sec = 700 × 1.3 / 1.25 = 728s
- perceived = 728 × 1.5 + 30 = 1122s
- **Not competitive**

**CORRECT EXPECTED: 180s**
**Current test expects: 120s** ❌

---

## Test 2: testFindRouteWithBoardAndRide

**Setup:**
- coord1 = (6.250, -75.560)
- coord2 = (6.255, -75.565) [~700m from coord1]
- coord3 = (6.260, -75.570) [~700m from coord2, ~1400m from coord1]
- Origin at coord1
- Destination at coord3
- walkState1 at coord1
- walkState2 at coord2
- busStop1 at coord2
- busStop2 at coord3
- walkState3 at coord3

**Edges:**
- walkState1 → walkState2: real=30s, perceived=75s (30×1.5+30)
- walkState2 → busStop1: BOARD, perceived=120s
- busStop1 → busStop2: RIDE, real=180s, perceived=180s
- busStop2 → walkState3: ALIGHT, perceived=0s

**Path via graph:**
- Access: origin (coord1) to walkState1 (coord1) = 0m
  - perceived = 0 × 1.5 + 30 = **30s**
- Edge walkState1→walkState2: **75s** (given)
- Edge walkState2→busStop1 (BOARD): **120s**
- Edge busStop1→busStop2 (RIDE): **180s**
- Edge busStop2→walkState3 (ALIGHT): **0s**
- Egress: walkState3 (coord3) to destination (coord3) = 0m
  - perceived = 0 × 1.5 + 30 = **30s**
- **Total perceived via graph: 30 + 75 + 120 + 180 + 0 + 30 = 435s**

**Walk-only candidate:**
- Direct: coord1 to coord3 = ~1400m
- walk_sec = 1400 × 1.3 / 1.25 = 1456s
- perceived = 1456 × 1.5 + 30 = 2214s
- **Not competitive**

**CORRECT EXPECTED: 435s**
**Current test expects: 375s** ❌

---

## Test 3: testFindRouteSelectsShorterPath

**Setup:**
- coord1 = (6.250, -75.560)
- coord2 = (6.253, -75.563) [~450m from coord1]
- coord3 = (6.256, -75.566) [~450m from coord2, ~850m from coord1]
- Origin at coord1
- Destination at coord3
- State1 at coord1
- State2 at coord2
- State3 at coord3

**Edges:**
- Path 1 (direct): state1 → state3, real=100s, perceived=180s (100×1.5+30)
- Path 2 (two-hop): 
  - state1 → state2, real=120s, perceived=210s (120×1.5+30)
  - state2 → state3, real=120s, perceived=210s (120×1.5+30)

**Path 1 (direct) via graph:**
- Access: origin (coord1) to state1 (coord1) = 0m
  - perceived = 0 × 1.5 + 30 = **30s**
- Edge state1→state3: **180s** (given)
- Egress: state3 (coord3) to destination (coord3) = 0m
  - perceived = 0 × 1.5 + 30 = **30s**
- **Total perceived: 30 + 180 + 30 = 240s**

**Path 2 (two-hop) via graph:**
- Access: **30s**
- Edge state1→state2: **210s**
- Edge state2→state3: **210s**
- Egress: **30s**
- **Total perceived: 30 + 210 + 210 + 30 = 480s**

**Walk-only candidate:**
- Direct: coord1 to coord3 = ~850m
- walk_sec = 850 × 1.3 / 1.25 = 884s
- perceived = 884 × 1.5 + 30 = 1356s
- **Not competitive**

**Should choose: Path 1 (direct)**
**CORRECT EXPECTED: 240s**
**Current test expects: 180s** ❌

---

## Test 4: testBoardingPenaltyAffectsRouteChoice

**Setup:**
- coord1 = (6.250, -75.560)
- coord2 = (6.255, -75.565) [~700m away]
- Origin at coord1
- Destination at coord2
- walkState1 at coord1
- busStop1 at coord1
- busStop2 at coord2
- walkState2 at coord2

**Edges:**
- Option 1 (bus):
  - walkState1 → busStop1: BOARD, perceived=120s
  - busStop1 → busStop2: RIDE, real=60s, perceived=60s
  - busStop2 → walkState2: ALIGHT, perceived=0s
- Option 2 (walk):
  - walkState1 → walkState2: real=100s, perceived=180s (100×1.5+30)

**Option 1 (bus) via graph:**
- Access: origin (coord1) to walkState1 (coord1) = 0m
  - perceived = 0 × 1.5 + 30 = **30s**
- Edge walkState1→busStop1 (BOARD): **120s**
- Edge busStop1→busStop2 (RIDE): **60s**
- Edge busStop2→walkState2 (ALIGHT): **0s**
- Egress: walkState2 (coord2) to destination (coord2) = 0m
  - perceived = 0 × 1.5 + 30 = **30s**
- **Total perceived: 30 + 120 + 60 + 0 + 30 = 240s**

**Option 2 (walk) via graph:**
- Access: **30s**
- Edge walkState1→walkState2: **180s**
- Egress: **30s**
- **Total perceived: 30 + 180 + 30 = 240s**

**Walk-only candidate:**
- Direct: coord1 to coord2 = ~700m
- walk_sec = 700 × 1.3 / 1.25 = 728s
- perceived = 728 × 1.5 + 30 = 1122s
- **Not competitive**

**Result: TIE at 240s** (Dijkstra may choose either)

**CORRECT EXPECTED: 240s** (or assertion should be `<= 240`)
**Current test expects: <= 180s** ❌

---

## Summary Table

| Test | Current Expected | Correct Expected | Difference | Reason |
|------|-----------------|------------------|------------|--------|
| testFindRouteSimpleWalkPath | 120s | **180s** | +60s | Missing both access (30) and egress (30) penalties |
| testFindRouteWithBoardAndRide | 375s | **435s** | +60s | Missing both access (30) and egress (30) penalties |
| testFindRouteSelectsShorterPath | 180s | **240s** | +60s | Missing both access (30) and egress (30) penalties |
| testBoardingPenaltyAffectsRouteChoice | ≤180s | **≤240s** | +60s | Both paths have +60s from access+egress |

## Conclusion

All 4 tests are missing **+60 seconds** from the access penalty (30s) and egress penalty (30s). This is consistent because:
- All tests have origin = state coordinate (0m access, but still 30s penalty)
- All tests have destination = state coordinate (0m egress, but still 30s penalty)
- The prototype applies WALK_PENALTY_SEC even for 0-meter walks

**Verification:** The current Router implementation (line 163) DOES apply:
```java
int perceivedCost = (int) Math.round(walkTimeSec * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();
```

For 0-meter access walk: `0 × 1.5 + 30 = 30s` ✓

But the current tests expect values WITHOUT this 30s access penalty, AND without the 30s egress penalty. Since the Router currently has the access penalty, the tests are getting +30s more than expected (120→150, 375→405, etc.).

After adding the egress penalty fix, tests will get another +30s (150→180, 405→435, etc.), matching the calculations above.

## Corrected Summary

| Test | Current Expected | Current Actual | After Egress Fix | Correct Expected |
|------|-----------------|----------------|------------------|------------------|
| testFindRouteSimpleWalkPath | 120s | 150s | 180s | **180s** ✓ |
| testFindRouteWithBoardAndRide | 375s | 405s | 435s | **435s** ✓ |
| testFindRouteSelectsShorterPath | 180s | 210s | 240s | **240s** ✓ |
| testBoardingPenaltyAffectsRouteChoice | ≤180s | fails | 240s | **≤240s** ✓ |

The current tests were written before the access penalty was implemented, so they're off by 30s. After the egress fix, they'll be off by 60s total.
