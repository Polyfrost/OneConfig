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
import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.backend.impl.FileBackend;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.FileSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.NightConfigSerializer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for issue #706 where a bare int such as {@code -65436} stored where a
 * {@link PolyColor} is expected used to crash the whole config load
 */
public class CorruptOptionLoadTest {

    static class Holder {
        public PolyColor col = PolyColor.Companion.rgba(255, 0, 100, 255);
        public boolean flag = false;
    }

    @SuppressWarnings("unchecked")
    private static FileBackend backend(Path dir) {
        return new FileBackend(dir, (FileSerializer<String>[]) NightConfigSerializer.ALL);
    }

    private static Tree codeTree(Holder h) throws Exception {
        Tree t = Tree.tree("706test.json");
        t.put(Properties.field("col", null, Holder.class.getDeclaredField("col"), h));
        t.put(Properties.field("flag", null, Holder.class.getDeclaredField("flag"), h));
        return t;
    }

    @Test
    void barePreFixLoadCrashes() throws Exception {
        Path dir = Files.createTempDirectory("oc706-crash");
        Files.write(dir.resolve("706test.json"),
                "{ \"col\": -65436, \"flag\": true }".getBytes(StandardCharsets.UTF_8));

        Holder h = new Holder();
        Tree code = codeTree(h);
        // with no failure collection active the incompatible value propagates and aborts the load
        assertThrows(RuntimeException.class, () -> backend(dir).register(code));
    }

    @Test
    void withCollectionOnlyTheColorResets() throws Exception {
        Path dir = Files.createTempDirectory("oc706-fix");
        Files.write(dir.resolve("706test.json"),
                "{ \"col\": -65436, \"flag\": true }".getBytes(StandardCharsets.UTF_8));

        Holder h = new Holder();
        PolyColor defaultColor = h.col;
        Tree code = codeTree(h);

        Tree.beginFailureCollection();
        assertDoesNotThrow(() -> backend(dir).register(code));
        List<String> failures = Tree.endFailureCollection();

        assertEquals(List.of("col"), failures);
        assertSame(defaultColor, h.col, "color must be left untouched at its default");
        assertEquals(0xFFFF0064, h.col.getRawArgb());
        assertTrue(h.flag, "the valid sibling option must still load from disk");
    }
}
