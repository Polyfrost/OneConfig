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
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Tree class represents a tree structure that contains properties and other trees as children.
 * It provides various methods to access, modify, and compare the tree and its elements.
 */
public class Tree extends Node implements Serializable {
    public static final Logger LOGGER = LoggerFactory.getLogger("OneConfig Config API");

    @UnmodifiableView
    public final Map<String, Node> map;

    private final Map<String, Node> theMap;
    private boolean locked;

    public Tree(@Nullable String id, @Nullable String name, @Nullable String description, @Nullable Map<String, Node> items) {
        super(id, name, description);
        if (items != null) {
            theMap = new HashMap<>(items.size());
            theMap.putAll(items);
        } else theMap = new HashMap<>();
        map = Collections.unmodifiableMap(theMap);
    }


    public Tree put(@NotNull Node... nodes) {
        for (Node n : nodes) {
            put(n);
        }
        return this;
    }

    public Tree put(Node n) {
        // security and sanity check.
        if (theMap.put(n.getID(), n) == null && locked) throw new IllegalStateException("Cannot add new nodes to a locked tree!");
        return this;
    }

    @Nullable
    public Node get(String id) {
        return theMap.get(id);
    }

    @Nullable
    public Node get(@NotNull String... id) {
        Tree t = this;
        Node n = null;
        for (String s : id) {
            n = t.get(s);
            if (n instanceof Tree) t = (Tree) n;
            else return n;
        }
        return n;
    }

    @Nullable
    public Tree getChild(@NotNull String... id) {
        Node n = get(id);
        return n instanceof Tree ? (Tree) n : null;
    }

    @Nullable
    public Tree getChild(@NotNull String id) {
        Node n = get(id);
        return n instanceof Tree ? (Tree) n : null;
    }

    @Nullable
    public Property<?> getProp(@NotNull String... id) {
        Node n = get(id);
        return n instanceof Property ? (Property<?>) n : null;
    }

    @Nullable
    public Property<?> getProp(@NotNull String id) {
        Node n = get(id);
        return n instanceof Property ? (Property<?>) n : null;
    }

    public void onAll(BiConsumer<String, Node> action) {
        _onAll(this, action);
    }

    public void onAllProps(BiConsumer<String, Property<?>> action) {
        _onAllProp(this, action);
    }

    private static void _onAll(Tree t, BiConsumer<String, Node> action) {
        for (Map.Entry<String, Node> e : t.theMap.entrySet()) {
            action.accept(e.getKey(), e.getValue());
        }
    }

    private static void _onAllProp(Tree t, BiConsumer<String, Property<?>> action) {
        for (Map.Entry<String, Node> e : t.theMap.entrySet()) {
            if (e.getValue() instanceof Property) action.accept(e.getKey(), (Property<?>) e.getValue());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Tree)) return false;
        Tree that = (Tree) obj;
        return this.theMap.size() == that.theMap.size() && this.getID().equals(that.getID());
    }

    /**
     * Check if the tree's contents equals another tree's. This will check the values and children of the tree.
     *
     * @param obj the tree to check against
     * @return whether the trees are equal
     */
    public boolean deepEquals(@Nullable Object obj) {
        if (!equals(obj)) return false;
        Tree that = (Tree) obj;
        for (Map.Entry<String, Node> e : this.theMap.entrySet()) {
            Node n = that.get(e.getKey());
            if (n == null) {
//                System.err.println("Second tree does not contain " + e);
                return false;
            }
            if (!e.getValue().deepEquals(n)) {
//                System.err.println(e.getValue() + " does not equal " + n);
                return false;
            }
        }
        return true;
    }


    @Override
    public void overwrite(@NotNull Node with) {
        if (!(with instanceof Tree)) throw new IllegalArgumentException("Cannot overwrite a tree with a non-tree node!");
        _overwrite(this, (Tree) with);
    }

    /**
     * Lock this tree, preventing any new data from being added to it. Nodes may still be overwritten by the {@link #put(Node)} methods.
     * <br><b>this operation is permanent!</b>
     */
    public void lock() {
        locked = true;
    }

    public boolean isLocked() {
        return locked;
    }

    private static void _overwrite(Tree self, Tree in) {
        for (Map.Entry<String, Node> e : in.theMap.entrySet()) {
            Node _this = self.get(e.getKey());
            Node that = e.getValue();
            if (_this == null) {
                // nop. means that the node has been removed.
                continue;
            }
            if (_this instanceof Tree) {
                if (that instanceof Tree) {
                    // if both are trees, recursively overwrite
                    _overwrite((Tree) _this, (Tree) that);
                    _this.addMetadata(that.getMetadata());
                }
                // nop. do not attempt to overwrite a tree with a property
                else continue;
            }
            if (_this.equals(that)) continue;
            _this.overwrite(that);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(96);
        _toString(builder, 0, this);
        return builder.toString();
    }

    private static void _toString(StringBuilder sb, int depth, Tree t) {
        for (int i = 0; i < depth; i++) sb.append('\t');
        sb.append(t.getTitle()).append(":\n");
        for (Map.Entry<String, Node> e : t.theMap.entrySet()) {
            if (e.getValue() instanceof Property) {
                for (int i = 0; i < depth + 1; i++) sb.append('\t');
                sb.append(e.getValue()).append('\n');
            } else {
                _toString(sb, depth + 1, (Tree) e.getValue());
            }
        }
    }

    public static @NotNull Tree tree() {
        return new Tree(null, null, null, null);
    }

    @Contract("_, -> new")
    public static @NotNull Tree tree(@NotNull String id) {
        return new Tree(id, null, null, null);
    }

    /**
     * Create a new builder from the given tree. This will copy all the data from the tree into the builder.
     */
    @Contract("_ -> new")
    public static @NotNull Tree tree(@NotNull Tree src) {
        return new Tree(src.getID(), src.getTitle(), src.description, src.theMap);
    }
}



