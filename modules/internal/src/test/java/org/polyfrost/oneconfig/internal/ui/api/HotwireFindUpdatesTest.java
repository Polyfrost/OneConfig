package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moon.hotwire.api.HotwireApi;
import dev.moon.hotwire.api.ModUpdate;
import dev.moon.hotwire.api.PendingTask;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HotwireFindUpdatesTest {
    private static List<String> offered(PendingTask.Kind kind) {
        HotwireApi.UPDATES.add(new ModUpdate("a", "A", "1", "2", "project-a"));
        HotwireApi.PENDING.add(new PendingTask("a", "A", kind));
        return HotwireBridge.INSTANCE.findUpdates().stream().map(HotwireBridge.StoreUpdate::getModId).toList();
    }

    @AfterEach
    void clear() {
        HotwireApi.UPDATES.clear();
        HotwireApi.PENDING.clear();
    }

    @Test
    void aQueuedUpdateIsNotOfferedAgain() {
        assertEquals(List.of(), offered(PendingTask.Kind.UPDATE));
    }

    @Test
    void aModTurningOffAtNextLaunchStillOffersItsUpdate() {
        assertEquals(List.of("a"), offered(PendingTask.Kind.OFF_AT_START));
    }

    @Test
    void aModKeepingItsOlderJarStillOffersItsUpdate() {
        assertEquals(List.of("a"), offered(PendingTask.Kind.REVERTABLE));
    }
}
