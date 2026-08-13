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
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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
    void aBurstOfWritesCoalescesIntoOneScheduledFlushAndLosesNothing() {
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);
        for (int i = 0; i < 200; i++) {
            store.putValue(profile, "controls", "key." + i, "key.keyboard." + i);
        }

        Path file = profileDirectory.resolve(FILE_NAME);
        assertTrue(
                waitUntil(() -> Files.isRegularFile(file)),
                "the debounced flush never wrote " + file
        );

        CompatSnapshotStore reloaded = new CompatSnapshotStore(FILE_NAME);
        for (int i = 0; i < 200; i++) {
            assertEquals("key.keyboard." + i, reloaded.getValue(profile, "controls", "key." + i));
        }
    }

    private static boolean waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return true;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
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
    void corruptSnapshotIsMovedAsideAndTheProfileStartsOver() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        Path quarantined = profileDirectory.resolve(FILE_NAME + ".corrupt");
        byte[] corrupt = "{ definitely-not-json".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertTrue(store.load(profile).isEmpty());
        assertFalse(store.hasLoadFailure(profile));
        assertArrayEquals(corrupt, Files.readAllBytes(quarantined));
        assertFalse(Files.exists(file));

        assertDoesNotThrow(() -> store.putValue(profile, "controls", "key.jump", "key.keyboard.space"));
        assertDoesNotThrow(() -> store.flushOrThrow(profile));
        assertEquals("key.keyboard.space",
                new CompatSnapshotStore(FILE_NAME).getValue(profile, "controls", "key.jump"));
    }

    @Test
    void snapshotWhichCouldNotBeReadAtAllStaysPoisonedAndUntouched() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        Files.createDirectories(file.resolve("occupied"));
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertThrows(IllegalStateException.class, () -> store.load(profile));
        assertTrue(store.hasLoadFailure(profile));
        assertThrows(IllegalStateException.class,
                () -> store.putValue(profile, "controls", "key.jump", "key.keyboard.space"));
        assertDoesNotThrow(() -> store.flush(profile));
        assertThrows(IllegalStateException.class, () -> store.flushOrThrow(profile));
        assertTrue(Files.isDirectory(file));
        assertFalse(Files.exists(profileDirectory.resolve(FILE_NAME + ".corrupt")));
    }

    @Test
    void aSecondCorruptSnapshotDoesNotOverwriteTheFirstQuarantinedCopy() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        byte[] first = "{ definitely-not-json".getBytes(StandardCharsets.UTF_8);
        Files.write(file, first);
        assertTrue(new CompatSnapshotStore(FILE_NAME).load(profile).isEmpty());

        byte[] second = "{ also-not-json".getBytes(StandardCharsets.UTF_8);
        Files.write(file, second);
        assertTrue(new CompatSnapshotStore(FILE_NAME).load(profile).isEmpty());

        assertArrayEquals(first, Files.readAllBytes(profileDirectory.resolve(FILE_NAME + ".corrupt")));
        assertArrayEquals(second, Files.readAllBytes(profileDirectory.resolve(FILE_NAME + ".corrupt.2")));
    }

    @Test
    void quarantinedCopiesStopAtTheCapAndReuseTheLastSlot() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        byte[] last = null;
        for (int i = 1; i <= CompatSnapshotStore.MAX_QUARANTINED + 2; i++) {
            last = ("{ not-json-" + i).getBytes(StandardCharsets.UTF_8);
            Files.write(file, last);
            assertTrue(new CompatSnapshotStore(FILE_NAME).load(profile).isEmpty());
        }

        assertArrayEquals("{ not-json-1".getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(profileDirectory.resolve(FILE_NAME + ".corrupt")));
        assertArrayEquals(last,
                Files.readAllBytes(profileDirectory.resolve(FILE_NAME + ".corrupt." + CompatSnapshotStore.MAX_QUARANTINED)));
        assertFalse(Files.exists(profileDirectory.resolve(FILE_NAME + ".corrupt." + (CompatSnapshotStore.MAX_QUARANTINED + 1))));
    }

    @Test
    void hasLoadFailureRepairsACorruptSnapshotBeforeAnythingHasLoadedIt() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        Path quarantined = profileDirectory.resolve(FILE_NAME + ".corrupt");
        byte[] corrupt = "{ definitely-not-json".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertFalse(store.hasLoadFailure(profile));
        assertArrayEquals(corrupt, Files.readAllBytes(quarantined));
        assertFalse(Files.exists(file));
    }

    @Test
    void blankSnapshotIsTreatedAsEmptyRatherThanCorrupt() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        Files.write(file, new byte[0]);
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertFalse(store.hasLoadFailure(profile));
        assertTrue(store.load(profile).isEmpty());
        assertDoesNotThrow(() -> store.putValue(profile, "controls", "key.jump", "key.keyboard.space"));
        assertDoesNotThrow(() -> store.flushOrThrow(profile));
        assertEquals("key.keyboard.space", store.getValue(profile, "controls", "key.jump"));

        CompatSnapshotStore reopened = new CompatSnapshotStore(FILE_NAME);
        assertEquals("key.keyboard.space", reopened.getValue(profile, "controls", "key.jump"));
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
    void structurallyInvalidSnapshotIsAlsoMovedAside() throws IOException {
        Path file = profileDirectory.resolve(FILE_NAME);
        Path quarantined = profileDirectory.resolve(FILE_NAME + ".corrupt");
        byte[] corrupt = "{\"controls\":\"not-an-object\"}".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);

        assertTrue(store.load(profile).isEmpty());
        assertArrayEquals(corrupt, Files.readAllBytes(quarantined));
        assertFalse(Files.exists(file));
    }

    @Test
    void snapshotOfAnArrayOfObjectsIsWrittenAndReadBackAsAList() throws IOException {
        CompatSnapshotStore store = new CompatSnapshotStore(FILE_NAME);
        java.util.Map<String, Object> entry = new java.util.HashMap<>();
        entry.put("class", "java.awt.Dimension");
        entry.put("width", 10);
        store.putValue(profile, "widgets", "sizes", new Object[]{entry});

        store.flushOrThrow(profile);

        CompatSnapshotStore reloaded = new CompatSnapshotStore(FILE_NAME);
        Object read = reloaded.getValue(profile, "widgets", "sizes");
        assertEquals(java.util.Collections.singletonList(entry), read);
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
