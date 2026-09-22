package org.polyfrost.oneconfig.api.hud.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Property;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudCompatRestartTest {

    static class ExternalBar {
        float fx = 100f / 960f;
        float fy = 200f / 540f;
        float w = 80f;
        float h = 10f;
        float savedFx = fx, savedFy = fy;
        int saves = 0;

        void save() { savedFx = fx; savedFy = fy; saves++; }
        void restoreFromDisk() { fx = savedFx; fy = savedFy; }
    }

    static class BarWrapper implements OneConfigHudWrapper {
        final ExternalBar bar;
        final String id;
        boolean ready = true;
        int positionWrites = 0;
        BarWrapper(ExternalBar bar) { this(bar, "restart_compat_bar"); }
        BarWrapper(ExternalBar bar, String id) { this.bar = bar; this.id = id; }

        @Override public boolean getPlacementReady() { return ready; }

        @Override public String getId() { return id; }
        @Override public void setId(String s) { }
        @Override public String getName() { return "Restart Bar"; }
        @Override public void setName(String s) { }
        @Override public String getModId() { return "fakemod"; }

        @Override public float getX() { return ready ? bar.fx * HudManager.guiScreenWidth : 0f; }
        @Override public void setX(float v) { positionWrites++; if (ready) bar.fx = v / HudManager.guiScreenWidth; }
        @Override public float getY() { return ready ? bar.fy * HudManager.guiScreenHeight : 0f; }
        @Override public void setY(float v) { positionWrites++; if (ready) bar.fy = v / HudManager.guiScreenHeight; }

        @Override public float getScale() { return 1f; }
        @Override public void setScale(float v) { }

        @Override public float getScaledWidth() { return bar.w; }
        @Override public void setScaledWidth(float v) { bar.w = v; }
        @Override public float getScaledHeight() { return bar.h; }
        @Override public void setScaledHeight(float v) { }

        @Override public boolean getHidden() { return false; }
        @Override public void setHidden(boolean h) { }
        @Override public boolean getSupportsScale() { return false; }
        @Override public HudResize getResizeAxes() { return HudResize.Width; }
        @Override public void onDragStart() { }
        @Override public void onDragEnd() { }
        @Override public List<Property<?>> linkedProperties() { return Collections.emptyList(); }
        @Override public void save() { bar.save(); }
    }

    @BeforeEach
    void setUp() {
        ConfigManager.active();
        ConfigManager.openProfile("");
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
    }

    private static Hud compatInstance() {
        for (Hud h : HudManager.INSTANCE.getActiveInstances()) {
            if (h instanceof LegacyHudMarker) return h;
        }
        throw new AssertionError("compat hud missing from activeInstances");
    }

    private static void removeCompatInstances() {
        HudManager.INSTANCE.getActiveInstances().removeIf(h -> h instanceof LegacyHudMarker);
    }

    @SuppressWarnings("unchecked")
    private static void wipeCompatMemory() throws Exception {
        Object inst = CompatSnapshots.INSTANCE;
        for (String name : new String[]{"known", "defaults", "wired", "applying"}) {
            Field f = CompatSnapshots.class.getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(inst);
            if (v instanceof Map) ((Map<?, ?>) v).clear();
            else if (v instanceof java.util.Set) ((java.util.Set<?>) v).clear();
        }
    }

    @Test
    void droppingAHudInTheEditorCapturesItsPlacementIntoTheStore() throws Exception {
        ExternalBar bar = new ExternalBar();
        new BarWrapper(bar, "dragged_compat_bar").register();
        Hud hud = compatInstance();

        hud.setAbsolutePosition(320f, 180f);
        hud.onEditorDragEnd();

        Field f = CompatSnapshots.class.getDeclaredField("store");
        f.setAccessible(true);
        Object store = f.get(CompatSnapshots.INSTANCE);
        java.lang.reflect.Method load = store.getClass().getMethod("load", String.class);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> snap =
                (Map<String, Map<String, Object>>) load.invoke(store, ConfigManager.activeProfile());
        Map<String, Object> values = snap.get(hud.getTree().getID());

        assertEquals(320f, ((Number) values.get("oc_compat_x")).floatValue(), 0.5f,
                "dropping a compat hud must write its live position into the placement store");
        assertEquals(180f, ((Number) values.get("oc_compat_y")).floatValue(), 0.5f,
                "dropping a compat hud must write its live position into the placement store");

        removeCompatInstances();
    }

    @Test
    void wrappedHudPositionSurvivesRestart() throws Exception {
        ExternalBar bar = new ExternalBar();

        new BarWrapper(bar).register();
        Hud hud = compatInstance();

        hud.setAbsolutePosition(300f, 150f);
        assertEquals(300f, hud.getX(), 0.5f);
        bar.save();
        ConfigManager.active().saveAll();

        removeCompatInstances();
        ConfigManager.active().unregister("huds/restart_compat_bar");
        wipeCompatMemory();
        bar.restoreFromDisk();
        new BarWrapper(bar).register();
        Hud again = compatInstance();

        assertEquals(300f, again.getX(), 0.5f, "compat hud X must survive a restart");
        assertEquals(150f, again.getY(), 0.5f, "compat hud Y must survive a restart");

        removeCompatInstances();
    }

    @Test
    void unreadyWrapperIsNeitherCapturedNorApplied() throws Exception {
        ExternalBar bar = new ExternalBar();

        BarWrapper first = new BarWrapper(bar, "unready_compat_bar");
        first.ready = false;
        first.register();
        first.ready = true;
        compatInstance().setAbsolutePosition(300f, 150f);
        bar.save();

        removeCompatInstances();
        ConfigManager.active().unregister("huds/unready_compat_bar");
        wipeCompatMemory();
        bar.restoreFromDisk();

        BarWrapper boot = new BarWrapper(bar, "unready_compat_bar");
        boot.ready = false;
        boot.register();
        assertEquals(0, boot.positionWrites,
                "the snapshot must not push boot-time placeholder zeroes back into the mod (issue #985)");

        boot.ready = true;
        assertEquals(300f, compatInstance().getX(), 0.5f,
                "the mod's own restored position must survive the unready registration");
        assertEquals(150f, compatInstance().getY(), 0.5f,
                "the mod's own restored position must survive the unready registration");

        removeCompatInstances();
    }
}
