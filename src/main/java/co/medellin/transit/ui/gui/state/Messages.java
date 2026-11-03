package co.medellin.transit.ui.gui.state;

import co.medellin.transit.ui.PlanResult;

/**
 * User-facing texts for each failure (DESIGN.md, section 5.4). Always "tú".
 */
public final class Messages {

    private Messages() {
    }

    public static String forFailure(PlanResult result, String inputText) {
        String text = inputText == null ? "" : inputText.trim();
        return switch (result.getStatus()) {
            case EMPTY_INPUT -> "Escribe el origen y el destino.";
            case INVALID_FORMAT -> "No reconocemos “" + text + "” como dirección. "
                    + "Prueba con algo como CL 10 43A-20 o CR 70 x CL 48.";
            case NOT_FOUND, OUT_OF_BOUNDS -> "“" + text + "” queda fuera del área que cubre MetroBus.";
            case TOO_CLOSE -> "Origen y destino están a menos de 50 m. Puedes ir caminando.";
            case NO_ROUTE -> "No encontramos una ruta entre estas direcciones. "
                    + "Prueba con un punto cercano a una estación o a una vía principal.";
            case ERROR -> "Algo falló al calcular la ruta. Intenta de nuevo.";
            case OK -> throw new IllegalArgumentException("Not a failure");
        };
    }
}
