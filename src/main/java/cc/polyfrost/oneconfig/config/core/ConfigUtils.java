/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package cc.polyfrost.oneconfig.config.core;

import cc.polyfrost.oneconfig.config.core.exceptions.InvalidTypeException;
import cc.polyfrost.oneconfig.config.data.OptionType;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionCategory;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.OptionSubcategory;
import cc.polyfrost.oneconfig.config.migration.Migrator;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigButton;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigCheckbox;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigColorElement;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigDropdown;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigDualOption;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigHeader;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigInfo;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigKeyBind;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigNumber;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigSlider;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigSwitch;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigTextBox;
import cc.polyfrost.oneconfig.internal.config.annotations.Option;
import cc.polyfrost.oneconfig.internal.config.profiles.Profiles;
import com.google.gson.FieldAttributes;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class ConfigUtils {
    public static BasicOption getOption(Option option, Field field, Object instance) {
        switch (option.type()) {
            case SWITCH:
                check(OptionType.SWITCH.toString(), field, boolean.class, Boolean.class);
                return ConfigSwitch.create(field, instance);
            case CHECKBOX:
                check(OptionType.CHECKBOX.toString(), field, boolean.class, Boolean.class);
                return ConfigCheckbox.create(field, instance);
            case INFO:
                return ConfigInfo.create(field, instance);
            case HEADER:
                return ConfigHeader.create(field, instance);
            case COLOR:
                check(OptionType.COLOR.toString(), field, OneColor.class);
                return ConfigColorElement.create(field, instance);
            case DROPDOWN:
                check(OptionType.DROPDOWN.toString(), field, int.class, Integer.class);
                return ConfigDropdown.create(field, instance);
            case TEXT:
                check(OptionType.TEXT.toString(), field, String.class);
                return ConfigTextBox.create(field, instance);
            case BUTTON:
                check(OptionType.BUTTON.toString(), field, Runnable.class);
                return ConfigButton.create(field, instance);
            case SLIDER:
                check(OptionType.SLIDER.toString(), field, int.class, float.class, Integer.class, Float.class);
                return ConfigSlider.create(field, instance);
            case NUMBER:
                check(OptionType.NUMBER.toString(), field, int.class, float.class, Integer.class, Float.class);
                return ConfigNumber.create(field, instance);
            case KEYBIND:
                check(OptionType.KEYBIND.toString(), field, OneKeyBind.class);
                return ConfigKeyBind.create(field, instance);
            case DUAL_OPTION:
                check(OptionType.DUAL_OPTION.toString(), field, boolean.class, Boolean.class);
                return ConfigDualOption.create(field, instance);
        }
        return null;
    }

    public static void check(String type, Field field, Class<?>... expectedType) {
        // I have tried to check for supertype classes like Boolean other ways.
        // because they actually don't extend their primitive types (because that is impossible) so isAssignableFrom doesn't work.
        for (Class<?> clazz : expectedType) {
            if (clazz.isAssignableFrom(field.getType())) return;
        }
        throw new InvalidTypeException("Field " + field.getName() + " in config " + field.getDeclaringClass().getName() + " is annotated as a " + type + ", but is not of valid type, expected " + Arrays.toString(expectedType) + " (found " + field.getType() + ")");
    }

    public static ArrayList<BasicOption> getClassOptions(Object object) {
        ArrayList<BasicOption> options = new ArrayList<>();
        ArrayList<Field> fields = getClassFields(object.getClass());
        for (Field field : fields) {
            Option option = findAnnotation(field, Option.class);
            if (option == null) continue;
            options.add(getOption(option, field, object));
        }
        return options;
    }

    public static ArrayList<Field> getClassFields(Class<?> object) {
        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(object.getDeclaredFields()));
        Class<?> parentClass = object;
        Class<?> clazz = object;
        while (true) {
            clazz = clazz.getSuperclass();
            if (clazz != null && clazz != parentClass) fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            else break;
            parentClass = clazz;
        }
        return fields;
    }

    public static BasicOption addOptionToPage(OptionPage page, Option option, Field field, Object instance, @Nullable Migrator migrator) {
        BasicOption configOption = getOption(option, field, instance);
        if (configOption == null) return null;
        if (migrator != null) {
            Object value = migrator.getValue(field, configOption.name, configOption.category, configOption.subcategory);
            if (value != null) setField(field, value, instance);
        }
        getSubCategory(page, configOption.category, configOption.subcategory).options.add(configOption);
        return configOption;
    }

    public static BasicOption addOptionToPage(OptionPage page, Option option, Field field, Object instance, String category, @Nullable Migrator migrator) {
        if (category == null) return addOptionToPage(page, option, field, instance, migrator);
        BasicOption configOption = getOption(option, field, instance);
        if (configOption == null) return null;
        configOption.category = category;
        if (migrator != null) {
            Object value = migrator.getValue(field, configOption.name, configOption.category, configOption.subcategory);
            if (value != null) setField(field, value, instance);
        }
        getSubCategory(page, category, configOption.subcategory).options.add(configOption);
        return configOption;
    }

    public static BasicOption addOptionToPage(OptionPage page, Option option, Field field, Object instance, String category, String subcategory, @Nullable Migrator migrator) {
        if (subcategory == null) return addOptionToPage(page, option, field, instance, category, migrator);
        BasicOption configOption = getOption(option, field, instance);
        if (configOption == null) return null;
        configOption.category = category;
        configOption.subcategory = subcategory;
        if (migrator != null) {
            Object value = migrator.getValue(field, configOption.name, configOption.category, configOption.subcategory);
            if (value != null) setField(field, value, instance);
        }
        getSubCategory(page, category, subcategory).options.add(configOption);
        return configOption;
    }


    public static BasicOption addOptionToPage(OptionPage page, Method method, Object instance) {
        BasicOption configOption = ConfigButton.create(method, instance);
        getSubCategory(page, configOption.category, configOption.subcategory).options.add(configOption);
        return configOption;
    }

    public static BasicOption addOptionToPage(OptionPage page, Method method, Object instance, String category) {
        if (category == null) {
            addOptionToPage(page, method, instance);
        }
        BasicOption configOption = ConfigButton.create(method, instance);
        configOption.category = category;
        getSubCategory(page, category, configOption.subcategory).options.add(configOption);
        return configOption;
    }

    public static BasicOption addOptionToPage(OptionPage page, Method method, Object instance, String category, String subcategory) {

        if (subcategory == null) {
            addOptionToPage(page, method, instance, category);
        }

        BasicOption configOption = ConfigButton.create(method, instance);
        configOption.category = category;
        configOption.subcategory = subcategory;
        getSubCategory(page, category, subcategory).options.add(configOption);
        return configOption;
    }

    public static OptionSubcategory getSubCategory(OptionPage page, String categoryName, String subcategoryName) {
        if (!page.categories.containsKey(categoryName)) page.categories.put(categoryName, new OptionCategory());
        OptionCategory category = page.categories.get(categoryName);
        OptionSubcategory subcategory = category.subcategories.stream().filter(s -> s.getName().equals(subcategoryName)).findFirst().orElse(null);
        if (category.subcategories.size() == 0 || subcategory == null) {
            category.subcategories.add((subcategory = new OptionSubcategory(subcategoryName, categoryName)));
        }
        return subcategory;
    }

    public static <T extends Annotation> T findAnnotation(Field field, Class<T> annotationType) {
        if (field.isAnnotationPresent(annotationType)) return field.getAnnotation(annotationType);
        for (Annotation ann : field.getDeclaredAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(annotationType))
                return ann.annotationType().getAnnotation(annotationType);
        }
        return null;
    }

    public static <T extends Annotation> T findAnnotation(Method method, Class<T> annotationType) {
        if (method.isAnnotationPresent(annotationType)) return method.getAnnotation(annotationType);
        for (Annotation ann : method.getDeclaredAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(annotationType))
                return ann.annotationType().getAnnotation(annotationType);
        }
        return null;
    }

    public static <T extends Annotation> T findAnnotation(FieldAttributes field, Class<T> annotationType) {
        T annotation = field.getAnnotation(annotationType);
        if (annotation != null) return annotation;
        for (Annotation ann : field.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(annotationType))
                return ann.annotationType().getAnnotation(annotationType);
        }
        return null;
    }

    public static <T extends Annotation> T findAnnotation(Class<?> clazz, Class<T> annotationType) {
        if (clazz.isAnnotationPresent(annotationType)) return clazz.getAnnotation(annotationType);
        for (Annotation ann : clazz.getDeclaredAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(annotationType))
                return ann.annotationType().getAnnotation(annotationType);
        }
        return null;
    }

    public static Object getField(Field field, Object parent) {
        try {
            field.setAccessible(true);
            return field.get(parent);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void setField(Field field, Object value, Object parent) {
        try {
            field.setAccessible(true);
            field.set(parent, value);
        } catch (Exception ignored) {
        }
    }

    public static String getCurrentProfile() {
        return Profiles.getCurrentProfile();
    }

    public static File getProfileDir() {
        return Profiles.getProfileDir();
    }

    public static File getNonSpecificProfileDir() {
        return Profiles.nonProfileSpecificDir;
    }

    public static File getProfileFile(String file) {
        return Profiles.getProfileFile(file);
    }

    public static File getNonProfileSpecificFile(String file) {
        return Profiles.getNonProfileSpecificFile(file);
    }
}
