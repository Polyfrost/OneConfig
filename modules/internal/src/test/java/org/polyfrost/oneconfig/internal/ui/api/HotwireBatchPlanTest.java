package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moon.hotwire.api.ChangePlan;
import dev.moon.hotwire.api.HotwireApi;
import dev.moon.hotwire.api.SwitchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HotwireBatchPlanTest {
    /** A batch where one mod can go live and another has to wait for a restart, which Hotwire reports as needing one */
    private static final class MixedPlan implements ChangePlan {
        final List<String> accepted = new ArrayList<>();

        private CompletableFuture<SwitchResult> record(String how) {
            accepted.add(how);
            return CompletableFuture.completedFuture(new SwitchResult(SwitchResult.Kind.DONE, ""));
        }

        @Override
        public boolean canApply() {
            return true;
        }

        @Override
        public Reason getReason() {
            return Reason.NEEDS_RESTART;
        }

        @Override
        public List<String> getLines() {
            return List.of();
        }

        @Override
        public CompletableFuture<SwitchResult> accept() {
            return record("accept");
        }

        @Override
        public boolean needsReopen() {
            return false;
        }

        @Override
        public boolean canWaitForNextStart() {
            return true;
        }

        @Override
        public CompletableFuture<SwitchResult> acceptAtNextStart() {
            return record("next start");
        }

        @Override
        public void cancel() {
        }
    }

    private final MixedPlan mixed = new MixedPlan();

    private HotwireBridge.JarPlan plan() {
        HotwireApi.plan = mixed;
        return HotwireBridge.INSTANCE.planUpdate(List.of(new HotwireBridge.StoreUpdate("a", "project", "A", "1", "2")));
    }

    @AfterEach
    void clear() {
        HotwireApi.plan = null;
    }

    @Test
    void theMainButtonGoesLiveWhereItCanEvenWhenPartNeedsARestart() throws Exception {
        HotwireBridge.JarAction main = plan().getActions().getFirst();
        assertTrue(main.getPrimary());
        main.attempt().get();
        assertEquals(List.of("accept"), mixed.accepted);
    }

    @Test
    void waitingForTheNextLaunchIsStillOffered() throws Exception {
        plan().getActions().get(1).attempt().get();
        assertEquals(List.of("next start"), mixed.accepted);
    }
}
