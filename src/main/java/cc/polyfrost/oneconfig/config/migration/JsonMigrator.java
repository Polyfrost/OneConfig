package cc.polyfrost.oneconfig.config.migration;

import com.google.gson.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JsonMigrator implements Migrator {
    public static final int CAMEL_CASE = 0;
    public static final int UNDERSCORES = 1;

    protected JsonObject object;
    protected HashMap<String, Object> values = null;
    protected final int delimiter;


    public JsonMigrator(String filePath, int nameDelimiter) {
        this.delimiter = nameDelimiter;
        File file = new File(filePath);
        try {
            object = new JsonParser().parse(new FileReader(file)).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            object = null;
        }
    }

    @Override
    public Object getValue(Field field, String name, @Nullable String category, @Nullable String subcategory) {
        if (object == null) return null;
        if (values == null) generateValues();
        if (field.isAnnotationPresent(SerializedName.class)) {
            SerializedName annotation = field.getAnnotation(SerializedName.class);
            name = annotation.name();
            category = annotation.category();
            subcategory = annotation.subcategory();
        }

        name = parse(name);
        category = parse(category);
        subcategory = parse(subcategory);
        String key = "";
        if (category != null) {
            key = category + ".";
            if (subcategory != null) {
                key += subcategory + ".";
            }
        }
        key += name;
        return values.get(key);
    }

    protected String parse(String value) {
        if (delimiter == UNDERSCORES) {
            return value.replace(" ", "_");
        } else {
            String[] words = value.split(" ");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(words[0]);
            for (int i = 1; i < words.length; i++) {
                String word = words[i];
                word = word.substring(0, 1).toUpperCase() + word.substring(1);
                stringBuilder.append(word);
            }
            return stringBuilder.toString();
        }
    }

    /**
     * Convert the JsonObject into a "." delimited HashMap.
     */
    protected void generateValues() {
        if (object == null) return;
        values = new HashMap<>();
        for (Map.Entry<String, JsonElement> master : object.entrySet()) {
            if (master.getValue().isJsonObject()) {
                for (Map.Entry<String, JsonElement> category : master.getValue().getAsJsonObject().entrySet()) {
                    if (category.getValue().isJsonObject()) {
                        for (Map.Entry<String, JsonElement> subcategory : category.getValue().getAsJsonObject().entrySet()) {
                            put(master.getKey() + "." + category.getKey() + "." + subcategory.getKey(), subcategory.getValue());
                        }
                    } else {
                        put(master.getKey() + "." + category.getKey(), category.getValue());
                    }
                }
            } else {
                put(master.getKey(), master.getValue());
            }
        }
    }

    /**
     * Take the JsonElement and add it as the correct type to the hashmap.
     *
     * @param key . delimited key
     * @param val value to be parsed
     */
    protected void put(String key, JsonElement val) {
        if (val.isJsonNull()) values.put(key, null);
        else if (val.isJsonPrimitive()) values.put(key, migrate(val.getAsJsonPrimitive()));
        else if (val.isJsonArray()) {
            JsonArray array = val.getAsJsonArray();
            Iterator<JsonElement> iterator = array.iterator();
            Object[] objects = new Object[array.size()];
            int i = 0;
            while (iterator.hasNext()) {
                objects[i] = migrate(iterator.next().getAsJsonPrimitive());
            }
            values.put(key, objects);
        } else values.put(key, val);
    }

    /**
     * Migrate the given JsonPrimitive to an appropriate number, boolean, or String.
     *
     * @param primitive the json primitive
     * @return the value in the correct type.
     */
    private Object migrate(JsonPrimitive primitive) {
        if (primitive.isJsonNull()) return null;
        else if (primitive.isBoolean()) return primitive.getAsBoolean();
        else if (primitive.isNumber()) {
            Number number = primitive.getAsNumber();
            if (number instanceof Integer) return number.intValue();
            else if (number instanceof Float) return number.floatValue();
            else if (number instanceof Long) return number.longValue();
            else if (number instanceof Double) return number.doubleValue();
            else if (number instanceof Short) return number.shortValue();
            else return number.byteValue();
        } else
            return primitive.getAsString();                             // if is not boolean, null or number return as String
    }
}
