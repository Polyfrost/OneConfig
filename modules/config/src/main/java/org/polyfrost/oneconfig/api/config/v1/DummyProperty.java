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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * A dummy property for properties that do not have a serializable representation, for
 * example a method for a button - which needs to be part of the tree, but does not need to be serialized.
 * <br>
 * Metadata and display conditions are supported, but callbacks are not.
 * <br>
 * Serializers should check and ignore any properties that are instances of this class, or check if the type is Void.class.
 * <br>
 * So that other parts of code do not need to have special checks for these, the methods <b>are NOT crashing</b>, and instead just return null or are noop.
 */
public class DummyProperty extends Property<Void> {
    public DummyProperty(@Nullable String id, @Nullable String name, @Nullable String description) {
        super(id, name, description, null, Void.class);
    }

    public static DummyProperty dummy(String id, String name, String description) {
        return new DummyProperty(id, name, description);
    }

    public static DummyProperty dummy(String id, String name) {
        return new DummyProperty(id, name, null);
    }

    public static DummyProperty dummy(String id) {
        return new DummyProperty(id, null, null);
    }

    @Override
    @Deprecated
    public @Nullable Void get() {
        return null;
    }

    @Override
    @Deprecated
    public void set(@Nullable Void value) {
        // good luck getting a void instance to set this with
    }

    @Override
    @Deprecated
    public <V> V getAs() {
        return null;
    }

    @Override
    @Deprecated
    public <V> void setAs(V value) {
        // nop so that overwrite works
    }

    @Override
    public boolean isPrimitiveArray() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    @Deprecated
    public void removeCallback(@NotNull Consumer<@Nullable Void> callback) {
    }

    @Override
    @Deprecated
    public Property<Void> addCallback(@NotNull Consumer<@Nullable Void> callback) {
        return this;
    }

    @Override
    @Deprecated
    public Property<Void> addCallback(@NotNull Collection<Consumer<@Nullable Void>> callbacks) {
        return this;
    }
}
