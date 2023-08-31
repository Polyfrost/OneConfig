/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config;

import org.junit.jupiter.api.Test;

import static org.polyfrost.oneconfig.api.config.Property.prop;
import static org.polyfrost.oneconfig.api.config.Tree.tree;

class TreeTest {
    @Test
    void treeFromTreeIsEqual() {
        Tree tree = tree("g").put(
                prop(2),
                prop("test"),
                prop(true),
                prop("chicken", 28.3789427848923),
                prop(new int[]{23, 42, 52})
        ).put(
                tree("bob").put(
                        prop("test2"),
                        prop(true),
                        prop("fish", 2000)
                ).put(
                        tree("bob2").put(
                                prop("c"),
                                prop("lc", "d"),
                                prop(new int[]{23, 42, 52})
                        )
                )
        ).build();
        Util.assertContentEquals(tree, tree(tree).build());
    }

    @Test
    void fromTreeOrNewWorksProperly() {
        Tree tree = tree("p").put(
                prop(2),
                prop("test"),
                prop(true),
                prop("chicken", 28.3789427848923),
                prop(new int[]{23, 42, 52})
        ).put(
                tree("bob").put(
                        prop("test2"),
                        prop(false),
                        prop("fish", 4000)
                ).put(
                        tree("bob2").put(
                                prop("GL"),
                                prop("lc", "d"),
                                prop(new int[]{23, 42, 52})
                        )
                )
        ).build();
        Tree tree2 = tree("g").put(
                prop(7),
                prop("test"),
                prop("newThing", "yes"),
                prop("newPointer", 394891),
                prop(true),
                prop("chicken", 28.3789427848923),
                prop(new int[]{23, 42, 52})
        ).put(
                tree("bob").put(
                        prop("test2"),
                        prop(true),
                        prop("fish", 2000)
                ).put(
                        tree("bob2").put(
                                prop("c"),
                                prop("lc", "d"),
                                prop("anotherNewThing", new float[]{42f, 52f, 62f}),
                                prop(new int[]{23, 42, 52})
                        )
                )
        ).build();
        Tree correct = tree("a").put(
                prop(2),        // !
                prop("test"),
                prop("newThing", "yes"),
                prop("newPointer", 394891),
                prop(true),
                prop("chicken", 28.3789427848923),
                prop(new int[]{23, 42, 52})
        ).put(
                tree("bob").put(
                        prop("test2"),
                        prop(false),        // !
                        prop("fish", 4000) // !
                ).put(
                        tree("bob2").put(
                                prop("GL"),        // !
                                prop("lc", "d"),
                                prop("anotherNewThing", new float[]{42f, 52f, 62f}),
                                prop(new int[]{23, 42, 52})
                        )
                )
        ).build();
        tree.overwriteWith(tree2, true);
        Util.assertContentEquals(correct, tree);
    }

}