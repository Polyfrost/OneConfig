package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HotwireAppliedUpdatesTest {
    private static HotwireBridge.ModState mod(String id, String version) {
        return new HotwireBridge.ModState(id, id.toUpperCase(), version, true, true, false);
    }

    private static final List<HotwireBridge.ModState> BEFORE = List.of(mod("a", "1"), mod("b", "1"));

    @Test
    void aLiveUpdateCountsAsLiveThoughItsJarSwapIsQueued() {
        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(BEFORE, List.of(mod("a", "2"), mod("b", "1")), Map.of(), Map.of("a", "A"));
        assertEquals(List.of("A"), result.getNow());
        assertEquals(List.of(), result.getNextLaunch());
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
    void aModAddedByThePlanIsNotAnUpdate() {
        HotwireBridge.AppliedUpdates result = HotwireBridge.INSTANCE.whenApplied(BEFORE, List.of(mod("a", "1"), mod("b", "1"), mod("new", "1")), Map.of(), Map.of());
        assertEquals(List.of(), result.getNow());
    }
}
