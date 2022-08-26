package cc.polyfrost.oneconfig.config.gson;

import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

public class InstanceSupplier<T> implements InstanceCreator<T> {
    private final T instance;

    public InstanceSupplier(T instance) {
        this.instance = instance;
    }

    @Override
    public T createInstance(Type type) {
        return instance;
    }
}