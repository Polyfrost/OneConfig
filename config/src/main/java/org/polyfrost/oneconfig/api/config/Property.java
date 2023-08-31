/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Property<T> implements Serializable {
    /**
     * This is the actual value of the property, and is what is stored.
     */
    @NotNull
    private T value;

    public final Class<T> type;


    /**
     * This is the name of the property, and is what is displayed.
     */
    @NotNull
    public final String name;

    private transient final ArrayList<@NotNull Consumer<@NotNull T>> callbacks = new ArrayList<>(3);
    private transient boolean display = true;
    private transient final ArrayList<Supplier<Boolean>> conditions = new ArrayList<>(2);
    private transient final Map<String, Object> metadata = new HashMap<>(10);

    @SuppressWarnings("unchecked")
    public Property(@NotNull String name, @NotNull T value) {
        this.value = value;
        this.name = name;
        this.type = (Class<T>) value.getClass();
    }

    public Property(@NotNull T value) {
        this(value.getClass().getSimpleName(), value);
    }

    /**
     * This is used by the frontend to know if this property is able to be displayed, which is controlled by {@link #conditions}.
     *
     * @see #addDisplayCondition(Supplier)
     */
    public boolean canDisplay() {
        return display;
    }

    /**
     * Add multiple display conditions to this property.
     */
    public void addDisplayConditions(@NotNull Collection<Supplier<Boolean>> conditions) {
        this.conditions.addAll(conditions);
        display = conditions.stream().allMatch(Supplier::get);
        this.conditions.trimToSize();
    }

    public void addMetadata(@NotNull String key, @Nullable Object value) {
        metadata.put(key, value);
    }

    public void removeMetadata(@NotNull String key) {
        metadata.remove(key);
    }

    void clearMetadata() {
        metadata.clear();
    }

    /**
     * Add a display condition to this property.
     */
    public void addDisplayCondition(@NotNull Supplier<Boolean> condition) {
        conditions.add(condition);
        display = conditions.stream().allMatch(Supplier::get);
    }

    /**
     * Remove a display condition from this property.
     */
    public void removeDisplayCondition(@NotNull Supplier<Boolean> condition) {
        conditions.remove(condition);
        display = conditions.stream().allMatch(Supplier::get);
    }

    /**
     * Remove all display conditions from this property.
     */
    void clearDisplayConditions() {
        conditions.clear();
        conditions.trimToSize();
        display = true;
    }

    /**
     * Add a callback to this property, which is called when the value changes.
     *
     * @param callback the callback to add. The new value is passed to the callback.
     * @see #addCallbacks(Collection)
     * @see #removeCallback(Consumer)
     */
    public Property<T> addCallback(@NotNull Consumer<@NotNull T> callback) {
        callbacks.add(callback);
        return this;
    }

    /**
     * Add multiple callbacks to this property.
     *
     * @see #addCallback(Consumer)
     */
    public void addCallbacks(@NotNull Collection<@NotNull Consumer<@NotNull T>> callbacks) {
        this.callbacks.addAll(callbacks);
    }

    /**
     * Remove a callback.
     */
    public void removeCallback(@NotNull Consumer<@NotNull T> callback) {
        callbacks.remove(callback);
    }

    /**
     * Remove all callbacks.
     */
    void clearCallbacks() {
        callbacks.clear();
        callbacks.trimToSize();
    }

    /**
     * Set the value of this property. This will call all callbacks.
     * <br>
     * The value (and callbacks) are only set/called if the value is different from the previous value (using {@link Object#equals(Object)}).
     */
    public void set(@NotNull T value) {
        if (value == this.value) return;
        try {
            callbacks.forEach(c -> c.accept(value));
        } catch (Exception e) {
            Tree.LOGGER.error("Error while calling callbacks for property " + name, e);
        }
        this.value = value;
    }

    /**
     * Set the value of this property. This will call all callbacks.
     * This method is unsafe, and will throw a {@link ClassCastException} if the value is not of the specified type.
     */
    @SuppressWarnings("unchecked")
    public void setUnchecked(@NotNull Object value) {
        set((T) value);
    }

    @Override
    public String toString() {
        return "Property: " + name + "=" + value;
    }

    /**
     * Get the value of this property.
     */
    @NotNull
    public T get() {
        return value;
    }

    /**
     * Get the value of this property, cast to the specified type.
     */
    @SuppressWarnings({"unchecked", "TypeParameterHidesVisibleType"})
    public <T> T getAs() {
        return (T) value;
    }

    /**
     * Set the value of this property, cast to the specified type.
     */
    @SuppressWarnings("TypeParameterHidesVisibleType")
    public <T> void setAs(T value) {
        setUnchecked(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Property) {
            Property<?> property = (Property<?>) obj;
            return property.name.equals(name);
        }
        return false;
    }

    /**
     * Deep equals for a property, meaning it will check {@link #equals(Object)} and the value of this property with the given obj.
     * <br>
     * In pretty much every case, you should use {@link #equals(Object)} instead. This is used for testing.
     */
    public boolean deepEquals(Object obj) {
        if (obj instanceof Property) {
            Property<?> p = (Property<?>) obj;
            if (isPrimitiveArray()) {
                // i hate java
                if (get() instanceof int[]) {
                    return Arrays.equals((int[]) get(), (int[]) p.get());
                } else if (get() instanceof double[]) {
                    return Arrays.equals((double[]) get(), (double[]) p.get());
                } else if (get() instanceof float[]) {
                    return Arrays.equals((float[]) get(), (float[]) p.get());
                } else if (get() instanceof boolean[]) {
                    return Arrays.equals((boolean[]) get(), (boolean[]) p.get());
                } else if (get() instanceof char[]) {
                    return Arrays.equals((char[]) get(), (char[]) p.get());
                } else if (get() instanceof byte[]) {
                    return Arrays.equals((byte[]) get(), (byte[]) p.get());
                } else if (get() instanceof long[]) {
                    return Arrays.equals((long[]) get(), (long[]) p.get());
                } else if (get() instanceof short[]) {
                    return Arrays.equals((short[]) get(), (short[]) p.get());
                } else {
                    throw new IllegalStateException("wow");
                }
            } else if (isArray()) {
                return Arrays.equals((Object[]) get(), (Object[]) p.get());
            }
            return equals(p) && get() == p.get();
        } else return false;
    }

    /**
     * Return some metadata attached to this property.
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

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * @return true if the value is a primitive type wrapper (Number, String, Boolean, Character)
     */
    public boolean isPrimitive() {
        return value instanceof Number || value instanceof String || value instanceof Boolean || value instanceof Character;
    }

    /**
     * @return true if the value is a primitive array.
     */
    public boolean isPrimitiveArray() {
        return value.getClass().isArray() && value.getClass().getComponentType().isPrimitive();
    }

    /**
     * @return true if the value is an array.
     */
    public boolean isArray() {
        return value.getClass().isArray();
    }

    public static <T> Property<T> prop(T value) {
        return new Property<>(value);
    }

    public static <T> Property<T> prop(String name, T value) {
        return new Property<>(name, value);
    }


}
