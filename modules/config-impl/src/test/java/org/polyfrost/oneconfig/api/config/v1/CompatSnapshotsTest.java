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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.polyfrost.compose.render.PolyColor;

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
