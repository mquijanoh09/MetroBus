package co.medellin.transit.ui.gui.state;

import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.gui.model.NetworkLines;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

/**
 * Turns user actions into the next state. It has no Swing code: planning runs
 * on a background executor, and the result goes back through the store.
 */
public final class AppController {

    /** Example trips offered on screen. All were checked against the real data. */
    public static final String[][] EXAMPLES = {
            {"CL 10 43A-20", "CL 77 65-30"},
            {"CL 50 53-48", "CR 70 x CL 48"},
            {"CR 80 32-15", "CL 51 46-10"},
    };

    private final AppStore store;
    private final Executor background;
    private volatile BiFunction<String, String, PlanResult> planner;

    public AppController(AppStore store, Executor background) {
        this.store = Objects.requireNonNull(store);
        this.background = Objects.requireNonNull(background);
    }

    public AppStore store() {
        return store;
    }

    // ---- lifecycle -------------------------------------------------------

    public void networkLoaded(NetworkLines lines) {
        store.update(s -> s.withNetwork(lines));
    }

    /** The graph is ready: enable searching and run a search the user queued. */
    public void plannerReady(BiFunction<String, String, PlanResult> planner) {
        this.planner = Objects.requireNonNull(planner);
        boolean queued = store.state().searchQueued();
        store.update(s -> s.phase() == Phase.LOADING ? s.withPhase(Phase.IDLE).withQueued(false) : s);
        if (queued) {
            search();
        }
    }

    public void loadFailed(String message) {
        store.update(s -> s.withFatal(message));
    }

    // ---- user actions ----------------------------------------------------

    public void editTexts(String origin, String destination) {
        store.update(s -> s.withTexts(origin, destination));
    }

    public void swap() {
        store.update(s -> s.withTexts(s.destinationText(), s.originText()));
    }

    public void pickExample(int index) {
        String[] ex = EXAMPLES[index];
        store.update(s -> s.withTexts(ex[0], ex[1]));
        search();
    }

    /** Back to the form, keeping what was typed. */
    public void edit() {
        store.update(s -> s.phase() == Phase.RESULT || s.phase() == Phase.ERROR
                ? s.withResult(Phase.IDLE, null, null, PlanResult.Field.NONE) : s);
    }

    public void toggleMap() {
        store.update(s -> s.withMapExpanded(!s.mapExpanded()));
    }

    public void resized(int width) {
        ScreenLayout layout = ScreenLayout.forWidth(width);
        store.update(s -> s.withLayout(layout));
    }

    public void search() {
        AppState s = store.state();
        if (s.phase() == Phase.LOADING) {
            store.update(st -> st.withQueued(true));
            return;
        }
        if (!s.canSearch() || planner == null) {
            return;
        }
        String origin = s.originText();
        String destination = s.destinationText();
        store.update(st -> st.withPhase(Phase.SEARCHING));
        BiFunction<String, String, PlanResult> p = planner;
        background.execute(() -> {
            PlanResult result;
            try {
                result = p.apply(origin, destination);
            } catch (RuntimeException e) {
                result = PlanResult.failure(PlanResult.Status.ERROR, String.valueOf(e.getMessage()));
            }
            finish(result, origin, destination);
        });
    }

    private void finish(PlanResult result, String origin, String destination) {
        if (result.isSuccess()) {
            store.update(s -> s.withResult(Phase.RESULT, result, null, PlanResult.Field.NONE));
        } else {
            String text = result.getField() == PlanResult.Field.DESTINATION ? destination : origin;
            store.update(s -> s.withResult(Phase.ERROR, result, Messages.forFailure(result, text),
                    result.getField()));
        }
    }
}
