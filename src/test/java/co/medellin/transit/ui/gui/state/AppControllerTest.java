package co.medellin.transit.ui.gui.state;

import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.TripPlanner;
import co.medellin.transit.ui.gui.RealNetwork;
import co.medellin.transit.ui.gui.model.NetworkLines;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Every state transition of DESIGN.md 8.1, without opening a window. */
class AppControllerTest {

    private static TripPlanner planner;
    private AppStore store;
    private AppController controller;

    @BeforeAll
    static void loadRealNetwork() throws Exception {
        planner = RealNetwork.planner();
    }

    @BeforeEach
    void setUp() {
        store = new AppStore(AppState.initial(), Runnable::run);
        controller = new AppController(store, Runnable::run);
    }

    @Test
    void startsLoadingAndBecomesIdleWhenPlannerIsReady() {
        assertEquals(Phase.LOADING, store.state().phase());
        assertFalse(store.state().canSearch());
        controller.plannerReady(planner::plan);
        assertEquals(Phase.IDLE, store.state().phase());
        assertTrue(store.state().canSearch());
    }

    @Test
    void searchDuringLoadingIsQueuedAndRunsWhenReady() {
        controller.editTexts("CL 10 43A-20", "CL 77 65-30");
        controller.search();
        assertTrue(store.state().searchQueued());
        assertEquals(Phase.LOADING, store.state().phase());

        controller.plannerReady(planner::plan);

        assertEquals(Phase.RESULT, store.state().phase());
        assertTrue(store.state().showsResult());
        assertFalse(store.state().searchQueued());
    }

    @Test
    void successfulSearchShowsResult() {
        controller.plannerReady(planner::plan);
        controller.editTexts("CL 50 53-48", "CR 70 x CL 48");
        controller.search();
        assertEquals(Phase.RESULT, store.state().phase());
        assertEquals("CL 50 53-48", store.state().result().getOrigin().getName());
    }

    @Test
    void searchGoesThroughSearchingPhase() {
        AtomicInteger searching = new AtomicInteger();
        store.subscribe(s -> {
            if (s.phase() == Phase.SEARCHING) {
                searching.incrementAndGet();
            }
        });
        controller.plannerReady(planner::plan);
        controller.editTexts("CL 50 53-48", "CR 70 x CL 48");
        controller.search();
        assertEquals(1, searching.get());
    }

    @Test
    void invalidOriginGivesErrorOnOriginWithFriendlyText() {
        controller.plannerReady(planner::plan);
        controller.editTexts("hola", "CL 77 65-30");
        controller.search();
        AppState s = store.state();
        assertEquals(Phase.ERROR, s.phase());
        assertEquals(PlanResult.Field.ORIGIN, s.errorField());
        assertEquals("No reconocemos “hola” como dirección. "
                + "Prueba con algo como CL 10 43A-20 o CR 70 x CL 48.", s.errorMessage());
    }

    @Test
    void invalidDestinationPointsAtDestination() {
        controller.plannerReady(planner::plan);
        controller.editTexts("CL 10 43A-20", "xyz");
        controller.search();
        assertEquals(PlanResult.Field.DESTINATION, store.state().errorField());
        assertTrue(store.state().errorMessage().contains("“xyz”"));
    }

    @Test
    void emptyInputAsksForBoth() {
        controller.plannerReady(planner::plan);
        controller.search();
        assertEquals("Escribe el origen y el destino.", store.state().errorMessage());
        assertEquals(PlanResult.Field.ORIGIN, store.state().errorField());
    }

    @Test
    void sameAddressSaysYouCanWalk() {
        controller.plannerReady(planner::plan);
        controller.editTexts("CL 10 43A-20", "CL 10 43A-20");
        controller.search();
        assertEquals("Origen y destino están a menos de 50 m. Puedes ir caminando.", store.state().errorMessage());
    }

    @Test
    void editGoesBackToFormKeepingTexts() {
        controller.plannerReady(planner::plan);
        controller.editTexts("CL 50 53-48", "CR 70 x CL 48");
        controller.search();
        controller.edit();
        assertEquals(Phase.IDLE, store.state().phase());
        assertNull(store.state().result());
        assertEquals("CL 50 53-48", store.state().originText());
        assertEquals("CR 70 x CL 48", store.state().destinationText());
    }

    @Test
    void swapExchangesTexts() {
        controller.editTexts("A", "B");
        controller.swap();
        assertEquals("B", store.state().originText());
        assertEquals("A", store.state().destinationText());
    }

    @Test
    void pickExampleFillsAndSearches() {
        controller.plannerReady(planner::plan);
        controller.pickExample(0);
        assertEquals(AppController.EXAMPLES[0][0], store.state().originText());
        assertEquals(Phase.RESULT, store.state().phase());
    }

    @Test
    void everyExampleFindsARoute() {
        controller.plannerReady(planner::plan);
        for (int i = 0; i < AppController.EXAMPLES.length; i++) {
            controller.pickExample(i);
            assertEquals(Phase.RESULT, store.state().phase(), "Example " + i + ": " + store.state().errorMessage());
            controller.edit();
        }
    }

    @Test
    void resizeChoosesLayout() {
        controller.resized(400);
        assertEquals(ScreenLayout.COMPACT, store.state().layout());
        controller.resized(759);
        assertEquals(ScreenLayout.COMPACT, store.state().layout());
        controller.resized(760);
        assertEquals(ScreenLayout.MEDIUM, store.state().layout());
        controller.resized(1099);
        assertEquals(ScreenLayout.MEDIUM, store.state().layout());
        controller.resized(1100);
        assertEquals(ScreenLayout.WIDE, store.state().layout());
    }

    @Test
    void loadFailureIsFatalAndBlocksSearch() {
        controller.loadFailed("No se pudieron cargar los datos de transporte");
        assertEquals(Phase.FATAL, store.state().phase());
        assertFalse(store.state().canSearch());
    }

    @Test
    void networkLinesReachTheState() throws Exception {
        NetworkLines lines = NetworkLines.load(RealNetwork.DATA);
        controller.networkLoaded(lines);
        assertSame(lines, store.state().network());
    }

    @Test
    void plannerExceptionBecomesErrorState() {
        controller.plannerReady((o, d) -> {
            throw new IllegalStateException("boom");
        });
        controller.editTexts("CL 10 43A-20", "CL 77 65-30");
        controller.search();
        assertEquals(Phase.ERROR, store.state().phase());
        assertEquals("Algo falló al calcular la ruta. Intenta de nuevo.", store.state().errorMessage());
    }
}
