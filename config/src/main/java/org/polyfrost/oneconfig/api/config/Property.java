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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class Property<T> extends Node implements Serializable {
    /**
     * This is the actual value of the property, and is what is stored.
     */
    @Nullable
    private T value;

    /**
     * This is the type of the property, and is used for casting.
     */
    public transient final Class<T> type;
    /**
     * If this is true, the value is not real and should not be written.
     */
    public transient final boolean synthetic;
    private transient List<@NotNull Consumer<@Nullable T>> callbacks = null;
    private transient boolean display = true;
    private transient List<BooleanSupplier> conditions = null;

    public Property(@NotNull String id, @Nullable T value, @NotNull Class<T> type, boolean synthetic) {
        super(synthetic ? id + "$synthetic" : id);
        this.value = value;
        this.type = type;
        this.synthetic = synthetic;
    }

    public Property(@NotNull String id, @Nullable T value, @NotNull Class<T> type) {
        this(id, value, type, false);
    }

    @SuppressWarnings("unchecked")
    public Property(@NotNull String id, @NotNull T value) {
        this(id, value, (Class<T>) value.getClass(), false);
    }

    public Property(@NotNull T value) {
        this(value.getClass().getSimpleName(), value);
    }

    /**
     * This is used by the frontend to know if this property is able to be displayed, which is controlled by {@link #conditions}.
     *
     * @see #addDisplayCondition(BooleanSupplier)
     */
    public boolean canDisplay() {
        return display;
    }


    /**
     * Add a display condition to this property.
     */
    public void addDisplayCondition(@NotNull BooleanSupplier condition) {
        if (conditions == null) conditions = new ArrayList<>(5);
        conditions.add(condition);
        evaluateDisplay();
    }

    public final void addDisplayCondition(@NotNull BooleanSupplier... conditions) {
        if (this.conditions == null) this.conditions = Arrays.asList(conditions);
        else {
            this.conditions.addAll(Arrays.asList(conditions));
        }
        evaluateDisplay();
    }

    private void evaluateDisplay() {
        display = true;
        if (conditions == null) return;
        for (BooleanSupplier s : conditions) {
            if (!s.getAsBoolean()) {
                display = false;
                break;
            }
        }
    }

    /**
     * Remove a display condition from this property.
     */
    public void removeDisplayCondition(@NotNull BooleanSupplier condition) {
        if (conditions == null) return;
        conditions.remove(condition);
        evaluateDisplay();
    }

    /**
     * Remove all display conditions from this property.
     */
    void clearDisplayConditions() {
        conditions = null;
        display = true;
    }

    /**
     * Add a callback to this property, which is called when the value changes.
     *
     * @param callback the callback to add. The new value is passed to the callback.
     * @see #removeCallback(Consumer)
     */
    public Property<T> addCallback(@NotNull Consumer<@Nullable T> callback) {
        if (callbacks == null) callbacks = new ArrayList<>();
        callbacks.add(callback);
        return this;
    }

    @SafeVarargs
    public final Property<T> addCallback(@NotNull Consumer<@Nullable T>... callbacks) {
        if (this.callbacks == null) this.callbacks = Arrays.asList(callbacks);
        else {
            this.callbacks.addAll(Arrays.asList(callbacks));
        }
        return this;
    }

    /**
     * Remove a callback.
     */
    public void removeCallback(@NotNull Consumer<@Nullable T> callback) {
        if (callbacks == null) return;
        callbacks.remove(callback);
    }

    /**
     * Remove all callbacks.
     */
    void clearCallbacks() {
        callbacks = null;
    }

    /**
     * Set the value of this property. This will call all callbacks.
     * <br>
     * The value (and callbacks) are only set/called if the value is different from the previous value (using {@link Object#equals(Object)}).
     */
    public void set(@Nullable T value) {
        if (value != null && value.equals(this.value)) return;
        if (callbacks != null) {
            try {
                for(Consumer<T> c : callbacks) {
                    c.accept(value);
                }
            } catch (Exception e) {
                Tree.LOGGER.error("Error while calling callbacks for property " + id, e);
            }
        }
        this.value = value;
    }


    @Override
    public String toString() {
        return id + ": " + value;
    }

    /**
     * Get the value of this property.
     */
    @Nullable
    public T get() {
        return value;
    }

    /**
     * Get the value of this property, cast to the specified type.
     * This method is unsafe, and will throw a {@link ClassCastException} if the value is not of the correct type.
     */
    @SuppressWarnings("unchecked")
    public <V> V getAs() {
        return (V) value;
    }

    /**
     * Set the value of this property. This will call all callbacks.
     * This method is unsafe, and will throw a {@link ClassCastException} if the value is not of the correct type.
     */
    @SuppressWarnings("unchecked")
    public <V> void setAs(V value) {
        set((T) value);
    }

    /**
     * Deep equals for a property, meaning it will check {@link #equals(Object)} and the value of this property with the given obj.
     * <br>
     * In pretty much every case, you should use {@link #equals(Object)} instead. This is used for testing. Note that primitive arrays are not checked.
     */
    public boolean deepEquals(Object obj) {
        if (!equals(obj)) return false;
        if (!this.isArray() || this.isPrimitiveArray()) return false;
        Property<?> that = (Property<?>) obj;
        return Arrays.equals((Object[]) this.value, (Object[]) that.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Property)) return false;
        Property<?> that = (Property<?>) obj;
        if (this.type != that.type) return false;
        return Objects.equals(value, that.value) && this.id.equals(that.id);
    }

    /**
     * @return true if the value is a primitive array.
     */
    public boolean isPrimitiveArray() {
        return type.isArray() && type.getComponentType().isPrimitive();
    }

    /**
     * @return true if the value is an array.
     */
    public boolean isArray() {
        return type.isArray();
    }

    public static <T> Property<T> prop(@NotNull T value) {
        return new Property<>(value);
    }

    @SuppressWarnings("ConstantConditions")
    public static <T> Property<T> prop(@NotNull String name, @NotNull T value) {
        if (value == null) throw new IllegalArgumentException("Cannot create a property with a null value and no class");
        return new Property<>(name, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Property<T> prop(@NotNull String name, @Nullable T value, @NotNull Class<?> type) {
        return new Property<>(name, value, (Class<T>) type);
    }


}
