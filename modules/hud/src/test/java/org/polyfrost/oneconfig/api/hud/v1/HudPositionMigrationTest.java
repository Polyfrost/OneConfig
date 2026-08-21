package org.polyfrost.oneconfig.api.hud.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPositionMigrationTest {
    static class MigHud extends TextHud {
        MigHud() { super("mig-hud", "Mig Hud", Hud.Category.getINFO(), "", ""); }
        @Override public boolean showByDefault() { return true; }
        @Override public String getText() { return "hi"; }
    }

    static class AnchorHud extends TextHud {
        AnchorHud() { super("anchor-hud", "Anchor Hud", Hud.Category.getINFO(), "", ""); }
        @Override public boolean showByDefault() { return true; }
        @Override public String getText() { return "target"; }
    }

    private static boolean registered = false;

    @BeforeEach
    void setUp() throws Exception {
        ConfigManager.active();
        ConfigManager.openProfile("");
        if (!registered) {
            HudManager.register(new MigHud());
            HudManager.register(new AnchorHud());
            registered = true;
        }
        HudManager.guiScreenWidth = 960f;
        HudManager.guiScreenHeight = 540f;
        resetManager();
    }

    private static void resetManager() throws Exception {
        HudManager.INSTANCE.getActiveInstances().clear();
        set("registryTree", null);
        set("init", false);
        for (Hud p : new ArrayList<>(HudManager.INSTANCE.providers())) {
            Field f = findField(p.getClass(), "tree");
            f.setAccessible(true);
            f.set(p, null);
        }
    }

    private static void set(String name, Object v) throws Exception {
        Field f = HudManager.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(HudManager.INSTANCE, v);
    }

    private static void relaunch() throws Exception {
        resetManager();
        HudManager.INSTANCE.initialize();
    }

    private static Field findField(Class<?> cls, String name) throws Exception {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name + " not found on " + cls);
    }

    private static Path hudFile() throws Exception {
        Path folder = ConfigManager.active().getFolder().resolve("huds");
        try (Stream<Path> s = Files.walk(folder)) {
            List<Path> hits = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().contains("mig-hud"))
                    .collect(Collectors.toList());
            assertEquals(1, hits.size(), "expected exactly one saved mig-hud config");
            return hits.get(0);
        }
    }

    @Test
    void oldSchemaPositionsAreDetectedAndConverted() throws Exception {
        HudManager.INSTANCE.initialize();
        ConfigManager.active().saveAll();

        Path file = hudFile();
        List<String> lines = Files.readAllLines(file);
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            // emulate a config written before POS_SCHEMA existed
            if (t.startsWith("posSchema:") || t.startsWith("layoutRefW:") || t.startsWith("layoutRefH:")) continue;
            if (t.startsWith("relativeX:")) { out.add("relativeX: 0.5"); continue; }
            if (t.startsWith("relativeY:")) { out.add("relativeY: 0.25"); continue; }
            out.add(line);
        }
        Files.write(file, out);
        System.out.println("=== rewritten old-schema file ===");
        System.out.println(String.join("\n", out));

        relaunch();

        Hud hud = HudManager.INSTANCE.getHudsOfType(MigHud.class).get(0);
        System.out.println("AFTER LOAD posSchema=" + hud.getPosSchema()
                + " relativeX=" + hud.getRelativeX() + " relativeY=" + hud.getRelativeY());

        assertEquals(0, hud.getPosSchema(), "an old config must be detected as pre-schema");
        assertEquals(0.5f, hud.getRelativeX(), 0.001f, "the raw fraction must survive load unconverted");

        Method m = HudManager.class.getDeclaredMethod("migratePositions", float.class, float.class);
        m.setAccessible(true);
        m.invoke(HudManager.INSTANCE, 1200f, 600f);

        System.out.println("AFTER MIGRATE posSchema=" + hud.getPosSchema()
                + " relativeX=" + hud.getRelativeX() + " relativeY=" + hud.getRelativeY());

        assertEquals(1, hud.getPosSchema(), "migration must stamp the current schema");
        assertEquals(200f, hud.getRelativeX(), 0.001f, "0.5 of a 400px grid cell is 200px");
        assertEquals(50f, hud.getRelativeY(), 0.001f, "0.25 of a 200px grid cell is 50px");
        assertTrue(HudManager.layoutRefWidth > 0f, "migration must record the window it converted at");
    }

    @Test
    void aTinyWindowDoesNotCrushStoredPositions() throws Exception {
        HudManager.INSTANCE.initialize();

        Hud hud = HudManager.INSTANCE.getHudsOfType(MigHud.class).get(0);
        hud.setSection(Section.TopLeft);

        HudManager.guiScreenWidth = 200f;
        HudManager.guiScreenHeight = 120f;

        HudManager.systemReposition = true;
        try {
            hud.setX(900f);
            hud.setY(500f);
        } finally {
            HudManager.systemReposition = false;
        }
        assertEquals(900f, hud.getRelativeX(), 0.001f,
                "a system reposition must not clamp the stored position to the shrunken window");
        assertEquals(500f, hud.getRelativeY(), 0.001f,
                "a system reposition must not clamp the stored position to the shrunken window");

        hud.setX(900f);
        assertEquals(400f, hud.getRelativeX(), 0.001f,
                "a user drag must still be clamped to the live window");
    }

    @Test
    void capturingDefaultsDoesNotMoveAnAnchoredHud() throws Exception {
        HudManager.INSTANCE.initialize();

        Hud hud = HudManager.INSTANCE.getHudsOfType(MigHud.class).get(0);
        Hud target = HudManager.INSTANCE.getHudsOfType(AnchorHud.class).get(0);

        target.setAbsolutePosition(300f, 200f);
        hud.setAbsolutePosition(500f, 250f);
        hud.anchorTo(target, HudAnchor.Right, HudAnchor.Left);

        float offX = hud.getAnchorOffsetX();
        float offY = hud.getAnchorOffsetY();
        float x = hud.getX();
        float y = hud.getY();

        hud.capturePositionDefaults();

        assertEquals(offX, hud.getAnchorOffsetX(), 0.001f,
                "capturing defaults must not rewrite the anchor offset");
        assertEquals(offY, hud.getAnchorOffsetY(), 0.001f,
                "capturing defaults must not rewrite the anchor offset");
        assertEquals(x, hud.getX(), 0.001f, "capturing defaults must not move the HUD");
        assertEquals(y, hud.getY(), 0.001f, "capturing defaults must not move the HUD");
    }
}
