package io.polyfrost.oneconfig.config.interfaces;

import com.google.gson.*;
import io.polyfrost.oneconfig.config.annotations.*;
import io.polyfrost.oneconfig.config.core.ConfigCore;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.gui.elements.config.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class Config {
    private final File configFile;
    private final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting()
            .registerTypeAdapterFactory(OneConfigTypeAdapterFactory.getStaticTypeAdapterFactory()).create();

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     */
    public Config(ModData modData, File configFile) {
        this.configFile = configFile;
        if (configFile.exists())
            load();
        else
            save();
        modData.config = this;
        ConfigCore.settings.put(modData, generateOptionList(this.getClass()));
    }

    /**
     * Save current config to file
     */
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this.getClass()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load file and overwrite current values
     */
    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            deserializePart(new JsonParser().parse(reader).getAsJsonObject(), this.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate the option list for internal use only
     *
     * @param clazz target class
     * @return list of options
     */
    private ArrayList<Option> generateOptionList(Class<?> clazz) {
        ArrayList<Option> options = new ArrayList<>();
        for (Class<?> innerClass : clazz.getClasses()) {
            if (innerClass.isAnnotationPresent(Category.class)) {
                Category category = innerClass.getAnnotation(Category.class);
                options.add(new OConfigCategory(category.name(), category.description(), generateOptionList(innerClass)));
            }
        }
        for (Field field : clazz.getFields()) {
            if (field.isAnnotationPresent(Button.class)) {
                Button button = field.getAnnotation(Button.class);
                options.add(new OConfigButton(field, button.name(), button.description(), button.text()));
            } else if (field.isAnnotationPresent(ColorPicker.class)) {
                ColorPicker colorPicker = field.getAnnotation(ColorPicker.class);
                options.add(new OConfigColor(field, colorPicker.name(), colorPicker.description(), colorPicker.allowAlpha()));
            } else if (field.isAnnotationPresent(Selector.class)) {
                Selector selector = field.getAnnotation(Selector.class);
                options.add(new OConfigSelector(field, selector.name(), selector.description(), selector.options(), selector.defaultSelection()));
            } else if (field.isAnnotationPresent(Slider.class)) {
                Slider slider = field.getAnnotation(Slider.class);
                options.add(new OConfigSlider(field, slider.name(), slider.description(), slider.min(), slider.max(), slider.precision()));
            } else if (field.isAnnotationPresent(Switch.class)) {
                Switch aSwitch = field.getAnnotation(Switch.class);
                options.add(new OConfigSwitch(field, aSwitch.name(), aSwitch.description()));
            } else if (field.isAnnotationPresent(TextField.class)) {
                TextField textField = field.getAnnotation(TextField.class);
                options.add(new OConfigText(field, textField.name(), textField.description(), textField.placeholder(), textField.hideText()));
            } else loadCustomType(field);
        }
        return options;
    }

    /**
     * Overwrite this method to add your own custom option types
     *
     * @param field target field
     */
    protected void loadCustomType(Field field) {
    }

    /**
     * Deserialize part of config and load values
     *
     * @param json  json to deserialize
     * @param clazz target class
     */
    private void deserializePart(JsonObject json, Class<?> clazz) {
        for (Map.Entry<String, JsonElement> element : json.entrySet()) {
            String name = element.getKey();
            JsonElement value = element.getValue();
            if (value.isJsonObject()) {
                for (Class<?> innerClass : clazz.getClasses()) {
                    if (innerClass.getSimpleName().equals(name)) {
                        deserializePart(value.getAsJsonObject(), innerClass);
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
