/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.backend.impl;

import com.electronwill.nightconfig.core.Config;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.backend.Backend;
import org.polyfrost.oneconfig.api.config.exceptions.SerializationException;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * This class serializes objects, mimicking the behaviour of GSON. It serializes any non-null, non-transient and non-final fields.
 * <br>
 * It takes an object, and serializes all its fields and its class. Upon deserialization, it looks for a no-args constructor, and invokes it, before assigning
 * all the fields back to the object.
 */
public class ObjectSerializer {
    public static final ObjectSerializer INSTANCE = new ObjectSerializer();
    private static final Unsafe theUnsafe = getUnsafe();

    public Object convertToField(Config value) {
        String s = value.get("classType");
        Object o;
        try {
            Constructor<?> ctor = Class.forName(s).getDeclaredConstructor();
            ctor.setAccessible(true);
            o = ctor.newInstance();
        } catch (NoSuchMethodException ignored) {
            if (theUnsafe != null) {
                try {
                    o = theUnsafe.allocateInstance(Class.forName(s));
                } catch (Exception e) {
                    throw new SerializationException("Failed to allocate deserializing object", e);
                }
            } else {
                throw new SerializationException("Failed to deserialize object: no no-args constructor found!");
            }
        } catch (Exception e) {
            throw new SerializationException("Failed to allocate deserializing object", e);
        }
        for (Config.Entry e : value.entrySet()) {
            if (e.getKey().equals("classType")) continue;
            try {
                Field f = getDeclaredField(o.getClass(), e.getKey());
                f.setAccessible(true);
                Class<?> type = f.getType();
                if (f.getType().isEnum()) {
                    f.set(o, Enum.valueOf((Class<Enum>) f.getType(), e.getValue()));
                } else if (type.equals(float.class) || type.equals(Float.class)) {
                    f.set(o, ((Number) e.getValue()).floatValue());
                } else if (e.getValue() instanceof Config) {
                    f.set(o, convertToField(e.getValue()));
                } else {
                    Object out = unboxFully(e.getValue(), getPrimitiveWrapperFor(type));
                    if (out instanceof Number[]) {
                        Number[] a = ((Number[]) out);
                        if (type.equals(float[].class) || type.equals(Float[].class)) {
                            for (int i = 0; i < a.length; i++) {
                                a[i] = ((Number) e.getValue()).floatValue();
                            }
                        }
                    } else if (List.class.isAssignableFrom(type)) {
                        List<?> list = e.getValue();
                        ArrayList<Object> arr = new ArrayList<>(list.size());
                        for (int i = 0; i < list.size(); i++) {
                            arr.add(i, ObjectSerializer.INSTANCE.convertFromField(list.get(i)));
                        }
                        out = arr;
                    }
                    f.set(o, out);
                }
            } catch (Exception ex) {
                throw new SerializationException("Failed to deserialize object", ex);
            }
        }
        return o;
    }

    public static Class<?> getPrimitiveWrapperFor(Class<?> cls) {
        if (cls.isArray() && cls.getComponentType().isPrimitive()) {
            return getPrimitiveWrapperFor(cls.getComponentType());
        }
        if (!cls.isPrimitive()) return cls;
        if (cls.equals(boolean.class)) return Boolean.class;
        if (cls.equals(byte.class)) return Byte.class;
        if (cls.equals(short.class)) return Short.class;
        if (cls.equals(int.class)) return Integer.class;
        if (cls.equals(long.class)) return Long.class;
        if (cls.equals(float.class)) return Float.class;
        if (cls.equals(double.class)) return Double.class;
        if (cls.equals(char.class)) return Character.class;
        return cls;
    }

    /**
     * Because casting arrays by default would be stupid!
     */
    public static Object unbox(@NotNull Object in, Class<?> typ) {
        if (!(in instanceof Object[])) return in;
        Object out;
        if (in instanceof Number[]) {
            if (in instanceof Float[]) {
                out = new float[((Float[]) in).length];
                for (int i = 0; i < ((Float[]) in).length; i++) {
                    Array.setFloat(out, i, ((Float[]) in)[i]);
                }
            } else if (in instanceof Double[]) {
                out = new double[((Double[]) in).length];
                for (int i = 0; i < ((Double[]) in).length; i++) {
                    Array.setDouble(out, i, ((Double[]) in)[i]);
                }
            } else if (in instanceof Byte[]) {
                out = new byte[((Byte[]) in).length];
                for (int i = 0; i < ((Byte[]) in).length; i++) {
                    Array.setByte(out, i, ((Byte[]) in)[i]);
                }
            } else if (in instanceof Short[]) {
                out = new short[((Short[]) in).length];
                for (int i = 0; i < ((Short[]) in).length; i++) {
                    Array.setShort(out, i, ((Short[]) in)[i]);
                }
            } else if (in instanceof Integer[]) {
                out = new int[((Integer[]) in).length];
                for (int i = 0; i < ((Integer[]) in).length; i++) {
                    Array.setInt(out, i, ((Integer[]) in)[i]);
                }
            } else if (in instanceof Long[]) {
                out = new long[((Long[]) in).length];
                for (int i = 0; i < ((Long[]) in).length; i++) {
                    Array.setLong(out, i, ((Long[]) in)[i]);
                }
            } else throw new IllegalArgumentException("crazy");
        } else if (in instanceof Boolean[]) {
            out = new boolean[((Boolean[]) in).length];
            for (int i = 0; i < ((Boolean[]) in).length; i++) {
                Array.setBoolean(out, i, ((Boolean[]) in)[i]);
            }
        } else if (in instanceof Character[]) {
            out = new char[((Character[]) in).length];
            for (int i = 0; i < ((Character[]) in).length; i++) {
                Array.setChar(out, i, ((Character[]) in)[i]);
            }
        } else out = in;
        return out;
    }

    public static Object unboxFully(Object in, Class<?> target) {
        if (in instanceof List<?>) {
            List<?> list = (List<?>) in;
            if (target == null && list.isEmpty()) throw new IllegalStateException("Cannot unbox empty list to array https://docs.polyfrost.org/oneconfig/config/unbox-empty-list");
            Object[] arr = list.toArray((Object[]) Array.newInstance(list.get(0).getClass(), 0));
            // cast arr to target
            return unbox(arr, target);
        }
        return unbox(in, target);
    }

    private static Field getDeclaredField(Class<?> cls, String name) {
        for (Field f : cls.getDeclaredFields()) {
            if (f.getName().equals(name)) return f;
        }
        for (Class<?> c : cls.getInterfaces()) {
            Field f = getDeclaredField(c, name);
            if (f != null) return f;
        }
        if (cls.getSuperclass() != null) {
            return getDeclaredField(cls.getSuperclass(), name);
        }
        return null;
    }

    public Config convertFromField(Object value) {
        Config cfg = Config.inMemory();
        cfg.add("classType", value.getClass().getName());
        convertInternal(value.getClass(), value, cfg);
        return cfg;
    }

    public static void convertInternal(Class<?> cls, Object value, Config cfg) {
        for (Field f : cls.getDeclaredFields()) {
            if (f.isSynthetic() || Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers()))
                continue;
            f.setAccessible(true);
            try {
                Object o = f.get(value);
                if (o != null) {
                    if (o instanceof List<?>) {
                        List<?> list = (List<?>) o;
                        Object t = list.get(0);
                        if (!(isPrimitiveArray(t) || t instanceof CharSequence || t instanceof Number || t instanceof Boolean || t instanceof Enum || t instanceof Config || t instanceof Object[])) {
                            Object[] out = list.toArray();
                            for (int i = 0; i < out.length; i++) {
                                out[i] = ObjectSerializer.INSTANCE.convertFromField(out[i]);
                            }
                            cfg.add(f.getName(), out);
                        } else cfg.add(f.getName(), o);
                    }
                    if (!(isPrimitiveArray(o) || o instanceof CharSequence || o instanceof Number || o instanceof Boolean || o instanceof Enum || o instanceof Config || o instanceof Object[])) {
                        cfg.add(f.getName(), ObjectSerializer.INSTANCE.convertFromField(o));
                    } else cfg.add(f.getName(), o);
                }
            } catch (IllegalAccessException e) {
                throw new SerializationException("Failed to serialize object", e);
            }
        }
        for (Class<?> c : cls.getInterfaces()) {
            convertInternal(c, value, cfg);
        }
        if (cls.getSuperclass() != null) {
            convertInternal(cls.getSuperclass(), value, cfg);
        }
    }

    private static boolean isPrimitiveArray(Object o) {
        Class<?> cls = o.getClass();
        return cls.isArray() && cls.getComponentType().isPrimitive();
    }

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            Backend.LOGGER.warn("Failed to get unsafe instance, classes without no-args constructors will fail to deserialize!");
            return null;
        }
    }
}
