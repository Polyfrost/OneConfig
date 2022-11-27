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

import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.CustomOption;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.HypixelKey;
import cc.polyfrost.oneconfig.config.annotations.Page;
import cc.polyfrost.oneconfig.config.core.ConfigUtils;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.OptionType;
import cc.polyfrost.oneconfig.config.data.PageLocation;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.OptionSubcategory;
import cc.polyfrost.oneconfig.config.gson.InstanceSupplier;
import cc.polyfrost.oneconfig.config.gson.exclusion.NonProfileSpecificExclusionStrategy;
import cc.polyfrost.oneconfig.config.gson.exclusion.ProfileExclusionStrategy;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigKeyBind;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.hud.HUDUtils;
import cc.polyfrost.oneconfig.internal.config.HypixelKeys;
import cc.polyfrost.oneconfig.internal.config.annotations.Option;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.config.core.KeyBindHandler;
import cc.polyfrost.oneconfig.internal.utils.Deprecator;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class Config {
    public final transient HashMap<String, BasicOption> optionNames = new HashMap<>();
    transient protected final String configFile;
    transient protected final Gson gson = addGsonOptions(new GsonBuilder()
            .setExclusionStrategies(new ProfileExclusionStrategy()))
            .create();
    transient protected final Gson nonProfileSpecificGson = addGsonOptions(new GsonBuilder()
            .setExclusionStrategies(new NonProfileSpecificExclusionStrategy()))
            .create();
    transient public Mod mod;
    public boolean enabled;
    public final boolean canToggle;

    /**
     * @param modData    information about the mod
     * @param configFile file where config is stored
     * @param enabled    whether the mod is enabled or not
     */
    public Config(Mod modData, String configFile, boolean enabled, boolean canToggle) {
        this.configFile = configFile;
        this.mod = modData;
        this.enabled = enabled;
        this.canToggle = canToggle;
    }

    public Config(Mod modData, String configFile, boolean enabled) {
        this(modData, configFile, enabled, true);
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
        if (ConfigUtils.getProfileFile(configFile).exists()) load();
        else if (mod.migrator != null) migrate = true;
        else save();
        mod.config = this;
        generateOptionList(this, mod.defaultPage, mod, migrate);
        if (migrate) save();
        ConfigCore.mods.add(mod);
    }

    public void reInitialize() {
        if (ConfigUtils.getProfileFile(configFile).exists()) load();
        else save();
    }

    /**
     * Save current config to file
     */
    public void save() {
        ConfigUtils.getProfileFile(configFile).getParentFile().mkdirs();
        ConfigUtils.getNonProfileSpecificFile(configFile).getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(ConfigUtils.getProfileFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(ConfigUtils.getNonProfileSpecificFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            writer.write(nonProfileSpecificGson.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load file and overwrite current values
     */
    public void load() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(ConfigUtils.getProfileFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            gson.fromJson(reader, this.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(ConfigUtils.getNonProfileSpecificFile(configFile).toPath()), StandardCharsets.UTF_8))) {
            nonProfileSpecificGson.fromJson(reader, this.getClass());
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
    protected final void generateOptionList(Object instance, OptionPage page, Mod mod, boolean migrate) {
        String pagePath = page.equals(mod.defaultPage) ? "" : page.name + ".";
        for (Field field : instance.getClass().getDeclaredFields()) {
            Option option = ConfigUtils.findAnnotation(field, Option.class);
            CustomOption customOption = ConfigUtils.findAnnotation(field, CustomOption.class);
            boolean isHypixelKey = field.isAnnotationPresent(HypixelKey.class);
            String optionName = pagePath + field.getName();
            if (option != null) {
                BasicOption configOption = ConfigUtils.addOptionToPage(page, option, field, instance, migrate ? mod.migrator : null);
                optionNames.put(optionName, configOption);
                if (isHypixelKey) {
                    if (option.type() == OptionType.TEXT) {
                        HypixelKeys.INSTANCE.addOption(configOption);
                    } else {
                        throw new IllegalStateException("Field " + field.getName() + " is missing @Text annotation! This is required for Hypixel keys!");
                    }
                }
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
            } else if (isHypixelKey) {
                throw new IllegalStateException("Field " + field.getName() + " is missing @Text annotation! This is required for Hypixel keys!");
            }
        }
        for (Method method : instance.getClass().getDeclaredMethods()) {
            Button button = ConfigUtils.findAnnotation(method, Button.class);
            String optionName = pagePath + method.getName();
            if (button != null) {
                BasicOption option = ConfigUtils.addOptionToPage(page, method, instance);
                optionNames.put(optionName, option);
            }
        }
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

    protected GsonBuilder addGsonOptions(GsonBuilder builder) {
        return builder
                .registerTypeAdapter(this.getClass(), new InstanceSupplier<>(this))
                .excludeFieldsWithModifiers(Modifier.TRANSIENT)
                .setPrettyPrinting();
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
    protected final void addDependency(String option, Supplier<Boolean> condition) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addDependency(condition);
    }

    /**
     * Disable an option if a certain condition is not met
     *
     * @param option          The name of the field, or if the field is in a page "pageName.fieldName"
     * @param dependentOption The option that has to be enabled
     */
    protected final void addDependency(String option, String dependentOption) {
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
    @Deprecated
    protected final void addDependency(String option, boolean value) {
        Deprecator.markDeprecated();
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addDependency(() -> value);
    }

    /**
     * Hide an option if a certain condition is met
     *
     * @param option    The name of the field, or if the field is in a page "pageName.fieldName"
     * @param condition The condition that has to be met for the option to be hidden
     */
    protected final void hideIf(String option, Supplier<Boolean> condition) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addHideCondition(condition);
    }

    /**
     * Disable an option if a certain condition is not met
     *
     * @param option          The name of the field, or if the field is in a page "pageName.fieldName"
     * @param dependentOption The option that has to be hidden
     */
    protected final void hideIf(String option, String dependentOption) {
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
    @Deprecated
    protected final void hideIf(String option, boolean value) {
        Deprecator.markDeprecated();
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addHideCondition(() -> value);
    }

    /**
     * Register a new listener for when an option changes
     *
     * @param option   The name of the field, or if the field is in a page "pageName.fieldName"
     * @param runnable What should be executed after the option is changed
     */
    protected final void addListener(String option, Runnable runnable) {
        if (!optionNames.containsKey(option)) return;
        optionNames.get(option).addListener(runnable);
    }

    /**
     * Register an action to a keybind
     *
     * @param keyBind  The keybind
     * @param runnable The code to be executed
     */
    protected final void registerKeyBind(OneKeyBind keyBind, Runnable runnable) {
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
     * @return If this mod supports profiles, false for compatibility mode
     */
    public boolean supportsProfiles() {
        return true;
    }
}
