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

package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.api.v1.config.option.OptionHolder;
import com.google.gson.FieldAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Static utility methods related to configuration option.
 */
public class ConfigUtils {
    public static boolean isAssignableFrom(Class<?> clazz, Class<?>... parentClass) {
        for (Class<?> c : parentClass) {
            if (c.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static void check(String type, Field field, Class<?>... expectedType) {
        // I have tried to check for supertype classes like Boolean other ways.
        // because they actually don't extend their primitive types (because that is impossible) so isAssignableFrom doesn't work.
        for (Class<?> clazz : expectedType) {
            if (clazz.isAssignableFrom(field.getType())) return;
        }
        throw new RuntimeException("Field " + field.getName() + " in config " + field.getDeclaringClass().getName() + " is annotated as a " + type + ", but is not of valid type, expected " + Arrays.toString(expectedType) + " (found " + field.getType() + ")");
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

    public static OptionHolder.NoReturnValue invokeMethod(Method method, Object parent) {
        try {
            method.setAccessible(true);
            method.invoke(parent);
        } catch (Exception ignored) {
        }
        return OptionHolder.NoReturnValue.INSTANCE;
    }
}
