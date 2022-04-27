package io.polyfrost.oneconfig.config.interfaces;

import com.google.gson.*;
import io.polyfrost.oneconfig.config.annotations.Option;
import io.polyfrost.oneconfig.config.core.ConfigCore;
import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.config.data.OptionPage;
import io.polyfrost.oneconfig.config.profiles.Profiles;
import io.polyfrost.oneconfig.gui.elements.config.ConfigPage;
import io.polyfrost.oneconfig.gui.elements.config.ConfigSwitch;
import io.polyfrost.oneconfig.test.TestConfig;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class Config {
    protected final String configFile;
    protected final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().registerTypeAdapterFactory(OneConfigTypeAdapterFactory.getStaticTypeAdapterFactory()).create();

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     */
    public Config(Mod modData, String configFile) {
        this.configFile = configFile;
        init(modData);
    }

    public void init(Mod mod) {
        if (Profiles.getProfileFile(configFile).exists()) load();
        else save();
        mod.config = this;
        generateOptionList(this.getClass(), mod.defaultPage, mod);
        ConfigCore.oneConfigMods.add(mod);
    }

    /**
     * Save current config to file
     */
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Profiles.getProfileFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this.getClass()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load file and overwrite current values
     */
    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Profiles.getProfileFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            deserializePart(new JsonParser().parse(reader).getAsJsonObject(), this.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate the option list, for internal use only
     *
     * @param clazz target class
     * @param page  page to add options too
     */
    protected void generateOptionList(Class<?> clazz, OptionPage page, Mod mod) {
        for (Field field : clazz.getDeclaredFields()) {
            System.out.println(field);
            if (!field.isAnnotationPresent(Option.class)) {
                processCustomOption(field, page);
                continue;
            }
            Option option = field.getAnnotation(Option.class);
            if (!page.categories.containsKey(option.category()))
                page.categories.put(option.category(), new HashMap<>());
            if (!page.categories.get(option.category()).containsKey(option.subcategory()))
                page.categories.get(option.category()).put(option.subcategory(), new ArrayList<>());
            ArrayList<BasicOption> options = page.categories.get(option.category()).get(option.subcategory());
            switch (option.type()) {
                case PAGE:
                    OptionPage newPage = new OptionPage(option.name(), mod);
                    try {
                        field.setAccessible(true);
                        Object object = field.get(clazz);
                        generateOptionList(object.getClass(), newPage, mod);
                        System.out.println(newPage.categories);
                        options.add(new ConfigPage(field, option.name(), option.description(), option.size(), newPage));
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                    break;
                case SWITCH:
                    options.add(new ConfigSwitch(field, option.name(), option.size()));
                    break;
            }
        }
    }

    /**
     * Overwrite this method to add your own custom option types
     *
     * @param field target field
     * @param page  page to add options too
     */
    protected void processCustomOption(Field field, OptionPage page) {
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
