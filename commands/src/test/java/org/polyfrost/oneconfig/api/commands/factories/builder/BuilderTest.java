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

package org.polyfrost.oneconfig.api.commands.factories.builder;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.commands.CommandTree;
import org.polyfrost.oneconfig.api.commands.arguments.ArgumentParser;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.polyfrost.oneconfig.api.commands.factories.builder.BuilderUtils.*;
import static org.polyfrost.oneconfig.api.commands.factories.builder.CommandBuilder.Arg.*;
import static org.polyfrost.oneconfig.api.commands.factories.builder.CommandBuilder.command;
import static org.polyfrost.oneconfig.api.commands.factories.builder.CommandBuilder.runs;

public class BuilderTest {
    @Test
    void test() {
        CommandBuilder b = command(Arrays.asList(ArgumentParser.defaultParsers), "test");
        b.then(
                        runs("chicken").with(intArg(), intArg())
                                .does(args -> {
                                    int res = getInt(args[0]) + getInt(args[1]);
                                    System.out.println(res);
                                    return res;
                                })
                ).then(
                        runs("bob").with(shortArg())
                                .does(args -> {
                                    System.out.println(getShort(args[0]));
                                })
                ).subcommand("something")
                .then(
                        runs("a").with(stringArg())
                                .does(args -> {
                                    System.out.println(getString(args[0]));
                                })
                ).then(
                        runs("a").with(stringArg(), stringArg())
                                .does(args -> {
                                    System.out.println(getString(args[0]) + getString(args[1]));
                                    return 0;
                                })
                ).then(
                        runs("gar").with(booleanArg(), booleanArg(), booleanArg())
                                .does(args -> args)
                );
        CommandTree t = b.tree;

        assertEquals(3, t.execute("chicken", "1", "2"));
        assertNull(t.execute("bob", "42"));
        assertNull(t.execute("something", "a", "HEY"));
        assertEquals("true", t.autocomplete("something", "gar", "tr").get(0));

        assertEquals(0, t.execute("something", "a", "HEY", "THERE"));

    }
}
