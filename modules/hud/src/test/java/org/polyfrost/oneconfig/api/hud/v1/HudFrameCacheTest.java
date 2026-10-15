package org.polyfrost.oneconfig.api.hud.v1;

import androidx.compose.runtime.snapshots.MutableSnapshot;
import androidx.compose.runtime.snapshots.Snapshot;
import kotlin.Unit;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudFrameCacheTest {
    static class CacheHud extends TextHud {
        CacheHud() { super("cache-hud", "Cache Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "hi"; }
    }

    static class ExternalHud extends TextHud {
        float externalX = 10f;
        ExternalHud() { super("external-hud", "External Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "ext"; }
        @Override public float getX() { return externalX; }
    }

    @Test
    void frameCachesFollowStateWrites() {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        CacheHud hud = new CacheHud();

        int rev = HudKt.getHudStateRevision();
        hud.setHidden(true);
        hud.setHidden(true);
        assertEquals(rev + 1, HudKt.getHudStateRevision(), "only a changing write bumps the revision");

        hud.setSection(Section.TopRight);
        hud.setRelativeX(50f);
        assertEquals(hud.getX(), hud.getFrameX(), "position writes refresh the cached x");

        HudManager.guiScreenWidth = 1920f;
        assertEquals(hud.getX(), hud.getFrameX(), "a screen resize refreshes the cached x");
        HudManager.guiScreenWidth = 960f;
    }

    @Test
    void overriddenGeometryIsReadLive() {
        ConfigManager.active();
        HudManager.isGuiScreenOpen = false;
        ExternalHud hud = new ExternalHud();
        assertEquals(10f, hud.getFrameX());
        hud.externalX = 42f;
        assertEquals(42f, hud.getFrameX(), "a class overriding x never serves a stale cached value");
    }

    @Test
    void snapshotWritesRefreshOnApply() {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        CacheHud hud = new CacheHud();
        hud.getFrameX();

        MutableSnapshot snapshot = Snapshot.Companion.takeMutableSnapshot(null, null);
        try {
            snapshot.enter(() -> { hud.setRelativeX(120f); return Unit.INSTANCE; });
            assertEquals(0f, hud.getFrameX());
            snapshot.apply().check();
        } finally {
            snapshot.dispose();
        }
        assertEquals(120f, hud.getX());
        assertEquals(hud.getX(), hud.getFrameX(), "applying the snapshot refreshes the cached x");
    }
}
