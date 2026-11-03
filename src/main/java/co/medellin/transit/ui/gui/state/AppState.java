package co.medellin.transit.ui.gui.state;

import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.gui.model.NetworkLines;

import java.util.Objects;

/**
 * Everything the window shows, in one immutable value. Views draw from it and
 * never change it; only {@link AppController} makes the next state.
 *
 * @param phase           current phase
 * @param originText      text in the origin field
 * @param destinationText text in the destination field
 * @param result          last plan result (success or failure), or null
 * @param errorMessage    message for the user in ERROR or FATAL, or null
 * @param errorField      which field caused the error
 * @param searchQueued    the user asked to search while the network was loading
 * @param network         lines for the map; empty until read
 * @param layout          layout for the current window width
 * @param mapExpanded     compact layout only: map takes half the height
 */
public record AppState(Phase phase, String originText, String destinationText, PlanResult result,
                       String errorMessage, PlanResult.Field errorField, boolean searchQueued,
                       NetworkLines network, ScreenLayout layout, boolean mapExpanded) {

    public AppState {
        Objects.requireNonNull(phase);
        originText = originText == null ? "" : originText;
        destinationText = destinationText == null ? "" : destinationText;
        errorField = errorField == null ? PlanResult.Field.NONE : errorField;
        network = network == null ? NetworkLines.empty() : network;
        layout = layout == null ? ScreenLayout.WIDE : layout;
    }

    public static AppState initial() {
        return new AppState(Phase.LOADING, "", "", null, null, PlanResult.Field.NONE, false,
                NetworkLines.empty(), ScreenLayout.WIDE, false);
    }

    public boolean canSearch() {
        return phase == Phase.IDLE || phase == Phase.RESULT || phase == Phase.ERROR;
    }

    public boolean showsResult() {
        return phase == Phase.RESULT && result != null && result.isSuccess();
    }

    public AppState withPhase(Phase p) {
        return new AppState(p, originText, destinationText, result, errorMessage, errorField, searchQueued,
                network, layout, mapExpanded);
    }

    public AppState withTexts(String origin, String destination) {
        return new AppState(phase, origin, destination, result, errorMessage, errorField, searchQueued,
                network, layout, mapExpanded);
    }

    public AppState withResult(Phase p, PlanResult r, String message, PlanResult.Field field) {
        return new AppState(p, originText, destinationText, r, message, field, false, network, layout,
                p == Phase.RESULT && mapExpanded);
    }

    public AppState withQueued(boolean queued) {
        return new AppState(phase, originText, destinationText, result, errorMessage, errorField, queued,
                network, layout, mapExpanded);
    }

    public AppState withNetwork(NetworkLines n) {
        return new AppState(phase, originText, destinationText, result, errorMessage, errorField, searchQueued,
                n, layout, mapExpanded);
    }

    public AppState withLayout(ScreenLayout l) {
        return new AppState(phase, originText, destinationText, result, errorMessage, errorField, searchQueued,
                network, l, mapExpanded);
    }

    public AppState withMapExpanded(boolean expanded) {
        return new AppState(phase, originText, destinationText, result, errorMessage, errorField, searchQueued,
                network, layout, expanded);
    }

    public AppState withFatal(String message) {
        return new AppState(Phase.FATAL, originText, destinationText, null, message, PlanResult.Field.NONE,
                false, network, layout, mapExpanded);
    }
}
