package cc.polyfrost.oneconfig.config;

import cc.polyfrost.oneconfig.config.annotations.CustomOption;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.Page;
import cc.polyfrost.oneconfig.config.core.ConfigUtils;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.PageLocation;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.OptionSubcategory;
import cc.polyfrost.oneconfig.config.profiles.Profiles;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.hud.HUDUtils;
import cc.polyfrost.oneconfig.internal.config.annotations.Option;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class Config {
    public final transient HashMap<String, BasicOption> optionNames = new HashMap<>();
    transient protected final String configFile;
    transient protected final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().create();
    transient public Mod mod;
    public transient boolean hasBeenInitialized = false;
    public boolean enabled = true;

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     */
    public Config(Mod modData, String configFile) {
        this.configFile = configFile;
        init(modData);
    }

    public void init(Mod mod) {
        boolean migrate = false;
        if (Profiles.getProfileFile(configFile).exists()) load();
        else if (!hasBeenInitialized && mod.migrator != null) migrate = true;
        else save();
        mod.config = this;
        generateOptionList(this, mod.defaultPage, mod, migrate);
        if (migrate) save();
        ConfigCore.oneConfigMods.add(mod);
        this.mod = mod;
        hasBeenInitialized = true;
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
     * @param migrate  whether the migrator should be run
     */
    protected void generateOptionList(Object instance, OptionPage page, Mod mod, boolean migrate) {
        for (Field field : instance.getClass().getDeclaredFields()) {
            Option option = ConfigUtils.findAnnotation(field, Option.class);
            if (option != null) {
                BasicOption configOption = ConfigUtils.addOptionToPage(page, option, field, instance, migrate ? mod.migrator : null);
                optionNames.put(page.equals(mod.defaultPage) ? "" : page.name + "." + field.getName(), configOption);
            } else if (field.isAnnotationPresent(CustomOption.class)) {
                BasicOption configOption = getCustomOption(field, page, mod, migrate);
                if (configOption == null) continue;
                optionNames.put(page.equals(mod.defaultPage) ? "" : page.name + "." + field.getName(), configOption);
            } else if (field.isAnnotationPresent(Page.class)) {
                Page optionPage = field.getAnnotation(Page.class);
                OptionSubcategory subcategory = ConfigUtils.getSubCategory(page, optionPage.category(), optionPage.subcategory());
                Object pageInstance = ConfigUtils.getField(field, instance);
                if (pageInstance == null) continue;
                OptionPage newPage = new OptionPage(optionPage.name(), mod);
                generateOptionList(pageInstance, newPage, mod, migrate);
                ConfigPageButton button = new ConfigPageButton(field, instance, optionPage.name(), optionPage.description(), optionPage.category(), optionPage.subcategory(), newPage);
                if (optionPage.location() == PageLocation.TOP) subcategory.topButtons.add(button);
                else subcategory.bottomButtons.add(button);
            } else if (field.isAnnotationPresent(HUD.class)) {
                HUDUtils.addHudOptions(page, field, instance);
            }
        }
    }

    /**
     * All fields with the CustomOption annotation are sent to this function
     *
     * @param field   Target field
     * @param page    Page to add options too
     * @param mod     The data of the mod
     * @param migrate If the data should be migrated
     */
    protected BasicOption getCustomOption(Field field, OptionPage page, Mod mod, boolean migrate) {
        return null;
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
        GuiUtils.displayScreen(new OneConfigGui(new ModConfigPage(mod.defaultPage)));
    }

    /**
     * Disable an option if a certain condition is not met
     *
     * @param option    The name of the field, or if the field is in a page "pageName.fieldName"
     * @param condition The condition that has to be met for the option to be enabled
     */
    protected void addDependency(String option, Supplier<Boolean> condition) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addDependency(condition);
    }
}
