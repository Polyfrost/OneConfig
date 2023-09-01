/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.api.config.backend.impl.file;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.Util;
import org.polyfrost.oneconfig.api.config.backend.impl.NightConfigSerializer;

import java.awt.*;
import java.util.ArrayList;

import static org.polyfrost.oneconfig.api.config.Property.prop;
import static org.polyfrost.oneconfig.api.config.Tree.tree;

class FileBackendTest {
    static final FileBackend backend = new FileBackend("./test/");
    static String type = "json";

    static {
        backend.addSerializers(NightConfigSerializer.ALL);
    }

    @Test
    void allFormatsWork() {
        type = "json";
        putAndGetAreEqual();
        type = "yaml";
        putAndGetAreEqual();
        type = "toml";
        putAndGetAreEqual();
        type = "hocon";
        putAndGetAreEqual();
    }

    @Test
    void formatsMakeEqualTrees() {
        Tree json = newTree("test2.json");
        Tree yaml = newTree("test2.yaml");
        Tree toml = newTree("test2.toml");
        Tree hocon = newTree("test2.hocon");
        backend.put(json);
        backend.put(yaml);
        backend.put(toml);
        backend.put(hocon);
        Util.assertContentEquals(json, yaml);
        Util.assertContentEquals(json, toml);
        Util.assertContentEquals(json, hocon);
        Util.assertContentEquals(yaml, toml);
        Util.assertContentEquals(yaml, hocon);
        Util.assertContentEquals(toml, hocon);
    }

    @Test
    void putAndGetAreEqual() {
        Tree tree = newTree("test." + type);
        backend.put(tree);
        Util.assertContentEquals(tree, backend.get("test." + type));
    }

    Tree newTree(String name) {
        ArrayList<Integer> l = new ArrayList<>();
        l.add(23);
        l.add(42);
        l.add(52);
        return Tree.tree(name).put(
                prop(2),
                prop("test"),
                prop(true),
                prop("chicken", 28.3789427848923),
                prop(new Color(244348291))
        ).put(
                tree("bob").put(
                        prop("test2"),
                        prop(true),
                        prop("fish", 2000)
                ).put(
                        tree("bob2").put(
                                prop("c"),
                                prop("lc", "d"),
                                prop(l)
                        )
                )
        ).build();
    }
}