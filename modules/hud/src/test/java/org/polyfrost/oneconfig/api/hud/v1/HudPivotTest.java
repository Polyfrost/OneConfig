package org.polyfrost.oneconfig.api.hud.v1;

import androidx.compose.runtime.snapshots.Snapshot;
import kotlin.Unit;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPivotTest {
    static class PlainHud extends TextHud {
        PlainHud() { super("pivot-hud", "Pivot Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "hi"; }
    }

    static class FixedXHud extends PlainHud {
        @Override public float getX() { return 123f; }
    }

    @Test
    void overriddenXHoldsStill() {
        ConfigManager.active();
        FixedXHud hud = new FixedXHud();
        assertAll(
            () -> assertEquals(123f, hud.xForWidth(hud.getScaledWidth() + 50f)),
            () -> assertEquals(0f, hud.getPivotFractionX())
        );
    }

    @Test
    void predictionsObserveInstanceChanges() {
        ConfigManager.active();
        PlainHud hud = new PlainHud();
        Set<Object> reads = new HashSet<>();
        Snapshot.Companion.observe(r -> { reads.add(r); return Unit.INSTANCE; }, null, () -> {
            hud.xForWidth(10f);
            hud.yForHeight(10f);
            return null;
        });
        assertTrue(reads.contains(HudManager.INSTANCE.getInstancesRevision()));
    }
}
