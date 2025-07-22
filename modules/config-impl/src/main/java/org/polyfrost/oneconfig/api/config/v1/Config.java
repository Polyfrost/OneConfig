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
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.v1.annotations.Include;
import org.polyfrost.polyui.data.PolyImage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Config {
    protected Tree tree;

    @Include
    public boolean enabled = true;
    public final String id, title, iconPath;
    public final Category category;

    /**
     * @param iconPath the path to your mod's icon file, must be located within your mod-specific assets folder as to avoid conflicts.
     */
    public Config(@NotNull String id, @Nullable String iconPath, @NotNull String title, @Nullable Category category) {
        this.title = title;
        this.id = id;
        this.iconPath = iconPath;
        this.category = category == null ? Category.OTHER : category;
        addToInitQueue();
    }

    public Config(@NotNull String id, @NotNull String title, @NotNull Category category) {
        this(id, null, title, category);
    }

    public final void addAliases(String... aliases) {
        if (tree == null) initialize(false);
        tree.getOrPutMetadata("aliases", () -> new ArrayList<String>(aliases.length)).addAll(Arrays.asList(aliases));
    }

    public final void addAliases(String option, String... aliases) {
        getProperty(option).getOrPutMetadata("aliases", () -> new ArrayList<String>(aliases.length)).addAll(Arrays.asList(aliases));
    }

    @ApiStatus.Internal
    protected Tree makeTree() {
        return ConfigManager.collect(this, id);
    }

    @ApiStatus.Internal
    protected void addToInitQueue() {
        ConfigManager.submitForInitialization(this);
    }

    /**
     * Use this method to add any initialization logic to your config, for example {@link #hideIf(String, String)}, etc.
     * <br>
     * <b>make sure to call super!</b>
     */
    @MustBeInvokedByOverriders
    protected void initialize(boolean byConfigManager) {
        if (!byConfigManager) ConfigManager.removePendingInitialization(this);
        if ((tree = makeTree()) != null) {
            tree.setTitle(title);
            if (iconPath != null) {
                validateIconPath(iconPath);
                tree.addMetadata("icon", new PolyImage(iconPath));
            }

            tree.addMetadata("category", category);
            ConfigManager.backup().backend.save0(tree);
            ConfigManager.active().register(tree);
        }
    }

    protected void addDependency(String option, String name, Supplier<Property.Display> condition) {
        Property<?> opt = getProperty(option).addDisplayCondition(condition);
        if (name != null) opt.getOrPutMetadata("dependencyNames", () -> new ArrayList<String>(3)).add(name);
    }

    protected void restoreDefaults() {
        if (tree == null) initialize(false);
        tree.overwrite(ConfigManager.backup().get(tree.getID()), false);
    }

    protected void restoreProperty(String option) {
        // first operation will be slow as the tree will have to be loaded from the disc, but this is intended as to not waste memory
        // once one property is restored/restore all is used, the backup tree will be in memory and so will be fast to restore more
        getProperty(option).overwrite(getProperty(ConfigManager.backup().get(tree.getID()), option), false);
    }

    protected void addDependency(String option, String condition) {
        addDependency(option, condition, false);
    }

    protected void hideIf(String option, BooleanSupplier condition) {
        addDependency(option, null, () -> condition.getAsBoolean() ? Property.Display.HIDDEN : Property.Display.SHOWN);
    }

    protected void hideIf(String option, String condition) {
        addDependency(option, condition, true);
    }

    /**
     * Add a dependency on the given option, which will gray out or hide the option unless condition is true.
     *
     * @param option    the option to add the dependency to
     * @param condition the <b>boolean option</b> which provides the dependency
     */
    @SuppressWarnings("unchecked")
    protected void addDependency(String option, String condition, boolean hide) {
        Property<?> cond = getProperty(condition);
        if (cond.type != boolean.class) throw new IllegalArgumentException("Condition property must be boolean");
        Property<?> opt = getProperty(option).addDisplayCondition((Property<Boolean>) cond, hide);
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
    protected void addCallback(String option, Runnable callback) {
        getProperty(option).addCallback(t -> {
            callback.run();
            return false;
        });
    }

    public Tree getTree() {
        return tree;
    }

    protected void loadFrom(String id) {
        if (tree == null) initialize(false);
        Tree in = ConfigManager.active().get(id);
        if (in == null) return;
        tree.overwrite(in, false);
    }

    protected void loadFrom(Path p) {
        if (tree == null) initialize(false);
        Tree in;
        try {
            in = ConfigManager.active().getNoRegister(p);
        } catch (Exception e) {
            return;
        }
        if (in == null) return;
        tree.overwrite(in, false);
    }


    protected Property<?> getProperty(String option) {
        if (tree == null) initialize(false);
        return getProperty(tree, option);
    }

    protected static Property<?> getProperty(Tree tree, String option) {
        Property<?> p = option.indexOf('.') >= 0 ? tree.getProp(option.split("\\.")) : tree.getProp(option);
        if (p == null) throw new IllegalArgumentException("Config does not contain property: " + option);
        return p;
    }

    private static IllegalStateException notInitialized() {
        return new IllegalStateException("not initialized. this should never happen in correct usage. please report to https://polyfrost.org/discord");
    }

    public void save() {
        if (tree == null) return; // not initialized, nothing to save
        ConfigManager.active().save(tree);
    }

    /**
     * If you intend for your Config to be its own self-contained class, you may need to call this method in your mod constructor to ensure that
     * this class is initialized by Java.
     * <br>
     * If you don't call this method, your config might not appear in the UI. It will still function correctly, and after some code that loads it is called, it will appear.
     */
    public void preload() {
        initialize(false);
    }

    private static void validateIconPath(@NotNull String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (!path.startsWith("assets/")) {
            path = "assets/" + path;
        }
    }

    /**
     * A category for the config, used for sorting in the UI.
     * <br>
     * IDs start at 1, as 0 is reserved for the default category ("All"). They are also subject to change at any time.
     * </br>
     */
    public static final class Category {
        public static final Category COMBAT = new Category("oneconfig.combat", 1);
        public static final Category QOL = new Category("oneconfig.qol", 2);
        public static final Category HYPIXEL = new Category("oneconfig.hypixel", 3);
        public static final Category OTHER = new Category("oneconfig.other", 4);

        private final String name;
        private final byte id;

        private Category(String name, int id) {
            this.name = name;
            this.id = (byte) id;
        }

        public String getName() {
            return name;
        }

        public byte getId() {
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
