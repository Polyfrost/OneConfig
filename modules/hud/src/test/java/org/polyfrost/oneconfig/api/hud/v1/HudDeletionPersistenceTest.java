/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.hud.v1;

import kotlin.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudDeletionPersistenceTest {

    @BeforeEach
    void setUp() throws Exception {
        ConfigManager.active();
        wipeHudState();
    }

    @AfterEach
    void tearDown() throws Exception {
        HudManager.INSTANCE.unregister(new TestHud(), true, false);
        HudManager.INSTANCE.unregister(new UndeletableHud(), true, false);
        HudManager.INSTANCE.unregister(new SingleInstanceHud(), true, false);
        wipeHudState();
    }

    @Test
    void deletedHudDoesNotComeBackOnNextLaunch() throws Exception {
        HudManager.register(new TestHud());

        launch();
        assertEquals(1, instancesOfTestHud(), "provider with a default position should be given an instance on first launch");

        Hud instance = HudManager.INSTANCE.getHudsOfType(TestHud.class).get(0);
        HudManager.INSTANCE.removeHud(instance, true);
        assertEquals(0, instancesOfTestHud(), "deleting should drop the instance");

        launch();
        assertEquals(0, instancesOfTestHud(), "a deleted HUD must not be re-created on the next launch");
    }

    @Test
    void hudSavedToDiskIsStillLoadedOnNextLaunch() throws Exception {
        HudManager.register(new TestHud());

        launch();
        assertEquals(1, instancesOfTestHud());

        launch();
        assertEquals(1, instancesOfTestHud(), "an existing HUD should be loaded back from disk");
    }

    @Test
    void undeletableHudIsRestoredWhenItsConfigGoesMissing() throws Exception {
        HudManager.register(new UndeletableHud());

        launch();
        assertEquals(1, instancesOf(UndeletableHud.class));

        // simulate a lost config where the tree is gone but the registry still remembers
        // the provider was given its instance
        Hud instance = HudManager.INSTANCE.getHudsOfType(UndeletableHud.class).get(0);
        ConfigManager.active().delete(instance.getTree().getID());

        launch();
        assertEquals(1, instancesOf(UndeletableHud.class),
                "a HUD the user cannot delete must be restored, not left in the HUD library");
    }

    @Test
    void undeletableHudKeepsItsConfigWhenDeletionIsRequested() throws Exception {
        HudManager.register(new UndeletableHud());

        launch();
        Hud instance = HudManager.INSTANCE.getHudsOfType(UndeletableHud.class).get(0);
        String id = instance.getTree().getID();
        HudManager.INSTANCE.removeHud(instance, true);

        assertEquals(true, Files.exists(ConfigManager.active().getFolder().resolve(id)),
                "the config of a HUD the user cannot delete must survive a delete request");
    }

    @Test
    void deletedSingleInstanceHudCanBeAddedAgainWithoutRestarting() throws Exception {
        HudManager.register(new SingleInstanceHud());

        launch();
        Hud instance = HudManager.INSTANCE.getHudsOfType(SingleInstanceHud.class).get(0);
        HudManager.INSTANCE.removeHud(instance, true);
        assertEquals(0, instancesOf(SingleInstanceHud.class));

        // a single-instance provider is its own instance so deleting it must hand the provider
        // back to the HUD library in a state where it can be made again
        Hud provider = HudManager.INSTANCE.getProvider(SingleInstanceHud.class);
        Hud remade = provider.make(null);
        HudManager.INSTANCE.getActiveInstances().add(remade);
        assertEquals(1, instancesOf(SingleInstanceHud.class),
                "a deleted single-instance HUD must be re-creatable from the HUD library");
    }

    /**
     * Simulates a game restart by dropping everything HudManager holds in memory so the next
     * initialize() sees only what was persisted to disk
     */
    private static void launch() throws Exception {
        HudManager.INSTANCE.getActiveInstances().clear();
        knownProviders().clear();
        set("registryTree", null);
        set("init", false);
        for (Hud provider : new ArrayList<>(HudManager.INSTANCE.providers())) {
            set(provider, "tree", null);
        }
        HudManager.INSTANCE.initialize();
    }

    private static int instancesOfTestHud() {
        return instancesOf(TestHud.class);
    }

    private static int instancesOf(Class<? extends Hud> cls) {
        return HudManager.INSTANCE.getHudsOfType(cls).size();
    }

    private static void wipeHudState() throws Exception {
        HudManager.INSTANCE.getActiveInstances().clear();
        knownProviders().clear();
        set("registryTree", null);
        set("init", false);
        Path folder = ConfigManager.active().getFolder();
        deleteRecursively(folder.resolve("huds"));
        Files.deleteIfExists(folder.resolve("hud-registry.json"));
        // the registry tree stays tracked by the backend across tests so drop it with the file
        ConfigManager.active().delete("hud-registry.json");
        for (Path p : trackedHudTrees()) ConfigManager.active().delete(p.toString());
    }

    private static Collection<Path> trackedHudTrees() {
        ArrayList<Path> out = new ArrayList<>();
        ConfigManager.active().trees().forEach(t -> {
            String id = t.getID();
            if (id != null && id.startsWith("huds")) out.add(java.nio.file.Paths.get(id));
        });
        return out;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path p : (Iterable<Path>) stream.sorted(Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static void set(String name, Object value) throws Exception {
        set(HudManager.INSTANCE, name, value);
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<String> knownProviders() throws Exception {
        Field f = HudManager.class.getDeclaredField("knownProviders");
        f.setAccessible(true);
        return (java.util.Set<String>) f.get(HudManager.INSTANCE);
    }

    private static void set(Object owner, String name, Object value) throws Exception {
        Class<?> cls = owner.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                f.set(owner, value);
                return;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    static class TestHud extends TextHud {
        TestHud() {
            super("test-deletion", "Test Deletion HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public Pair<Float, Float> defaultPosition() {
            return new Pair<>(10f, 10f);
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public String getText() {
            return "test";
        }
    }

    static class UndeletableHud extends TextHud {
        UndeletableHud() {
            super("test-undeletable", "Test Undeletable HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public Pair<Float, Float> defaultPosition() {
            return new Pair<>(10f, 10f);
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public boolean deletable() {
            return false;
        }

        @Override
        public String getText() {
            return "test";
        }
    }

    static class SingleInstanceHud extends TextHud {
        SingleInstanceHud() {
            super("test-single-instance", "Test Single Instance HUD", Hud.Category.getINFO(), "", "");
        }

        @Override
        public Pair<Float, Float> defaultPosition() {
            return new Pair<>(10f, 10f);
        }

        @Override
        public boolean showByDefault() {
            return true;
        }

        @Override
        public boolean multipleInstancesAllowed() {
            return false;
        }

        @Override
        public String getText() {
            return "test";
        }
    }
}
