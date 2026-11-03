package co.medellin.transit.ui.gui.state;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Holds the one current {@link AppState} and tells subscribers about every
 * change on the UI executor (the Swing thread in the app, direct in tests).
 */
public final class AppStore {

    private final Executor uiExecutor;
    private final List<Consumer<AppState>> subscribers = new CopyOnWriteArrayList<>();
    private volatile AppState state;

    public AppStore(AppState initial, Executor uiExecutor) {
        this.state = Objects.requireNonNull(initial);
        this.uiExecutor = Objects.requireNonNull(uiExecutor);
    }

    public AppState state() {
        return state;
    }

    /** Applies a change and notifies subscribers if the state changed. */
    public synchronized void update(UnaryOperator<AppState> change) {
        AppState next = Objects.requireNonNull(change.apply(state));
        if (next.equals(state)) {
            return;
        }
        state = next;
        uiExecutor.execute(() -> {
            AppState current = state;
            for (Consumer<AppState> s : subscribers) {
                s.accept(current);
            }
        });
    }

    /** Subscribes and immediately renders the current state. */
    public void subscribe(Consumer<AppState> subscriber) {
        subscribers.add(subscriber);
        uiExecutor.execute(() -> subscriber.accept(state));
    }
}
