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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
