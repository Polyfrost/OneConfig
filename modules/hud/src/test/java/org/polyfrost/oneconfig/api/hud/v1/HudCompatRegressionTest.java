package org.polyfrost.oneconfig.api.hud.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Property;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudCompatRegressionTest {

    static class FakeBarWrapper implements OneConfigHudWrapper {
        float barX = 100f;
        float barY = 200f;
        float barW = 80f;
        float barH = 10f;
        int detaches = 0;

        @Override public String getId() { return "fake_compat_bar"; }
        @Override public void setId(String s) { }
        @Override public String getName() { return "Fake Bar"; }
        @Override public void setName(String s) { }
        @Override public String getModId() { return "fakemod"; }

        @Override public float getX() { return barX; }
        @Override public void setX(float v) { detaches++; barX = v; }
        @Override public float getY() { return barY; }
        @Override public void setY(float v) { barY = v; }

        @Override public float getScale() { return 1f; }
        @Override public void setScale(float v) { }

        @Override public float getScaledWidth() { return barW; }
        @Override public void setScaledWidth(float v) { barW = v; }
        @Override public float getScaledHeight() { return barH; }
        @Override public void setScaledHeight(float v) { }

        @Override public boolean getHidden() { return false; }
        @Override public void setHidden(boolean h) { }
        @Override public boolean getSupportsScale() { return false; }
        @Override public HudResize getResizeAxes() { return HudResize.Width; }
        @Override public void onDragStart() { }
        @Override public void onDragEnd() { }
        @Override public List<Property<?>> linkedProperties() { return Collections.emptyList(); }
        @Override public void save() { }
    }

    @BeforeEach
    void setUp() {
        ConfigManager.active();
        ConfigManager.openProfile("");
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        HudManager.layoutRefWidth = 0f;
        HudManager.layoutRefHeight = 0f;
    }

    private static Hud findCompat() {
        for (Hud h : HudManager.INSTANCE.getActiveInstances()) {
            if (h instanceof LegacyHudMarker) return h;
        }
        throw new AssertionError("compat hud not registered into activeInstances");
    }

    private static void migrate(float w, float h) throws Exception {
        Method m = HudManager.class.getDeclaredMethod("migratePositions", float.class, float.class);
        m.setAccessible(true);
        m.invoke(HudManager.INSTANCE, w, h);
    }

    @Test
    void compatHudSurvivesRegistrationMigrationAndMoves() throws Exception {
        FakeBarWrapper wrapper = new FakeBarWrapper();
        wrapper.register();
        Hud hud = findCompat();

        assertEquals(100f, wrapper.barX, 0.001f, "registration must not move the external bar");
        assertEquals(200f, wrapper.barY, 0.001f, "registration must not move the external bar");
        assertEquals(100f, hud.getX(), 0.001f, "hud.x must read through to the wrapper");
        assertEquals(200f, hud.getY(), 0.001f, "hud.y must read through to the wrapper");

        migrate(960f, 540f);
        assertEquals(100f, wrapper.barX, 0.001f, "migratePositions must not touch a compat hud");
        assertEquals(200f, wrapper.barY, 0.001f, "migratePositions must not touch a compat hud");

        hud.setAbsolutePosition(300f, 150f);
        assertEquals(300f, wrapper.barX, 0.001f, "editor move must land on the external bar");
        assertEquals(150f, wrapper.barY, 0.001f, "editor move must land on the external bar");
        assertEquals(300f, hud.getX(), 0.001f);
        assertEquals(150f, hud.getY(), 0.001f);

        migrate(1200f, 600f);
        assertEquals(300f, wrapper.barX, 0.001f, "redimension migration must not touch a compat hud");
        assertEquals(150f, wrapper.barY, 0.001f, "redimension migration must not touch a compat hud");

        HudManager.INSTANCE.getActiveInstances().remove(hud);
    }
}
