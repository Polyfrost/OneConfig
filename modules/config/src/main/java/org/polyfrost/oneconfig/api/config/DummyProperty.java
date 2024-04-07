package org.polyfrost.oneconfig.api.config;

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


    @Override
    public @Nullable Void get() {
        return null;
    }

    @Override
    public void set(@Nullable Void value) {
        // good luck getting a void instance to set this with
    }

    @Override
    public <V> void setAs(V value) {
        // nop so that overwrite works
    }

    @Override
    public <V> V getAs() {
        return null;
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
    public void removeCallback(@NotNull Consumer<@Nullable Void> callback) {}

    @Override
    public Property<Void> addCallback(@NotNull Consumer<@Nullable Void> callback) {
        return this;
    }

    @Override
    public Property<Void> addCallback(@NotNull Collection<Consumer<@Nullable Void>> callbacks) {
        return this;
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
}
