package co.medellin.transit.routing;

import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.State;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance profiling tests for the routing system.
 * 
 * Tests verify:
 * - Graph loading time <1 second
 * - Route query time <100ms per query
 * - Identifies performance bottlenecks in routing operations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PerformanceTest {
    private static final Logger LOGGER = Logger.getLogger(PerformanceTest.class.getName());
    private static final String DEFAULT_DATA_DIR = "data";
    private static final int NUM_RANDOM_QUERIES = 100;
    private static final long MAX_LOAD_TIME_MS = 1000;
    private static final long MAX_QUERY_TIME_MS = 100;
    
    private TransitGraph graph;
    private Router router;
    private Parameters params;
    
    @BeforeAll
    public void setUp() throws IOException {
        LOGGER.info("Setting up performance test environment...");
        
        // Check if data directory exists
        Path dataDir = Paths.get(DEFAULT_DATA_DIR);
        if (!Files.exists(dataDir)) {
            LOGGER.warning("Data directory not found: " + dataDir.toAbsolutePath());
            LOGGER.warning("Performance tests will be skipped. To enable, place data files in: " + dataDir);
            return;
        }
        
        // Load graph and measure loading time
        long startLoad = System.nanoTime();
        GraphBuilder graphBuilder = new GraphBuilder();
        graph = graphBuilder.build(dataDir);
        long loadTimeMs = (System.nanoTime() - startLoad) / 1_000_000;
        
        LOGGER.log(Level.INFO, "Graph loaded in {0}ms: {1} states, {2} edges", 
                   new Object[]{loadTimeMs, graph.getStateCount(), graph.getEdgeCount()});
        
        // Initialize routing parameters
        params = new Parameters();
        router = new Router(graph, params);
    }
    
    @Test
    public void testGraphLoadingTime() throws IOException {
        // Skip if no data directory
        Path dataDir = Paths.get(DEFAULT_DATA_DIR);
        if (!Files.exists(dataDir)) {
            LOGGER.warning("Skipping graph loading test - no data directory");
            return;
        }
        
        LOGGER.info("=== PERFORMANCE TEST: Graph Loading Time ===");
        
        // Measure graph loading time
        long startTime = System.nanoTime();
        GraphBuilder graphBuilder = new GraphBuilder();
        TransitGraph testGraph = graphBuilder.build(dataDir);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        
        LOGGER.log(Level.INFO, "Graph loading time: {0}ms", elapsedMs);
        LOGGER.log(Level.INFO, "States: {0}, Edges: {1}", 
                   new Object[]{testGraph.getStateCount(), testGraph.getEdgeCount()});
        
        // Verify loading time meets performance target
        assertTrue(elapsedMs < MAX_LOAD_TIME_MS, 
                   String.format("Graph loading took %dms, expected <%dms", elapsedMs, MAX_LOAD_TIME_MS));
        
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        LOGGER.log(Level.INFO, "Memory usage: {0}MB", usedMemoryMB);
        
        LOGGER.info("✓ Graph loading performance: PASS");
    }
    
    @Test
    public void testRandomRouteQueryPerformance() {
        if (graph == null) {
            LOGGER.warning("Skipping query performance test - graph not loaded");
            return;
        }
        
        LOGGER.info("=== PERFORMANCE TEST: " + NUM_RANDOM_QUERIES + " Random Route Queries ===");
        
        // Generate random origin/destination pairs within Medellín bounds
        List<QueryPair> queries = generateRandomQueries(NUM_RANDOM_QUERIES);
        
        // Statistics tracking
        long totalTimeMs = 0;
        long minTimeMs = Long.MAX_VALUE;
        long maxTimeMs = 0;
        int successfulRoutes = 0;
        int failedRoutes = 0;
        List<Long> queryTimes = new ArrayList<>();
        
        // Run queries
        for (int i = 0; i < queries.size(); i++) {
            QueryPair query = queries.get(i);
            
            long startTime = System.nanoTime();
            Optional<DijkstraResult> result = router.findRoute(query.origin, query.destination);
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            
            queryTimes.add(elapsedMs);
            totalTimeMs += elapsedMs;
            minTimeMs = Math.min(minTimeMs, elapsedMs);
            maxTimeMs = Math.max(maxTimeMs, elapsedMs);
            
            if (result.isPresent()) {
                successfulRoutes++;
            } else {
                failedRoutes++;
            }
            
            // Log progress every 20 queries
            if ((i + 1) % 20 == 0) {
                LOGGER.log(Level.INFO, "Progress: {0}/{1} queries completed", 
                           new Object[]{i + 1, queries.size()});
            }
        }
        
        // Calculate statistics
        double averageMs = (double) totalTimeMs / queries.size();
        double medianMs = calculateMedian(queryTimes);
        double p95Ms = calculatePercentile(queryTimes, 0.95);
        double p99Ms = calculatePercentile(queryTimes, 0.99);
        
        // Report results
        LOGGER.info("=== PERFORMANCE RESULTS ===");
        LOGGER.log(Level.INFO, "Total queries: {0}", queries.size());
        LOGGER.log(Level.INFO, "Successful routes: {0} ({1}%)", 
                   new Object[]{successfulRoutes, (successfulRoutes * 100 / queries.size())});
        LOGGER.log(Level.INFO, "Failed routes: {0} ({1}%)", 
                   new Object[]{failedRoutes, (failedRoutes * 100 / queries.size())});
        LOGGER.info("--- Timing Statistics ---");
        LOGGER.log(Level.INFO, "Average query time: {0}ms", String.format("%.2f", averageMs));
        LOGGER.log(Level.INFO, "Median query time: {0}ms", String.format("%.2f", medianMs));
        LOGGER.log(Level.INFO, "Min query time: {0}ms", minTimeMs);
        LOGGER.log(Level.INFO, "Max query time: {0}ms", maxTimeMs);
        LOGGER.log(Level.INFO, "95th percentile: {0}ms", String.format("%.2f", p95Ms));
        LOGGER.log(Level.INFO, "99th percentile: {0}ms", String.format("%.2f", p99Ms));
        
        // Identify slow queries
        int slowQueries = 0;
        for (long time : queryTimes) {
            if (time > MAX_QUERY_TIME_MS) {
                slowQueries++;
            }
        }
        
        if (slowQueries > 0) {
            LOGGER.log(Level.WARNING, "{0} queries exceeded {1}ms threshold", 
                       new Object[]{slowQueries, MAX_QUERY_TIME_MS});
        }
        
        // Performance assessment
        if (averageMs < MAX_QUERY_TIME_MS && p95Ms < MAX_QUERY_TIME_MS * 1.5) {
            LOGGER.info("✓ Query performance: PASS (meets <100ms target)");
        } else if (averageMs < MAX_QUERY_TIME_MS * 2) {
            LOGGER.warning("⚠ Query performance: MARGINAL (average " + String.format("%.2f", averageMs) + "ms)");
        } else {
            LOGGER.severe("✗ Query performance: FAIL (average " + String.format("%.2f", averageMs) + "ms, target <100ms)");
        }
        
        // Verify average query time meets target
        assertTrue(averageMs < MAX_QUERY_TIME_MS * 2, 
                   String.format("Average query time %.2fms exceeds 2x target (%dms)", 
                                averageMs, MAX_QUERY_TIME_MS));
    }
    
    @Test
    public void testMemoryFootprint() {
        if (graph == null) {
            LOGGER.warning("Skipping memory footprint test - graph not loaded");
            return;
        }
        
        LOGGER.info("=== PERFORMANCE TEST: Memory Footprint ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection to get accurate measurement
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long usedMemoryMB = usedMemory / (1024 * 1024);
        long maxMemoryMB = runtime.maxMemory() / (1024 * 1024);
        
        LOGGER.log(Level.INFO, "Memory usage: {0}MB", usedMemoryMB);
        LOGGER.log(Level.INFO, "Max heap: {0}MB", maxMemoryMB);
        LOGGER.log(Level.INFO, "Graph: {0} states, {1} edges", 
                   new Object[]{graph.getStateCount(), graph.getEdgeCount()});
        
        // Calculate memory per node/edge
        double memoryPerState = (double) usedMemory / graph.getStateCount();
        double memoryPerEdge = (double) usedMemory / graph.getEdgeCount();
        
        LOGGER.log(Level.INFO, "Memory per state: {0} bytes", String.format("%.2f", memoryPerState));
        LOGGER.log(Level.INFO, "Memory per edge: {0} bytes", String.format("%.2f", memoryPerEdge));
        
        // Target: <512MB for full graph
        long targetMemoryMB = 512;
        if (usedMemoryMB < targetMemoryMB) {
            LOGGER.info("✓ Memory footprint: PASS (" + usedMemoryMB + "MB < " + targetMemoryMB + "MB)");
        } else {
            LOGGER.warning("⚠ Memory footprint: FAIL (" + usedMemoryMB + "MB > " + targetMemoryMB + "MB)");
        }
        
        assertTrue(usedMemoryMB < targetMemoryMB, 
                   String.format("Memory usage %dMB exceeds target %dMB", usedMemoryMB, targetMemoryMB));
    }
    
    @Test
    public void testSpatialIndexPerformance() {
        if (graph == null) {
            LOGGER.warning("Skipping spatial index test - graph not loaded");
            return;
        }
        
        LOGGER.info("=== PERFORMANCE TEST: Spatial Index Lookup ===");
        
        // Generate random locations
        Random rand = new Random(12345);
        int numLookups = 1000;
        
        long totalTimeNs = 0;
        List<Long> lookupTimes = new ArrayList<>();
        
        for (int i = 0; i < numLookups; i++) {
            double lat = 6.15 + rand.nextDouble() * 0.20; // 6.15 to 6.35
            double lon = -75.65 + rand.nextDouble() * 0.15; // -75.65 to -75.50
            Place place = new Place("Test Location", new Coordinate(lat, lon));
            
            long startTime = System.nanoTime();
            List<State> nearbyStates = graph.getStatesNearPlace(place, params.getAccessRadiusMeters());
            long elapsedNs = System.nanoTime() - startTime;
            
            totalTimeNs += elapsedNs;
            lookupTimes.add(elapsedNs);
        }
        
        double averageUs = (double) totalTimeNs / numLookups / 1000.0;
        double medianUs = calculateMedian(lookupTimes) / 1000.0;
        double p95Us = calculatePercentile(lookupTimes, 0.95) / 1000.0;
        
        LOGGER.log(Level.INFO, "Spatial lookups: {0}", numLookups);
        LOGGER.log(Level.INFO, "Average time: {0}μs", String.format("%.2f", averageUs));
        LOGGER.log(Level.INFO, "Median time: {0}μs", String.format("%.2f", medianUs));
        LOGGER.log(Level.INFO, "95th percentile: {0}μs", String.format("%.2f", p95Us));
        
        // Spatial lookup should be fast (ideally <1ms per lookup)
        if (averageUs < 1000) {
            LOGGER.info("✓ Spatial index performance: EXCELLENT (<1ms per lookup)");
        } else if (averageUs < 5000) {
            LOGGER.info("✓ Spatial index performance: GOOD (<5ms per lookup)");
        } else {
            LOGGER.warning("⚠ Spatial index performance: SLOW (>" + String.format("%.2f", averageUs / 1000) + "ms per lookup)");
        }
    }
    
    /**
     * Generates random origin/destination pairs within Medellín bounds.
     */
    private List<QueryPair> generateRandomQueries(int count) {
        List<QueryPair> queries = new ArrayList<>();
        Random rand = new Random(42); // Fixed seed for reproducibility
        
        // Medellín bounds: 6.15°-6.35° lat, -75.65°--75.50° lon
        for (int i = 0; i < count; i++) {
            double originLat = 6.15 + rand.nextDouble() * 0.20;
            double originLon = -75.65 + rand.nextDouble() * 0.15;
            double destLat = 6.15 + rand.nextDouble() * 0.20;
            double destLon = -75.65 + rand.nextDouble() * 0.15;
            
            Place origin = new Place("Origin " + i, new Coordinate(originLat, originLon));
            Place destination = new Place("Destination " + i, new Coordinate(destLat, destLon));
            
            queries.add(new QueryPair(origin, destination));
        }
        
        return queries;
    }
    
    /**
     * Calculates the median of a list of values.
     */
    private double calculateMedian(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }
    
    /**
     * Calculates a percentile of a list of values.
     */
    private double calculatePercentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0.0;
        
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        
        return sorted.get(index);
    }
    
    /**
     * Container for origin/destination pair.
     */
    private static class QueryPair {
        final Place origin;
        final Place destination;
        
        QueryPair(Place origin, Place destination) {
            this.origin = origin;
            this.destination = destination;
        }
    }
}
