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
 * License along with OneConfig. If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;
import org.polyfrost.oneconfig.api.config.v1.backend.impl.FileBackend;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.FileSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.NightConfigSerializer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CompatBackendPersistenceTest {

    @SuppressWarnings("unchecked")
    private static FileBackend backend(Path dir) {
        return new FileBackend(dir, (FileSerializer<String>[]) NightConfigSerializer.ALL);
    }

    @Test
    void uiOnlyCompatTreeRunsExternalSaveWithoutWritingBridgeFile() throws Exception {
        Path dir = Files.createTempDirectory("oc-compat-save");
        AtomicInteger saves = new AtomicInteger();
        Tree tree = Tree.tree("voicechat");
        tree.addMetadata(Backend.UI_ONLY_METADATA, Boolean.TRUE);
        tree.addMetadata("custom_save", (Runnable) saves::incrementAndGet);
        tree.addMetadata(Backend.CUSTOM_SAVE_TRACKED_METADATA, Boolean.TRUE);
        tree.addMetadata(Backend.CUSTOM_SAVE_DIRTY_METADATA, Boolean.TRUE);

        FileBackend backend = backend(dir);
        backend.register(tree);
        backend.save("voicechat");

        assertEquals(1, saves.get());
        assertFalse(Files.exists(dir.resolve("voicechat")));
    }
}
