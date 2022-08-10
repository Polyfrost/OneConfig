/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.config;

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.ConfigUtils;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.PageLocation;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.OptionSubcategory;
import cc.polyfrost.oneconfig.config.gson.NonProfileSpecificExclusionStrategy;
import cc.polyfrost.oneconfig.config.gson.ProfileExclusionStrategy;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigKeyBind;
import cc.polyfrost.oneconfig.internal.config.profiles.Profiles;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.hud.HUDUtils;
import cc.polyfrost.oneconfig.internal.config.annotations.Option;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.config.core.KeyBindHandler;
import cc.polyfrost.oneconfig.utils.JsonUtils;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class Config {
    public final transient HashMap<String, BasicOption> optionNames = new HashMap<>();
    transient protected final String configFile;
    transient protected final Gson gson = new GsonBuilder().setExclusionStrategies(new ProfileExclusionStrategy()).excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().create();
    transient protected final Gson nonProfileSpecificGson = new GsonBuilder().setExclusionStrategies(new NonProfileSpecificExclusionStrategy()).excludeFieldsWithModifiers(Modifier.TRANSIENT).setPrettyPrinting().create();
    transient protected final HashMap<Field, Object> defaults = new HashMap<>();
    transient public Mod mod;
    public transient boolean hasBeenInitialized = false;
    public boolean enabled;

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     * @param enabled    whether the mod is enabled or not
     */
    public Config(Mod modData, String configFile, boolean enabled) {
        this.configFile = configFile;
        this.mod = modData;
        this.enabled = enabled;
    }

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     */
    public Config(Mod modData, String configFile) {
        this(modData, configFile, true);
    }

    public void initialize() {
        boolean migrate = false;
        if (Profiles.getProfileFile(configFile).exists()) load();
        else if (!hasBeenInitialized && mod.migrator != null) migrate = true;
        else save();
        if (hasBeenInitialized) return;
        mod.config = this;
        generateOptionList(this, mod.defaultPage, mod, migrate);
        if (migrate) save();
        ConfigCore.mods.add(mod);
        hasBeenInitialized = true;
    }

    /**
     * Save current config to file
     */
    public void save() {
        Profiles.getProfileFile(configFile).getParentFile().mkdirs();
        Profiles.getNonProfileSpecificDir(configFile).getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Profiles.getProfileFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Profiles.getNonProfileSpecificDir(configFile).toPath()), StandardCharsets.UTF_8))) {
            writer.write(nonProfileSpecificGson.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load file and overwrite current values
     */
    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Profiles.getProfileFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            deserializePart(JsonUtils.PARSER.parse(reader).getAsJsonObject(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Profiles.getNonProfileSpecificDir(configFile).toPath()), StandardCharsets.UTF_8))) {
            deserializePart(JsonUtils.PARSER.parse(reader).getAsJsonObject(), this);
        } catch (Exception e) {
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
        String pagePath = page.equals(mod.defaultPage) ? "" : page.name + ".";
        for (Field field : instance.getClass().getDeclaredFields()) {
            Option option = ConfigUtils.findAnnotation(field, Option.class);
            CustomOption customOption = ConfigUtils.findAnnotation(field, CustomOption.class);
            String optionName = pagePath + field.getName();
            if (option != null) {
                BasicOption configOption = ConfigUtils.addOptionToPage(page, option, field, instance, migrate ? mod.migrator : null);
                optionNames.put(optionName, configOption);
            } else if (customOption != null) {
                BasicOption configOption = getCustomOption(field, customOption, page, mod, migrate);
                if (configOption == null) continue;
                optionNames.put(optionName, configOption);
            } else if (field.isAnnotationPresent(Page.class)) {
                Page optionPage = field.getAnnotation(Page.class);
                OptionSubcategory subcategory = ConfigUtils.getSubCategory(page, optionPage.category(), optionPage.subcategory());
                Object pageInstance = ConfigUtils.getField(field, instance);
                if (pageInstance == null) continue;
                ConfigPageButton button;
                if (pageInstance instanceof cc.polyfrost.oneconfig.gui.pages.Page) {
                    button = new ConfigPageButton(field, instance, optionPage.name(), optionPage.description(), optionPage.category(), optionPage.subcategory(), (cc.polyfrost.oneconfig.gui.pages.Page) pageInstance);
                } else {
                    OptionPage newPage = new OptionPage(optionPage.name(), mod);
                    generateOptionList(pageInstance, newPage, mod, migrate);
                    button = new ConfigPageButton(field, instance, optionPage.name(), optionPage.description(), optionPage.category(), optionPage.subcategory(), newPage);
                }
                if (optionPage.location() == PageLocation.TOP) subcategory.topButtons.add(button);
                else subcategory.bottomButtons.add(button);
            } else if (field.isAnnotationPresent(HUD.class)) {
                HUDUtils.addHudOptions(page, field, instance, this);
            }
        }
        /*for (Method method : instance.getClass().getDeclaredMethods()) {
            Button button = ConfigUtils.findAnnotation(method, Button.class);
            String optionName = pagePath + method.getName();
            if (button != null) {
                ConfigButton option = ConfigButton.create(method, instance);
                ConfigUtils.getSubCategory(page, button.category(), button.subcategory()).options.add(option);
                optionNames.put(optionName, option);
            }
        }*/
    }

    /**
     * All fields with the CustomOption annotation are sent to this function, overwrite this function to handle custom options,
     * For documentation please see: <a href="https://docs.polyfrost.cc/oneconfig/config/adding-options/custom-options">https://docs.polyfrost.cc/oneconfig/config/adding-options/custom-options</a>
     *
     * @param field      Target field
     * @param annotation The annotation the field has
     * @param page       Page to add options too
     * @param mod        The data of the mod
     * @param migrate    If the data should be migrated
     */
    protected BasicOption getCustomOption(Field field, CustomOption annotation, OptionPage page, Mod mod, boolean migrate) {
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
        ArrayList<Field> fields = ConfigUtils.getClassFields(clazz);
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
                Field field = null;
                for (Field f : fields) {
                    if (f.getName().equals(name)) {
                        field = f;
                        break;
                    }
                }
                if (field != null) {
                    TypeAdapter<?> adapter = gson.getAdapter(field.getType());
                    Object object = adapter.fromJsonTree(value);
                    field.setAccessible(true);
                    field.set(instance, object);
                } else {
                    System.out.println("Could not deserialize " + name + " in class " + clazz.getSimpleName());
                }
            } catch (Exception ignored) {
                System.out.println("Could not deserialize " + name + " in class " + clazz.getSimpleName());
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

    /**
     * Disable an option if a certain condition is not met
     *
     * @param option          The name of the field, or if the field is in a page "pageName.fieldName"
     * @param dependentOption The option that has to be enabled
     */
    protected void addDependency(String option, String dependentOption) {
        if (!optionNames.containsKey(option) || !optionNames.containsKey(dependentOption)) return;
        optionNames.get(option).addDependency(() -> {
            try {
                return (boolean) optionNames.get(dependentOption).get();
            } catch (IllegalAccessException ignored) {
                return true;
            }
        });
    }

    /**
     * Disable an option if a certain condition is not met
     *
     * @param option The name of the field, or if the field is in a page "pageName.fieldName"
     * @param value  The value of the dependency
     */
    protected void addDependency(String option, boolean value) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addDependency(() -> value);
    }

    /**
     * Hide an option if a certain condition is met
     *
     * @param option    The name of the field, or if the field is in a page "pageName.fieldName"
     * @param condition The condition that has to be met for the option to be hidden
     */
    protected void hideIf(String option, Supplier<Boolean> condition) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addHideCondition(condition);
    }

    /**
     * Disable an option if a certain condition is not met
     *
     * @param option          The name of the field, or if the field is in a page "pageName.fieldName"
     * @param dependentOption The option that has to be hidden
     */
    protected void hideIf(String option, String dependentOption) {
        if (!optionNames.containsKey(option) || !optionNames.containsKey(dependentOption)) return;
        optionNames.get(option).addHideCondition(() -> {
            try {
                return (boolean) optionNames.get(dependentOption).get();
            } catch (IllegalAccessException ignored) {
                return true;
            }
        });
    }

    /**
     * Hide an option if a certain condition is met
     *
     * @param option The name of the field, or if the field is in a page "pageName.fieldName"
     * @param value  The value of the condition
     */
    protected void hideIf(String option, boolean value) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addHideCondition(() -> value);
    }

    /**
     * Register a new listener for when an option changes
     *
     * @param option   The name of the field, or if the field is in a page "pageName.fieldName"
     * @param runnable What should be executed after the option is changed
     */
    protected void addListener(String option, Runnable runnable) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addListener(runnable);
    }

    /**
     * Register an action to a keybind
     *
     * @param keyBind  The keybind
     * @param runnable The code to be executed
     */
    protected void registerKeyBind(OneKeyBind keyBind, Runnable runnable) {
        Field field = null;
        Object instance = null;
        for (BasicOption option : optionNames.values()) {
            if (!(option instanceof ConfigKeyBind)) continue;
            try {
                Field f = option.getField();
                OneKeyBind keyBind1 = (OneKeyBind) option.get();
                if (keyBind1 != keyBind) continue;
                field = f;
                instance = option.getParent();
            } catch (IllegalAccessException ignored) {
                continue;
            }
            break;
        }
        keyBind.setRunnable(runnable);
        KeyBindHandler.INSTANCE.addKeyBind(field, instance, keyBind);
    }

    /**
     * @param field The field to get the default value from
     * @return The default value of the given field
     */
    public Object getDefault(Field field) {
        return defaults.get(field);
    }

    /**
     * Reset this config file to its defaults.
     */
    public void reset() {
        for (BasicOption option : optionNames.values()) {
            option.reset(this);
        }
    }
}
