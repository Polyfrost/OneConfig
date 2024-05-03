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
import org.polyfrost.oneconfig.utils.v1.MHUtils;
import org.polyfrost.oneconfig.utils.v1.WrappingUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Class which represents a property in a tree.
 * <br>
 * <b>to actually create a property, see the {@link Properties} class.</b>
 *
 * @param <T>
 */
@SuppressWarnings("unused")
public abstract class Property<T> extends Node implements Serializable {
    /**
     * This is the type of the property, and is used for casting.
     * <br><b>Note that this is not {@code Class<T>}. this is because it holds the unwrapped type of the stored value.</b>
     */
    @NotNull
    public transient final Class<?> type;
    protected transient List<@NotNull Consumer<@Nullable T>> callbacks = null;
    private transient boolean display = true;
    private transient List<BooleanSupplier> conditions = null;

    protected Property(@Nullable String id, @Nullable String title, @Nullable String description, @NotNull Class<T> type) {
        super(id, title, description);
        this.type = WrappingUtils.getUnwrapped(type);
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
    public final boolean canDisplay() {
        return display;
    }

    /**
     * Add a display condition to this property.
     */
    public final Property<T> addDisplayCondition(@NotNull BooleanSupplier condition) {
        if (conditions == null) conditions = new ArrayList<>(5);
        conditions.add(condition);
        revaluateDisplay();
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
        revaluateDisplay();
        return this;
    }

    public void revaluateDisplay() {
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
        revaluateDisplay();
    }

    /**
     * Remove all display conditions from this property.
     */
    protected final void clearDisplayConditions() {
        conditions = null;
        display = true;
    }

    /**
     * Add a callback to this property, which is called when the value changes.
     *
     * @param callback the callback to add. The new value is passed to the callback.
     * @see #removeCallback(Consumer)
     */
    public final Property<T> addCallback(@NotNull Consumer<@Nullable T> callback) {
        if (callbacks == null) callbacks = new ArrayList<>(2);
        callbacks.add(callback);
        return this;
    }

    @SafeVarargs
    public final Property<T> addCallback(@NotNull Consumer<@Nullable T>... callbacks) {
        return addCallback(Arrays.asList(callbacks));
    }

    public final Property<T> addCallback(@NotNull Collection<Consumer<@Nullable T>> callbacks) {
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
    public final void removeCallback(@NotNull Consumer<@Nullable T> callback) {
        if (callbacks == null) return;
        callbacks.remove(callback);
    }

    /**
     * Remove all callbacks.
     */
    protected final void clearCallbacks() {
        callbacks = null;
    }

    /**
     * Set the value of this property. This will call all callbacks.
     * <br>
     * The value (and callbacks) are only set/called if the value is different from the previous value (using {@link Object#equals(Object)}).
     */
    public void set(@Nullable T value) {
        if (value != null && value.equals(this.get())) return;
        if (callbacks != null) {
            for (Consumer<T> c : callbacks) {
                try {
                    c.accept(value);
                } catch (Throwable t) {
                    LOGGER.error("failed to call callback {} on property {}", c, this.getID(), t);
                }
            }
        }
        set0(value);
    }

    protected abstract void set0(@Nullable T value);

    @Override
    public final String toString() {
        return getID() + ": " + get();
    }

    /**
     * Get the value of this property.
     */
    @Nullable
    public abstract T get();

    /**
     * Get the value of this property, cast to the specified type.
     * This method is unsafe, and will throw a {@link ClassCastException} if the value is not of the correct type.
     */
    @SuppressWarnings("unchecked")
    public final <V> V getAs() {
        return (V) get();
    }

    /**
     * Set the value of this property. This will call all callbacks.
     * This method is unsafe, and will throw a {@link ClassCastException} if the value is not of the correct type.
     */
    @SuppressWarnings("unchecked")
    public final <V> void setAs(V value) {
        set((T) WrappingUtils.richCast(value, type));
    }

    /**
     * Deep equals for a property, meaning it will check {@link #equals(Object)} and the value of this property with the given obj.
     * <br>
     * In pretty much every case, you should use {@link #equals(Object)} instead. This is used for testing. Note that primitive arrays are not checked.
     */
    @Override
    public boolean deepEquals(Object obj) {
        if (!equals(obj)) return false;
        if (!this.isArray() || this.isPrimitive()) return false;
        Property<?> that = (Property<?>) obj;
        return Arrays.equals((Object[]) this.get(), (Object[]) that.get());
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Property)) return false;
        Property<?> that = (Property<?>) obj;
        if (this.type != WrappingUtils.getUnwrapped(that.type)) return false;
        return Objects.equals(this.getID(), that.getID()) && Objects.equals(this.get(), that.get());
    }

    /**
     * @return true if the value is a primitive array.
     */
    public final boolean isPrimitive() {
        return type.isPrimitive() || (type.isArray() && type.getComponentType().isPrimitive());
    }

    /**
     * @return true if the value is an array.
     */
    public final boolean isArray() {
        return type.isArray();
    }


    // classes are package-private so that NO internal implementation details are leaked to the user
    // they can only see the Property<T> type
    static final class Simple<T> extends Property<T> {
        private T value;

        @SuppressWarnings("unchecked")
        Simple(@Nullable String id, @Nullable String title, @Nullable String description, @Nullable T value, @Nullable Class<T> type) {
            super(id, title, description, type == null ? (Class<T>) Objects.requireNonNull(value).getClass() : type);
            this.value = value;
        }

        @Override
        protected void set0(@Nullable T value) {
            this.value = value;
        }

        @Override
        public @Nullable T get() {
            return value;
        }
    }

    static final class Functional<T> extends Property<T> {
        private final Consumer<T> setter;
        private final Supplier<T> getter;

        @SuppressWarnings("unchecked")
        Functional(@Nullable String id, @Nullable String title, @Nullable String description, @NotNull Consumer<T> setter, @NotNull Supplier<T> getter, @Nullable Class<T> type) {
            super(id, title, description, type == null ? (Class<T>) getter.get().getClass() : type);
            this.setter = setter;
            this.getter = getter;
        }

        @Override
        protected void set0(@Nullable T value) {
            setter.accept(value);
        }

        @Override
        public @Nullable T get() {
            return getter.get();
        }
    }

    @SuppressWarnings("unchecked")
    static final class Field<T> extends Property<T> {
        private final java.lang.reflect.Field field;
        private final Object owner;

        Field(@Nullable String title, @Nullable String description, @NotNull java.lang.reflect.Field field, @Nullable Object owner) {
            super(field.getName(), title, description, (Class<T>) field.getType());
            this.field = field;
            this.owner = owner;
            MHUtils.setAccessible(field);
        }

        @Override
        public void set0(@Nullable T value) {
            try {
                field.set(owner, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public @Nullable T get() {
            try {
                return (T) field.get(owner);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public static final class KtProperty<T> extends Property<T> {
        private final kotlin.reflect.KMutableProperty0<T> ref;

        public KtProperty(@Nullable String title, @Nullable String description, @NotNull kotlin.reflect.KMutableProperty0<T> ref, Class<T> type) {
            super(ref.getName(), title, description, type);
            this.ref = ref;
        }

        @Override
        public @Nullable T get() {
            return ref.get();
        }

        @Override
        protected void set0(@Nullable T value) {
            ref.set(value);
        }
    }

    static final class Dummy extends Property<Void> {
        private static short r = 0;

        Dummy(@Nullable String id, @Nullable String title, @Nullable String description) {
            super(id == null ? "dummy$" + r++ : id, title, description, void.class);
        }

        @Override
        @Deprecated
        public @Nullable Void get() {
            return null;
        }

        @Override
        @Deprecated
        public void set(@Nullable Void value) {
        }

        @Override
        protected void set0(@Nullable Void value) {
        }
    }
}
