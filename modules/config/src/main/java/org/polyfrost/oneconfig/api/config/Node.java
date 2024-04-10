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

package org.polyfrost.oneconfig.api.config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;


public abstract class Node {
    // @jdk.internal.vm.annotation.Stable
    private transient String id;
    // @jdk.internal.vm.annotation.Stable
    private transient String title;

    public transient String description;
    private transient Map<String, Object> metadata = null;

    public Node(@Nullable String id, @Nullable String title, @Nullable String description) {
        this.id = strv(id);
        this.title = strv(title);
        this.description = strv(description);
    }

    /**
     * The ID of a tree
     */
    public String getID() {
        return id;
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

    /**
     * Set the ID of this tree. the ID is used solely by the backend to identify it.
     * It may only contain alphanumeric characters, and underscores.
     * <br>note that <b>this operation is permanent</b> and cannot be undone/changed!
     */
    public void setID(String id) {
        if (id == null) throw new IllegalArgumentException("input ID cannot be null");
        if (this.id != null) throw new IllegalStateException("ID is already set");
        String s = id.trim().replaceAll("\\W", "_");
        if (s.isEmpty()) throw new IllegalArgumentException("ID cannot be empty (or contain only whitespace)");
        this.id = s;
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
     * Return some metadata attached to this tree.
     *
     * @param key the key of the metadata
     * @param <M> the type of the metadata
     * @return the metadata, or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    public final @Nullable <M> M getMetadata(String key) {
        if (metadata == null) return null;
        return (M) metadata.get(key);
    }

    public final @UnmodifiableView @Nullable Map<String, Object> getMetadata() {
        return metadata;
    }

    public final boolean hasMetadata() {
        return metadata != null;
    }

    public abstract void overwrite(Node with);


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @VisibleForTesting
    @Contract(pure = true)
    public abstract boolean deepEquals(Object other);

    @Override
    public final int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
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
}
