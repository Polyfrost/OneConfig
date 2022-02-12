package io.polyfrost.oneconfig.interfaces;

import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Config {
    private final File configFile;

    public Config(File configFile) {
        this.configFile = configFile;
        if (configFile.exists())
            load();
        else
            save();
    }

    Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting()
            .registerTypeAdapterFactory(OneConfigTypeAdapterFactory.getStaticTypeAdapterFactory()).create();

    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this.getClass()));
        } catch (IOException ignored) {
        }
    }

    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            processPart(new JsonParser().parse(reader).getAsJsonObject(), this.getClass());
        } catch (IOException ignored) {
        }
    }

    private void processPart(JsonObject json, Class<?> clazz) {
        for (Map.Entry<String, JsonElement> element : json.entrySet()) {
            String name = element.getKey();
            JsonElement value = element.getValue();
            if (value.isJsonObject()) {
                for (Class<?> innerClass : clazz.getClasses()) {
                    if (innerClass.getSimpleName().equals(name)) {
                        processPart(value.getAsJsonObject(), innerClass);
                        break;
                    }
                }
            } else {
                try {
                    Field field = clazz.getField(name);
                    TypeAdapter<?> adapter = gson.getAdapter(field.getType());
                    Object object = adapter.fromJsonTree(value);
                    field.setAccessible(true);
                    field.set(null, object);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
