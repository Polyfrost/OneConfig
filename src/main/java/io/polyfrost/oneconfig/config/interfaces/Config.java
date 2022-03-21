package io.polyfrost.oneconfig.config.interfaces;

import com.google.gson.*;
import io.polyfrost.oneconfig.config.annotations.*;
import io.polyfrost.oneconfig.config.core.ConfigCore;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.config.profiles.Profiles;
import io.polyfrost.oneconfig.gui.elements.config.*;
import io.polyfrost.oneconfig.hud.HudCore;
import io.polyfrost.oneconfig.hud.interfaces.BasicHud;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class Config {
    protected final String configFile;
    protected final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().registerTypeAdapterFactory(OneConfigTypeAdapterFactory.getStaticTypeAdapterFactory()).create();

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     */
    public Config(ModData modData, String configFile) {
        this.configFile = configFile;
        init(modData);
    }

    public void init(ModData modData) {
        if (Profiles.getProfileFile(configFile).exists()) load();
        else save();
        modData.config = this;
        ConfigCore.settings.put(modData, generateOptionList(this.getClass()));
    }

    /**
     * Save current config to file
     */
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Profiles.getProfileFile(configFile)), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this.getClass()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load file and overwrite current values
     */
    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Profiles.getProfileFile(configFile)), StandardCharsets.UTF_8))) {
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
    protected ArrayList<Option> generateOptionList(Class<?> clazz) {
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
            } else if (field.isAnnotationPresent(HudComponent.class)) {
                HudComponent hudComponent = field.getAnnotation(HudComponent.class);
                options.add(new OConfigHud(field, hudComponent.name(), hudComponent.description()));
                try {
                    Object hud = field.get(BasicHud.class);
                    HudCore.huds.add((BasicHud) hud);
                    System.out.println("here");
                    System.out.println(HudCore.huds.size());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                Option customOption = processCustomOption(field);
                if (customOption != null) options.add(customOption);
            }
        }
        return options;
    }

    /**
     * Overwrite this method to add your own custom option types
     *
     * @param field target field
     * @return custom option
     */
    protected Option processCustomOption(Field field) {
        return null;
    }

    /**
     * Deserialize part of config and load values
     *
     * @param json  json to deserialize
     * @param clazz target class
     */
    protected void deserializePart(JsonObject json, Class<?> clazz) {
        for (Map.Entry<String, JsonElement> element : json.entrySet()) {
            String name = element.getKey();
            JsonElement value = element.getValue();
            if (value.isJsonObject()) {
                Optional<Class<?>> innerClass = Arrays.stream(clazz.getClasses()).filter(aClass -> aClass.getSimpleName().equals(name)).findFirst();
                if (innerClass.isPresent()) {
                    deserializePart(value.getAsJsonObject(), innerClass.get());
                    continue;
                }
            }
            try {
                Field field = clazz.getField(name);
                TypeAdapter<?> adapter = gson.getAdapter(field.getType());
                Object object = adapter.fromJsonTree(value);
                field.setAccessible(true);
                field.set(null, object);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
    }
}
