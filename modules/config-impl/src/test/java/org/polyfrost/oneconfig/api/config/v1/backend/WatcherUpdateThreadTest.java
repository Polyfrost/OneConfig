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

package org.polyfrost.oneconfig.api.config.v1.backend;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.backend.impl.FileBackend;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.FileSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.NightConfigSerializer;
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform;

import static org.junit.jupiter.api.Assertions.*;

public class WatcherUpdateThreadTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    @AfterEach
    void clearTasks() {
        QueueScreenPlatform.TASKS.clear();
    }

    public static final class QueueScreenPlatform implements ScreenPlatform {
        static final BlockingQueue<Runnable> TASKS = new LinkedBlockingQueue<>();

        @Override
        public synchronized void runOnUiThread(Runnable action) {
            TASKS.add(action);
        }

        @Override public int viewportWidth() { return 0; }
        @Override public int viewportHeight() { return 0; }
        @Override public int windowWidth() { return 0; }
        @Override public int windowHeight() { return 0; }
        @Override public int guiWidth() { return 0; }
        @Override public int guiHeight() { return 0; }
        @Override public void display(@Nullable Object screen, int frames) {}
        @Override public <T> T current() { return null; }
    }

    static class Holder {
        public boolean flag = false;
    }

    @Test
    @SuppressWarnings("unchecked")
    void requestUpdateAppliesOnUiThread() throws Exception {
        Path dir = tempDir;
        FileBackend backend = new FileBackend(dir, (FileSerializer<String>[]) NightConfigSerializer.ALL);
        Holder h = new Holder();
        Tree tree = Tree.tree("watch.json");
        tree.put(Properties.field("flag", null, Holder.class.getDeclaredField("flag"), h));
        backend.register(tree);

        Thread[] callbackThread = new Thread[1];
        ((org.polyfrost.oneconfig.api.config.v1.Property<Boolean>) tree.getProp("flag")).addCallback(v -> {
            callbackThread[0] = Thread.currentThread();
            return false;
        });
        Files.write(dir.resolve("watch.json"), "{ \"flag\": true }".getBytes(StandardCharsets.UTF_8));

        Thread watcher = new Thread(() -> backend.requestUpdate("watch.json"), "OneConfig Config Watcher");
        watcher.start();
        watcher.join();

        assertFalse(h.flag, "value must not be applied on the watcher thread");
        assertNull(callbackThread[0]);
        assertEquals(1, QueueScreenPlatform.TASKS.size());

        QueueScreenPlatform.TASKS.poll().run();
        assertTrue(h.flag);
        assertSame(Thread.currentThread(), callbackThread[0]);
    }

    @Test
    @SuppressWarnings("unchecked")
    void closedWatcherCannotOverwriteReboundProfile() throws Exception {
        Path oldDir = Files.createDirectory(tempDir.resolve("old"));
        Path newDir = Files.createDirectory(tempDir.resolve("new"));
        Files.writeString(oldDir.resolve("watch.json"), "{ \"flag\": false }");
        Files.writeString(newDir.resolve("watch.json"), "{ \"flag\": false }");
        FileBackend oldBackend = new FileBackend(oldDir, (FileSerializer<String>[]) NightConfigSerializer.ALL);
        FileBackend newBackend = new FileBackend(newDir, (FileSerializer<String>[]) NightConfigSerializer.ALL);
        Holder holder = new Holder();
        Tree tree = Tree.tree("watch.json");
        tree.put(Properties.field("flag", null, Holder.class.getDeclaredField("flag"), holder));
        oldBackend.register(tree);
        oldBackend.addWatcher();
        try {
            Files.writeString(oldDir.resolve("watch.json"), "{ \"flag\": true }");
            Runnable staleUpdate = awaitTask();
            oldBackend.closeWatcher();
            newBackend.register(tree);
            // Reopening must not make work from the previous watcher valid again.
            oldBackend.addWatcher();
            staleUpdate.run();
            assertFalse(holder.flag, "closed watcher must not overwrite the new profile");

            Files.writeString(oldDir.resolve("watch.json"), "{ \"flag\": true }");
            awaitTask().run();
            assertTrue(holder.flag, "the new watcher must still apply its own updates");
        } finally {
            oldBackend.closeWatcher();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"tree", "id", "all"})
    @SuppressWarnings("unchecked")
    void newerSaveInvalidatesQueuedUpdate(String saveMethod) throws Exception {
        Path file = tempDir.resolve("watch.json");
        Files.writeString(file, "{ \"flag\": false }");
        FileBackend backend = new FileBackend(tempDir, (FileSerializer<String>[]) NightConfigSerializer.ALL);
        Tree tree = Tree.tree("watch.json");
        Property<Boolean> flag = Properties.simple("flag", null, null, false);
        tree.put(flag);
        backend.register(tree);
        int[] callbacks = {0};
        flag.addCallback(value -> {
            callbacks[0]++;
            return false;
        });

        Files.writeString(file, "{ \"flag\": true }");
        backend.requestUpdate("watch.json");
        Runnable staleUpdate = awaitTask();
        switch (saveMethod) {
            case "tree" -> assertTrue(backend.save(tree));
            case "id" -> assertTrue(backend.save("watch.json"));
            case "all" -> backend.saveAll();
        }
        staleUpdate.run();
        assertFalse(flag.get(), "queued reload must not undo a newer local save");
        assertEquals(0, callbacks[0], "discarded reload must not fire callbacks");
        assertEquals(Boolean.FALSE, backend.load("watch.json").getProp("flag").get());

        Files.writeString(file, "{ \"flag\": true }");
        backend.requestUpdate("watch.json");
        Tree other = Tree.tree("other.json");
        other.put(Properties.simple("flag", null, null, false));
        assertTrue(backend.save(other));
        awaitTask().run();
        assertTrue(flag.get());
        assertEquals(1, callbacks[0]);
    }

    @Test
    @SuppressWarnings("unchecked")
    void closedWatcherCannotRestoreDeletedFile() throws Exception {
        Path file = tempDir.resolve("watch.json");
        Files.writeString(file, "{ \"flag\": false }");
        FileBackend backend = new FileBackend(tempDir, (FileSerializer<String>[]) NightConfigSerializer.ALL);
        Tree tree = Tree.tree("watch.json");
        tree.put(Properties.simple("flag", null, null, false));
        backend.register(tree);
        backend.addWatcher();
        try {
            Files.delete(file);
            Runnable staleSave = awaitTask();
            backend.closeWatcher();
            backend.addWatcher();
            staleSave.run();
            assertFalse(Files.exists(file), "closed watcher must not recreate a file after profile switch");
        } finally {
            backend.closeWatcher();
        }
    }

    private static Runnable awaitTask() throws InterruptedException {
        Runnable task = QueueScreenPlatform.TASKS.poll(10, TimeUnit.SECONDS);
        assertNotNull(task, "watcher must queue an update");
        return task;
    }
}
