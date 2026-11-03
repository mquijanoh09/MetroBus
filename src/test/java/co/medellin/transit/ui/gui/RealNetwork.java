package co.medellin.transit.ui.gui;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareConfig;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.graph.GraphBuilder;
import co.medellin.transit.graph.TransitGraph;
import co.medellin.transit.routing.Parameters;
import co.medellin.transit.routing.Router;
import co.medellin.transit.routing.StepBuilder;
import co.medellin.transit.ui.TripPlanner;

import java.nio.file.Path;

/** Builds the real planner from ./data once for all frontend tests. */
public final class RealNetwork {

    public static final Path DATA = Path.of("data");
    private static TripPlanner planner;

    private RealNetwork() {
    }

    public static synchronized TripPlanner planner() throws Exception {
        if (planner == null) {
            String previous = System.getProperty("transit.data.dir");
            System.setProperty("transit.data.dir", DATA.toString());
            try {
                TransitGraph graph = new GraphBuilder().build(DATA);
                planner = new TripPlanner(new Router(graph, new Parameters()), new AddressParser(),
                        new AddressLocator(), new FareCalculator(FareConfig.defaults(), graph), new StepBuilder(graph));
            } finally {
                if (previous == null) {
                    System.clearProperty("transit.data.dir");
                } else {
                    System.setProperty("transit.data.dir", previous);
                }
            }
        }
        return planner;
    }
}
