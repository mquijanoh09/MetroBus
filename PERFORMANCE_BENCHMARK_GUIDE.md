# Performance Benchmark Guide

## Quick Start

### Running the Performance Benchmark

The performance benchmark utility runs 100 random route queries and measures:
- Graph loading time
- Memory usage
- Query performance (average, median, P95, P99)
- Success/failure rates

### Option 1: Using Maven (Recommended)

```bash
# Run performance tests (requires data directory at project root)
mvn test -Dtest=PerformanceTest

# View detailed test output
mvn test -Dtest=PerformanceTest -Dlogging.level=FINE
```

### Option 2: Direct Execution

```bash
# Compile project
mvn compile

# Run benchmark with default data directory (./data)
java -cp "target/classes;target/dependency/*" co.medellin.transit.util.PerformanceBenchmark

# Run benchmark with custom data directory
java -cp "target/classes;target/dependency/*" co.medellin.transit.util.PerformanceBenchmark /path/to/data

# Example with actual data location
java -cp "target/classes;target/dependency/*" co.medellin.transit.util.PerformanceBenchmark "C:\Users\marti\Downloads\transporte-medellin\kiro\data"
```

### Option 3: Using the data from Downloads

```bash
# Create symlink to data directory (Windows - requires admin)
cd C:\Users\marti\MetroBus
New-Item -ItemType SymbolicLink -Path "data" -Target "C:\Users\marti\Downloads\transporte-medellin\kiro\data"

# Or copy the data
Copy-Item -Path "C:\Users\marti\Downloads\transporte-medellin\kiro\data" -Destination "." -Recurse

# Then run benchmark
java -cp "target/classes;target/dependency/*" co.medellin.transit.util.PerformanceBenchmark
```

---

## Sample Output

```
======================================================================
 PERFORMANCE BENCHMARK - Medellín Transit Routing
======================================================================

[1/4] Graph Loading Performance
----------------------------------------------------------------------
  Graph loaded in 623 ms
  States: 13,895
  Edges: 82,160
  Target: < 1000 ms
  Status: ✓ PASS

[2/4] Memory Footprint Analysis
----------------------------------------------------------------------
  Memory usage: 187 MB
  Target: < 512 MB
  Status: ✓ PASS

[3/4] Warmup Phase
----------------------------------------------------------------------
  Warmup completed: 20 queries

[4/4] Query Performance Benchmark
----------------------------------------------------------------------
  Progress: 25/100 queries completed
  Progress: 50/100 queries completed
  Progress: 75/100 queries completed
  Progress: 100/100 queries completed

  Results:
    Total queries: 100
    Successful: 87 (87.0%)
    Failed: 13 (13.0%)

  Timing Statistics:
    Average: 42.35 ms
    Median:  38.50 ms
    P95:     78.20 ms
    P99:     95.40 ms
    Min:     15 ms
    Max:     102 ms

  Performance Assessment:
    Queries < 100ms: 98.0%
    Target: > 90%
    Status: ✓ PASS (meets performance targets)

  Hotspot Analysis:
    See PERFORMANCE_PROFILING.md for detailed optimization recommendations

======================================================================
 BENCHMARK COMPLETE
======================================================================
```

---

## Profiling with VisualVM

For deeper performance analysis:

### 1. Start VisualVM
```bash
# VisualVM is included with JDK
jvisualvm

# Or download from https://visualvm.github.io/
```

### 2. Run the Application
```bash
# Start the main application
java -jar target/transit-routing-1.0.0.jar

# Or run benchmark in a loop
while ($true) { 
    java -cp "target/classes;target/dependency/*" co.medellin.transit.util.PerformanceBenchmark
}
```

### 3. Profile in VisualVM
1. Find the Java process in VisualVM's process list
2. Double-click to open
3. Go to "Profiler" tab
4. Click "CPU" to start CPU profiling
5. Run queries in the application
6. Click "Stop" to see hotspots
7. Review "Hot Spots" view to identify bottlenecks

### 4. Analyze Results
- Look for methods consuming >10% CPU time
- Check for excessive object allocations
- Identify slow loops or unnecessary operations

---

## Profiling with IntelliJ IDEA

### 1. Open Project in IntelliJ
```bash
# Open the project directory
idea .
```

### 2. Profile Test
1. Open `PerformanceTest.java`
2. Right-click on the class or a test method
3. Select "Run 'PerformanceTest' with Profiler"
4. View results in Profiler window

### 3. Profile Benchmark
1. Open `PerformanceBenchmark.java`
2. Right-click on `main` method
3. Select "Run 'PerformanceBenchmark.main()' with Profiler"
4. Analyze flame graph and CPU samples

---

## Understanding the Results

### Graph Loading Performance
- **Target**: <1000ms
- **Expected**: 400-800ms
- **If slower**: Check disk I/O, increase heap size, or optimize CSV parsing

### Memory Footprint
- **Target**: <512MB
- **Expected**: 150-250MB
- **If higher**: Memory leak? Enable GC logging: `-Xlog:gc*`

### Query Performance
- **Target**: Average <100ms, 90%+ of queries <100ms
- **Expected**: Average 35-60ms
- **If slower**: See optimization recommendations in PERFORMANCE_PROFILING.md

### Success Rate
- **Expected**: 80-90% (some random queries won't have valid routes)
- **If lower**: Check data integrity or access radius parameter

---

## Performance Tuning

### JVM Options for Better Performance

```bash
# Increase heap size
java -Xmx1G -Xms1G ...

# Use G1 garbage collector (better for large heaps)
java -XX:+UseG1GC ...

# Enable GC logging
java -Xlog:gc*:file=gc.log ...

# Optimize for throughput
java -XX:+UseParallelGC -XX:ParallelGCThreads=4 ...

# Complete example
java -Xmx1G -Xms1G -XX:+UseG1GC \
     -cp "target/classes;target/dependency/*" \
     co.medellin.transit.util.PerformanceBenchmark
```

### Application Parameters

Edit `src/main/resources/config.properties`:

```properties
# Access radius (meters) - affects query performance
routing.accessRadiusMeters=500

# Boarding penalty (seconds) - affects route selection
routing.boardingPenaltySec=120

# Walk penalty multiplier - affects walking vs transit preference  
routing.walkPenaltyMultiplier=1.5
```

---

## Interpreting Hotspots

### Common Hotspots

1. **PriorityQueue operations** (40-50% of time)
   - `java.util.PriorityQueue.offer`
   - `java.util.PriorityQueue.poll`
   - **Solution**: Implement custom int-based binary heap

2. **Edge iteration** (30-40% of time)
   - `TransitGraph.getOutgoingEdges`
   - Loop over edges in Dijkstra
   - **Solution**: Use direct array access

3. **Spatial queries** (5-10% of time)
   - `TransitGraph.getStatesNearPlace`
   - Linear scan through all states
   - **Solution**: Implement grid-based spatial index

4. **Distance calculations** (3-5% of time)
   - `Geo.distance` (Haversine formula)
   - **Solution**: Pre-compute walking edges, cache calculations

---

## Next Steps After Benchmarking

### If Performance Meets Targets (✓ PASS)
1. Document results
2. Run regression tests periodically
3. Monitor production performance
4. Consider optimizations for future improvements

### If Performance is Marginal (⚠ MARGINAL)
1. Implement Priority 1 optimizations from PERFORMANCE_PROFILING.md
2. Focus on spatial index first (highest ROI, lowest risk)
3. Re-run benchmark to measure improvement

### If Performance Fails (✗ FAIL)
1. Profile with VisualVM/JProfiler to identify actual hotspots
2. Implement top 3 optimizations:
   - Grid spatial index
   - Custom binary heap
   - Direct edge array access
3. Consider algorithmic improvements (bidirectional Dijkstra)
4. Review data quality and graph construction

---

## Continuous Performance Monitoring

### Automated Testing
```bash
# Add to CI/CD pipeline
mvn test -Dtest=PerformanceTest

# Fail build if performance degrades
mvn test -Dtest=PerformanceTest -Dperformance.threshold=100
```

### Performance Regression Detection
- Run benchmark before/after major changes
- Track metrics over time
- Set up alerts for degradation

### Production Monitoring
- Log query times in production
- Track P95/P99 latency
- Monitor memory usage
- Alert on performance anomalies

---

## Troubleshooting

### "Data directory not found"
- Ensure data directory exists at project root
- Or specify path: `... PerformanceBenchmark /path/to/data`

### "OutOfMemoryError"
- Increase heap: `-Xmx2G`
- Check for memory leaks with profiler

### Queries timing out
- Increase timeout in Router
- Check for infinite loops in graph

### Low success rate (<70%)
- Verify data integrity
- Check access radius parameter
- Review graph construction logs

---

## Additional Resources

- **PERFORMANCE_PROFILING.md**: Detailed optimization analysis
- **PerformanceTest.java**: JUnit-based performance tests
- **PerformanceBenchmark.java**: Standalone benchmark utility
- **VisualVM Guide**: https://visualvm.github.io/documentation.html
- **JProfiler Documentation**: https://www.ej-technologies.com/products/jprofiler/overview.html

---

## Summary

✓ Performance test suite created  
✓ Benchmark utility implemented  
✓ Profiling methodology documented  
✓ Optimization recommendations provided  

**Task 16.2 Complete**: Ready for performance profiling with real data.
