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

package org.polyfrost.oneconfig.api.config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The Tree class represents a tree structure that contains properties and other trees as children.
 * It provides various methods to access, modify, and compare the tree and its elements. The properties in a tree are modifiable, but the tree itself is not.
 */
public class Tree implements Serializable {
    public static final Logger LOGGER = LoggerFactory.getLogger("OneConfig Config API");
    @NotNull
    public String id;
    @NotNull
    public final ArrayList<Property<?>> values;
    @NotNull
    public final ArrayList<Tree> children;
    private transient final Map<String, Object> metadata = new HashMap<>(4);

    public Tree(@NotNull String id, @Nullable ArrayList<Property<?>> values, @Nullable ArrayList<Tree> children) {
        this.id = id;
        this.values = values == null ? new ArrayList<>() : values;
        this.children = children == null ? new ArrayList<>(5) : children;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tree) {
            Tree tree = (Tree) obj;
            return id.equals(tree.id);
        }
        return false;
    }

    @Nullable
    public Property<?> get(@NotNull String name) {
        if (name.contains(".")) {
            String[] names = name.split("\\.");
            Tree t = this;
            int i = 0;
            while (i < names.length - 1) {
                t = t.getChild(names[i]);
                if (t == null) {
                    return null;
                }
                i++;
            }
            return t.get(names[i + 1]);
        } else {
            for (Property<?> p : values) {
                if (p.name.equals(name)) {
                    return p;
                }
            }
            return null;
        }
    }

    @Nullable
    public Tree getChild(@NotNull String id) {
        for (Tree child : children) {
            if (child.id.equals(id)) {
                return child;
            }
        }
        return null;
    }

    public void onAll(Consumer<Property<?>> action) {
        _onAll(this, action);
    }

    private static void _onAll(Tree t, Consumer<Property<?>> action) {
        for(Property<?> p : t.values) {
            action.accept(p);
        }
        for(Tree tt : t.children) {
            _onAll(tt, action);
        }
    }

    public void addMetadata(@NotNull String key, @Nullable Object value) {
        metadata.put(key, value);
    }

    public void addMetadata(@NotNull Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
    }

    public void removeMetadata(@NotNull String key) {
        metadata.remove(key);
    }

    void clearMetadata() {
        metadata.clear();
    }

    /**
     * Return some metadata attached to this tree.
     *
     * @param key the key of the metadata
     * @param M   the type of the metadata
     * @return the metadata, or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <M> M getMetadata(@NotNull String key) {
        return (M) metadata.get(key);
    }

    /**
     * Check if the tree's contents equals another tree's. This will check the name, values, and children of the tree.
     *
     * @param obj the tree to check against
     * @param log whether to log the differences
     * @return whether the trees are equal
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean contentEquals(@Nullable Object obj, boolean log) {
        if (obj instanceof Tree) {
            Tree tree = (Tree) obj;
            if (!id.equals(tree.id)) {
                if (log) LOGGER.warn("Tree {} has different name (expected {}, got {})", id, this.id, tree.id);
            }
            if (tree.values.size() != this.values.size()) {
                if (log)
                    LOGGER.error("Tree {} has different amount of values (expected {}, got {})", id, this.values.size(), tree.values.size());
                return false;
            }
            if (tree.children.size() != this.children.size()) {
                if (log)
                    LOGGER.error("Tree {} has different amount of children (expected {}, got {})", id, this.children.size(), tree.children.size());
                return false;
            }
            for (int i = 0; i < values.size(); i++) {
                Property<?> p = tree.values.get(i);

                if (p.isArray()) {
                    if (!this.values.get(i).deepEquals(p)) {
                        if (log)
                            LOGGER.error("Tree {} has different array value at index {} (expected {}, got {})", id, i, this.values.get(i).get(), p.get());
//                        return false;
                    }
                } else if (!p.get().equals(this.values.get(i).get())) {
                    if (log)
                        LOGGER.error("Tree {} has different value at index {} (expected {}, got {})", id, i, this.values.get(i).get(), p.get());
                    return false;
                }
            }
            for (int i = 0; i < children.size(); i++) {
                if (!tree.children.get(i).contentEquals(this.children.get(i), log)) {
                    if (log)
                        LOGGER.error("Tree {} has different child at index {} (expected {}, got {})", id, i, this.children.get(i).id, tree.children.get(i).id);
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    /**
     * Merge two trees into one. This method is very powerful and should be used with care.
     * <br>
     * If the input tree contains values that are not in this tree, it will be added to this tree.
     * <br>
     * If overwrite is true, all values that are in both trees will be overwritten from the provided tree into this tree.
     * <br>
     * This tree should have identical structure to the passed tree when this method is completed.
     * <br>
     * copyMeta will mean that the metadata from the input tree will be copied into this tree.
     *
     * @throws ClassCastException        if the types of the properties (with the same name) do not match
     * @throws IndexOutOfBoundsException if the trees are not somewhat similar in structure.
     */
    public void merge(Tree tree, boolean overwrite, boolean copyMeta) {
        if (tree == null) return;
        for (int i = 0; i < tree.values.size(); i++) {
            Property<?> value = tree.values.get(i);
            Property<?> p = this.get(value.name);
            if (p != null) {
                if (overwrite) p.setUnchecked(value.get());
                if (copyMeta) {
                    p.addMetadata(value.metadata);
                    p.addCallbacks(value.callbacks);
                }
            } else {
                this.values.add(i, value);
            }
        }
        for (int i = 0; i < tree.children.size(); i++) {
            Tree child = tree.children.get(i);
            Tree c = this.getChild(child.id);
            if (c != null) {
                c.merge(child, overwrite, copyMeta);
                if (copyMeta) c.addMetadata(child.metadata);
            } else {
                this.children.add(child);
            }
        }
    }


    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Tree '").append(id).append("':\n");
        for (Property<?> value : values) {
            builder.append("  ").append(value).append("\n");
        }
        for (Tree child : children) {
            builder.append("  ").append(child).append("\n");
        }
        return builder.toString();
    }

    public static class Builder {
        private final Set<Property<?>> values;
        private final Set<Tree> children;
        public String id;

        @NotNull
        @Contract("_ -> this")
        public Builder putTrees(@NotNull Collection<Tree> trees) {
            for (Tree tree : trees) {
                put(tree);
            }
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder put(@NotNull Collection<Property<?>> properties) {
            for (Property<?> p : properties) {
                put(p);
            }
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder put(@NotNull Tree tree) {
            if (!children.add(tree)) {
                throw new IllegalArgumentException("Tree with name " + tree.id + " already exists in this tree!");
            }
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder put(Tree @NotNull ... trees) {
            for (Tree tree : trees) {
                put(tree);
            }
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder put(@NotNull Builder tree) {
            put(tree.build());
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder put(Builder @NotNull ... trees) {
            for (Builder tree : trees) {
                put(tree.build());
            }
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder put(@NotNull Property<?> property) {
            if (!values.add(property)) {
                throw new IllegalArgumentException("Property with name " + property.name + " already exists in this tree!");
            }
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder put(Property<?> @NotNull ... properties) {
            for (Property<?> property : properties) {
                put(property);
            }
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder setId(@NotNull String id) {
            this.id = id;
            return this;
        }

        public Builder(String id) {
            this.id = id;
            values = new LinkedHashSet<>();
            children = new LinkedHashSet<>();
        }

        @Contract(value = " -> new", pure = true)
        public Tree build() {
            return new Tree(id, new ArrayList<>(values), new ArrayList<>(children));
        }
    }

    @Contract("_ -> new")
    public static @NotNull Builder tree(@NotNull String id) {
        return new Builder(id);
    }

    /**
     * Create a new builder from the given tree. This will copy all the data from the tree into the builder.
     */
    @Contract("_ -> new")
    public static @NotNull Builder tree(@NotNull Tree src) {
        return new Builder(src.id).putTrees(src.children).put(src.values);
    }
}



