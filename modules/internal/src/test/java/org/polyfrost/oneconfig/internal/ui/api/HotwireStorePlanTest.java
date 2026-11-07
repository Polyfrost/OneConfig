package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moon.hotwire.api.HotwireApi;
import dev.moon.hotwire.api.StoreAdd;
import dev.moon.hotwire.api.SwitchResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HotwireStorePlanTest {
    private static List<String> actions(boolean canAdd) {
        HotwireApi.storeAdd = new StoreAdd() {
            @Override
            public String getTitle() {
                return "Add";
            }

            @Override
            public List<String> getLines() {
                return List.of();
            }

            @Override
            public boolean needsReopen() {
                return false;
            }

            @Override
            public boolean canAdd() {
                return canAdd;
            }

            @Override
            public CompletableFuture<SwitchResult> accept() {
                throw new AssertionError("nothing accepts this plan");
            }

            @Override
            public void cancel() {
            }
        };
        HotwireBridge.StoreHit hit = new HotwireBridge.StoreHit("project", "slug", "Mod", "", "", 0, null, false);
        return HotwireBridge.INSTANCE.storePlan(hit).getActions().stream().map(HotwireBridge.JarAction::getLabel).toList();
    }

    @AfterEach
    void clear() {
        HotwireApi.storeAdd = null;
    }

    @Test
    void anAddHotwireAllowsCanWaitForTheNextLaunch() {
        assertEquals(List.of("Add now", "Add at next launch", "Save to Downloads"), actions(true));
    }

    @Test
    void anAddHotwireRefusedCanOnlyBeSaved() {
        assertEquals(List.of("Save to Downloads"), actions(false));
    }
}
