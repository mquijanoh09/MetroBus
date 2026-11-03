package co.medellin.transit.ui.gui.model;

import co.medellin.transit.data.ArcGisReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Translates a bus {@code id_ruta} ("90111") into what riders know: its public
 * {@code codigo} ("263") and its {@code nombre} ("12 de Octubre - Calle 90"),
 * read from data/raw/routes.json the first time it is needed.
 */
public final class RouteCatalog {

    private static final Logger LOGGER = Logger.getLogger(RouteCatalog.class.getName());

    /** Public name of a bus route. */
    public record RouteLabel(String id, String code, String name) {
    }

    private final Path routesFile;
    private Map<String, RouteLabel> byId;

    public RouteCatalog(Path dataDir) {
        this.routesFile = dataDir.resolve("raw").resolve("routes.json");
    }

    /** Looks up a bus route; empty if the file or the id is missing. */
    public synchronized Optional<RouteLabel> find(String routeId) {
        if (routeId == null) {
            return Optional.empty();
        }
        if (byId == null) {
            byId = load();
        }
        return Optional.ofNullable(byId.get(routeId.trim()));
    }

    private Map<String, RouteLabel> load() {
        Map<String, RouteLabel> map = new HashMap<>();
        if (!Files.isRegularFile(routesFile)) {
            LOGGER.warning("routes.json not found: " + routesFile.toAbsolutePath());
            return map;
        }
        long start = System.nanoTime();
        try {
            List<RouteLabel> labels = new Reader().read(routesFile);
            for (RouteLabel label : labels) {
                map.put(label.id(), label);
            }
        } catch (IOException e) {
            LOGGER.warning("Could not read routes.json: " + e.getMessage());
        }
        LOGGER.info(String.format("Route catalog loaded: %d routes in %d ms",
                map.size(), (System.nanoTime() - start) / 1_000_000));
        return map;
    }

    private static final class Reader extends ArcGisReader<RouteLabel> {
        @Override
        protected Optional<RouteLabel> parseFeature(String featureJson, int featureNumber) {
            String attributes = extractAttributes(featureJson);
            Optional<String> id = getStringField(attributes, "id_ruta");
            Optional<String> code = getStringField(attributes, "codigo");
            Optional<String> name = getStringField(attributes, "nombre");
            if (id.isEmpty() || code.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new RouteLabel(id.get().trim(), code.get().trim(), name.orElse("").trim()));
        }
    }
}
