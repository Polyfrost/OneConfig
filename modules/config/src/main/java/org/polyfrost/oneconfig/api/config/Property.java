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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class Property<T> extends Node implements Serializable {
    /**
     * This is the type of the property, and is used for casting.
     */
    @NotNull
    public transient final Class<T> type;
    /**
     * This is the actual value of the property, and is what is stored.
     */
    @Nullable
    private T value;
    private transient List<@NotNull Consumer<@Nullable T>> callbacks = null;
    private transient boolean display = true;
    private transient List<BooleanSupplier> conditions = null;

    public Property(@Nullable String id, @Nullable String name, @Nullable String description, @Nullable T value, @NotNull Class<T> type) {
        super(id, name, description);
        this.value = value;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public Property(@Nullable String id, @Nullable String name, @Nullable String description, @NotNull T value) {
        this(id, name, description, value, (Class<T>) value.getClass());
    }

    public Property(@NotNull T value) {
        this(value.getClass().getSimpleName(), value.getClass().getSimpleName(), null, value);
    }

    @SuppressWarnings("ConstantConditions")
    public static <T> Property<T> prop(@Nullable String id, @NotNull T value) {
        if (value == null) throw new IllegalArgumentException("Cannot create a property with a null value and no class");
        return new Property<>(id, null, null, value);
    }

    public static <T> Property<T> prop(@NotNull T value) {
        return new Property<>(null, null, null, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Property<T> prop(@Nullable String id, @Nullable T value, Class<?> type) {
        return new Property<>(id, null, null, value, (Class<T>) type);
    }

    @SuppressWarnings("unchecked")
    public static <T> Property<T> prop(@Nullable String id, @Nullable String name, @Nullable T value, @NotNull Class<?> type) {
        return new Property<>(id, name, null, value, (Class<T>) type);
    }

    @SuppressWarnings("unchecked")
    public static <T> Property<T> recast(@NotNull Property<?> prop) {
        return (Property<T>) prop;
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
    public final Property<T> addDisplayCondition(@NotNull BooleanSupplier condition) {
        if (conditions == null) conditions = new ArrayList<>(5);
        conditions.add(condition);
        evaluateDisplay();
        return this;
    }

    public final Property<T> addDisplayCondition(@NotNull BooleanSupplier... conditions) {
        return addDisplayCondition(Arrays.asList(conditions));
    }

    public final Property<T> addDisplayCondition(@NotNull Collection<BooleanSupplier> conditions) {
        if (this.conditions == null) this.conditions = new ArrayList<>(conditions);
        else {
            this.conditions.addAll(conditions);
        }
        evaluateDisplay();
        return this;
    }

    protected void evaluateDisplay() {
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
    public final void removeDisplayCondition(@NotNull BooleanSupplier condition) {
        if (conditions == null) return;
        conditions.remove(condition);
        evaluateDisplay();
    }

    /**
     * Remove all display conditions from this property.
     */
    final void clearDisplayConditions() {
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
        if (callbacks == null) callbacks = new ArrayList<>(2);
        callbacks.add(callback);
        return this;
    }

    @SafeVarargs
    public final Property<T> addCallback(@NotNull Consumer<@Nullable T>... callbacks) {
        return addCallback(Arrays.asList(callbacks));
    }

    public Property<T> addCallback(@NotNull Collection<Consumer<@Nullable T>> callbacks) {
        if (this.callbacks == null) this.callbacks = new ArrayList<>(callbacks);
        else {
            this.callbacks.addAll(callbacks);
        }
        return this;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void overwrite(Node with) {
        if (!(with instanceof Property)) throw new IllegalArgumentException("Cannot overwrite a property with a non-property");
        if (!Objects.equals(this.getID(), with.getID())) throw new IllegalArgumentException("ID should be the same for overwrite");
        Property<?> that = (Property<?>) with;
        this.addMetadata(that.getMetadata());
        this.setAs(that.get());
        if (that.conditions != null) this.addDisplayCondition(that.conditions);
        if (that.callbacks != null) addCallback((Collection) that.callbacks);
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
                for (Consumer<T> c : callbacks) {
                    c.accept(value);
                }
            } catch (Exception e) {
                LOGGER.error("Error while calling callbacks for property {}", getID(), e);
            }
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return getID() + ": " + value;
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
    public <V> void setAs(V value) {
        set(nconvert(value));
    }

    // i actually hate java
    @SuppressWarnings("unchecked")
    protected final T nconvert(Object o) {
        if (o == null) return null;
        if (!(o instanceof Number)) return (T) o;
        Number in = (Number) o;
        Class<?> c = in.getClass();
        Class<?> type = this.value == null ? this.type : this.value.getClass();
        if (type == Integer.class) return (T) Integer.valueOf(in.intValue());
        if (type == Float.class) return (T) Float.valueOf(in.floatValue());
        if (type == Double.class) return (T) Double.valueOf(in.doubleValue());
        if (type == Long.class) return (T) Long.valueOf(in.longValue());
        if (type == Byte.class) return (T) Byte.valueOf(in.byteValue());
        if (type == Short.class) return (T) Short.valueOf(in.shortValue());
        throw new IllegalArgumentException("Cannot convert number to " + type);
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
        // w: see nconvert
//        if (this.type != that.type) return false;
        return Objects.equals(this.getID(), that.getID()) && Objects.equals(this.value, that.value);
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
}
