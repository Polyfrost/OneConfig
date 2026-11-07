package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moon.hotwire.api.HotwireApi;
import dev.moon.hotwire.api.PendingTask;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HotwireAppliedUpdatesTest {
    private static HotwireBridge.ModState mod(String id, String version) {
        return new HotwireBridge.ModState(id, id.toUpperCase(), version, true, true, false);
    }

    private static final List<HotwireBridge.ModState> BEFORE = List.of(mod("a", "1"), mod("b", "1"));

    @AfterEach
    void clear() {
        HotwireApi.PENDING.clear();
    }

    @Test
    void aLiveUpdateCountsAsLiveThoughItsJarSwapIsQueued() {
        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(BEFORE, List.of(mod("a", "2"), mod("b", "1")), Map.of(), Map.of("a", "A"));
        assertEquals(List.of("A"), result.getNow());
        assertEquals(List.of(), result.getNextLaunch());
    }

    @Test
    void anUpdateRunningFromACopyCountsAsLiveThoughTheOriginalsSwapIsQueued() {
        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(
                BEFORE,
                List.of(new HotwireBridge.ModState("a_live1", "A", "2", true, true, false), mod("b", "1")),
                Map.of(),
                Map.of("a", "A")
        );
        assertEquals(List.of("A"), result.getNow());
        assertEquals(List.of(), result.getNextLaunch());
    }

    @Test
    void aDependentCopiedAlongUnchangedIsNotAnUpdate() {
        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(
                BEFORE,
                List.of(new HotwireBridge.ModState("a_live1", "A", "2", true, true, false), new HotwireBridge.ModState("b_live1", "B", "1", true, true, false)),
                Map.of(),
                Map.of("a", "A")
        );
        assertEquals(List.of("A"), result.getNow());
    }

    @Test
    void aQueuedUpdateStillRunsItsOldVersion() {
        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(BEFORE, BEFORE, Map.of(), Map.of("b", "B"));
        assertEquals(List.of(), result.getNow());
        assertEquals(List.of("B"), result.getNextLaunch());
    }

    @Test
    void whatWasAlreadyQueuedIsNotThisBatch() {
        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(BEFORE, BEFORE, Map.of("old", "Old"), Map.of("old", "Old"));
        assertEquals(List.of(), result.getNow());
        assertEquals(List.of(), result.getNextLaunch());
    }

    @Test
    void anUpdateQueuedForAModAlreadyTurningOffIsStillReported() {
        HotwireApi.PENDING.add(new PendingTask("b", "B", PendingTask.Kind.OFF_AT_START));
        Map<String, String> before = HotwireBridge.INSTANCE.queuedMods();
        HotwireApi.PENDING.add(new PendingTask("b", "B", PendingTask.Kind.UPDATE));

        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(BEFORE, BEFORE, before, HotwireBridge.INSTANCE.queuedMods());
        assertEquals(List.of("B"), result.getNextLaunch());
    }

    @Test
    void aJarCopiedForTheNextLaunchIsNotAnUpdate() {
        Map<String, String> before = HotwireBridge.INSTANCE.queuedMods();
        HotwireApi.PENDING.add(new PendingTask("new", "New", PendingTask.Kind.COPIED));

        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(BEFORE, BEFORE, before, HotwireBridge.INSTANCE.queuedMods());
        assertEquals(List.of(), result.getNextLaunch());
    }

    @Test
    void aModAddedByThePlanIsNotAnUpdate() {
        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(BEFORE, List.of(mod("a", "1"), mod("b", "1"), mod("new", "1")), Map.of(), Map.of());
        assertEquals(List.of(), result.getNow());
    }
}
