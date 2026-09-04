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

package org.polyfrost.oneconfig.api.config.v1.collect.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.collect.PropertyCollector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class ReflectiveCollector implements PropertyCollector {
    protected static final Logger LOGGER = LogManager.getLogger("OneConfig/Config");
    protected final int maxDepth;

    public ReflectiveCollector(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public @Nullable Tree collect(@NotNull Object src) {
        Tree b = Tree.tree();
        handle(b, src, 0);
        return b;
    }


    protected abstract void handleField(@NotNull Field f, @NotNull Object src, @NotNull Tree tree);

    protected abstract void handleMethod(@NotNull Method m, @NotNull Object src, @NotNull Tree tree);

    protected abstract void handleInnerClass(@NotNull Class<?> c, @NotNull Object src, int depth, @NotNull Tree tree);


    public void handle(@NotNull Tree tree, @NotNull Object src, int depth) {
        // tree first: the object's message names the tree, so it cannot run while the tree is null
        if (tree == null) {
            LOGGER.error("Failed to collect properties from object {} as the tree was null", src);
            return;
        }
        if (src == null) {
            LOGGER.error("Failed to collect properties for {} as the object was null", tree.getID());
            return;
        }
        Class<?> cls = src.getClass();
        for (Field f : cls.getDeclaredFields()) {
            handleField(f, src, tree);
        }
        for (Method m : cls.getDeclaredMethods()) {
            handleMethod(m, src, tree);
        }
        // stops at Object, which declares no config properties: walking it reflected over its
        // eleven methods per config per nesting level. ObjectSerializer.getAllFields stops there too
        Class<?> superClass = cls.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            for (Field f : superClass.getDeclaredFields()) {
                handleField(f, src, tree);
            }
            for (Method m : superClass.getDeclaredMethods()) {
                handleMethod(m, src, tree);
            }
            superClass = superClass.getSuperclass();
        }
        for (Class<?> sub : cls.getDeclaredClasses()) {
            if (depth >= maxDepth) {
                LOGGER.warn("Reached max depth for tree {} ignoring further subclasses!", tree.getID());
                return;
            }
            handleInnerClass(sub, src, depth + 1, tree);
        }
    }
}
