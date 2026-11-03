package co.medellin.transit.ui.gui.state;

/** Where the app is in its single task (DESIGN.md, section 8.1). */
public enum Phase {
    /** The graph is still loading; the user can already type. */
    LOADING,
    /** Ready, nothing searched yet or editing again. */
    IDLE,
    /** A search is running in the background. */
    SEARCHING,
    /** A route is on screen. */
    RESULT,
    /** The last search failed; the message says why. */
    ERROR,
    /** The network could not load; the app cannot plan trips. */
    FATAL
}
