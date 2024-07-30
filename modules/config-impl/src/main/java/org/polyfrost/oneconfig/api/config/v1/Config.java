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
import org.polyfrost.oneconfig.api.config.v1.annotations.Include;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class Config {
    protected Tree tree;

    @Include
    public boolean enabled = true;

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

    protected void addDependency(String option, String name, BooleanSupplier condition) {
        Property<?> opt = getProperty(option).addDisplayCondition(condition);
        opt.getOrPutMetadata("dependencyNames", () -> new ArrayList<String>(3)).add(name);
    }

    /**
     * Add a dependency on the given option, which will gray out or hide the option unless condition is true.
     *
     * @param option    the option to add the dependency to
     * @param condition the <b>boolean option</b> which provides the dependency
     */
    protected void addDependency(String option, String condition) {
        Property<?> cond = getProperty(condition);
        if (cond.type != boolean.class) throw new IllegalArgumentException("Condition property must be boolean");
        Property<?> opt = getProperty(option).addDisplayCondition(cond::getAs);
        opt.getOrPutMetadata("dependencyNames", () -> new ArrayList<String>(3)).add(cond.getTitle());
    }

    /**
     * Add a callback to the specified option path, which is dot-separated for sub-configs.
     * <br>
     * The name of the option should be the name of the field.
     */
    @SuppressWarnings("unchecked")
    @kotlin.OverloadResolutionByLambdaReturnType
    protected <T> void addCallback(String option, Predicate<T> callback) {
        ((Property<T>) getProperty(option)).addCallback(callback);
    }

    /**
     * Add a callback to the specified option path, which is dot-separated for sub-configs.
     * <br>
     * The name of the option should be the name of the field.
     */
    @SuppressWarnings("unchecked")
    @kotlin.OverloadResolutionByLambdaReturnType
    protected <T> void addCallback(String option, Consumer<T> callback) {
        ((Property<T>) getProperty(option)).addCallback(callback);
    }

    /**
     * Add a callback to the specified option path, which is dot-separated for sub-configs.
     * <br>
     * The name of the option should be the name of the field.
     */
    protected void addCallback(String option, Runnable callback) {
        getProperty(option).addCallback(t -> {
            callback.run();
        });
    }

    public Tree getTree() {
        return tree;
    }

    protected void loadFrom(String id) {
        if (tree == null) throw notInitialized();
        Tree in = ConfigManager.active().get(id);
        if (in == null) return;
        tree.overwrite(in);
    }

    protected void loadFrom(Path p) {
        if (tree == null) throw notInitialized();
        Tree in;
        try {
            in = ConfigManager.active().getNoRegister(p);
        } catch (Exception e) {
            return;
        }
        if (in == null) return;
        tree.overwrite(in);
    }


    protected Property<?> getProperty(String option) {
        if (tree == null) throw notInitialized();
        Property<?> p = option.indexOf('.') >= 0 ? tree.getProp(option.split("\\.")) : tree.getProp(option);
        if (p == null) throw new IllegalArgumentException("Config does not contain property: " + option);
        return p;
    }

    private static IllegalStateException notInitialized() {
        return new IllegalStateException("not initialized. this should never happen in correct usage. please report to https://polyfrost.org/discord");
    }

    public void save() {
        ConfigManager.active().save(tree);
    }

    /**
     * If you intend for your Config to be its own self-contained class, you may need to call this method in your mod constructor to ensure that
     * this class is initialized by Java.
     * <br>
     * If you don't call this method, your config might not appear in the UI. It will still function correctly, and after some code that loads it is called, it will appear.
     *
     * @apiNote this method does literally nothing.
     */
    public void preload() {
        // <clinit>
    }

    /**
     * A category for the config, used for sorting in the UI.
     * <br>
     * IDs start at 1, as 0 is reserved for the default category ("All"). They are also subject to change at any time.
     * </br>
     */
    public static final class Category {
        public static final Category COMBAT = new Category("Combat", 1);
        public static final Category QOL = new Category("Quality of Life", 2);
        public static final Category HYPIXEL = new Category("Hypixel", 3);
        public static final Category OTHER = new Category("Other", 4);

        private final String name;
        private final int id;

        private Category(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }
}
