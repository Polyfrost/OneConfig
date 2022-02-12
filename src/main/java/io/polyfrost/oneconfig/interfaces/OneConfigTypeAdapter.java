package io.polyfrost.oneconfig.interfaces;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;

final class OneConfigTypeAdapter<T> extends TypeAdapter<Class<T>> {

    private final Gson gson;
    private final JsonParser parser = new JsonParser();

    private OneConfigTypeAdapter(final Gson gson) {
        this.gson = gson;
    }

    static <T> TypeAdapter<Class<T>> getStaticTypeAdapter(final Gson gson) {
        return new OneConfigTypeAdapter<>(gson);
    }

    @Override
    public void write(final JsonWriter out, final Class<T> value) throws IOException {
        try {
            out.beginObject();
            for (Field field : value.getFields()) {
                out.name(field.getName());
                field.setAccessible(true);
                final TypeAdapter<Object> adapter = (TypeAdapter) gson.getAdapter(field.getType());
                adapter.write(out, field.get(null));
            }
            for (Class<?> clazz : value.getClasses()) {
                out.name(clazz.getSimpleName());
                final TypeAdapter<JsonElement> adapter = gson.getAdapter(JsonElement.class);
                adapter.write(out, parser.parse(gson.toJson(clazz)));
            }
            out.endObject();
        } catch (final IllegalAccessException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public Class<T> read(final JsonReader in) throws IOException {
        return null;
    }
}
