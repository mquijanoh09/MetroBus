package co.medellin.transit.ui.gui.model;

import co.medellin.transit.model.Coordinate;
import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.gui.RealNetwork;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Legs, modes, formats, map lines and the bus catalog, against the real data. */
class ModelTest {

    @Test
    void routeCodesMapToModes() {
        assertEquals(TransitMode.METRO, TransitMode.ofRoute("A"));
        assertEquals(TransitMode.METRO, TransitMode.ofRoute("B"));
        assertEquals(TransitMode.METROPLUS, TransitMode.ofRoute("1"));
        assertEquals(TransitMode.METROPLUS, TransitMode.ofRoute("2"));
        for (String c : List.of("H", "J", "K", "L", "M", "P")) {
            assertEquals(TransitMode.CABLE, TransitMode.ofRoute(c));
        }
        assertEquals(TransitMode.TRAM, TransitMode.ofRoute("T-A"));
        assertEquals(TransitMode.BUS, TransitMode.ofRoute("90111"));
    }

    @Test
    void metroTripBecomesWalkRideWalk() throws Exception {
        PlanResult r = RealNetwork.planner().plan("CL 10 43A-20", "CL 77 65-30");
        assertTrue(r.isSuccess(), r.getMessage());
        List<Leg> legs = LegBuilder.legs(r.getTrip());
        List<Leg> rides = legs.stream().filter(l -> !l.isWalk()).toList();
        assertEquals(1, rides.size(), legs.toString());
        Leg ride = rides.get(0);
        assertEquals(TransitMode.METRO, ride.mode());
        assertEquals("A", ride.routeCode());
        assertEquals("Poblado", ride.fromName());
        assertEquals("Caribe", ride.toName());
        assertEquals(0, LegBuilder.transfers(legs));
        int sum = legs.stream().mapToInt(Leg::durationSec).sum();
        int stepSum = r.getTrip().getSteps().stream().mapToInt(s -> s.getDurationSec()).sum();
        assertEquals(stepSum, sum, "Grouping must not lose or add time");
    }

    @Test
    void everyStepEndsUpInExactlyOneLeg() throws Exception {
        for (String[] ex : new String[][]{{"CR 82 x CL 51B", "CL 57A 30-87"}, {"CR 80 32-15", "CL 51 46-10"},
                {"CL 67 43-26", "CL 71 65-173"}}) {
            PlanResult r = RealNetwork.planner().plan(ex[0], ex[1]);
            assertTrue(r.isSuccess(), r.getMessage());
            List<Leg> legs = LegBuilder.legs(r.getTrip());
            long walks = r.getTrip().getSteps().stream().filter(s -> s.getType().name().equals("WALK")).count();
            long boards = r.getTrip().getSteps().stream().filter(s -> s.getType().name().equals("BOARD")).count();
            assertEquals(walks + boards, legs.size(), ex[0] + " > " + ex[1]);
        }
    }

    @Test
    void formatsAreColombian() {
        assertEquals("$3.820", Format.pesos(3820));
        assertEquals("$11.420", Format.pesos(11420));
        assertEquals("2 min", Format.duration(61));
        assertEquals("1 h 05 min", Format.duration(65 * 60));
        assertEquals("349 m", Format.meters(349.4));
        assertEquals("1,5 km", Format.meters(1500));
        assertEquals("sin transbordos", Format.transfers(0));
        assertEquals("1 transbordo", Format.transfers(1));
        assertEquals("2 transbordos", Format.transfers(2));
        assertEquals("CL 56 x CR 36", Format.place("CL 56 x CR 36, Medellin"));
        assertEquals("Poblado", Format.place("Poblado"));
        assertEquals("22 minutos", Format.durationSpoken(22 * 60));
    }

    @Test
    void networkLinesComeFromTheCsv() throws Exception {
        NetworkLines n = NetworkLines.load(RealNetwork.DATA);
        assertEquals(11, n.lines().size());
        NetworkLines.Line a = n.line("A").orElseThrow();
        assertEquals(TransitMode.METRO, a.mode());
        assertEquals(21, a.stations().size());
        assertEquals("Niquía", a.first().name());
        assertEquals("La Estrella", a.last().name());
        assertEquals(TransitMode.TRAM, n.line("T-A").orElseThrow().mode());
        assertEquals(TransitMode.METROPLUS, n.line("1").orElseThrow().mode());
    }

    @Test
    void stretchFollowsTheLineInTravelDirection() throws Exception {
        NetworkLines n = NetworkLines.load(RealNetwork.DATA);
        NetworkLines.Line a = n.line("A").orElseThrow();
        Coordinate niquia = a.first().coord();
        Coordinate estrella = a.last().coord();
        List<NetworkLines.Station> down = n.stretch("A", niquia, estrella);
        assertEquals(21, down.size());
        assertEquals("Niquía", down.get(0).name());
        List<NetworkLines.Station> up = n.stretch("A", estrella, niquia);
        assertEquals("La Estrella", up.get(0).name());
        assertTrue(n.stretch("90111", niquia, estrella).isEmpty());
    }

    @Test
    void busCatalogTranslatesIdToPublicCode() {
        RouteCatalog c = new RouteCatalog(RealNetwork.DATA);
        RouteCatalog.RouteLabel l = c.find("90111").orElseThrow();
        assertEquals("263", l.code());
        assertEquals("12 de Octubre - Calle 90", l.name());
        assertEquals("310", c.find("90007").orElseThrow().code());
        assertTrue(c.find("nope").isEmpty());
    }

    @Test
    void busCatalogWithoutFileIsEmptyNotBroken() {
        RouteCatalog c = new RouteCatalog(java.nio.file.Path.of("no-existe"));
        assertTrue(c.find("90111").isEmpty());
    }
}
