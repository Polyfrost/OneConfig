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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatSnapshotsTest {
    private static final String TREE_ID = "compat_metadata_default_test";

    @Test
    void blankProfileDefaultsPreferPropertyMetadataOverTheLiveValue() throws Exception {
        Property<Boolean> property = Properties.simple("enabled", "Enabled", "", true);
        property.addMetadata("default", Boolean.FALSE);
        Tree tree = Tree.tree(TREE_ID).put(property);

        Method capture = CompatSnapshots.class.getDeclaredMethod("captureDefaults", Tree.class);
        Method restore = CompatSnapshots.class.getDeclaredMethod("restoreDefaults", Tree.class);
        capture.setAccessible(true);
        restore.setAccessible(true);
        try {
            capture.invoke(CompatSnapshots.INSTANCE, tree);
            property.setAs(true);
            restore.invoke(CompatSnapshots.INSTANCE, tree);

            assertEquals(Boolean.FALSE, property.get(),
                    "a blank profile must use the compat adapter's declared default");
        } finally {
            defaults().remove(TREE_ID);
        }
    }

    @Test
    void optionsThatCompareEqualAreEachWired() throws Exception {
        Property<Boolean> first = Properties.simple("enabled", "Enabled", "", true);
        Property<Boolean> second = Properties.simple("enabled", "Enabled", "", true);
        Tree firstTree = Tree.tree(TREE_ID + "_a").put(first);
        Tree secondTree = Tree.tree(TREE_ID + "_b").put(second);
        assertEquals(first, second);

        Method wire = CompatSnapshots.class.getDeclaredMethod("wire", Tree.class);
        wire.setAccessible(true);
        wire.invoke(CompatSnapshots.INSTANCE, firstTree);
        wire.invoke(CompatSnapshots.INSTANCE, secondTree);
        try {
            second.set(false);
        } catch (Throwable ignored) {
        }

        assertEquals(Boolean.TRUE, secondTree.getMetadata(Backend.CUSTOM_SAVE_DIRTY_METADATA),
                "an option equal to one already wired must still mark its own tree dirty");
    }

    @Test
    void aModsOwnResetHookWinsOverTheCapturedDefaults() throws Exception {
        Property<Boolean> property = Properties.simple("enabled", "Enabled", "", true);
        Tree tree = Tree.tree(TREE_ID).put(property);
        boolean[] reset = {false};
        tree.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, (Runnable) () -> {
            reset[0] = true;
            property.setAs(false);
        });

        Method capture = CompatSnapshots.class.getDeclaredMethod("captureDefaults", Tree.class);
        Method restore = CompatSnapshots.class.getDeclaredMethod("restoreDefaults", Tree.class);
        capture.setAccessible(true);
        restore.setAccessible(true);
        try {
            capture.invoke(CompatSnapshots.INSTANCE, tree);
            restore.invoke(CompatSnapshots.INSTANCE, tree);

            assertTrue(reset[0], "the mod's own reset hook must be used when it declares one");
            assertEquals(Boolean.FALSE, property.get(), "the captured default must not overwrite the mod's reset");
        } finally {
            defaults().remove(TREE_ID);
        }
    }

    @Test
    void aTurnedOffModsCallbacksSitOutANewProfileWhichGivesItItsDefaultsOnceBack() throws Exception {
        String id = TREE_ID + "_off";
        String profile = "compat-off-profile";
        Property<Boolean> property = Properties.simple("enabled", "Enabled", "", true);
        property.addMetadata("default", Boolean.FALSE);
        Tree tree = Tree.tree(id).put(property);
        int[] called = {0, 0};
        tree.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, (Runnable) () -> {
            called[0]++;
            property.setAs(false);
        });
        tree.addMetadata("custom_save", (Runnable) () -> called[1]++);
        CompatSnapshots.register(tree);
        try {
            called[1] = 0;
            ConfigManager.setOff(Set.of(id));
            ConfigManager.createProfile(profile);

            assertEquals(0, called[0], "the mod's reset hook is its code, which is off");
            assertEquals(0, called[1], "the mod's save hook is its code, which is off");
            assertEquals(Boolean.TRUE, property.get());

            ConfigManager.setOff(Set.of());

            assertEquals(1, called[0], "the mod's reset hook runs once it's back");
            assertEquals(Boolean.FALSE, property.get(), "a new profile starts from the defaults");
            assertTrue(called[1] > 0, "what the profile changed reaches the mod's own save");
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(profile);
            CompatSnapshots.untrack(id);
            ConfigManager.active().unregister(id);
        }
    }

    @Test
    void aTurnedOffModsUnsavedChangeWaitsForItToComeBack() {
        String id = TREE_ID + "_off_save";
        Property<Boolean> property = Properties.simple("enabled", "Enabled", "", true);
        Tree tree = Tree.tree(id).put(property);
        int[] saves = {0};
        tree.addMetadata("custom_save", (Runnable) () -> saves[0]++);
        CompatSnapshots.register(tree);
        try {
            saves[0] = 0;
            // the snapshot of an earlier run can already have set the other value
            property.setAs(!property.get());
            ConfigManager.setOff(Set.of(id));

            ConfigManager.active().saveAll();
            assertEquals(0, saves[0], "the mod's save hook is its code, which is off");

            ConfigManager.setOff(Set.of());
            ConfigManager.active().saveAll();
            assertEquals(1, saves[0], "the change was still never written");
        } finally {
            ConfigManager.setOff(Set.of());
            CompatSnapshots.untrack(id);
            ConfigManager.active().unregister(id);
        }
    }

    // a mod's own screen writes to its options without going through their callbacks
    private static Property<Integer> offModProperty(String id) {
        Property<Integer> property = Properties.simple("value", "Value", "", 1);
        CompatSnapshots.register(Tree.tree(id).put(property));
        return property;
    }

    @Test
    void aChangeInAModsOwnScreenStaysWithTheProfileItWentOffIn() {
        String id = TREE_ID + "_off_native";
        String other = "compat-off-native-profile";
        Property<Integer> property = offModProperty(id);
        try {
            ConfigManager.createProfile(other);
            property.setAs(2);
            ConfigManager.openProfile("");

            property.setAsSilently(3);
            ConfigManager.setOff(Set.of(id));
            ConfigManager.openProfile(other);
            ConfigManager.setOff(Set.of());
            assertEquals(2, property.get(), "the profile it came back in keeps its own value");

            ConfigManager.openProfile("");
            assertEquals(3, property.get(), "the profile it went off in keeps the change");
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(other);
            CompatSnapshots.untrack(id);
            ConfigManager.active().unregister(id);
        }
    }

    @Test
    void aChangeInAModsOwnScreenFollowsARenameWhileItIsOff() {
        String id = TREE_ID + "_off_renamed";
        String profile = "compat-off-rename-profile";
        String renamed = "compat-off-renamed-profile";
        Property<Integer> property = offModProperty(id);
        try {
            ConfigManager.createProfile(profile);
            property.setAsSilently(3);
            ConfigManager.setOff(Set.of(id));
            ConfigManager.renameProfile(profile, renamed);
            ConfigManager.openProfile("");
            ConfigManager.setOff(Set.of());

            ConfigManager.openProfile(renamed);
            assertEquals(3, property.get());
            assertFalse(Files.exists(ConfigManager.profileDir(profile)));
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(renamed);
            CompatSnapshots.untrack(id);
            ConfigManager.active().unregister(id);
        }
    }

    @Test
    void aProfileDeletedWhileItsModIsOffIsNotBroughtBack() {
        String id = TREE_ID + "_off_deleted";
        String profile = "compat-off-deleted-profile";
        offModProperty(id);
        try {
            ConfigManager.createProfile(profile);
            ConfigManager.setOff(Set.of(id));
            ConfigManager.deleteProfile(profile);
            ConfigManager.setOff(Set.of());

            assertFalse(Files.exists(ConfigManager.profileDir(profile)));
        } finally {
            ConfigManager.setOff(Set.of());
            CompatSnapshots.untrack(id);
            ConfigManager.active().unregister(id);
        }
    }

    @Test
    void aProfileCreatedWhileAModIsOffGetsItsOwnResetOnceBackWhereverItComesBack() {
        String id = TREE_ID + "_off_reset";
        String created = "compat-off-reset-profile";
        // the hook is all that knows the defaults, so what was captured is only whatever the mod held
        Property<Integer> property = Properties.simple("value", "Value", "", 5);
        Tree tree = Tree.tree(id).put(property);
        tree.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, (Runnable) () -> property.setAs(0));
        CompatSnapshots.register(tree);
        try {
            ConfigManager.setOff(Set.of(id));
            ConfigManager.createProfile(created);
            ConfigManager.openProfile("");
            ConfigManager.setOff(Set.of());
            assertEquals(5, property.get(), "the profile it came back in keeps its own value");

            ConfigManager.openProfile(created);
            assertEquals(0, property.get(), "the new profile starts from what the mod's reset hook says");

            ConfigManager.openProfile("");
            assertEquals(5, property.get(), "the hook ran for the new profile alone");
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(created);
            CompatSnapshots.untrack(id);
            ConfigManager.active().unregister(id);
        }
    }

    @Test
    void aTreeRegisteredUnderAnOffIdIsNotOff() throws Exception {
        String id = TREE_ID + "_off_replaced";
        String profile = "compat-off-replaced-profile";
        boolean[] oldReset = {false};
        boolean[] replacementReset = {false};
        Tree old = Tree.tree(id).put(Properties.simple("enabled", "Enabled", "", true));
        old.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, (Runnable) () -> oldReset[0] = true);
        CompatSnapshots.register(old);
        try {
            ConfigManager.setOff(Set.of(id));
            Tree replacement = Tree.tree(id).put(Properties.simple("enabled", "Enabled", "", true));
            replacement.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, (Runnable) () -> replacementReset[0] = true);
            CompatSnapshots.register(replacement);

            ConfigManager.createProfile(profile);

            assertFalse(oldReset[0]);
            assertTrue(replacementReset[0], "only the instance that was registered when its mod went off is off");
        } finally {
            ConfigManager.setOff(Set.of());
            ConfigManager.openProfile("");
            ConfigManager.deleteProfile(profile);
            CompatSnapshots.untrack(id);
            ConfigManager.active().unregister(id);
        }
    }

    @Test
    void isApplyingOnlyReportsTrueWhileTheSnapshotIsWriting() throws Exception {
        Property<Boolean> property = Properties.simple("enabled", "Enabled", "", true);
        property.addMetadata("default", Boolean.FALSE);
        Tree tree = Tree.tree(TREE_ID).put(property);
        boolean[] applyingDuringWrite = {false};
        property.addCallback(value -> {
            applyingDuringWrite[0] = CompatSnapshots.isApplying();
            return false;
        });

        Method capture = CompatSnapshots.class.getDeclaredMethod("captureDefaults", Tree.class);
        Method restore = CompatSnapshots.class.getDeclaredMethod("restoreDefaults", Tree.class);
        capture.setAccessible(true);
        restore.setAccessible(true);
        try {
            capture.invoke(CompatSnapshots.INSTANCE, tree);
            property.setAs(true);
            assertFalse(applyingDuringWrite[0], "a plain user edit is not a snapshot write");

            restore.invoke(CompatSnapshots.INSTANCE, tree);
            assertTrue(applyingDuringWrite[0], "restoring the profile defaults must report as a snapshot write");
            assertFalse(CompatSnapshots.isApplying(), "the flag must not leak past the write");
        } finally {
            defaults().remove(TREE_ID);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> defaults() throws Exception {
        Field field = CompatSnapshots.class.getDeclaredField("defaults");
        field.setAccessible(true);
        return (Map<String, Map<String, Object>>) field.get(CompatSnapshots.INSTANCE);
    }

    @Test
    void legacyIndexKeysSurviveARowInsertedAheadOfThem() throws Exception {
        String profile = "legacy-key-migration-test";
        String treeId = "compat_legacy_key_test";
        Tree tree = Tree.tree(treeId)
                .put(Properties.simple("header", "Header", "", "new row"))
                .put(Properties.simple("first", "Enabled", "", false))
                .put(Properties.simple("speed", "Speed", "", 1))
                .put(Properties.simple("second", "Enabled", "", false));
        CompatSnapshotStore store = new CompatSnapshotStore("compat-legacy-key-test.json");
        store.putValueWithoutScheduling(profile, treeId, "0|Enabled", true);
        store.putValueWithoutScheduling(profile, treeId, "1|Speed", 5);
        store.putValueWithoutScheduling(profile, treeId, "2|Enabled", false);

        Method ensureKeys = CompatSnapshots.class.getDeclaredMethod("ensureKeys", Tree.class);
        ensureKeys.setAccessible(true);
        ensureKeys.invoke(null, tree);
        CompatSnapshots.migrateLegacyKeys(tree, store, profile, treeId, false);

        Map<String, Object> bucket = store.load(profile).get(treeId);
        assertEquals(true, bucket.get("path:first"));
        assertEquals(5, bucket.get("path:speed"));
        assertEquals(false, bucket.get("path:second"));
        assertFalse(bucket.containsKey("path:header"), "a row with no legacy value must not get one");
        assertFalse(bucket.keySet().stream().anyMatch(k -> k.contains("|")), "legacy keys must be dropped");
    }

    @Test
    void legacyValuesForATitleWhoseCountChangedAreDroppedNotShifted() throws Exception {
        String profile = "legacy-key-count-test";
        String treeId = "compat_legacy_count_test";
        Tree tree = Tree.tree(treeId)
                .put(Properties.simple("speed", "Speed", "", 1))
                .put(Properties.simple("added", "Enabled", "", false))
                .put(Properties.simple("first", "Enabled", "", false))
                .put(Properties.simple("second", "Enabled", "", false));
        CompatSnapshotStore store = new CompatSnapshotStore("compat-legacy-count-test.json");
        store.putValueWithoutScheduling(profile, treeId, "0|Speed", 7);
        store.putValueWithoutScheduling(profile, treeId, "1|Enabled", true);
        store.putValueWithoutScheduling(profile, treeId, "2|Enabled", true);

        Method ensureKeys = CompatSnapshots.class.getDeclaredMethod("ensureKeys", Tree.class);
        ensureKeys.setAccessible(true);
        ensureKeys.invoke(null, tree);
        CompatSnapshots.migrateLegacyKeys(tree, store, profile, treeId, false);

        Map<String, Object> bucket = store.load(profile).get(treeId);
        assertEquals(7, bucket.get("path:speed"), "titles whose count is unchanged still migrate");
        assertFalse(bucket.containsKey("path:added"), "values must not slide onto the new row");
        assertFalse(bucket.containsKey("path:first"));
        assertFalse(bucket.containsKey("path:second"));
        assertFalse(bucket.containsKey("0|Speed"), "migrated legacy keys must be dropped");
        assertTrue(bucket.containsKey("1|Enabled") && bucket.containsKey("2|Enabled"), "unmatched legacy keys are kept for a later pass");
    }

    @Test
    void legacyKeysForRowsAddedLaterStillMigrate() throws Exception {
        String profile = "legacy-key-growing-test";
        String treeId = "compat_legacy_growing_test";
        Tree tree = Tree.tree(treeId).put(Properties.simple("speed", "Speed", "", 1));
        CompatSnapshotStore store = new CompatSnapshotStore("compat-legacy-growing-test.json");
        store.putValueWithoutScheduling(profile, treeId, "0|Speed", 7);
        store.putValueWithoutScheduling(profile, treeId, "1|Enabled", true);

        Method ensureKeys = CompatSnapshots.class.getDeclaredMethod("ensureKeys", Tree.class);
        ensureKeys.setAccessible(true);
        ensureKeys.invoke(null, tree);
        CompatSnapshots.migrateLegacyKeys(tree, store, profile, treeId, false);

        tree.put(Properties.simple("enabled", "Enabled", "", false));
        ensureKeys.invoke(null, tree);
        CompatSnapshots.migrateLegacyKeys(tree, store, profile, treeId, false);

        Map<String, Object> bucket = store.load(profile).get(treeId);
        assertEquals(7, bucket.get("path:speed"));
        assertEquals(true, bucket.get("path:enabled"), "a row registered after the first pass must still get its legacy value");
        assertFalse(bucket.keySet().stream().anyMatch(k -> k.contains("|")), "legacy keys must be dropped once migrated");
    }

    @Test
    void storedValuesAdaptToTheLivePropertyType() {
        assertEquals(Arrays.asList(1, 2), CompatSnapshots.coerceStored(new int[]{0}, Arrays.asList(1, 2)),
                "arrays are left to setAsReferential, which handles primitive element types");
        assertEquals(List.of("minecraft:apple"), CompatSnapshots.coerceStored(new ArrayList<>(), "minecraft:apple"));
        assertNull(CompatSnapshots.coerceStored(new ArrayList<>(List.of(1)), "x"), "text must not enter a list of numbers");
        assertEquals(0x80FF0000, ((PolyColor) CompatSnapshots.coerceStored(PolyColor.Companion.rgba(0, 0, 0, 255), 0x80FF0000)).getRawArgb());
        assertEquals(3, CompatSnapshots.coerceStored(1.0, 3));
        assertNull(CompatSnapshots.coerceStored(true, 5), "unrelated types must not be applied");
    }
}
