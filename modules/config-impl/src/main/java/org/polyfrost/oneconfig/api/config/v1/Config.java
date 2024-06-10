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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public abstract class Config {
    protected Tree tree;

    public Config(@NotNull String id, @Nullable String iconPath, @NotNull String title, @Nullable Category category) {
        // written this way so that trees can be lateinit
        if ((tree = makeTree(id)) != null) {
            tree.setTitle(title);
            tree.addMetadata("icon", iconPath);
            tree.addMetadata("category", category);
            ConfigManager.active().register(tree);
        }
    }

    public Config(@NotNull String id, @NotNull String title, @NotNull Category category) {
        this(id, null, title, category);
    }

    @ApiStatus.Internal
    protected Tree makeTree(@NotNull String id) {
        return ConfigManager.collect(this, id);
    }

    protected void addDependency(String option, BooleanSupplier condition) {
        getProperty(option).addDisplayCondition(condition);
    }

    /**
     * Add a dependency on the given option, which will gray out or hide the option unless condition is true.
     * @param option the option to add the dependency to
     * @param condition the <b>boolean option</b> which provides the dependency
     */
    protected void addDependency(String option, String condition) {
        Property<?> cond = getProperty(condition);
        if (cond.type != boolean.class && cond.type != Boolean.class) throw new IllegalArgumentException("Condition property must be boolean");
        getProperty(option).addDisplayCondition(cond::getAs);
    }

    /**
     * Add a callback to the specified option path, which is dot-separated for sub-configs.
     * <br>
     * The name of the option should be the name of the field.
     */
    @SuppressWarnings("unchecked")
    protected  <T> void addCallback(String option, Consumer<T> callback) {
        ((Property<T>) getProperty(option)).addCallback(callback);
    }

    /**
     * Add a callback to the specified option path, which is dot-separated for sub-configs.
     * <br>
     * The name of the option should be the name of the field.
     */
    protected void addCallback(String option, Runnable callback) {
        getProperty(option).addCallback(t -> callback.run());
    }

    public Tree getTree() {
        return tree;
    }


    protected Property<?> getProperty(String option) {
        if (tree == null) throw new IllegalStateException("not initialized. this should never happen in correct usage. please report to https://polyfrost.org/discord");
        Property<?> p = option.indexOf('.') >= 0 ? tree.getProp(option.split("\\.")) : tree.getProp(option);
        if (p == null) throw new IllegalArgumentException("Config does not contain property: " + option);
        return p;
    }


    public enum Category {
        COMBAT,
        QOL,
        HYPIXEL,
        OTHER
    }
}
