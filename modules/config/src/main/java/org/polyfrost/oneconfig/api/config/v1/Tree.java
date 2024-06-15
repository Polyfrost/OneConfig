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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The Tree class represents a tree structure that contains properties and other trees as children.
 * It provides various methods to access, modify, and compare the tree and its elements.
 */
@SuppressWarnings("unused")
public class Tree extends Node implements Serializable {
    @UnmodifiableView
    public final Map<String, Node> map;

    private final Map<String, Node> theMap;

    public Tree(@Nullable String id, @Nullable String title, @Nullable String description, @Nullable Map<String, Node> items) {
        super(id, title, description);
        if (items != null) {
            theMap = new HashMap<>(items.size());
            theMap.putAll(items);
        } else theMap = new HashMap<>();
        map = Collections.unmodifiableMap(theMap);
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

    private static void _overwrite(Tree self, Tree in, Function<String, String> keyMapper) {
        for (Map.Entry<String, Node> e : in.theMap.entrySet()) {
            String key = keyMapper == null ? e.getKey() : keyMapper.apply(e.getKey());
            Node _this = self.get(key);
            Node that = e.getValue();
            if (_this == null) {
                // nop. means that the node has been removed.
                continue;
            }
            if (_this instanceof Tree) {
                if (that instanceof Tree) {
                    // if both are trees, recursively overwrite
                    _overwrite((Tree) _this, (Tree) that, keyMapper);
                    _this.addMetadata(that.getMetadata());
                }
                // nop. do not attempt to overwrite a tree with a property
                else continue;
            }
            _this.overwrite(that);
        }
    }

    private static void _unpack(Map<String, Object> out, Tree t) {
        for (Map.Entry<String, Node> e : t.theMap.entrySet()) {
            Node n = e.getValue();
            if (n instanceof Tree) {
                Tree tt = (Tree) n;
                Map<String, Object> sub = new HashMap<>(tt.theMap.size());
                _unpack(sub, tt);
                out.put(e.getKey(), sub);
            } else {
                Property<?> p = (Property<?>) n;
                if (p.type == Void.class) continue;
                out.put(e.getKey(), p.get());
            }
        }
    }

    private static void _contentToString(StringBuilder sb, int depth, Tree t) {
        for (int i = 0; i < depth; i++) sb.append('\t');
        sb.append(t.getTitle()).append(":\n");
        for (Map.Entry<String, Node> e : t.theMap.entrySet()) {
            if (e.getValue() instanceof Property) {
                for (int i = 0; i < depth + 1; i++) sb.append('\t');
                sb.append(e.getValue()).append('\n');
            } else {
                _contentToString(sb, depth + 1, (Tree) e.getValue());
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

    public Tree put(@NotNull Node... nodes) {
        for (Node n : nodes) {
            put(n);
        }
        return this;
    }

    public Tree put(Node n) {
        theMap.put(n.getID(), n);
        return this;
    }

    public Tree set(String id, Node n) {
        if (n.getID() == null) n.setID(id);
        else if (!id.equals(n.getID())) {
            //noinspection DataFlowIssue
            n.setID(null);
            n.setID(id);
        }
        theMap.put(n.getID(), n);
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

    public Tree getChild(@NotNull String... id) {
        Node n = get(id);
        return n instanceof Tree ? (Tree) n : null;
    }

    public Tree getChild(@NotNull String id) {
        Node n = get(id);
        return n instanceof Tree ? (Tree) n : null;
    }

    public Tree getOrPutChild(@NotNull String id) {
        Tree n = getChild(id);
        if (n == null) {
            n = new Tree(id, null, null, null);
            put(n);
        }
        return n;
    }

    public Property<?> getProp(@NotNull String... id) {
        Node n = get(id);
        return n instanceof Property ? (Property<?>) n : null;
    }

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

    /**
     * Overwrite this tree with the supplied tree, using the keyMapper to map between key names from old to new if needed.
     * @param with the tree to overwrite with
     * @param keyMapper the key mapper function to use
     */
    public void overwrite(Tree with, @Nullable Function<String, String> keyMapper) {
        _overwrite(this, with, keyMapper);
    }

    @Override
    public void overwrite(@NotNull Node with) {
        if (!(with instanceof Tree)) throw new IllegalArgumentException("Cannot overwrite a tree with a non-tree node!");
        _overwrite(this, (Tree) with, null);
    }

    /**
     * Unpack this tree into a map of objects, containing either more Maps or the values of the properties of this tree.
     */
    public Map<String, Object> unpack() {
        Map<String, Object> out = new HashMap<>(theMap.size());
        _unpack(out, this);
        return out;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(72);
        sb.append("Tree(id=").append(getID());
        if (getTitle() != null) sb.append(", title=").append(getTitle());
        if (description != null) sb.append(", description=").append(description);
        sb.append(", size=").append(theMap.size()).append(')');
        return sb.toString();
    }

    public String contentToString() {
        StringBuilder sb = new StringBuilder(128);
        _contentToString(sb, 0, this);
        return sb.toString();
    }

    /**
     * Clear this tree of all its members.
     * <br>
     * This function is used internally to discard trees to prevent illegal usage of 'dead' trees.
     */
    @ApiStatus.Internal
    public void clear() {
        theMap.clear();
        clearMetadata();
    }
}



