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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;


public abstract class Node {
    protected final String id;
    private Map<String, Object> metadata = null;

    public Node(@NotNull String id) {
        this.id = id;
    }

    public final @NotNull String getID() {
        return id;
    }

    public final void addMetadata(String key, Object value) {
        if (metadata == null) metadata = new HashMap<>(8);
        metadata.put(key, value);
    }

    public final void addMetadata(Map<String, Object> metadata) {
        if (metadata == null) return;
        if (this.metadata == null) this.metadata = new HashMap<>(metadata.size());
        this.metadata.putAll(metadata);
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

    public final @Nullable Map<String, Object> getMetadata() {
        return metadata;
    }

    public final boolean hasMetadata() {
        return metadata != null;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean deepEquals(Object other);

    @Override
    public final int hashCode() {
        return id.hashCode();
    }
}