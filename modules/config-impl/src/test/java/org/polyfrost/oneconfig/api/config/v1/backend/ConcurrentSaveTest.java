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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.backend.impl.FileBackend;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.FileSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.NightConfigSerializer;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrentSaveTest {
    private static final int ROUNDS = 2000;

    @TempDir
    Path tempDir;

    static class Holder {
        public volatile int value = 0;
    }

    // closing the OneConfig screen saves on a worker while the game thread may be saving the same trees
    @Test
    @SuppressWarnings("unchecked") // NightConfigSerializer.ALL holds only string serializers
    void concurrentSavesNeitherFailNorLeaveAnOlderValue() throws Exception {
        FileBackend backend = new FileBackend(tempDir, (FileSerializer<String>[]) NightConfigSerializer.ALL);
        Holder holder = new Holder();
        Tree tree = Tree.tree("race.json");
        tree.put(Properties.field("value", null, Holder.class.getDeclaredField("value"), holder));
        backend.register(tree);

        AtomicInteger counter = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        Runnable writer = () -> {
            for (int i = 0; i < ROUNDS; i++) {
                holder.value = counter.incrementAndGet();
                if (!backend.save(tree)) failures.incrementAndGet();
            }
        };
        Thread a = new Thread(writer);
        Thread b = new Thread(writer);
        a.start();
        b.start();
        a.join();
        b.join();

        assertEquals(0, failures.get(), "no save may fail because another one moved its temporary file");
        Holder reloaded = new Holder();
        Tree check = Tree.tree("race.json");
        check.put(Properties.field("value", null, Holder.class.getDeclaredField("value"), reloaded));
        new FileBackend(tempDir, (FileSerializer<String>[]) NightConfigSerializer.ALL).register(check);
        assertEquals(holder.value, reloaded.value, "the file must hold the value of the last save");
    }
}
