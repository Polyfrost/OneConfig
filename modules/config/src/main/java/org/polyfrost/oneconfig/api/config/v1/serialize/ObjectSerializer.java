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
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.v1.serialize;

import static org.polyfrost.oneconfig.utils.v1.WrappingUtils.*;

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
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.polyfrost.oneconfig.api.config.v1.exceptions.SerializationException;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl.ColorAdapter;
import org.polyfrost.oneconfig.utils.v1.MHUtils;


public class ObjectSerializer {
    public static final ObjectSerializer INSTANCE = new ObjectSerializer();
    /**
     * Modifier.SYNTHETIC | Modifier.TRANSIENT | Modifier.STATIC
     */
    public static final int FIELD_SKIP_MODIFIERS = Modifier.STATIC | Modifier.TRANSIENT | 0x00001000;
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Config");

    static {
        INSTANCE.registerTypeAdapter(new ColorAdapter());
    }

    private final HashMap<Class<?>, Adapter<Object, Object>> adapters = new HashMap<>();

    private ObjectSerializer() {
    }

    public static boolean isSerializable(Object in) {
        if (in == null) return true;
        Class<?> cls = in.getClass();
        // these classes are never serializable.
        return !Runnable.class.isAssignableFrom(cls) && !Function.class.getPackage().equals(cls.getPackage());
    }


    /**
     * Return a stream of all the fields in this object, including the fields of its superclasses.
     */
    public static Stream<Field> fieldStream(Object o) {
        Stream<Field> fields = Arrays.stream(o.getClass().getDeclaredFields());
        Class<?> superClass = o.getClass().getSuperclass();
        return superClass == null ? fields : Stream.concat(fields, Arrays.stream(superClass.getDeclaredFields()));
    }

    private static void stderrMap(Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            LOGGER.error("  {}: {}", e.getKey(), e.getValue());
        }
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

    public Object serialize(Object in) {
        return serialize(in, false, false);
    }

    public Object serialize(Object in, boolean useLists) {
        return serialize(in, useLists, false);
    }

    @SuppressWarnings("unchecked")
    public <T> Adapter<T, ?> getAdapter(Class<T> cls) {
        Class<?> cl = cls;
        Adapter<?, ?> ad = adapters.get(cl);
        while(ad == null && cl.getSuperclass() != null) {
            cl = cl.getSuperclass();
            ad = adapters.get(cl);
        }
        return (Adapter<T, ?>) ad;
    }

    /**
     * Convert the given object into a series of simple values that should be supported by most backend serializers.
     *
     * @param in        the object
     * @param useLists  if your serializer uses lists instead of arrays for collections of items
     * @param boxArrays if your serializer requires boxed primitive arrays
     * @return the source object, in a simple form
     * @see Adapter#serialize(Object)
     */
    @SuppressWarnings("unchecked")
    public Object serialize(Object in, boolean useLists, boolean boxArrays) {
        if (in == null) return null;
        Class<?> cls = in.getClass();
        if (!isSerializable(in)) return null;

        // check 1: return Number, CharSequence or Boolean
        if (isSimpleObject(in)) {
            return in;
        }

        // check 2: pack up Enums and return those
        if (cls.isEnum()) {
            Map<String, Object> enumMap = new HashMap<>(2, 1f);
            enumMap.put("class", cls.getName());
            enumMap.put("value", ((Enum<?>) in).name());
            return enumMap;
        }

        // check 3: array
        if (cls.isArray()) {
            return _serializeArray(in, cls.getComponentType(), useLists, boxArrays);
        }
        // check 4: collection
        if (in instanceof Collection) {
            return _serializeCollection((Collection<?>) in, useLists, boxArrays);
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
                out.put(first.getKey(), serialize(first.getValue(), useLists, boxArrays));
                while (iter.hasNext()) {
                    Map.Entry<?, ?> e = iter.next();
                    out.put(e.getKey(), serialize(e.getValue(), useLists, boxArrays));
                }
            } else {
                out.put(serialize(first.getKey(), useLists, boxArrays), serialize(first.getValue(), useLists, boxArrays));
                while (iter.hasNext()) {
                    Map.Entry<?, ?> e = iter.next();
                    out.put(serialize(e.getKey(), useLists, boxArrays), serialize(e.getValue(), useLists, boxArrays));
                }
            }
            return out;
        }

        // check 6: complex object, do we have an adapter available?
        Adapter<Object, Object> ad = (Adapter<Object, Object>) getAdapter(cls);
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
        _serialize(cls, in, cfg, useLists, boxArrays);
        return cfg;
    }

    private Object _serializeArray(Object in, Class<?> cType, boolean useLists, boolean boxArrays) {
        if (cType.isPrimitive()) {
            if (boxArrays || useLists) {
                Object a = Array.newInstance(getWrapped(cType), Array.getLength(in));
                for (int i = 0; i < Array.getLength(in); i++) {
                    Array.set(a, i, Array.get(in, i));
                }
                return useLists ? Arrays.asList((Object[]) a) : a;
            } else return in;
        }
        Object[] arr = (Object[]) in;
        if (isSimpleClass(cType)) {
            return useLists ? Arrays.asList(arr) : arr;
        }
        if (useLists) {
            List<Object> out = new ArrayList<>(arr.length);
            for (Object o : arr) {
                out.add(serialize(o, true, boxArrays));
            }
            return out;
        } else {
            Object[] out = new Object[arr.length];
            for (int i = 0; i < arr.length; i++) {
                out[i] = serialize(arr[i], false, boxArrays);
            }
            return out;
        }
    }

    private Object _serializeCollection(Collection<?> c, boolean useLists, boolean boxArrays) {
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
                out.add(serialize(iter.next(), true, boxArrays));
            }
            return out;
        } else {
            Object[] out = new Object[c.size()];
            out[0] = first;
            int i = 1;
            while (iter.hasNext()) {
                out[i] = serialize(iter.next(), false, boxArrays);
                i++;
            }
            return out;
        }
    }

    /**
     * Simple object serializer. Serializes, the object and its parents' classes non-synthetic, non-transient and non-static fields.
     */
    private void _serialize(Class<?> cls, Object value, Map<String, Object> cfg, boolean useLists, boolean boxArrays) {
        for (Field f : cls.getDeclaredFields()) {
            if ((f.getModifiers() & FIELD_SKIP_MODIFIERS) != 0) continue;
            try {
                Object o = MHUtils.setAccessible(f).get(value);
                // skip self references
                if (o == value) continue;
                if (o == null) continue;
                if (o instanceof Number && ((Number) o).doubleValue() == 0.0) continue;
                cfg.put(f.getName(), serialize(o, useLists, boxArrays));
            } catch (Throwable e) {
                throw new SerializationException("Failed to serialize object " + value + ": no detail message (potential field access issue, try making " + f + " public?)", e);
            }
        }
        if (cls.getSuperclass() != null) {
            _serialize(cls.getSuperclass(), value, cfg, useLists, boxArrays);
        }
    }

    /**
     * Deserialize the given complex object map. The map must contain the class field.
     *
     * @param in the map
     * @return the completed object
     */
    @SuppressWarnings("unchecked")
    public Object deserialize(Map<String, Object> in) {
        if (in == null) return null;
        String clsName = (String) in.get("class");
        if (clsName == null) {
            LOGGER.error("Offending map: {}", in);
            stderrMap(in);
            throw new SerializationException("Cannot deserialize object: missing class field (internal error)!");
        }
        Class<?> cls;
        try {
            cls = Class.forName(clsName, true, ObjectSerializer.class.getClassLoader());
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize object: Class " + clsName + " not found, potential classloading issue?", e);
        }
        Adapter<Object, Object> ad = (Adapter<Object, Object>) getAdapter(cls);
        if (ad != null) {
            Object value = in.get("value");
            if (value == null) {
                value = in;
            }
            Object out = ad.deserialize(richCast(value, ad.getOutputClass()));
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
        if (cls.isEnum()) {
            return Enum.valueOf((Class) cls, (String) in.get("value"));
        }
        Object o = MHUtils.instantiate(cls, true).getOrNull();
        if (o == null) {
            throw new SerializationException("Failed to deserialize object: Failed to instantiate " + cls.getName());
        }
        // asm: it is much faster to iterate over every field and use map to get the potential serialized object
        // than the other way around.
        fieldStream(o).filter(f -> (f.getModifiers() & FIELD_SKIP_MODIFIERS) == 0).forEach(f -> {
            Object value = in.get(f.getName());
            if (value == null) return;
            try {
                MHUtils.setAccessible(f);
                if (value instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) value;
                    f.set(o, _deserialize(m, f.getType()));
                } else {
                    Object out = richCast(value, f.getType());
                    f.set(o, out);
                }
            } catch (Throwable e) {
                throw new SerializationException("Failed to deserialize " + cls.getName() + ": no detail message (potential field access issue?)", e);
            }
        });
        return o;
    }

}
