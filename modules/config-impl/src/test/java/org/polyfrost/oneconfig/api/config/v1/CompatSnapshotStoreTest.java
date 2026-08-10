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
 *   OneConfig is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License. If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatSnapshotStoreTest {
    private static final String FILE_NAME = "snapshot-store-test.json";

    private String profile;
    private Path profileDirectory;

    @BeforeEach
    void setUp() {
        ConfigManager.active();
        ConfigManager.openProfile("");
        profile = "oc_snapshot_store_" + UUID.randomUUID().toString().replace("-", "");
        ConfigManager.createProfile(profile);
        profileDirectory = ConfigManager.profileDir(profile);
    }

    @AfterEach
    void tearDown() {
        if (!ConfigManager.activeProfile().isEmpty()) ConfigManager.openProfile("");
        if (Files.isDirectory(profileDirectory)) ConfigManager.deleteProfile(profile);
    }

    @Test
    void flushOrThrowWritesAReadableSnapshot() throws IOException {
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);
        store.putValue(profile, "controls", "key.jump", "key.keyboard.space");

        store.flushOrThrow(profile);

        Path file = profileDirectory.resolve(FILE_NAME);
        assertTrue(Files.isRegularFile(file));
        CompatSnapshotStore reloaded = new CompatSnapshotStore(FILE_NAME);
        assertEquals("key.keyboard.space", reloaded.getValue(profile, "controls", "key.jump"));
    }

    @Test
    void flushOrThrowPropagatesWriteFailure() throws IOException {
        Path blockedParent = profileDirectory.resolve("blocked");
        Files.writeString(blockedParent, "not a directory", StandardCharsets.UTF_8);
        CompatSnapshotStore store = new CompatSnapshotStore("blocked/snapshot.json");
        store.putValue(profile, "controls", "key.jump", "key.keyboard.space");

        assertThrows(IOException.class, () -> store.flushOrThrow(profile));
        assertEquals("not a directory", Files.readString(blockedParent, StandardCharsets.UTF_8));
    }

    @Test
    void corruptSnapshotCannotBecomeAnEmptyWritableCache() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        byte[] corrupt = "{ definitely-not-json".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertThrows(IllegalStateException.class, () -> store.load(profile));
        assertTrue(store.hasLoadFailure(profile));
        assertThrows(IllegalStateException.class,
                () -> store.putValue(profile, "controls", "key.jump", "key.keyboard.space"));
        assertDoesNotThrow(() -> store.flush(profile));
        assertThrows(IllegalStateException.class, () -> store.flushOrThrow(profile));
        assertArrayEquals(corrupt, Files.readAllBytes(file));
    }

    @Test
    void hasLoadFailureReportsACorruptSnapshotBeforeAnythingHasLoadedIt() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        byte[] corrupt = "{ definitely-not-json".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertTrue(store.hasLoadFailure(profile));
        assertArrayEquals(corrupt, Files.readAllBytes(file));
    }

    @Test
    void hasLoadFailureDoesNotCacheOrCreateAReadableSnapshot() throws IOException {
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertFalse(store.hasLoadFailure(profile));

        assertFalse(Files.exists(profileDirectory.resolve(FILE_NAME)));
        assertDoesNotThrow(() -> store.flushOrThrow(profile));
        assertFalse(Files.exists(profileDirectory.resolve(FILE_NAME)));
    }

    @Test
    void structurallyInvalidSnapshotIsAlsoPreserved() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        byte[] corrupt = "{\"controls\":\"not-an-object\"}".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertThrows(IllegalStateException.class, () -> store.load(profile));
        assertTrue(store.hasLoadFailure(profile));
        assertArrayEquals(corrupt, Files.readAllBytes(file));

        store.deleteProfile(profile);
        assertTrue(store.hasLoadFailure(profile));

        Files.delete(file);
        store.deleteProfile(profile);
        assertFalse(store.hasLoadFailure(profile));
    }

    @Test
    void failedSnapshotWriteDoesNotAdvanceItsBaseline() throws IOException {
        String snapshotName = "snapshot-order-" + UUID.randomUUID() + ".json";
        String baselineName = "baseline-order-" + UUID.randomUUID() + ".json";
        CompatSnapshotStore snapshot = new CompatSnapshotStore(snapshotName);
        CompatSnapshotStore baseline = new CompatSnapshotStore(baselineName);
        Path baselineFile = ConfigManager.profileDir("").resolve(baselineName);

        try {
            baseline.putValueWithoutScheduling("", "tree", "value", "old");
            baseline.flushOrThrow("");
            baseline.putValueWithoutScheduling("", "tree", "value", "new");

            snapshot.putValueWithoutScheduling(profile, "tree", "value", "new");
            Files.createDirectory(profileDirectory.resolve(snapshotName));

            assertThrows(IllegalStateException.class, () ->
                    CompatSnapshots.flushSnapshotThenBaseline(snapshot, profile, baseline, ""));

            CompatSnapshotStore reloadedBaseline = new CompatSnapshotStore(baselineName);
            assertEquals("old", reloadedBaseline.getValue("", "tree", "value"));
        } finally {
            Files.deleteIfExists(baselineFile);
        }
    }
}
