package org.polyfrost.oneconfig.api.hud.v1;

import androidx.compose.runtime.snapshots.MutableSnapshot;
import androidx.compose.runtime.snapshots.Snapshot;
import kotlin.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudFrameCacheTest {
    static class CacheHud extends TextHud {
        CacheHud() { super("cache-hud", "Cache Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "hi"; }
    }

    static class ExternalHud extends TextHud {
        float externalX = 10f;
        float externalY = 10f;
        ExternalHud() { super("external-hud", "External Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "ext"; }
        @Override public float getX() { return externalX; }
        @Override public float getY() { return externalY; }
    }

    static class StaticSizeHud extends TextHud {
        float side = 40f;
        StaticSizeHud() { super("static-size-hud", "Static Size Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "size"; }
        @Override public float getStaticW() { return side; }
        @Override public float getStaticH() { return side; }
    }

    private float savedWidth;
    private float savedHeight;
    private boolean savedGuiOpen;
    private boolean savedConfigUiOpen;
    private boolean savedPreviewInConfigUi;

    @BeforeEach
    void saveGlobals() {
        savedWidth = HudManager.guiScreenWidth;
        savedHeight = HudManager.guiScreenHeight;
        savedGuiOpen = HudManager.isGuiScreenOpen;
        savedConfigUiOpen = HudManager.isConfigUiOpen;
        savedPreviewInConfigUi = HudManager.previewInConfigUi;
    }

    @AfterEach
    void restoreGlobals() {
        HudManager.guiScreenWidth = savedWidth;
        HudManager.guiScreenHeight = savedHeight;
        HudManager.isGuiScreenOpen = savedGuiOpen;
        HudManager.isConfigUiOpen = savedConfigUiOpen;
        HudManager.previewInConfigUi = savedPreviewInConfigUi;
    }

    @Test
    void frameCachesFollowStateWrites() {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        CacheHud hud = new CacheHud();

        int stamp = hud.getGeometryStamp$hud();
        hud.setHidden(true);
        int hidden = hud.getGeometryStamp$hud();
        assertNotEquals(stamp, hidden, "a changing write invalidates the cache");
        hud.setHidden(true);
        assertEquals(hidden, hud.getGeometryStamp$hud(), "a write of the same value leaves the cache alone");

        hud.setSection(Section.TopRight);
        hud.setRelativeX(50f);
        assertEquals(hud.getX(), hud.getFrameX(), "position writes refresh the cached x");

        HudManager.guiScreenWidth = 1920f;
        assertEquals(hud.getX(), hud.getFrameX(), "a screen resize refreshes the cached x");
    }

    @Test
    void geometryStampOnlyFollowsOwnChanges() {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        CacheHud hud = new CacheHud();
        CacheHud other = new CacheHud();

        int stamp = hud.getGeometryStamp$hud();
        other.setRelativeX(other.getRelativeX() + 30f);
        other.setRenderedW(other.getRenderedW() + 30f);
        assertEquals(stamp, hud.getGeometryStamp$hud(), "another HUD changing must not invalidate this one");

        hud.setBgColor(0xFF00FF00);
        int styled = hud.getGeometryStamp$hud();
        assertNotEquals(stamp, styled, "a background change must invalidate the merge check");
        hud.setRelativeX(hud.getRelativeX() + 30f);
        assertNotEquals(styled, hud.getGeometryStamp$hud(), "a move must invalidate it");
    }

    @Test
    void cacheStaysOnWithScreenOpenUntilEditing() {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = true;
        HudManager.isConfigUiOpen = false;
        CacheHud hud = new CacheHud();

        int stamp = hud.getGeometryStamp$hud();
        assertNotEquals(-1, stamp, "a plain screen like chat keeps the cache on");
        assertEquals(stamp, hud.getGeometryStamp$hud(), "nothing changed so the cached geometry is reused");
        hud.setRelativeX(hud.getRelativeX() + 30f);
        assertNotEquals(stamp, hud.getGeometryStamp$hud(), "a move still invalidates it");
        assertEquals(hud.getX(), hud.getFrameX());

        HudManager.previewInConfigUi = true;
        HudManager.isConfigUiOpen = true;
        assertEquals(-1, hud.getGeometryStamp$hud(), "editing turns the cache off");
    }

    @Test
    void menuWithoutPreviewsKeepsCacheOn() {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = true;
        HudManager.isConfigUiOpen = true;
        HudManager.previewInConfigUi = false;
        CacheHud hud = new CacheHud();

        assertFalse(HudManager.INSTANCE.isEditing(), "the menu alone is not editing with previews turned off");
        assertNotEquals(-1, hud.getGeometryStamp$hud(), "so the cache stays on like behind any other screen");
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

    private static void nextFrame() throws Exception {
        Field frameId = HudManager.class.getDeclaredField("frameId");
        frameId.setAccessible(true);
        frameId.setLong(HudManager.INSTANCE, frameId.getLong(HudManager.INSTANCE) + 1);
    }

    @Test
    void overriddenStaticSizeKeepsTheCacheAndIsPolled() throws Exception {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        StaticSizeHud hud = new StaticSizeHud();
        hud.setSection(Section.TopRight);

        int stamp = hud.getGeometryStamp$hud();
        assertNotEquals(-1, stamp, "a custom static size leaves the rest of the geometry cacheable");
        assertEquals(hud.getX(), hud.getFrameX());

        hud.side = 80f;
        nextFrame();
        assertNotEquals(stamp, hud.getGeometryStamp$hud(), "a new static size invalidates the cache without any state write");
        assertEquals(hud.getX(), hud.getFrameX(), "the cached x follows the new width");
    }

    @Test
    void frameFlagsFollowOwnWrites() {
        ConfigManager.active();
        HudManager.isGuiScreenOpen = false;
        StaticSizeHud hud = new StaticSizeHud();

        assertFalse(hud.getFrameHidden$hud());
        assertFalse(hud.getFrameAlwaysRedraw$hud());
        hud.setHidden(true);
        assertTrue(hud.getFrameHidden$hud());
        hud.setTextChroma(true);
        assertTrue(hud.getFrameAlwaysRedraw$hud());
        hud.setTextChroma(false);
        assertFalse(hud.getFrameAlwaysRedraw$hud());
        hud.setBgChroma(true);
        assertTrue(hud.getFrameAlwaysRedraw$hud());
        hud.setBgChroma(false);
        hud.setShadowChroma(true);
        assertTrue(hud.getFrameAlwaysRedraw$hud());
        hud.setCustomScale(2f);
        assertEquals(2f, hud.getFrameEffectiveScale$hud());
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

    @Test
    void cachedPositionFollowsEachAxisMergeParent() throws Exception {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        CacheHud left = new CacheHud();
        CacheHud above = new CacheHud();
        CacheHud child = new CacheHud();
        left.setAbsolutePosition(100f, 300f);
        above.setAbsolutePosition(300f, 100f);
        child.setAbsolutePosition(300f, 300f);

        child.applyMergeLink$hud(left, HudAnchor.Left, HudAnchor.Right, MergeAxis.X);
        child.applyMergeLink$hud(above, HudAnchor.Top, HudAnchor.Bottom, MergeAxis.Y);

        float before = child.getFrameY();
        assertEquals(child.getY(), before, 0.001f);
        above.setAbsolutePosition(300f, 150f);
        assertNotEquals(before, child.getY(), 0.001f, "the Y merge parent moved so the child must move");
        assertEquals(child.getY(), child.getFrameY(), 0.001f, "the Y merge parent's move must refresh the cached y");
    }

    @Test
    void cachedPositionFollowsEveryExternalMergeParent() throws Exception {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        ExternalHud left = new ExternalHud();
        ExternalHud above = new ExternalHud();
        CacheHud child = new CacheHud();
        left.externalX = 100f;
        left.externalY = 300f;
        above.externalX = 300f;
        above.externalY = 100f;
        child.setAbsolutePosition(300f, 300f);

        child.applyMergeLink$hud(left, HudAnchor.Left, HudAnchor.Right, MergeAxis.X);
        child.applyMergeLink$hud(above, HudAnchor.Top, HudAnchor.Bottom, MergeAxis.Y);

        float before = child.getFrameY();
        assertEquals(child.getY(), before, 0.001f);

        above.externalY = 150f;
        Field frameId = HudManager.class.getDeclaredField("frameId");
        frameId.setAccessible(true);
        frameId.setLong(HudManager.INSTANCE, frameId.getLong(HudManager.INSTANCE) + 1);
        assertNotEquals(before, child.getY(), 0.001f, "the Y merge parent moved so the child must move");
        assertEquals(child.getY(), child.getFrameY(), 0.001f, "a move of any external parent must refresh the cached y");
    }

    @Test
    void releasedMergeLinkIgnoresCollapsedParent() {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        CacheHud above = new CacheHud();
        CacheHud child = new CacheHud();
        above.setRenderedW(60f);
        above.setRenderedH(30f);
        child.setRenderedW(60f);
        child.setRenderedH(20f);
        above.setAbsolutePosition(10f, 10f);
        child.setAbsolutePosition(10f, 40f);

        child.applyMergeLink$hud(above, HudAnchor.TopLeft, HudAnchor.BottomLeft, MergeAxis.Y);
        child.keepMergedPosition$hud();
        float merged = child.getY();
        above.setRenderedH(5f);
        child.releaseMergeLinks$hud(false, true);
        assertEquals(merged, child.getY(), 0.001f, "losing the merge parent must not move the HUD");
    }

    @Test
    void releasedHudDoesNotDragHudItTouchedWhileStale() throws Exception {
        ConfigManager.active();
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.isGuiScreenOpen = false;
        StaticSizeHud above = new StaticSizeHud();
        StaticSizeHud child = new StaticSizeHud();
        StaticSizeHud other = new StaticSizeHud();
        above.side = 40f;
        child.side = 60f;
        other.side = 20f;
        for (Hud hud : new Hud[]{above, child, other}) {
            hud.setStaticWidth(true);
            hud.setRenderedW(hud.getStaticW());
            hud.setRenderedH(hud.getStaticH());
        }
        above.setAbsolutePosition(10f, 10f);
        child.setAbsolutePosition(10f, 50f);
        other.setAbsolutePosition(70f, 20f);
        List<Hud> huds = Arrays.asList(above, child, other);
        HudManager.INSTANCE.getActiveInstances().addAll(huds);
        try {
            Method groups = HudManager.class.getDeclaredMethod("updateBackgroundGroups", List.class, float.class, float.class, float.class);
            groups.setAccessible(true);
            groups.invoke(HudManager.INSTANCE, huds, 960f, 540f, 1f);
            assertTrue(child.getMergeLinkY$hud() != null && child.getMergeLinkY$hud().getParent() == above, "child must hang under the HUD above it");

            above.side = 0f;
            above.setRenderedH(0f);
            assertTrue(child.layoutY$hud(540f) < 30f, "setup: the collapsed parent must pull the child beside the other HUD");
            groups.invoke(HudManager.INSTANCE, huds, 960f, 540f, 1f);
            assertEquals(50f, child.getY(), 0.001f, "losing the merge parent must not move the HUD");
            assertEquals(20f, other.getY(), 0.001f, "a HUD the stale child brushed past must stay put");
            assertTrue(other.getMergeLinkY$hud() == null && other.getMergeLinkX$hud() == null, "the brushed HUD must not stay merged");
        } finally {
            HudManager.INSTANCE.getActiveInstances().removeAll(huds);
        }
    }
}
