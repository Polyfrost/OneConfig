package cc.polyfrost.oneconfig.config.interfaces;

import cc.polyfrost.oneconfig.gui.elements.config.*;
import com.google.gson.*;
import cc.polyfrost.oneconfig.config.annotations.ConfigPage;
import cc.polyfrost.oneconfig.config.annotations.Option;
import cc.polyfrost.oneconfig.config.core.ConfigCore;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.OptionCategory;
import cc.polyfrost.oneconfig.config.data.OptionPage;
import cc.polyfrost.oneconfig.config.profiles.Profiles;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class Config {
    protected final String configFile;
    protected final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().registerTypeAdapterFactory(OneConfigTypeAdapterFactory.getStaticTypeAdapterFactory()).create();
    private static Mod mod;

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
        Config.mod = mod;
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
            if (!field.isAnnotationPresent(Option.class) && !field.isAnnotationPresent(ConfigPage.class)) {
                processCustomOption(field, page);
                continue;
            } else if (field.isAnnotationPresent(ConfigPage.class)) {
                ConfigPage option = field.getAnnotation(ConfigPage.class);
                if (!page.categories.containsKey(option.category()))
                    page.categories.put(option.category(), new OptionCategory());
                OptionPage newPage = new OptionPage(option.name(), mod);
                try {
                    field.setAccessible(true);
                    Object object = field.get(clazz);
                    generateOptionList(object.getClass(), newPage, mod);
                    switch (option.location()) {
                        case TOP:
                            page.categories.get(option.category()).topPages.add(new ConfigPageButton(field, option.name(), option.description(), newPage));
                            break;
                        case BOTTOM:
                            page.categories.get(option.category()).bottomPages.add(new ConfigPageButton(field, option.name(), option.description(), newPage));
                            break;
                    }
                } catch (IllegalAccessException e) {
                    continue;
                }
                continue;
            }
            Option option = field.getAnnotation(Option.class);
            if (!page.categories.containsKey(option.category()))
                page.categories.put(option.category(), new OptionCategory());
            if (!page.categories.get(option.category()).subcategories.containsKey(option.subcategory()))
                page.categories.get(option.category()).subcategories.put(option.subcategory(), new ArrayList<>());
            ArrayList<BasicOption> options = page.categories.get(option.category()).subcategories.get(option.subcategory());
            switch (option.type()) {
                case SWITCH:
                    options.add(new ConfigSwitch(field, option.name(), option.size()));
                    break;
                case CHECKBOX:
                    options.add(new ConfigCheckbox(field, option.name(), option.size()));
                    break;
                case TEXT:
                    options.add(new ConfigTextBox(field, option.name(), option.size(), option.placeholder(), option.secure(), option.multiLine()));
                    break;
                case DUAL_OPTION:
                    options.add(new ConfigDualOption(field, option.name(), option.size(), option.options()));
                    break;
                case UNI_SELECTOR:
                    options.add(new ConfigUniSelector(field, option.name(), option.size(), option.options()));
                    break;
                case DROPDOWN:
                    options.add(new ConfigDropdown(field, option.name(), option.size(), option.options(), option.dividers()));
                    break;
                case SLIDER:
                    options.add(new ConfigSlider(field, option.name(), option.size(), option.min(), option.max(), option.step()));
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

    /**
     * Function to open the gui of this mod
     */
    public void openGui() {
        if (mod == null) return;
        Minecraft.getMinecraft().displayGuiScreen(new OneConfigGui(new ModConfigPage(mod.defaultPage)));
    }
}
