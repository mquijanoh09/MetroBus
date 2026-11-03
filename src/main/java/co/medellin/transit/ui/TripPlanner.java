package co.medellin.transit.ui;

import co.medellin.transit.fares.FareCalculator;
import co.medellin.transit.fares.FareResult;
import co.medellin.transit.geocoding.AddressLocator;
import co.medellin.transit.geocoding.AddressParser;
import co.medellin.transit.geocoding.ParsedAddress;
import co.medellin.transit.model.Coordinate;
import co.medellin.transit.model.Place;
import co.medellin.transit.model.Trip;
import co.medellin.transit.routing.DijkstraResult;
import co.medellin.transit.routing.Router;
import co.medellin.transit.routing.StepBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Plans one trip from two address strings, with no console or window code.
 *
 * <p>It runs the same steps as {@link Menu}: parse and locate both addresses,
 * reject points closer than 50 m, find the route, build the steps and price
 * the trip. Instead of printing, it returns a {@link PlanResult} that any
 * interface (console or Swing) can show. User-facing messages are in Spanish.</p>
 */
public class TripPlanner {

    /** Origin and destination closer than this need no public transport. */
    public static final double MIN_DISTANCE_METERS = 50.0;
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private final Router router;
    private final AddressParser addressParser;
    private final AddressLocator addressLocator;
    private final FareCalculator fareCalculator;
    private final StepBuilder stepBuilder;

    public TripPlanner(Router router, AddressParser addressParser, AddressLocator addressLocator,
                       FareCalculator fareCalculator, StepBuilder stepBuilder) {
        this.router = Objects.requireNonNull(router, "router");
        this.addressParser = Objects.requireNonNull(addressParser, "addressParser");
        this.addressLocator = Objects.requireNonNull(addressLocator, "addressLocator");
        this.fareCalculator = Objects.requireNonNull(fareCalculator, "fareCalculator");
        this.stepBuilder = Objects.requireNonNull(stepBuilder, "stepBuilder");
    }

    /**
     * Plans a trip between two Medellín addresses.
     *
     * @param originText      origin address, for example "Calle 10 # 43A - 20"
     * @param destinationText destination address
     * @return the outcome; never null
     */
    public PlanResult plan(String originText, String destinationText) {
        String originStr = originText == null ? "" : originText.trim();
        String destStr = destinationText == null ? "" : destinationText.trim();

        if (originStr.isEmpty()) {
            return PlanResult.failure(PlanResult.Status.EMPTY_INPUT, "Escriba la dirección de origen.");
        }
        if (destStr.isEmpty()) {
            return PlanResult.failure(PlanResult.Status.EMPTY_INPUT, "Escriba la dirección de destino.");
        }

        PlanResult originError = checkAddress(originStr, "origen");
        if (originError != null) {
            return originError;
        }
        PlanResult destError = checkAddress(destStr, "destino");
        if (destError != null) {
            return destError;
        }

        Coordinate originCoord = addressLocator.locate(addressParser.parse(originStr).get()).get();
        Coordinate destCoord = addressLocator.locate(addressParser.parse(destStr).get()).get();
        Place origin = new Place(originStr, originCoord);
        Place destination = new Place(destStr, destCoord);

        if (distanceMeters(originCoord, destCoord) < MIN_DISTANCE_METERS) {
            return PlanResult.failure(PlanResult.Status.TOO_CLOSE,
                    "El origen y el destino están muy cerca (menos de 50 metros). "
                            + "No es necesario usar transporte público.");
        }

        try {
            Optional<DijkstraResult> result = router.findRoute(origin, destination);
            if (result.isEmpty()) {
                return PlanResult.failure(PlanResult.Status.NO_ROUTE,
                        "No se encontró una ruta entre las direcciones indicadas. "
                                + "Intente con direcciones diferentes o más cercanas al transporte público.");
            }
            Trip trip = stepBuilder.buildTrip(result.get());
            FareResult fare = fareCalculator.calculate(trip);
            return PlanResult.success(origin, destination, trip, fare);
        } catch (RuntimeException e) {
            return PlanResult.failure(PlanResult.Status.ERROR,
                    "Error al calcular la ruta: " + e.getMessage());
        }
    }

    /** Returns a failure result if the address cannot be used, or null if it is fine. */
    private PlanResult checkAddress(String address, String label) {
        Optional<ParsedAddress> parsed = addressParser.parse(address);
        if (parsed.isEmpty()) {
            return PlanResult.failure(PlanResult.Status.INVALID_FORMAT,
                    "La dirección de " + label + " no tiene un formato válido. "
                            + "Ejemplos: Calle 10 # 50 - 30, Carrera 43A # 1 Sur - 100, CL 50 x CR 70.");
        }
        Optional<Coordinate> coord = addressLocator.locate(parsed.get());
        if (coord.isEmpty()) {
            return PlanResult.failure(PlanResult.Status.NOT_FOUND,
                    "No se pudo localizar la dirección de " + label
                            + ". Puede estar fuera del área de cobertura.");
        }
        if (!addressLocator.isInBounds(coord.get())) {
            return PlanResult.failure(PlanResult.Status.OUT_OF_BOUNDS,
                    "La dirección de " + label + " está fuera de los límites de Medellín.");
        }
        return null;
    }

    private static double distanceMeters(Coordinate c1, Coordinate c2) {
        double lat1 = Math.toRadians(c1.getLat());
        double lat2 = Math.toRadians(c2.getLat());
        double deltaLat = Math.toRadians(c2.getLat() - c1.getLat());
        double deltaLon = Math.toRadians(c2.getLon() - c1.getLon());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.asin(Math.sqrt(a));
    }
}
