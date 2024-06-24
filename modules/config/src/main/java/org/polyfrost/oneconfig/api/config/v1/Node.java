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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.VisibleForTesting;


public abstract class Node {
    protected static final Logger LOGGER = LogManager.getLogger("OneConfig/Config");
    @Nullable
    public transient String description;
    // @jdk.internal.vm.annotation.Stable
    private transient String id;
    // @jdk.internal.vm.annotation.Stable
    private transient String title;
    @Nullable
    private transient Map<String, Object> metadata = null;

    public Node(@Nullable String id, @Nullable String title, @Nullable String description) {
        this.id = strv(id);
        this.title = strv(title);
        this.description = strv(description);
    }

    /**
     * validate and trim the given string, returning null if it is empty.
     */
    @Contract("null -> null")
    public static String strv(String s) {
        if (s == null) return null;
        String s1 = s.trim();
        if (s1.isEmpty()) return null;
        return s1;
    }

    /**
     * The ID of this node.
     */
    public String getID() {
        return id;
    }

    /**
     * Set the ID of this node. the ID is used solely by the backend to identify it.
     * It may only contain alphanumeric characters, and underscores.
     * <br>note that <b>this operation is permanent</b> and cannot be undone/changed!
     */
    public void setID(@NotNull String id) {
        //noinspection ConstantValue
        if (id == null) {
            this.id = null;
            return;
        }
        if (this.id != null) throw new IllegalStateException("ID is already set");
        String s = id.trim().replaceAll("[^\\w$\\-./\\\\]", "_");
        if (s.isEmpty()) throw new IllegalArgumentException("ID cannot be empty (or contain only whitespace)");
        this.id = s;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (this.title != null) throw new IllegalArgumentException("title is already set");
        title = strv(title);
        if (title == null) throw new IllegalArgumentException("title cannot be null or empty");
        this.title = title;
    }

    public final void addMetadata(String key, Object value) {
        if (value == null || value.equals("")) return;
        if (key.equals("title")) {
            this.title = strv(value.toString());
            return;
        }
        if (key.equals("description")) {
            this.description = strv(value.toString());
            return;
        }
        if (metadata == null) metadata = new HashMap<>(4);
        metadata.put(key, value);
    }

    public final void addMetadata(Map<String, Object> metadata) {
        if (metadata == null) return;
        if (this.metadata == null) this.metadata = new HashMap<>(metadata.size());
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            addMetadata(entry.getKey(), entry.getValue());
        }
    }

    public final boolean removeMetadata(String key) {
        if (metadata == null) return false;
        boolean res = metadata.remove(key) != null;
        if (metadata.isEmpty()) metadata = null;
        return res;
    }

    /**
     * Return some metadata attached to this node.
     *
     * @param key the key of the metadata
     * @param <M> the type of the metadata
     * @return the metadata, or null if it doesn't exist
     * @throws ClassCastException if the metadata is not of the expected type M
     */
    @SuppressWarnings("unchecked")
    public final @Nullable <M> M getMetadata(String key) {
        if (metadata == null) return null;
        return (M) metadata.get(key);
    }

    /**
     * Return some metadata attached to this node, or put def it is isn't present.
     *
     * @param key the key of the metadata
     * @param def the default value to put if there currently is no metadata on this value
     * @param <M> the type of the metadata
     * @return the metadata, or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    public final @NotNull <M> M getOrPutMetadata(String key, Supplier<M> def) {
        if (metadata == null) metadata = new HashMap<>(4);
        return (M) metadata.computeIfAbsent(key, k -> def.get());
    }

    /**
     * Consume some metadata attached to this node, meaning it will be removed.
     *
     * @param key the key of the metadata
     * @param <M> the type of the metadata
     * @return the metadata, or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    public final @Nullable <M> M consumeMetadata(String key) {
        if (metadata == null) return null;
        return (M) metadata.remove(key);
    }

    public final @UnmodifiableView @Nullable Map<String, Object> getMetadata() {
        return metadata;
    }

    protected final void clearMetadata() {
        if (metadata != null) metadata.clear();
        metadata = null;
    }

    public final boolean hasMetadata() {
        return metadata != null;
    }

    /**
     * Overwrite all data in this node with the data from another node.
     * <br><ul>
     * <li>For a node, all metadata will be copied from the input onto this. any duplicate keys will be overwritten.</li>
     * <li>For a {@link Property}, the value, callbacks and display conditions are copied onto this.</li>
     * <li>For a {@link Tree}, all children and properties will be overwritten if they are present. this operation is recursive.</li>
     * </ul>
     *
     * @param with the node to overwrite this with
     */
    public abstract void overwrite(Node with);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @VisibleForTesting
    @Contract(pure = true)
    public abstract boolean deepEquals(Object other);

    @Override
    public final int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
