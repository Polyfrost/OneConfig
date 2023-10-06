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

package org.polyfrost.oneconfig.api.config.util;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.adapter.Adapter;
import org.polyfrost.oneconfig.api.config.adapter.impl.ColorAdapter;
import org.polyfrost.oneconfig.api.config.exceptions.SerializationException;
import org.polyfrost.oneconfig.utils.MHUtils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.polyfrost.oneconfig.api.config.Tree.LOGGER;

public class ObjectSerializer {
    public static final ObjectSerializer INSTANCE = new ObjectSerializer();
    private final Set<Adapter<?>> adapters = new HashSet<>();

    static {
        INSTANCE.registerTypeAdapter(new ColorAdapter());
    }

    public void registerTypeAdapter(Adapter<?> adapter) {
        if (!this.adapters.add(adapter)) {
            LOGGER.warn("Failed to register type adapter: An adapter for type {} is already registered", adapter.getTargetClass());
        }
    }

    public void registerTypeAdapter(Adapter<?>... adapters) {
        for (Adapter<?> a : adapters) {
            registerTypeAdapter(a);
        }
    }

    @SuppressWarnings("unchecked")
    public Object serialize(Object in) {
        if (in == null) return null;
        Class<?> cls = in.getClass();
        if (isSimpleObject(in)) {
            return in;
        }
        if (cls.isArray()) {
            in = Arrays.asList((Object[]) box(in));
        }
        if (cls.isEnum()) {
            Map<String, Object> enumMap = new HashMap<>(2);
            enumMap.put("classType", cls.getName());
            enumMap.put("value", ((Enum<?>) in).name());
            return enumMap;
        }
        if (in instanceof Collection) {
            Collection<?> c = (Collection<?>) in;
            if (c.isEmpty()) return new Object[]{};
            Object first = c.iterator().next();
            if (isSimpleObject(first)) {
                return c;
            }
            return c.stream().map(this::serialize).collect(Collectors.toList());
        }
        if (in instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) in;
            if (m.isEmpty()) return Collections.emptyMap();
            Iterator<? extends Map.Entry<?, ?>> iter = m.entrySet().iterator();
            Map.Entry<?, ?> first = iter.next();
            if ((isSimpleObject(first.getKey()) || isPrimitiveArray(first.getKey().getClass())) && (isSimpleObject(first.getValue()) || isPrimitiveArray(first.getValue().getClass()))) {
                return m;
            }
            Map<Object, Object> out = new HashMap<>();
            out.put(serialize(first.getKey()), serialize(first.getValue()));
            while (iter.hasNext()) {
                Map.Entry<?, ?> e = iter.next();
                out.put(serialize(e.getKey()), serialize(e.getValue()));
            }
            return out;
        }

        for (Adapter<?> a : adapters) {
            if (a.getTargetClass().equals(cls)) {
                Adapter<Object> ad = (Adapter<Object>) a;
                Object out = ad.serialize(in);
                boolean isMap = out instanceof Map;
                // ClassCastException when the Map does not have String keys (the doc explains required types)
                Map<String, Object> outMap = isMap ? (Map<String, Object>) out : new HashMap<>(2);
                if (!isMap) {
                    outMap.put("value", out);
                } else {
                    if (outMap.get("classType") != null) throw new IllegalArgumentException("Failed to serialize " + out + ": 'classType' is a reserved key!");
                    if (outMap.get("value") != null) throw new IllegalArgumentException("Failed to serialize " + out + ": 'value' is a reserved key!");
                }
                outMap.put("classType", ad.getTargetClass().getName());
                return outMap;
            }
        }
        // we have a complex type with no adapter, amazing.
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("classType", cls.getName());
        _serialize(cls, in, cfg);
        return cfg;
    }

    /**
     * Simple object serializer. Serializes, the object and its parents' classes non-synthetic, non-transient and non-static fields.
     */
    private void _serialize(Class<?> cls, Object value, Map<String, Object> cfg) {
        for (Field f : cls.getDeclaredFields()) {
            if (f.isSynthetic() || Modifier.isTransient(f.getModifiers()) || Modifier.isStatic(f.getModifiers()))
                continue;
            try {
                Object o = MHUtils.getFieldGetter(f, value).invoke();
                // skip self references
                if (o == value) continue;
                if (o != null) {
                    if (!isSimpleObject(o)) {
                        cfg.put(f.getName(), serialize(o));
                    } else cfg.put(f.getName(), o);
                }
            } catch (Throwable e) {
                throw new SerializationException("Failed to serialize object " + value, e);
            }
        }
        for (Class<?> c : cls.getInterfaces()) {
            _serialize(c, value, cfg);
        }
        if (cls.getSuperclass() != null) {
            _serialize(cls.getSuperclass(), value, cfg);
        }
    }

    @SuppressWarnings("unchecked")
    public Object deserialize(Map<String, Object> in) {
        if (in == null) return null;
        String clsName = (String) in.get("classType");
        if (clsName == null) {
            System.err.println("Offending map: " + in);
            mapToString(in);
            throw new SerializationException("Cannot deserialize object: missing classType field!");
        }
        for (Adapter<?> a : adapters) {
            if (a.getTargetClass().getName().equals(clsName)) {
                Adapter<Object> ad = (Adapter<Object>) a;
                Object value = in.get("value");
                if (value == null) {
                    value = in;
                }
                return ad.deserialize(value);
            }
        }
        Class<?> cls;
        try {
            cls = Class.forName(clsName);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize object: Target class not found", e);
        }
        return _deserialize(in, cls);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object _deserialize(Map<String, Object> in, Class<?> cls) {
        Object o;
        if (cls.isArray()) {
            o = new ArrayList<>();
        } else o = MHUtils.instantiate(cls, true);
        if (o == null) {
            throw new SerializationException("Failed to deserialize object: Failed to instantiate " + cls.getName());
        }
        for (Map.Entry<String, Object> e : in.entrySet()) {
            if (e.getKey().equals("classType")) continue;
            try {
                Field f = getDeclaredField(o.getClass(), e.getKey());
                if (f == null) continue;
                MethodHandle setter = MHUtils.getFieldSetter(f, o);
                if (setter == null) continue;
                if (f.getType().isEnum()) {
                    setter.invoke(Enum.valueOf((Class) f.getType(), (String) e.getValue()));
                } else if (e.getValue() instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) e.getValue();
                    setter.invoke(_deserialize(m, m.getClass()));
                } else {
                    Object out = unbox(e.getValue(), f.getType());
                    setter.invoke(out);
                }
            } catch (Throwable ex) {
                throw new SerializationException("Failed to deserialize object", ex);
            }
        }
        return o;
    }

    public static Field getDeclaredField(Class<?> cls, String name) {
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

    public static Object unbox(@NotNull Object in, Class<?> target) {
        if (target == null) return in;
        if (in instanceof List && target.isArray()) {
            List<?> list = (List<?>) in;
            if (list.isEmpty()) throw new IllegalStateException("Cannot unbox empty list to array https://docs.polyfrost.org/oneconfig/config/unbox-empty-list");
            if (target.getComponentType().isPrimitive()) {
                Class<?> cType = target.getComponentType();
                Object array = Array.newInstance(cType, list.size());
                for (int i = 0; i < list.size(); i++) {
                    Array.set(array, i, unbox(list.get(i), cType));
                }
                return array;
            }
        }
        if (in instanceof Number) {
            Number n = (Number) in;
            if (target == float.class || target == Float.class) return n.floatValue();
            if (target == double.class || target == Double.class) return n.doubleValue();
            if (target == byte.class || target == Byte.class) return n.byteValue();
            if (target == short.class || target == Short.class) return n.shortValue();
            if (target == int.class || target == Integer.class) return n.intValue();
            if (target == long.class || target == Long.class) return n.longValue();
        }
        return in;
    }

    public static Class<?> getPrimitiveWrapper(Class<?> prim) {
        if (prim == boolean.class) return Boolean.class;
        if (prim == int.class) return Integer.class;
        if (prim == float.class) return Float.class;
        if (prim == short.class) return Short.class;
        if (prim == long.class) return Long.class;
        if (prim == byte.class) return Byte.class;
        if (prim == double.class) return Double.class;
        if (prim == char.class) return Character.class;
        return prim;
    }

    public static Object box(Object in) {
        Class<?> type = in.getClass().getComponentType();
        if (type == null || !type.isPrimitive()) return in;
        int len = Array.getLength(in);
        Object out = Array.newInstance(getPrimitiveWrapper(type), len);
        for (int i = 0; i < len; i++) {
            Array.set(out, i, Array.get(in, i));
        }
        return out;
    }


    public static void mapToString(Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            System.err.println("  " + e.getKey() + ": " + e.getValue());
        }
    }


    public static boolean isSimpleObject(Object o) {
        if (o == null) return true;
        return isPrimitiveWrapper(o) || o instanceof CharSequence;
    }

    public static boolean isPrimitiveWrapper(Object o) {
        return o instanceof Number || o instanceof Boolean || o instanceof Character;
    }

    public static boolean isPrimitiveArray(Class<?> cls) {
        return cls.isArray() && cls.getComponentType().isPrimitive();
    }
}
