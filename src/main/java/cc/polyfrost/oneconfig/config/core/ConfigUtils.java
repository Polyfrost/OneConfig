package cc.polyfrost.oneconfig.config.core;

import cc.polyfrost.oneconfig.config.core.exceptions.InvalidTypeException;
import cc.polyfrost.oneconfig.config.data.OptionType;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionCategory;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.OptionSubcategory;
import cc.polyfrost.oneconfig.config.migration.Migrator;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import cc.polyfrost.oneconfig.internal.config.annotations.Option;
import com.google.gson.FieldAttributes;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class ConfigUtils {
    public static BasicOption getOption(Option option, Field field, Object instance) {
        switch (option.type()) {
            case SWITCH:
                check(OptionType.SWITCH, field, boolean.class, Boolean.class);
                return ConfigSwitch.create(field, instance);
            case CHECKBOX:
                check(OptionType.CHECKBOX, field, boolean.class, Boolean.class);
                return ConfigCheckbox.create(field, instance);
            case INFO:
                return ConfigInfo.create(field, instance);
            case HEADER:
                return ConfigHeader.create(field, instance);
            case COLOR:
                check(OptionType.COLOR, field, OneColor.class);
                return ConfigColorElement.create(field, instance);
            case DROPDOWN:
                check(OptionType.DROPDOWN, field, int.class, Integer.class);
                return ConfigDropdown.create(field, instance);
            case TEXT:
                check(OptionType.TEXT, field, String.class);
                return ConfigTextBox.create(field, instance);
            case BUTTON:
                check(OptionType.BUTTON, field, Runnable.class);
                return ConfigButton.create(field, instance);
            case SLIDER:
                check(OptionType.SLIDER, field, int.class, float.class, Integer.class, Float.class);
                return ConfigSlider.create(field, instance);
            case KEYBIND:
                check(OptionType.KEYBIND, field, OneKeyBind.class);
                return ConfigKeyBind.create(field, instance);
            case DUAL_OPTION:
                check(OptionType.DUAL_OPTION, field, boolean.class, Boolean.class);
                return ConfigDualOption.create(field, instance);
        }
        return null;
    }

    private static void check(OptionType type, Field field, Class<?>... expectedType) {
        // I have tried to check for supertype classes like Boolean other ways.
        // but they actually don't extend their primitive types (because that is impossible) so isAssignableFrom doesn't work.
        for (Class<?> clazz : expectedType) {
            if (field.getType().equals(clazz)) return;
        }
        throw new InvalidTypeException("Field " + field.getName() + " in config " + field.getDeclaringClass().getName() + " is annotated as a " + type.toString() + ", but is not of valid type, expected " + Arrays.toString(expectedType) + " (found " + field.getType() + ")");
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

    public static OptionSubcategory getSubCategory(OptionPage page, String categoryName, String subcategoryName) {
        if (!page.categories.containsKey(categoryName)) page.categories.put(categoryName, new OptionCategory());
        OptionCategory category = page.categories.get(categoryName);
        if (category.subcategories.size() == 0 || !category.subcategories.get(category.subcategories.size() - 1).getName().equals(subcategoryName))
            category.subcategories.add(new OptionSubcategory(subcategoryName));
        return category.subcategories.get(category.subcategories.size() - 1);
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
}
