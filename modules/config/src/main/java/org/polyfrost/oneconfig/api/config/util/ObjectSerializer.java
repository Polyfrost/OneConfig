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
import org.jetbrains.annotations.Unmodifiable;
import org.polyfrost.oneconfig.api.config.serialize.adapter.Adapter;
import org.polyfrost.oneconfig.api.config.serialize.adapter.impl.ColorAdapter;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.polyfrost.oneconfig.api.config.Tree.LOGGER;

public class ObjectSerializer {
    public static final ObjectSerializer INSTANCE = new ObjectSerializer();
    @Unmodifiable
    public static final Map<Class<?>, Class<?>> primitiveWrappers;
    @Unmodifiable
    public static final Map<Class<?>, Function<Number, Number>> numberSuppliers;
    /**
     * Modifier.SYNTHETIC | Modifier.TRANSIENT | Modifier.STATIC
     */
    public static final int FIELD_SKIP_MODIFIERS = Modifier.STATIC | Modifier.TRANSIENT | 0x00001000;
    private final HashMap<Class<?>, Adapter<Object, Object>> adapters = new HashMap<>();

    static {
        Map<Class<?>, Class<?>> pw = new HashMap<>(8, 1f);
        pw.put(boolean.class, Boolean.class);
        pw.put(int.class, Integer.class);
        pw.put(float.class, Float.class);
        pw.put(short.class, Short.class);
        pw.put(long.class, Long.class);
        pw.put(byte.class, Byte.class);
        pw.put(double.class, Double.class);
        pw.put(char.class, Character.class);
        primitiveWrappers = Collections.unmodifiableMap(pw);

        Map<Class<?>, Function<Number, Number>> ns = new HashMap<>(8, 1f);
        ns.put(Integer.class, Number::intValue);
        ns.put(Float.class, Number::floatValue);
        ns.put(Short.class, Number::shortValue);
        ns.put(Long.class, Number::longValue);
        ns.put(Byte.class, Number::byteValue);
        ns.put(Double.class, Number::doubleValue);
        numberSuppliers = Collections.unmodifiableMap(ns);
    }

    private ObjectSerializer() {
    }

    static {
        INSTANCE.registerTypeAdapter(new ColorAdapter());
    }

    @SuppressWarnings("unchecked")
    public void registerTypeAdapter(Adapter<?, ?> adapter) {
        if (this.adapters.put(adapter.getTargetClass(), (Adapter<Object, Object>) adapter) != null) {
            LOGGER.warn("Failed to register type adapter: An adapter for type {} is already registered", adapter.getTargetClass());
        }
    }

    public void registerTypeAdapter(Adapter<?, ?>... adapters) {
        for (Adapter<?, ?> a : adapters) {
            registerTypeAdapter(a);
        }
    }

    public void unregisterTypeAdapter(Adapter<?, ?> adapter) {
        if (this.adapters.remove(adapter.getTargetClass()) == null) {
            LOGGER.warn("Failed to remove type adapter for {}: Not registered/already removed", adapter.getTargetClass());
        }
    }

    /**
     * Convert the given object into a series of simple values that should be supported by most backend serializers.
     *
     * @param in       the object
     * @param useLists if your serializer uses lists instead of arrays for collections of items
     * @return the source object, in a simple form
     * @see Adapter#serialize(Object)
     */
    @SuppressWarnings("unchecked")
    public Object serialize(Object in, boolean useLists) {
        if (in == null) return null;
        Class<?> cls = in.getClass();

        // check 1: return Number, CharSequence or Boolean
        if (isSimpleObject(in)) {
            return in;
        }

        // check 2: box up Enums and return those
        if (cls.isEnum()) {
            Map<String, Object> enumMap = new HashMap<>(2, 1f);
            enumMap.put("class", cls.getName());
            enumMap.put("value", ((Enum<?>) in).name());
            return enumMap;
        }

        // check 3: array
        if (cls.isArray()) {
            return _serializeArray(in, cls.getComponentType(), useLists);
        }
        // check 4: collection
        if (in instanceof Collection) {
            return _serializeCollection((Collection<?>) in, useLists);
        }

        // check 5: maps
        if (in instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) in;
            if (m.isEmpty()) return Collections.emptyMap();
            Iterator<? extends Map.Entry<?, ?>> iter = m.entrySet().iterator();
            Map.Entry<?, ?> first = iter.next();
            boolean keysSimple = isSimpleObject(first.getKey());
            if (keysSimple && isSimpleObject(first.getValue())) {
                return m;
            }
            Map<Object, Object> out = new HashMap<>(m.size(), 1f);
            if (keysSimple) {
                out.put(first.getKey(), serialize(first.getValue(), useLists));
                while (iter.hasNext()) {
                    Map.Entry<?, ?> e = iter.next();
                    out.put(e.getKey(), serialize(e.getValue(), useLists));
                }
            } else {
                out.put(serialize(first.getKey(), useLists), serialize(first.getValue(), useLists));
                while (iter.hasNext()) {
                    Map.Entry<?, ?> e = iter.next();
                    out.put(serialize(e.getKey(), useLists), serialize(e.getValue(), useLists));
                }
            }
            return out;
        }

        // check 6: complex object, do we have an adapter available?
        Adapter<Object, Object> ad = adapters.get(cls);
        if (ad != null) {
            Object out = ad.serialize(in);
            if (out == null) throw new SerializationException("Failed to serialize " + in + ": adapter for it (" + cls.getName() + ") returned null");
            boolean isMap = out instanceof Map;
            // ClassCastException when the Map does not have String keys (the doc explains required types)
            Map<String, Object> outMap = isMap ? (Map<String, Object>) out : new HashMap<>(2, 1f);
            if (!isMap) {
                outMap.put("value", out);
            } else {
                if (outMap.get("class") != null) throw new IllegalArgumentException("Failed to serialize " + out + ": 'class' is a reserved key!");
                if (outMap.get("value") != null) throw new IllegalArgumentException("Failed to serialize " + out + ": 'value' is a reserved key!");
            }
            outMap.put("class", cls.getName());
            return outMap;
        }

        // we have a complex type with no adapter, amazing.
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("class", cls.getName());
        _serialize(cls, in, cfg, useLists);
        return cfg;
    }

    private Object _serializeArray(Object in, Class<?> cType, boolean useLists) {
        // primitive array, just box
        if (!(in instanceof Object[])) {
            Object[] out = (Object[]) box(in);
            return useLists ? Arrays.asList(out) : out;
        }
        Object[] arr = (Object[]) in;
        if (isSimpleClass(cType)) {
            return useLists ? Arrays.asList(arr) : arr;
        }
        if (useLists) {
            List<Object> out = new ArrayList<>(arr.length);
            for (Object o : arr) {
                out.add(serialize(o, true));
            }
            return out;
        } else {
            Object[] out = new Object[arr.length];
            for (int i = 0; i < arr.length; i++) {
                out[i] = serialize(arr[i], false);
            }
            return out;
        }
    }

    private Object _serializeCollection(Collection<?> c, boolean useLists) {
        if (c.isEmpty()) return useLists ? c : new Object[0];
        Iterator<?> iter = c.iterator();
        Object first = iter.next();
        if (isSimpleObject(first)) {
            return useLists ? c : c.toArray();
        }
        if (useLists) {
            List<Object> out = new ArrayList<>(c.size());
            out.add(first);
            while (iter.hasNext()) {
                out.add(serialize(iter.next(), true));
            }
            return out;
        } else {
            Object[] out = new Object[c.size()];
            out[0] = first;
            int i = 1;
            while (iter.hasNext()) {
                out[i] = serialize(iter.next(), false);
                i++;
            }
            return out;
        }
    }


    /**
     * Simple object serializer. Serializes, the object and its parents' classes non-synthetic, non-transient and non-static fields.
     */
    private void _serialize(Class<?> cls, Object value, Map<String, Object> cfg, boolean useLists) {
        for (Field f : cls.getDeclaredFields()) {
            if ((f.getModifiers() & FIELD_SKIP_MODIFIERS) != 0) {
                continue;
            }
            try {
                Object o = MHUtils.getFieldGetter(f, value).getOrThrow().invoke();
                // skip self references
                if (o == value) continue;
                if (o == null) continue;
                cfg.put(f.getName(), serialize(o, useLists));
            } catch (Throwable e) {
                throw new SerializationException("Failed to serialize object " + value + ": no detail message (potential field access issue, try making " + f + " public?)", e);
            }
        }
        for (Class<?> c : cls.getInterfaces()) {
            _serialize(c, value, cfg, useLists);
        }
        if (cls.getSuperclass() != null) {
            _serialize(cls.getSuperclass(), value, cfg, useLists);
        }
    }

    /**
     * Deserialize the given complex object map. The map must contain the class field.
     *
     * @param in the map
     * @return the completed object
     */

    public Object deserialize(Map<String, Object> in) {
        if (in == null) return null;
        String clsName = (String) in.get("class");
        if (clsName == null) {
            System.err.println("Offending map: " + in);
            stderrMap(in);
            throw new SerializationException("Cannot deserialize object: missing class field (internal error)!");
        }
        Class<?> cls;
        try {
            cls = Class.forName(clsName, true, ObjectSerializer.class.getClassLoader());
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize object: Class " + clsName + " not found, potential classloading issue?", e);
        }
        Adapter<Object, Object> ad = adapters.get(cls);
        if (ad != null) {
            Object value = in.get("value");
            if (value == null) {
                value = in;
            }
            Object out = ad.deserialize(value);
            if (out == null) throw new SerializationException("Failed to deserialize " + value + ": adapter for it (" + cls + ") returned null");
            return out;
        }
        return _deserialize(in, cls);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object _deserialize(Map<String, Object> in, Class<?> cls) {
        if (cls.isArray()) {
            throw new SerializationException("Failed to deserialize object: Cannot deserialize into an array type " + cls.getName());
        }
        if (cls.isEnum()){
            return Enum.valueOf((Class) cls, (String) in.get("value"));
        }
        Object o = MHUtils.instantiate(cls, true).getOrNull();
        if (o == null) {
            throw new SerializationException("Failed to deserialize object: Failed to instantiate " + cls.getName());
        }
        for (Map.Entry<String, Object> e : in.entrySet()) {
            if (e.getKey().equals("class")) continue;
            try {
                Field f = getDeclaredField(o.getClass(), e.getKey());
                if (f == null) continue;
                MethodHandle setter = MHUtils.getFieldSetter(f, o).getOrNull();
                if (setter == null) continue;
                if (e.getValue() instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) e.getValue();
                    setter.invoke(_deserialize(m, m.getClass()));
                } else {
                    Object out = unbox(e.getValue(), f.getType());
                    setter.invoke(out);
                }
            } catch (Throwable ex) {
                throw new SerializationException("Failed to deserialize " + cls.getName() + ": no detail message (potential field access issue?)", ex);
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
        if (in instanceof Number) return numberSuppliers.get(getWrapped(target)).apply((Number) in);
        if (target.isArray() && in instanceof List) {
            List<?> list = (List<?>) in;
            Class<?> cType = target.getComponentType();
            if (cType.isPrimitive()) {
                Object array = Array.newInstance(cType, list.size());
                for (int i = 0; i < list.size(); i++) {
                    Array.set(array, i, unbox(list.get(i), cType));
                }
                return array;
            }
        }
        return in;
    }

    /**
     * box the specified primitive array into its primitive wrapper, e.g. int[] -> Integer[]
     *
     * @param in an object, null -> null, not a primitive array -> same, or the boxed array.
     */
    public static Object box(Object in) {
        if (in == null) return null;
        Class<?> cType = in.getClass().getComponentType();
        if (cType == null || !cType.isPrimitive()) return in;
        int len = Array.getLength(in);
        Object out = Array.newInstance(getWrapped(cType), len);
        for (int i = 0; i < len; i++) {
            Array.set(out, i, Array.get(in, i));
        }
        return out;
    }

    public static Class<?> getWrapped(Class<?> cls) {
        Class<?> c = primitiveWrappers.get(cls);
        return c == null ? cls : c;
    }


    public static void stderrMap(Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            System.err.println("  " + e.getKey() + ": " + e.getValue());
        }
    }


    /**
     * returns true if: <br>
     * - the object is a primitive wrapper <br>
     * - the object is a CharSequence <br>
     */
    public static boolean isSimpleObject(Object o) {
        if (o == null) return true;
        return isPrimitiveWrapper(o) || o instanceof CharSequence;
    }

    public static boolean isSimpleClass(Class<?> c) {
        if (c == null) return true;
        return Number.class.isAssignableFrom(c) || CharSequence.class.isAssignableFrom(c) || c == Boolean.class || c == Character.class;
    }

    /**
     * returns true if the object is a primitive wrapper
     */
    public static boolean isPrimitiveWrapper(Object o) {
        return o instanceof Number || o instanceof Boolean || o instanceof Character;
    }

    /**
     * returns true if the class is a primitive array type
     */
    public static boolean isPrimitiveArray(Class<?> cls) {
        return cls.isArray() && cls.getComponentType().isPrimitive();
    }
}
