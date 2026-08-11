/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 *   You should have received annotation copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received annotation copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.v1.collect.impl;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.utils.v1.MHUtils;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import static org.polyfrost.oneconfig.api.config.v1.Node.strv;

/**
 * Collects properties from an object using reflection and from its inner classes
 * <br>
 * Ignores fields without an annotation with the {@link Option} annotation
 */
public class OneConfigCollector extends ReflectiveCollector {
    public OneConfigCollector() {
        super(1);
    }

    @Override
    public @Nullable Tree collect(@NotNull Object src) {
        if (!(src instanceof Config)) return null;
        Tree tree = super.collect(src);
        assert tree != null;
        tree.onAllProps((s, p) -> {
            String[] conditions = p.consumeMetadata("conditions");
            if (conditions == null) return;
            for (String cond : conditions) {
                Property<?> condition = tree.getProp(cond);
                if (condition == null) throw new IllegalArgumentException("Property " + p.getID() + " is dependant on property " + cond + ", but that property does not exist");
                if (condition.type == boolean.class) {
                    //noinspection unchecked
                    p.addDisplayCondition((Property<Boolean>) condition, false);
                } else throw new IllegalArgumentException("Property " + p.getID() + " is dependant on property " + cond + ", but it is not annotation boolean property");
            }
        });
        return tree;
    }

    @Override
    protected void handleField(@NotNull Field f, @NotNull Object src, @NotNull Tree tree) {
        for (Annotation a : f.getDeclaredAnnotations()) {
            Class<? extends Annotation> type = a.annotationType();
            if (type == Include.class) {
                Property<?> p = Properties.field(null, null, f, src);
                p.addMetadata("hidden", true);
                tree.put(p);
                continue;
            }
            if (type == Accordion.class) {
                try {
                    Tree t = Tree.tree(f.getName());
                    t.addMetadata(MHUtils.getAnnotationValues(a).getOrThrow());
                    handle(t, MHUtils.instantiate(f.getType(), true).getOrThrow(), 1);
                    tree.put(t);
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to collect accordion-type field " + f.getName(), e);
                }
                continue;
            }
            Option opt = type.getDeclaredAnnotation(Option.class);
            if (opt == null) continue;
            try {
                Property<?> p = Properties.field(null, null, f, src);
                handleMetadata(tree, p, a, opt, f);
                tree.put(p);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to create setter for field " + f.getName() + "; ensure it is not static final", e);
            }
            break;
        }
    }

    @Override
    protected void handleMethod(@NotNull Method m, @NotNull Object src, @NotNull Tree builder) {
        Button b = m.getDeclaredAnnotation(Button.class);
        if (b == null) return;
        if (m.getParameterCount() != 0) throw new IllegalArgumentException("Button method " + m.getName() + " must have no parameters");
        Property<?> p = Properties.dummy(m.getName(), b.title(), b.description());
        final MethodHandle mh = MHUtils.getMethodHandle(m, src).getOrThrow();
        final String methodString = m.toString();
        p.addMetadata("runnable", (Runnable) () -> {
            try {
                mh.invokeExact();
            } catch (Throwable e) {
                LOGGER.error("Failed to invoke method for button {}", methodString, e);
            }
        });
        p.addMetadata("visualizer", b.annotationType().getDeclaredAnnotation(Option.class).display());
        p.addMetadata("text", strv(b.text()));
        p.addMetadata("icon", strv(b.icon()));
        p.addMetadata("category", b.category());
        p.addMetadata("subcategory", b.subcategory());
        builder.put(p);
    }

    @Override
    protected void handleInnerClass(@NotNull Class<?> c, @NotNull Object src, int depth, @NotNull Tree builder) {
        Accordion a = c.getDeclaredAnnotation(Accordion.class);
        if (a == null) return;
        try {
            Tree t = Tree.tree(c.getSimpleName());
            t.addMetadata(MHUtils.getAnnotationValues(a).getOrThrow());
            handle(t, MHUtils.instantiate(c, false).getOrThrow(), depth + 1);
            builder.put(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void handleMetadata(Tree tree, @NotNull Property<?> property, @NotNull Annotation annotation, Option option, Field field) {
        property.addMetadata("visualizer", option.display());
        var metadata = MHUtils.getAnnotationValues(annotation).getOrNull();
        if (metadata != null) {
            var tempData = new HashMap<>(metadata);

            metadata.forEach((key, value) -> {
                var translationKey = key + "Translation";
                if (metadata.containsKey(translationKey) && metadata.get(translationKey) instanceof Boolean && ((Boolean) metadata.get(translationKey)) == true) {
                    tempData.put(key + "Key", value);
                } else if (isIsDefaultTranslation(annotation, key, value)) {
                    tempData.put(key + "Key", value);
                }
            });

            property.addMetadata(tempData);
        }
        DependsOn dependsOn = field.getDeclaredAnnotation(DependsOn.class);
        if (dependsOn != null) property.addMetadata("conditions", dependsOn.value());

        switch (annotation) {
            case Slider slider -> {
                float range = slider.max() - slider.min();
                if (slider.step() > 0f && slider.step() > range) {
                    throw new IllegalArgumentException(String.format("@Slider field '%s' has step (%s) larger than its range (%s to %s, range=%s). The slider will not function correctly.",
                        field.getName(), slider.step(), slider.min(), slider.max(), range));
                }
            }
            case RangeSlider slider -> {
                float range = slider.max() - slider.min();
                if (slider.step() > 0f && slider.step() > range) {
                    throw new IllegalArgumentException(String.format("@RangeSlider field '%s' has step (%s) larger than its range (%s to %s, range=%s). The slider will not function correctly.",
                        field.getName(), slider.step(), slider.min(), slider.max(), range));
                }
            }
            case SliderList slider -> {
                float range = slider.max() - slider.min();
                if (slider.step() > 0f && slider.step() > range) {
                    throw new IllegalArgumentException(String.format("@SliderList field '%s' has step (%s) larger than its range (%s to %s, range=%s). The sliders will not function correctly.",
                        field.getName(), slider.step(), slider.min(), slider.max(), range));
                }
            }
            case Color color -> {
                if (!color.alpha()) property.addMetadata("noAlpha", Unit.INSTANCE);
            }
            case ColorList color -> {
                if (!color.alpha()) property.addMetadata("noAlpha", Unit.INSTANCE);
            }
            case ItemList itemList -> validateItemListField(field, itemList);
            default -> {}
        }

        PreviousNames previousName = field.getDeclaredAnnotation(PreviousNames.class);
        if (previousName != null) {
            String[] names = previousName.value();
            int len = names.length;
            HashMap<String, String> map = tree.getOrPutMetadata("migrationMap", () -> new HashMap<>(len));
            for (String s : names) {
                map.put(s, property.getID());
            }
        }
    }

    private void validateItemListField(Field field, ItemList annotation) {
        Class<?> type = field.getType();
        boolean stringArray = type.isArray() && type.getComponentType() == String.class;
        boolean stringList = List.class.isAssignableFrom(type) && hasStringListElements(field.getGenericType());
        if (!stringArray && !stringList) {
            throw new IllegalArgumentException(String.format(
                "@ItemList field '%s' must be a String[] or List<String>, but was %s.",
                field.getName(), field.getGenericType().getTypeName()
            ));
        }
        if (annotation.maxEntries() < 0) {
            throw new IllegalArgumentException(String.format(
                "@ItemList field '%s' has a negative maxEntries value (%s).",
                field.getName(), annotation.maxEntries()
            ));
        }
    }

    private boolean hasStringListElements(Type type) {
        if (!(type instanceof ParameterizedType parameterized)) return false;
        Type[] arguments = parameterized.getActualTypeArguments();
        return arguments.length == 1 && arguments[0] == String.class;
    }

    private boolean isIsDefaultTranslation(@NonNull Annotation annotation, String key, Object value) {
        try {
            var method = annotation.annotationType().getMethod(key);
            if (method.isAnnotationPresent(TranslatedDefault.class)){
                var translated = method.getAnnotation(TranslatedDefault.class);
                if (translated != null) {
                    return translated.value().equals(value);
                }
            }
        } catch (NoSuchMethodException ignored) {}
        return false;
    }

}
