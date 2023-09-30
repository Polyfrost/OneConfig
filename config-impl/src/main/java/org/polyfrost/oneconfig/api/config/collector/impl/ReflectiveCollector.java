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

package org.polyfrost.oneconfig.api.config.collector.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.collector.PropertyCollector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class ReflectiveCollector implements PropertyCollector {
    protected final int maxDepth;

    public ReflectiveCollector(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public ReflectiveCollector() {
        this(1);
    }


    @Override
    public @Nullable Tree collect(@Nullable String id, @NotNull Object src) {
        Tree b = Tree.tree(id == null ? src.getClass().getSimpleName() : id);
        handle(b, src, 0);
        return b;
    }


    public abstract void handleField(@NotNull Field f, @NotNull Object src, @NotNull Tree tree);

    public abstract void handleMethod(@NotNull Method m, @NotNull Object src, @NotNull Tree tree);

    public abstract void handleInnerClass(@NotNull Class<?> c, @NotNull Object src, int depth, @NotNull Tree tree);


    public final void handle(@NotNull Tree tree, @NotNull Object src, int depth) {
        for (Field f : src.getClass().getDeclaredFields()) {
            handleField(f, src, tree);
        }
        for (Method m : src.getClass().getDeclaredMethods()) {
            handleMethod(m, src, tree);
        }
        for (Class<?> c : src.getClass().getDeclaredClasses()) {
            if (depth == maxDepth) {
                Tree.LOGGER.warn("Reached max depth for tree " + tree.getID() + " ignoring further subclasses!");
                return;
            }
            handleInnerClass(c, src, depth, tree);
        }
    }
}
