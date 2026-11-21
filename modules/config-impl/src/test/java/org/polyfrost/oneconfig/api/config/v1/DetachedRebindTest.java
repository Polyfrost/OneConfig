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

package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link ConfigManager#rebindDetached()} has to repair after Hotwire turned a mod off and on again: Hotwire takes
 * the mod's config and trees out of OneConfig's maps and later puts the same objects back into the same maps
 */
class DetachedRebindTest {
    private static final String PROFILE_A = "oc_detached_profile_a";
    private static final String PROFILE_B = "oc_detached_profile_b";
    private static final String CONFIG = "detached_rebind_test.json";
    private static final String COMPAT = "detached_rebind_compat";

    @BeforeEach
    void setUp() throws Exception {
        ConfigManager.active();
        ConfigManager.openProfile("");
        cleanup();
    }

    @AfterEach
    void tearDown() throws Exception {
        ConfigManager.openProfile("");
        cleanup();
    }

    @Test
    void configPutBackAfterASwitchGetsTheNewProfilesValues() throws Exception {
        DetachedConfig config = configWith(true);
        ConfigManager.createProfile(PROFILE_A);
        assertFalse(config.enabled, "the new profile starts from the defaults");
        ConfigManager.openProfile("");
        Off off = turnOff(config);

        ConfigManager.openProfile(PROFILE_A);
        off.turnOn();
        ConfigManager.rebindDetached();

        assertFalse(config.enabled);
        assertSame(config.getTree(), ConfigManager.active().get(CONFIG));
        config.enabled = true;
        config.save();
        ConfigManager.openProfile("");
        assertTrue(config.enabled, "the profile it went off in keeps its own value");
        ConfigManager.openProfile(PROFILE_A);
        assertTrue(config.enabled, "what it saved after coming back went to the profile it came back in");
    }

    @Test
    void configPutBackAfterSwitchingAwayAndBackKeepsItsValues() throws Exception {
        DetachedConfig config = configWith(true);
        ConfigManager.createProfile(PROFILE_A);
        ConfigManager.openProfile("");
        Off off = turnOff(config);

        ConfigManager.openProfile(PROFILE_A);
        ConfigManager.openProfile("");
        off.turnOn();
        ConfigManager.rebindDetached();

        assertTrue(config.enabled);
        assertSame(config.getTree(), ConfigManager.active().get(CONFIG), "the same profile name is still a new backend");
    }

    @Test
    void configPutBackOnAProfileWithoutItsFileGetsItsDefaults() throws Exception {
        DetachedConfig config = configWith(true);
        Off off = turnOff(config);

        ConfigManager.createProfile(PROFILE_A);
        assertFalse(Files.exists(ConfigManager.profileDir(PROFILE_A).resolve(CONFIG)));
        off.turnOn();
        ConfigManager.rebindDetached();

        assertFalse(config.enabled);
        assertTrue(Files.exists(ConfigManager.profileDir(PROFILE_A).resolve(CONFIG)));
        ConfigManager.openProfile("");
        assertTrue(config.enabled, "the profile it went off in keeps its own value");
    }

    @Test
    void configPutBackAfterItsProfileWasRenamedKeepsItsValues() throws Exception {
        ConfigManager.createProfile(PROFILE_A);
        DetachedConfig config = configWith(true);
        Off off = turnOff(config);

        ConfigManager.renameProfile(PROFILE_A, PROFILE_B);
        off.turnOn();
        ConfigManager.rebindDetached();

        assertTrue(config.enabled);
        assertSame(config.getTree(), ConfigManager.active().get(CONFIG));
    }

    // Hotwire 0.9.5.1 also resets Config.tree of a mod it turned off, without which Config.save() does nothing
    @Test
    void configWhoseTreeWasResetIsBoundToTheTreePutBack() throws Exception {
        DetachedConfig config = configWith(true);
        Tree tree = config.getTree();
        Off off = turnOff(config);
        config.tree = null;
        off.turnOn();
        ConfigManager.rebindDetached();

        assertSame(tree, config.getTree());
        assertSame(tree, ConfigManager.active().get(CONFIG));
        assertTrue(config.enabled);
    }

    @Test
    void compatTreePutBackAfterASwitchGetsTheNewProfilesValues() throws Exception {
        Property<Integer> value = Properties.simple("value", "Value", "", 1);
        Tree tree = CompatSnapshots.register(Tree.tree(COMPAT).put(value));
        value.setAs(5);
        ConfigManager.createProfile(PROFILE_A);
        value.setAs(9);
        ConfigManager.openProfile("");
        assertEquals(5, value.get());
        Off off = turnOff(tree);

        ConfigManager.openProfile(PROFILE_A);
        off.turnOn();
        ConfigManager.rebindDetached();

        assertEquals(9, value.get());
        assertSame(tree, ConfigManager.active().get(COMPAT));
        ConfigManager.openProfile("");
        assertEquals(5, value.get(), "the profile it went off in keeps its own value");
    }

    @Test
    void compatTreePutBackOnAProfileMadeMeanwhileGetsItsDefaults() throws Exception {
        Property<Integer> value = Properties.simple("value", "Value", "", 1);
        Tree tree = CompatSnapshots.register(Tree.tree(COMPAT).put(value));
        value.setAs(5);
        Off off = turnOff(tree);

        ConfigManager.createProfile(PROFILE_A);
        off.turnOn();
        ConfigManager.rebindDetached();

        assertEquals(1, value.get());
        ConfigManager.openProfile("");
        assertEquals(5, value.get(), "the profile it went off in keeps its own value");
    }

    private static DetachedConfig configWith(boolean enabled) {
        DetachedConfig config = new DetachedConfig();
        config.initialize(false);
        config.enabled = enabled;
        config.save();
        return config;
    }

    /** Takes a config out the way Hotwire does at a mod's off, after everything was saved as OneConfig's listener does */
    private static Off turnOff(Config config) throws Exception {
        ConfigManager.active().saveAll();
        ConfigManager manager = ConfigManager.active();
        Tree tree = config.tree;
        configs().remove(config.id);
        manager.unregister(config.id);
        return () -> {
            configs().put(config.id, config);
            trees(manager).put(tree.getID(), tree);
        };
    }

    private static Off turnOff(Tree compat) throws Exception {
        ConfigManager.active().saveAll();
        CompatSnapshots.INSTANCE.onProfileSaving(ConfigManager.activeProfile());
        ConfigManager manager = ConfigManager.active();
        manager.unregister(compat.getID());
        known().remove(compat.getID());
        return () -> {
            known().put(compat.getID(), compat);
            trees(manager).put(compat.getID(), compat);
        };
    }

    private interface Off {
        void turnOn() throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Config> configs() throws Exception {
        return (Map<String, Config>) read(ConfigManager.class, null, "initializedConfigs");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Tree> known() throws Exception {
        return (Map<String, Tree>) read(CompatSnapshots.class, CompatSnapshots.INSTANCE, "known");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Tree> trees(ConfigManager manager) throws Exception {
        return (Map<String, Tree>) read(Backend.class, manager.backend, "trees");
    }

    private static Object read(Class<?> type, Object target, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void cleanup() throws Exception {
        configs().remove(CONFIG);
        ConfigManager.active().delete(CONFIG);
        ConfigManager.active().unregister(COMPAT);
        CompatSnapshots.untrack(COMPAT);
        Files.deleteIfExists(ConfigManager.profileDir("").resolve(CONFIG));
        deletePath(ConfigManager.PROFILES_DIR.resolve(PROFILE_A));
        deletePath(ConfigManager.PROFILES_DIR.resolve(PROFILE_B));
    }

    private static void deletePath(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private static final class DetachedConfig extends Config {
        @Switch(title = "Enabled")
        boolean enabled = false;

        private DetachedConfig() {
            super(CONFIG, "Detached Rebind Test", Category.OTHER);
            addCallback("enabled", () -> {
            });
        }
    }
}
