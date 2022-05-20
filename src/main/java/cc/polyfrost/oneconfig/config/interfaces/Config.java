package cc.polyfrost.oneconfig.config.interfaces;

import cc.polyfrost.oneconfig.config.annotations.ConfigPage;
import cc.polyfrost.oneconfig.config.annotations.Option;
import cc.polyfrost.oneconfig.config.core.ConfigCore;
import cc.polyfrost.oneconfig.config.data.*;
import cc.polyfrost.oneconfig.config.profiles.Profiles;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.hud.HudCore;
import com.google.gson.*;
import gg.essential.universal.UScreen;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;

public class Config {
    transient protected final String configFile;
    transient protected final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().create();
    transient public Mod mod;
    public final transient HashMap<String, BasicOption> optionNames = new HashMap<>();
    public boolean enabled = true;

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     */
    public Config(Mod modData, String configFile) {
        this(modData, configFile, true);
    }

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     * @param initialize whether to load the config immediately or not
     */
    public Config(Mod modData, String configFile, boolean initialize) {
        this.configFile = configFile;
        if (initialize) init(modData);
    }

    public void init(Mod mod) {
        if (Profiles.getProfileFile(configFile).exists()) load();
        else save();
        mod.config = this;
        generateOptionList(this, mod.defaultPage, mod);
        ConfigCore.oneConfigMods.add(mod);
        this.mod = mod;
    }

    /**
     * Save current config to file
     */
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Profiles.getProfileFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load file and overwrite current values
     */
    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Profiles.getProfileFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            deserializePart(new JsonParser().parse(reader).getAsJsonObject(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate the option list, for internal use only
     *
     * @param instance instance of target class
     * @param page     page to add options too
     * @param mod      data about the mod
     */
    protected void generateOptionList(Object instance, OptionPage page, Mod mod) {
        Class<?> clazz = instance.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            String pagePrefix = page.equals(mod.defaultPage) ? "" : page.name + ".";
            if (!field.isAnnotationPresent(Option.class) && !field.isAnnotationPresent(ConfigPage.class)) {
                processCustomOption(field, page);
                continue;
            } else if (field.isAnnotationPresent(ConfigPage.class)) {
                ConfigPage option = field.getAnnotation(ConfigPage.class);
                if (!page.categories.containsKey(option.category()))
                    page.categories.put(option.category(), new OptionCategory());
                OptionCategory category = page.categories.get(option.category());
                if (category.subcategories.size() == 0 || !category.subcategories.get(category.subcategories.size() - 1).getName().equals(option.subcategory()))
                    category.subcategories.add(new OptionSubcategory(option.subcategory()));
                OptionSubcategory subcategory = category.subcategories.get(category.subcategories.size() - 1);
                OptionPage newPage = new OptionPage(option.name(), mod);
                try {
                    field.setAccessible(true);
                    Object object = field.get(clazz);
                    generateOptionList(object, newPage, mod);
                    ConfigPageButton configPageButton = new ConfigPageButton(field, instance, option.name(), option.description(), newPage);
                    switch (option.location()) {
                        case TOP:
                            subcategory.topButtons.add(configPageButton);
                            break;
                        case BOTTOM:
                            subcategory.bottomButtons.add(configPageButton);
                            break;
                    }
                    optionNames.put(pagePrefix + field.getName(), configPageButton);
                } catch (IllegalAccessException ignored) {
                }
                continue;
            }
            Option option = field.getAnnotation(Option.class);
            if (!page.categories.containsKey(option.category()))
                page.categories.put(option.category(), new OptionCategory());
            OptionCategory category = page.categories.get(option.category());
            if (category.subcategories.size() == 0 || !category.subcategories.get(category.subcategories.size() - 1).getName().equals(option.subcategory()))
                category.subcategories.add(new OptionSubcategory(option.subcategory()));
            ArrayList<BasicOption> options = category.subcategories.get(category.subcategories.size() - 1).options;
            switch (option.type()) {
                case SWITCH:
                    options.add(new ConfigSwitch(field, instance, option.name(), option.size()));
                    break;
                case CHECKBOX:
                    options.add(new ConfigCheckbox(field, instance, option.name(), option.size()));
                    break;
                case TEXT:
                    options.add(new ConfigTextBox(field, instance, option.name(), option.size(), option.placeholder(), option.secure(), option.multiLine()));
                    break;
                case DUAL_OPTION:
                    options.add(new ConfigDualOption(field, instance, option.name(), option.size(), option.options()));
                    break;
                case UNI_SELECTOR:
                    options.add(new ConfigUniSelector(field, instance, option.name(), option.size(), option.options()));
                    break;
                case DROPDOWN:
                    options.add(new ConfigDropdown(field, instance, option.name(), option.size(), option.options(), option.dividers()));
                    break;
                case SLIDER:
                    options.add(new ConfigSlider(field, instance, option.name(), option.size(), option.min(), option.max(), option.step()));
                    break;
                case INFO:
                    options.add(new ConfigInfo(field, instance, option.name(), option.size(), option.infoType()));
                    break;
                case COLOR:
                    options.add(new ConfigColorElement(field, instance, option.name(), option.size()));
                    break;
                case HEADER:
                    options.add(new ConfigHeader(field, instance, option.name(), option.size()));
                    break;
                case BUTTON:
                    options.add(new ConfigButton(field, instance, option.name(), option.size(), option.buttonText()));
                    break;
                case KEYBIND:
                    options.add(new ConfigKeyBind(field, instance, option.name(), option.size()));
                    break;
                case HUD:
                    try {
                        field.setAccessible(true);
                        BasicHud hud = (BasicHud) field.get(instance);
                        HudCore.huds.add(hud);
                        options.add(new ConfigHeader(field, hud, option.name(), 1));
                        options.add(new ConfigSwitch(hud.getClass().getField("enabled"), hud, "Enabled", 1));
                        options.add(new ConfigCheckbox(hud.getClass().getField("rounded"), hud, "Rounded corners", 1));
                        options.get(options.size() - 1).setDependency(() -> hud.enabled);
                        options.add(new ConfigCheckbox(hud.getClass().getField("border"), hud, "Outline/border", 1));
                        options.get(options.size() - 1).setDependency(() -> hud.enabled);
                        options.add(new ConfigColorElement(hud.getClass().getField("bgColor"), hud, "Background color:", 1));
                        options.get(options.size() - 1).setDependency(() -> hud.enabled);
                        options.add(new ConfigColorElement(hud.getClass().getField("borderColor"), hud, "Border color:", 1));
                        options.get(options.size() - 1).setDependency(() -> hud.enabled && hud.border);
                        options.add(new ConfigSlider(hud.getClass().getField("cornerRadius"), hud, "Corner radius:", 2, 0, 10, 0));
                        options.get(options.size() - 1).setDependency(() -> hud.enabled && hud.rounded);
                        options.add(new ConfigSlider(hud.getClass().getField("borderSize"), hud, "Border thickness:", 2, 0, 10, 0));
                        options.get(options.size() - 1).setDependency(() -> hud.enabled && hud.border);
                        options.add(new ConfigSlider(hud.getClass().getField("paddingX"), hud, "X-Padding", 2, 0, 50, 0));
                        options.get(options.size() - 1).setDependency(() -> hud.enabled);
                        options.add(new ConfigSlider(hud.getClass().getField("paddingY"), hud, "Y-Padding", 2, 0, 50, 0));
                        options.get(options.size() - 1).setDependency(() -> hud.enabled);
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    break;
            }
            if (!option.type().equals(OptionType.HUD))
                optionNames.put(pagePrefix + field.getName(), options.get(options.size() - 1));
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
     * @param json     json to deserialize
     * @param instance instance of target class
     */
    protected void deserializePart(JsonObject json, Object instance) {
        Class<?> clazz = instance.getClass();
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
                field.set(instance, object);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Function to open the gui of this mod
     */
    public void openGui() {
        if (mod == null) return;
        UScreen.displayScreen(new OneConfigGui(new ModConfigPage(mod.defaultPage)));
    }

    /**
     * Disable an option if a certain condition is not met
     *
     * @param option    The name of the field, or if the field is in a page "pageName.fieldName"
     * @param condition The condition that has to be met for the option to be enabled
     */
    protected void addDependency(String option, Supplier<Boolean> condition) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).setDependency(condition);
    }
}
