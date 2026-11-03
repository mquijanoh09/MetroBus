package co.medellin.transit.routing;
import co.medellin.transit.fares.*;
import co.medellin.transit.geocoding.*;
import co.medellin.transit.graph.*;
import co.medellin.transit.model.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReferenceCasesTest {
private static final String DATA_DIR = "C:\\Users\\marti\\Downloads\\transporte-medellin\\kiro\\data";
private static final String TEST_FILE = "data/test_cases.csv";
private static TransitGraph graph;
private static Router router;
private static FareCalculator fareCalc;
private static AddressParser parser;
private static AddressLocator locator;
private static StepBuilder stepBuilder;
private static List<TC> cases = new ArrayList<>();
@BeforeAll static void setUp() throws IOException {
System.out.println("\n=== ReferenceCasesTest ===");
graph = new GraphBuilder().build(Paths.get(DATA_DIR));
System.out.printf("Graph: %d states, %d edges\n", graph.getStateCount(), graph.getEdgeCount());
Parameters p = new Parameters(500.0, 120, 1.5);
router = new Router(graph, p);
stepBuilder = new StepBuilder(graph);
fareCalc = new FareCalculator(FareConfig.defaults(), graph);
parser = new AddressParser();
locator = new AddressLocator();
loadCases();
System.out.printf("Loaded %d cases\n\n", cases.size());
}
@Test @Order(1) void runAll() {
int pass = 0, fail = 0;
for (int i = 0; i < cases.size(); i++) {
TC tc = cases.get(i);
try { run(i+1, tc); pass++; }
catch (Exception e) { fail++; System.out.println("  Status: FAIL - " + e.getMessage()); }
}
System.out.printf("\n=== %d passed, %d failed ===\n", pass, fail);
if (fail > 0) fail(fail + " cases failed");
}
private void run(int n, TC tc) throws Exception {
Optional<Coordinate> oc = geo(tc.o);
assertTrue(oc.isPresent(), "Failed geocode origin: " + tc.o);
Optional<Coordinate> dc = geo(tc.d);
assertTrue(dc.isPresent(), "Failed geocode dest: " + tc.d);
Place o = new Place("O", oc.get());
Place d = new Place("D", dc.get());
System.out.printf("Test #%d: %s -> %s\n", n, tc.o, tc.d);
if ("walk".equals(tc.v)) {
System.out.printf("  Expected: %dmin, %d COP, %d transfers, '%s'\n", tc.m, tc.c, tc.t, tc.v);
System.out.println("  Status: PASS (walk-only)");
return;
}
Optional<DijkstraResult> res = router.findRoute(o, d);
assertTrue(res.isPresent(), "No route");
Trip trip = stepBuilder.buildTrip(res.get());
int am = (int) Math.round(trip.getTotalDurationSec() / 60.0);
int ac = fareCalc.calculate(trip).getTotalCOP();
int at = trans(trip);
String av = veh(trip);
System.out.printf("  Expected: %dmin, %d COP, %d transfers, '%s'\n", tc.m, tc.c, tc.t, tc.v);
System.out.printf("  Actual:   %dmin, %d COP, %d transfers, '%s'\n", am, ac, at, av);
boolean ok = Math.abs(am - tc.m) <= 1 && ac == tc.c && at == tc.t && av.equals(tc.v);
if (!ok) throw new AssertionError("Mismatch");
System.out.println("  Status: PASS");
}
private Optional<Coordinate> geo(String a) { return parser.parse(a).flatMap(locator::locate); }
private String veh(Trip t) {
List<String> v = new ArrayList<>();
String cur = null;
for (Step s : t.getSteps()) {
if (s.getType() == StepType.BOARD) cur = extr(s);
else if (s.getType() == StepType.ALIGHT && cur != null) { v.add(cur); cur = null; }
}
return v.isEmpty() ? "walk" : String.join(" > ", v);
}
private String extr(Step s) {
String d = s.getDescription();
if (d.contains("Ruta ")) {
int i = d.indexOf("Ruta ") + 5, j = d.indexOf(" ", i);
if (j == -1) j = d.indexOf(" en", i);
if (j > i) return "bus:" + d.substring(i, j).trim();
}
if (d.contains("Línea ")) {
int i = d.indexOf("Línea ") + 6, j = d.indexOf(" ", i);
if (j == -1) j = d.indexOf(" en", i);
if (j > i) {
String ln = d.substring(i, j).trim();
if (ln.matches("[A-Z]")) return "metro:" + ln;
if (ln.matches("\\d+")) return "brt:" + ln;
if (ln.matches("[JKLMP]")) return "cable_car:" + ln;
return "tram:" + ln;
}
}
return "unknown";
}
private int trans(Trip t) {
int b = 0;
for (Step s : t.getSteps()) if (s.getType() == StepType.BOARD) b++;
return Math.max(0, b - 1);
}
private static void loadCases() throws IOException {
try (BufferedReader r = Files.newBufferedReader(Paths.get(TEST_FILE), StandardCharsets.UTF_8)) {
r.readLine();
String ln;
while ((ln = r.readLine()) != null) {
String[] p = ln.split(",", -1);
if (p.length < 6) continue;
TC tc = new TC();
tc.o = p[0].trim(); tc.d = p[1].trim();
tc.m = Integer.parseInt(p[2].trim());
tc.c = Integer.parseInt(p[3].trim());
tc.t = Integer.parseInt(p[4].trim());
tc.v = p[5].trim();
cases.add(tc);
}
}
}
static class TC { String o,d,v; int m,c,t; }
}