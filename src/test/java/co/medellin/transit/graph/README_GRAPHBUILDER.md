# GraphBuilder Implementation - Task 6.2

## Overview

Implemented `GraphBuilder` class that constructs the Medellín transit routing graph from CSV and JSON data files according to the specifications in the design document.

## Implementation Details

### Key Features

1. **Data Loading**:
   - Loads stops from paginated ArcGIS JSON files (stops_*.json)
   - Loads routes from ArcGIS JSON (routes.json)
   - Reads pre-computed edges from CSV files:
     - `bus_segments.csv`: Bus route edges
     - `transit_lines.csv`: Metro/tram/cable car edges
     - `walk_links.csv`: Walking connection edges

2. **State Creation**:
   - Automatically detects the number of states from edge data
   - Creates placeholder states with IDs from 0 to maxStateId
   - In production, states would be loaded from a separate states.csv file mapping IDs to locations, routes, and directions

3. **Edge Loading**:
   - Supports multiple CSV field name conventions:
     - `source_id` or `from_id` for source state
     - `target_id` or `to_id` for target state
     - `edge_kind`, `kind`, or `type` for edge type
     - `real_sec`, `duration`, or `time_sec` for travel time
     - `perceived_cost` or `cost` for routing cost
   - Validates all edge data before adding to graph
   - Logs warnings for malformed data but continues processing

4. **Error Handling**:
   - Spanish language error messages for user-facing errors
   - Comprehensive validation of input data
   - Graceful handling of missing files (logs warning, continues)
   - Throws IOException when data directory is missing or graph is empty
   - Validates graph structure after construction

5. **Performance**:
   - Pre-sizes graph capacity to expected 13,895 states
   - Uses string interning for memory optimization (placeholder names)
   - Logs construction time to verify < 1 second target
   - Issues warning if construction exceeds 1 second

### Edge Cost Specifications

The implementation follows graph-structure.md section 3.2 specifications:

- **RIDE edges**: `perceivedCost = realSec` (no penalty)
- **BOARD edges**: `realSec = average wait time`, `perceivedCost = wait + 120s penalty`
- **ALIGHT edges**: `realSec = 0`, `perceivedCost = 0`
- **WALK edges**: `realSec = distance × 1.3 ÷ 1.25 m/s`, `perceivedCost = realSec × 1.5 + 30s`

Note: Edge costs are **loaded from CSV files** (pre-computed), not calculated in GraphBuilder.

## Expected Data Format

### CSV Edge Files

All CSV files should have a header row and support these field names:

```csv
source_id,target_id,edge_kind,real_sec,perceived_cost
0,1,WALK,120,210
1,2,RIDE,180,180
2,3,BOARD,300,420
3,4,ALIGHT,0,0
```

Alternative field names supported:
- `from_id` instead of `source_id`
- `to_id` instead of `target_id`
- `kind` or `type` instead of `edge_kind`
- `duration` or `time_sec` instead of `real_sec`
- `cost` instead of `perceived_cost`

### Graph Structure

Expected graph dimensions:
- **13,895 states** (nodes)
- **82,160 edges**
- **3,590 physical stops**
- **252 routes**

## Unit Tests

Comprehensive test suite in `GraphBuilderTest.java`:

1. **testBuildThrowsExceptionWhenDirectoryDoesNotExist**: Validates error handling for missing directory
2. **testBuildThrowsExceptionWhenPathIsNotDirectory**: Validates error handling for file instead of directory
3. **testBuildThrowsExceptionWhenDataDirectoryIsEmpty**: Validates error handling for empty directory
4. **testBuildCreatesGraphFromEdgeData**: Tests basic graph construction from minimal CSV
5. **testBuildHandlesMultipleCsvFiles**: Tests loading from multiple CSV files
6. **testBuildHandlesAlternativeFieldNames**: Tests flexibility in CSV field naming
7. **testBuildContinuesWhenSomeFilesAreMissing**: Tests graceful degradation with partial data

## Running Tests

```bash
cd C:\Users\marti\MetroBus
mvn test -Dtest=GraphBuilderTest
```

Or run all tests:
```bash
mvn test
```

## Future Enhancements

1. **State Metadata Loading**: Currently uses placeholder states. Production implementation should:
   - Load state metadata from `states.csv` with columns: `state_id, place_name, lat, lon, state_type, route_id, direction`
   - Map state IDs to actual Place objects with real coordinates
   - Distinguish WALK, BUS_STOP, and METRO_STATION state types

2. **Spatial Indexing**: Implement grid-based spatial index for faster nearby state queries (currently O(n), target O(k))

3. **Memory Optimization**: 
   - Use string interning for actual place names and route IDs (not just placeholders)
   - Consider primitive array-based edge storage for lower memory footprint

4. **Validation**: 
   - Add checks for disconnected graph components
   - Validate edge costs match expected formulas
   - Warn about states with no incoming or outgoing edges

## Notes

- All user-facing messages are in Spanish per project requirements
- Code comments and technical logs are in English for maintainability
- Implementation matches design document specifications from `design.md`
- Follows expected workflow from `tasks.md` task 6.2

## Status

✅ Implementation complete
✅ Unit tests written
⏸️ Tests not executed (Java/Maven not in PATH on build machine)

The implementation is ready for integration with the routing engine once test data files are available.
