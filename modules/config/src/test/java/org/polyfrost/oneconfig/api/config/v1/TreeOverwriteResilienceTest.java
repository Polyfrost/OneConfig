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

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for issue #706
 * <br>
 * A single incompatible stored value must reset only that one option to its default
 * rather than aborting the load of the whole config
 */
public class TreeOverwriteResilienceTest {

    /** Owner of a complex field so setting an incompatible value throws in {@code set0} */
    static class Bean {
        public Point pt = new Point(1, 1);
    }

    private static Tree codeTree(Bean b) throws Exception {
        Tree t = Tree.tree("t");
        // complex field-backed property that fails on an Integer overwrite just like PolyColor
        t.put(Properties.field("pt", null, Bean.class.getDeclaredField("pt"), b));
        // simple sibling that must still be loaded when the complex one fails
        t.put(Properties.simple("count", null, null, 0, Integer.class));
        return t;
    }

    private static Tree storedTree(Object ptValue, int countValue) {
        Tree t = Tree.tree("t");
        t.put(Properties.simple("pt", null, null, ptValue, Object.class));
        t.put(Properties.simple("count", null, null, countValue, Integer.class));
        return t;
    }

    @Test
    void withoutCollectionOneBadValueAbortsTheWholeOverwrite() throws Exception {
        Bean b = new Bean();
        Tree code = codeTree(b);
        // stored "pt" is a raw int incompatible with the Point field which is the pre-#706 behaviour
        Tree stored = storedTree(99, 42);

        assertThrows(RuntimeException.class, () -> code.overwrite(stored, false));

        // the bad option threw so the good sibling never got applied
        assertEquals(0, code.getProp("count").get());
        assertEquals(new Point(1, 1), b.pt);
    }

    @Test
    void withCollectionOnlyTheBadOptionResetsAndTheRestLoads() throws Exception {
        Bean b = new Bean();
        Tree code = codeTree(b);
        Tree stored = storedTree(99, 42);

        Tree.beginFailureCollection();
        assertDoesNotThrow(() -> code.overwrite(stored, false));
        List<String> failures = Tree.endFailureCollection();

        assertEquals(List.of("pt"), failures);
        assertEquals(new Point(1, 1), b.pt);
        assertEquals(42, code.getProp("count").get());
    }

    @Test
    void withCollectionAllGoodValuesLoadWithNoFailures() throws Exception {
        Bean b = new Bean();
        Tree code = codeTree(b);
        // "pt" is missing from stored so only the compatible "count" is applied
        Tree stored = Tree.tree("t");
        stored.put(Properties.simple("count", null, null, 7, Integer.class));

        Tree.beginFailureCollection();
        code.overwrite(stored, false);
        List<String> failures = Tree.endFailureCollection();

        assertTrue(failures.isEmpty());
        assertEquals(7, code.getProp("count").get());
        assertEquals(new Point(1, 1), b.pt);
    }

    @Test
    void endWithoutBeginReturnsEmptyList() {
        assertTrue(Tree.endFailureCollection().isEmpty());
    }
}
