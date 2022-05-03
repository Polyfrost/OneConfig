package cc.polyfrost.oneconfig.config.interfaces;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class OneConfigTypeAdapterFactory implements TypeAdapterFactory {

    private static final TypeAdapterFactory staticTypeAdapterFactory = new OneConfigTypeAdapterFactory();

    public static TypeAdapterFactory getStaticTypeAdapterFactory() {
        return staticTypeAdapterFactory;
    }

    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> typeToken) {
        final Type type = typeToken.getType();
        if (type.equals(Class.class)) {
            @SuppressWarnings("unchecked") final TypeAdapter<T> castStaticTypeAdapter = (TypeAdapter<T>) OneConfigTypeAdapter.getStaticTypeAdapter(gson);
            return castStaticTypeAdapter;
        }
        return null;
    }

}
