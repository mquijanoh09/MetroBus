package co.medellin.transit.util;

import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.routing.DijkstraResult;
import co.medellin.transit.routing.Parameters;
import co.medellin.transit.routing.Router;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Manual performance benchmarking utility.
 * Run this class directly to profile the routing system.
 * 
 * Usage: java -cp target/classes co.medellin.transit.util.PerformanceBenchmark [dataDir]
 */
public class PerformanceBenchmark {
    
    private static final int NUM_WARMUP_QUERIES = 20;
    private static final int NUM_BENCHMARK_QUERIES = 100;
    
    public static void main(String[] args) {
        try {
            String dataDirPath = args.length > 0 ? args[0] : "data";
            Path dataDir = Paths.get(dataDirPath);
            
            System.out.println("=".repeat(70));
            System.out.println(" PERFORMANCE BENCHMARK - Medellín Transit Routing");
            System.out.println("=".repeat(70));
            System.out.println();
            
            // Phase 1: Graph Loading
            System.out.println("[1/4] Graph Loading Performance");
            System.out.println("-".repeat(70));
            
            long startLoad = System.nanoTime();
            GraphBuilder graphBuilder = new GraphBuilder();
            TransitGraph graph = graphBuilder.build(dataDir);
            long loadTimeMs = (System.nanoTime() - startLoad) / 1_000_000;
            
            System.out.printf("  Graph loaded in %d ms%n", loadTimeMs);
            System.out.printf("  States: %,d%n", graph.getStateCount());
            System.out.printf("  Edges: %,d%n", graph.getEdgeCount());
            System.out.printf("  Target: < 1000 ms%n");
            System.out.printf("  Status: %s%n", loadTimeMs < 1000 ? "✓ PASS" : "✗ FAIL");
            System.out.println();
            
            // Phase 2: Memory Footprint
            System.out.println("[2/4] Memory Footprint Analysis");
            System.out.println("-".repeat(70));
            
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            Thread.sleep(100);
            
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long usedMemoryMB = usedMemory / (1024 * 1024);
            
            System.out.printf("  Memory usage: %d MB%n", usedMemoryMB);
            System.out.printf("  Target: < 512 MB%n");
            System.out.printf("  Status: %s%n", usedMemoryMB < 512 ? "✓ PASS" : "✗ FAIL");
            System.out.println();
            
            // Phase 3: Warmup
            System.out.println("[3/4] Warmup Phase");
            System.out.println("-".repeat(70));
            
            Parameters params = new Parameters();
            Router router = new Router(graph, params);
            List<QueryPair> warmupQueries = generateRandomQueries(NUM_WARMUP_QUERIES);
            
            for (QueryPair query : warmupQueries) {
                router.findRoute(query.origin, query.destination);
            }
            
            System.out.printf("  Warmup completed: %d queries%n", NUM_WARMUP_QUERIES);
            System.out.println();
            
            // Phase 4: Performance Benchmark
            System.out.println("[4/4] Query Performance Benchmark");
            System.out.println("-".repeat(70));
            
            List<QueryPair> queries = generateRandomQueries(NUM_BENCHMARK_QUERIES);
            List<Long> queryTimes = new ArrayList<>();
            int successCount = 0;
            
            for (int i = 0; i < queries.size(); i++) {
                QueryPair query = queries.get(i);
                
                long startQuery = System.nanoTime();
                Optional<DijkstraResult> result = router.findRoute(query.origin, query.destination);
                long elapsedMs = (System.nanoTime() - startQuery) / 1_000_000;
                
                queryTimes.add(elapsedMs);
                if (result.isPresent()) {
                    successCount++;
                }
                
                if ((i + 1) % 25 == 0) {
                    System.out.printf("  Progress: %d/%d queries completed%n", i + 1, queries.size());
                }
            }
            
            // Calculate statistics
            queryTimes.sort(Long::compareTo);
            long totalTime = queryTimes.stream().mapToLong(Long::longValue).sum();
            double averageMs = (double) totalTime / queryTimes.size();
            double medianMs = calculateMedian(queryTimes);
            double p95Ms = calculatePercentile(queryTimes, 0.95);
            double p99Ms = calculatePercentile(queryTimes, 0.99);
            long minMs = queryTimes.get(0);
            long maxMs = queryTimes.get(queryTimes.size() - 1);
            
            System.out.println();
            System.out.println("  Results:");
            System.out.printf("    Total queries: %d%n", queries.size());
            System.out.printf("    Successful: %d (%.1f%%)%n", successCount, 
                            100.0 * successCount / queries.size());
            System.out.printf("    Failed: %d (%.1f%%)%n", 
                            queries.size() - successCount, 
                            100.0 * (queries.size() - successCount) / queries.size());
            System.out.println();
            System.out.println("  Timing Statistics:");
            System.out.printf("    Average: %.2f ms%n", averageMs);
            System.out.printf("    Median:  %.2f ms%n", medianMs);
            System.out.printf("    P95:     %.2f ms%n", p95Ms);
            System.out.printf("    P99:     %.2f ms%n", p99Ms);
            System.out.printf("    Min:     %d ms%n", minMs);
            System.out.printf("    Max:     %d ms%n", maxMs);
            System.out.println();
            
            // Performance assessment
            int slowQueries = (int) queryTimes.stream().filter(t -> t > 100).count();
            double passRate = 100.0 * (queries.size() - slowQueries) / queries.size();
            
            System.out.println("  Performance Assessment:");
            System.out.printf("    Queries < 100ms: %.1f%%%n", passRate);
            System.out.printf("    Target: > 90%%%n");
            
            if (averageMs < 100 && passRate >= 90) {
                System.out.println("    Status: ✓ PASS (meets performance targets)");
            } else if (averageMs < 150 && passRate >= 80) {
                System.out.println("    Status: ⚠ MARGINAL (close to targets)");
            } else {
                System.out.println("    Status: ✗ FAIL (optimization needed)");
            }
            
            // Hotspot analysis
            System.out.println();
            System.out.println("  Hotspot Analysis:");
            System.out.println("    See PERFORMANCE_PROFILING.md for detailed optimization recommendations");
            
            if (averageMs > 100) {
                System.out.println();
                System.out.println("  Recommended Optimizations:");
                System.out.println("    1. Implement grid-based spatial index (5-10% speedup)");
                System.out.println("    2. Use custom int-based binary heap (15-20% speedup)");
                System.out.println("    3. Direct edge array access (10-15% speedup)");
            }
            
            System.out.println();
            System.out.println("=".repeat(70));
            System.out.println(" BENCHMARK COMPLETE");
            System.out.println("=".repeat(70));
            
        } catch (IOException e) {
            System.err.println("Error loading graph: " + e.getMessage());
            System.err.println("Usage: java co.medellin.transit.util.PerformanceBenchmark [dataDir]");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Benchmark interrupted");
            System.exit(1);
        }
    }
    
    private static List<QueryPair> generateRandomQueries(int count) {
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
    
    private static double calculateMedian(List<Long> sortedValues) {
        int size = sortedValues.size();
        if (size % 2 == 0) {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
        } else {
            return sortedValues.get(size / 2);
        }
    }
    
    private static double calculatePercentile(List<Long> sortedValues, double percentile) {
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
    
    private static class QueryPair {
        final Place origin;
        final Place destination;
        
        QueryPair(Place origin, Place destination) {
            this.origin = origin;
            this.destination = destination;
        }
    }
}
