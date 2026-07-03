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
 * Tests for the resilient-overwrite path that backs the fix for issue #706. A single incompatible
 * stored value (e.g. a raw int where a complex type is expected) must reset only that one option to
 * its default rather than aborting the load of the whole config.
 */
public class TreeOverwriteResilienceTest {

    /** Owner of a complex (non-simple) field, so setting an incompatible value throws in {@code set0}. */
    static class Bean {
        public Point pt = new Point(1, 1);
    }

    private static Tree codeTree(Bean b) throws Exception {
        Tree t = Tree.tree("t");
        // a complex field-backed property: overwriting it with an Integer fails just like PolyColor does.
        t.put(Properties.field("pt", null, Bean.class.getDeclaredField("pt"), b));
        // a normal simple property that must still be loaded when a sibling fails.
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
        // stored "pt" is a raw int, incompatible with the Point field -> the pre-#706-fix behaviour.
        Tree stored = storedTree(99, 42);

        assertThrows(RuntimeException.class, () -> code.overwrite(stored, false));

        // proof of the bug: because the bad option threw, the good sibling never got applied.
        assertEquals(0, code.getProp("count").get());
        assertEquals(new Point(1, 1), b.pt);
    }

    @Test
    void withCollectionOnlyTheBadOptionResetsAndTheRestLoads() throws Exception {
        Bean b = new Bean();
        Tree code = codeTree(b);
        Tree stored = storedTree(99, 42);

        Tree.beginFailureCollection();
        // must not throw, unlike the case above.
        assertDoesNotThrow(() -> code.overwrite(stored, false));
        List<String> failures = Tree.endFailureCollection();

        // only the incompatible option was recorded...
        assertEquals(List.of("pt"), failures);
        // ...it kept its default (was never mutated)...
        assertEquals(new Point(1, 1), b.pt);
        // ...and the good sibling was loaded normally.
        assertEquals(42, code.getProp("count").get());
    }

    @Test
    void withCollectionAllGoodValuesLoadWithNoFailures() throws Exception {
        Bean b = new Bean();
        Tree code = codeTree(b);
        // "pt" is missing from stored, so only the compatible "count" is applied.
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
