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
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer;
import org.polyfrost.oneconfig.utils.v1.WrappingUtils;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Config {
    protected Tree tree;
    /** code-defined defaults captured at first initialization keyed by dot-separated property path */
    private Map<String, Object> defaultSnapshot;

    public final String id, title, iconPath;
    public final Category category;

    /**
     * @param iconPath the path to your mod icon file which must be located within your mod-specific assets folder to avoid conflicts
     */
    public Config(@NotNull String id, @Nullable String iconPath, @NotNull String title, @Nullable Category category) {
        this.title = title;
        this.id = id;
        this.iconPath = validateIconPath(iconPath);
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
     * Use this method to add any initialization logic to your config for example {@link #hideIf(String, String)}
     * <br>
     * <b>make sure to call super</b>
     */
    @MustBeInvokedByOverriders
    protected void initialize(boolean byConfigManager) {
        if (!byConfigManager) ConfigManager.removePendingInitialization(this);
        if (tree != null) {
            ConfigManager.LOGGER.warn("Config {} is already initialized, skipping initialization", id);
            return;
        }
        if ((tree = makeTree()) != null) {
            tree.setTitle(title);
            if (iconPath != null) {
                tree.addMetadata("icon", iconPath);
                tree.addMetadata("icon_path", iconPath);
            }

            tree.addMetadata("category", category);
            if (!ConfigManager.isRebindingProfiles()) {
                ConfigManager.backup().backend.save0(tree);
            }
            // capture code defaults before register() loads stored values over them so the UI can offer a reset action
            if (defaultSnapshot == null) {
                defaultSnapshot = new HashMap<>();
                captureDefaults(tree, "", defaultSnapshot);
            } else {
                applyDefaultSnapshot(tree, "", defaultSnapshot);
            }
            Tree.beginFailureCollection();
            List<String> resetOptions;
            try {
                tree = ConfigManager.active().register(tree).get();
            } finally {
                resetOptions = Tree.endFailureCollection();
            }
            ConfigManager.markInitialized(this);
            if (!resetOptions.isEmpty()) {
                ConfigManager.reportResetOptions(this, resetOptions);
            }
        }
    }

    /**
     * Recursively record the current value of every property in [tree] as transient {@code "default"} metadata
     * <br>
     * Call before {@link ConfigManager#register(Tree)} so stored profile values do not overwrite the captured code defaults
     * <br>
     * For complex (non-simple) types a deep copy is stored because {@link Property.Field#set0} mutates such values in place
     * <br>
     * Storing the live reference would alias the working value and make a reset a no-op
     */
    @ApiStatus.Internal
    public static void captureDefaults(Tree tree) {
        captureDefaults(tree, null, null);
    }

    @ApiStatus.Internal
    public static void restoreCapturedDefaults(Tree tree) {
        restoreCapturedDefaults(tree, false);
    }

    @ApiStatus.Internal
    public static void restoreCapturedDefaults(Tree tree, boolean silent) {
        for (Node node : tree.map.values()) {
            if (node instanceof Property) {
                Property<?> property = (Property<?>) node;
                Object value = property.getMetadata("default");
                if (value == null) continue;
                Object restored = copyDefault(property.type, value);
                if (silent) property.setAsSilently(restored);
                else property.setAsReferential(restored);
            } else if (node instanceof Tree) {
                restoreCapturedDefaults((Tree) node, silent);
            }
        }
    }

    private static void captureDefaults(Tree tree, String prefix, Map<String, Object> out) {
        for (Map.Entry<String, Node> entry : tree.map.entrySet()) {
            Node node = entry.getValue();
            if (node instanceof Property) {
                Property<?> p = (Property<?>) node;
                Object value = p.get();
                if (value == null) continue;
                Object def = p.getOrPutMetadata("default", () -> copyDefault(p.type, value));
                if (out != null) out.put(prefix + entry.getKey(), def);
            } else if (node instanceof Tree) {
                captureDefaults((Tree) node, out == null ? null : prefix + entry.getKey() + ".", out);
            }
        }
    }

    private static void applyDefaultSnapshot(Tree tree, String prefix, Map<String, Object> snapshot) {
        for (Map.Entry<String, Node> entry : tree.map.entrySet()) {
            Node node = entry.getValue();
            if (node instanceof Property) {
                Property<?> p = (Property<?>) node;
                Object def = snapshot.get(prefix + entry.getKey());
                if (def == null) continue;
                p.addMetadata("default", copyDefault(p.type, def));
            } else if (node instanceof Tree) {
                applyDefaultSnapshot((Tree) node, prefix + entry.getKey() + ".", snapshot);
            }
        }
    }

    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public static Object copyDefault(Class<?> type, Object value) {
        if (WrappingUtils.isSimpleClass(type)) return value;
        Object container = copyContainer(value);
        if (container != null) return container;
        try {
            Object serialized = ObjectSerializer.INSTANCE.serialize(value, false, false);
            if (serialized instanceof Map) {
                Object copy = ObjectSerializer.INSTANCE.deserialize((Map<String, Object>) serialized);
                if (copy != null) return copy;
            }
        } catch (Throwable t) {
            ConfigManager.LOGGER.warn("failed to deep-copy default for type {}, falling back to reference", type, t);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object copyContainer(@Nullable Object value) {
        if (value == null) return null;
        Class<?> cls = value.getClass();
        try {
            if (cls.isArray()) {
                Class<?> component = cls.getComponentType();
                Object copy = shallowCopy(value, cls);
                if (copy == null || component.isPrimitive()) return copy;
                for (int i = 0, length = Array.getLength(copy); i < length; i++) {
                    Object entry = Array.get(copy, i);
                    Object copied = copyEntry(entry);
                    if (copied == entry) continue;
                    try {
                        Array.set(copy, i, copied);
                    } catch (IllegalArgumentException mismatch) {
                        ConfigManager.LOGGER.warn("failed to copy default array entry of type {}", component, mismatch);
                    }
                }
                return copy;
            }
            if (value instanceof Collection) {
                Collection<Object> in = (Collection<Object>) value;
                Collection<Object> out = value instanceof Set ? new LinkedHashSet<>() : new ArrayList<>(in.size());
                for (Object entry : in) out.add(copyEntry(entry));
                return out;
            }
            if (value instanceof Map) {
                LinkedHashMap<Object, Object> out = new LinkedHashMap<>();
                for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                    out.put(entry.getKey(), copyEntry(entry.getValue()));
                }
                return out;
            }
        } catch (Throwable t) {
            ConfigManager.LOGGER.warn("failed to copy default container of type {}", cls, t);
            return shallowCopy(value, cls);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object shallowCopy(Object value, Class<?> cls) {
        try {
            if (cls.isArray()) {
                int length = Array.getLength(value);
                Object copy = Array.newInstance(cls.getComponentType(), length);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(value, 0, copy, 0, length);
                return copy;
            }
            if (value instanceof Set) return new LinkedHashSet<>((Set<Object>) value);
            if (value instanceof Collection) return new ArrayList<>((Collection<Object>) value);
            if (value instanceof Map) return new LinkedHashMap<>((Map<Object, Object>) value);
        } catch (Throwable t) {
            ConfigManager.LOGGER.warn("failed to copy default container of type {}", cls, t);
        }
        return null;
    }

    private static @Nullable Object copyEntry(@Nullable Object entry) {
        if (entry == null) return null;
        try {
            return copyDefault(entry.getClass(), entry);
        } catch (Throwable t) {
            ConfigManager.LOGGER.warn("failed to copy default entry of type {}", entry.getClass(), t);
            return entry;
        }
    }

    @ApiStatus.Internal
    void rebindToActiveProfile(boolean restoreDefaults) {
        if (restoreDefaults) restoreDefaults();
        tree = null;
        initialize(true);
    }

    protected void addDependency(String option, String name, Supplier<Property.Display> condition) {
        Property<?> opt = getProperty(option).addDisplayCondition(condition);
        if (name != null) opt.getOrPutMetadata("dependencyNames", () -> new ArrayList<String>(3)).add(name);
        // unlike the boolean-option variant the supplier has no specific parent to subscribe to so re-evaluate
        // whenever any sibling property changes
        java.lang.ref.WeakReference<Property<?>> ref = new java.lang.ref.WeakReference<>(opt);
        tree.onAllProps((s, p) -> {
            if (p == opt) return;
            p.addCallback(t -> {
                Property<?> self = ref.get();
                if (self != null) self.revaluateDisplay();
                return false;
            });
        });
    }

    protected void restoreDefaults() {
        if (tree == null) initialize(false);
        tree.overwrite(ConfigManager.backup().get(tree.getID()), false);
    }

    protected void restoreProperty(String option) {
        // first restore is slow as the backup tree loads from disc but it then stays in memory
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
     * Add a dependency on the given option which will gray out or hide the option unless condition is true
     *
     * @param option    the option to add the dependency to
     * @param condition the <b>boolean option</b> which provides the dependency
     */
    @SuppressWarnings("unchecked")
    protected void addDependency(String option, String condition, boolean hide) {
        Property<?> cond = getProperty(condition);
        if (cond.type != boolean.class) throw new IllegalArgumentException("Condition property must be boolean");
        Property<?> opt = getProperty(option).addDisplayCondition((Property<Boolean>) cond, hide);
        Object title = cond.getTitle();
        opt.getOrPutMetadata("dependencyNames", () -> new ArrayList<String>(3)).add(title != null ? title.toString() : condition);
    }

    /**
     * Add a callback to the specified option path which is dot-separated for sub-configs
     * <br>
     * The name of the option should be the name of the field
     */
    @SuppressWarnings("unchecked")
    @kotlin.OverloadResolutionByLambdaReturnType
    protected <T> void addCallback(String option, Predicate<T> callback) {
        ((Property<T>) getProperty(option)).addCallback(callback);
    }

    /**
     * Add a callback to the specified option path which is dot-separated for sub-configs
     * <br>
     * The name of the option should be the name of the field
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

    /**
     * Add a migration entry to the config
     * <br>
     * This should be in the format of oldName -> newName
     * <br>To be used in conjunction with {@link #loadFrom(String)} or {@link #loadFrom(Path)} to migrate old configs to new ones
     */
    protected void addMigrationEntry(String oldName, String newName) {
        if (tree == null) initialize(false);
        tree.getOrPutMetadata("migrationMap", () -> new HashMap<String, String>()).put(oldName, newName);
    }

    /**
     * Add multiple migration entries to the config
     * <br>
     * This should be in the format of pairs where the first element is the old name and the second element is the new name
     * <br>To be used in conjunction with {@link #loadFrom(String)} or {@link #loadFrom(Path)} to migrate old configs to new ones
     */
    protected void addMigrationEntries(String... entries) {
        if (tree == null) initialize(false);
        HashMap<String, String> map = tree.getOrPutMetadata("migrationMap", () -> new HashMap<>(entries.length / 2));
        for (int i = 0; i < entries.length; i += 2) {
            map.put(entries[i], entries[i + 1]);
        }
    }

    protected void loadFrom(String id) {
        if (tree == null) initialize(false);
        Tree in = ConfigManager.active().get(id);
        if (in == null) return;
        tree.overwrite(in, false, true, tree);
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
        tree.overwrite(in, false, true, tree);
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

    public void save() {
        if (tree == null) return;
        ConfigManager.active().save(tree);
    }

    /**
     * If you intend for your Config to be its own self-contained class you may need to call this method in your mod constructor
     * to ensure that this class is initialized by Java
     * <br>
     * If you do not call this method your config might not appear in the UI
     * <br>
     * It will still function correctly and it will appear once some code that loads it is called
     */
    public void preload() {
        initialize(false);
    }

    private static String validateIconPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (!path.startsWith("assets/")) {
            throw new IllegalArgumentException("icons must be located inside a valid assets directory under your mod id");
        }
        return path;
    }

    /**
     * A category for the config used for sorting in the UI
     * <br>
     * IDs start at 1 because 0 is reserved for the default category ("All")
     * <br>
     * They are also subject to change at any time
     * </br>
     */
    public static final class Category {
        public static final Category COMBAT = new Category("oneconfig.combat", 1);
        public static final Category QOL = new Category("oneconfig.qol", 2);
        public static final Category HYPIXEL = new Category("oneconfig.hypixel", 3);
        public static final Category OTHER = new Category("oneconfig.other", 4);
        public static final Category PERFORMANCE = new Category("oneconfig.performance", 5);
        public static final Category VISUALS = new Category("oneconfig.visuals", 6);
        public static final Category HUD = new Category("oneconfig.hud", 7);
        public static final Category UTILITY = new Category("oneconfig.utility", 8);

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
