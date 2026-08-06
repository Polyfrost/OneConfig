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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudProfileIsolationTest {
    private static final String PROFILE = "oc_test_hud_profile";

    @BeforeEach
    void setUp() throws Exception {
        ConfigManager.active();
        ConfigManager.openProfile("");
        deleteProfile();
        wipeHudState();
    }

    @AfterEach
    void tearDown() throws Exception {
        ConfigManager.openProfile("");
        HudManager.INSTANCE.unregister(new ProfileTestHud(), true, false);
        wipeHudState();
        deleteProfile();
    }

    @Test
    void hudSettingsDoNotLeakBetweenProfiles() throws Exception {
        HudManager.register(new ProfileTestHud());
        launch();
        assertEquals(1, instances());

        ConfigManager.createProfile(PROFILE);
        drain();
        assertEquals(1, instances(), "the copied profile should have its own instance of the HUD");

        hud().setHidden(true);
        ConfigManager.active().saveAll();

        ConfigManager.openProfile("");
        drain();
        assertFalse(hud().getHidden(), "hiding a HUD in one profile must not hide it in another");

        ConfigManager.openProfile(PROFILE);
        drain();
        assertTrue(hud().getHidden(), "the HUD must come back hidden in the profile it was hidden in");
    }

    @Test
    void hudDeletedInOneProfileStillExistsInTheOther() throws Exception {
        HudManager.register(new ProfileTestHud());
        launch();

        ConfigManager.createProfile(PROFILE);
        drain();

        HudManager.INSTANCE.removeHud(hud(), true);
        assertEquals(0, instances());

        ConfigManager.openProfile("");
        drain();
        assertEquals(1, instances(), "deleting a HUD in one profile must not delete it in another");
    }

    @Test
    void hudTreeOfTheOldProfileIsNotCarriedOntoTheNewOne() throws Exception {
        HudManager.register(new ProfileTestHud());
        launch();
        Object old = hud().getTree();

        ConfigManager.createProfile(PROFILE);
        assertFalse(ConfigManager.active().trees().stream().anyMatch(t -> t == old),
                "the old profile's HUD tree must not be carried onto the new profile");

        drain();
        assertFalse(hud().getTree() == old, "the reload must build the HUD from the new profile's own tree");
    }

    private static Hud hud() {
        return HudManager.INSTANCE.getHudsOfType(ProfileTestHud.class).get(0);
    }

    private static int instances() {
        return HudManager.INSTANCE.getHudsOfType(ProfileTestHud.class).size();
    }

    private static void drain() throws Exception {
        Method m = HudManager.class.getDeclaredMethod("drainProfileReload");
        m.setAccessible(true);
        m.invoke(HudManager.INSTANCE);
    }

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

    private static void deleteProfile() throws IOException {
        deleteRecursively(ConfigManager.PROFILES_DIR.resolve(PROFILE));
    }

    private static void wipeHudState() throws Exception {
        HudManager.INSTANCE.getActiveInstances().clear();
        knownProviders().clear();
        set("registryTree", null);
        set("init", false);
        set("pendingProfileReload", null);
        Path folder = ConfigManager.active().getFolder();
        deleteRecursively(folder.resolve("huds"));
        Files.deleteIfExists(folder.resolve("hud-registry.json"));
        ConfigManager.active().delete("hud-registry.json");
        for (Path p : trackedHudTrees()) ConfigManager.active().delete(p.toString());
    }

    private static Collection<Path> trackedHudTrees() {
        ArrayList<Path> out = new ArrayList<>();
        ConfigManager.active().trees().forEach(t -> {
            String id = t.getID();
            if (id != null && id.startsWith("huds")) out.add(Paths.get(id));
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

    @SuppressWarnings("unchecked")
    private static java.util.Set<String> knownProviders() throws Exception {
        Field f = HudManager.class.getDeclaredField("knownProviders");
        f.setAccessible(true);
        return (java.util.Set<String>) f.get(HudManager.INSTANCE);
    }

    private static void set(String name, Object value) throws Exception {
        set(HudManager.INSTANCE, name, value);
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

    static class ProfileTestHud extends TextHud {
        ProfileTestHud() {
            super("test-profile-isolation", "Test Profile Isolation HUD", Hud.Category.getINFO(), "", "");
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
