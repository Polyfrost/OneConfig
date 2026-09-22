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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.v1.exceptions.SerializationException;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl.ColorAdapter;
import org.polyfrost.oneconfig.utils.v1.MHUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

import static org.polyfrost.oneconfig.utils.v1.WrappingUtils.*;


public class ObjectSerializer {
    public static final ObjectSerializer INSTANCE = new ObjectSerializer();
    /**
     * Modifier.SYNTHETIC | Modifier.TRANSIENT | Modifier.STATIC
     */
    public static final int FIELD_SKIP_MODIFIERS = Modifier.STATIC | Modifier.TRANSIENT | 0x00001000;
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Config");
    private static final IdentityHashMap<Class<?>, Field[]> CLASS_FIELD_CACHE = new IdentityHashMap<>(32);
    private static final IdentityHashMap<Class<?>, Boolean> IMMUTABLE_CACHE = new IdentityHashMap<>(32);
    /**
     * objects currently being reflectively serialized on this thread used to break reference cycles
     * such as Minecraft Item -> Holder.Reference -> Item which would otherwise blow the stack
     */
    private static final ThreadLocal<Set<Object>> IN_PROGRESS = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    static {
        INSTANCE.registerTypeAdapter(new ColorAdapter());
    }

    private final HashMap<Class<?>, Adapter<Object, Object>> adapters = new HashMap<>();

    private ObjectSerializer() {
    }

    public static boolean isSerializable(Object in) {
        if (in == null) return true;
        Class<?> cls = in.getClass();
        // these classes are never serializable
        return !cls.isSynthetic() &&
                !Runnable.class.isAssignableFrom(cls) &&
                !Thread.class.isAssignableFrom(cls) &&
                !ClassLoader.class.isAssignableFrom(cls) &&
                !cls.getName().startsWith("java.util.function");
    }

    public static Field[] getAllFields(Class<?> cls) {
        return CLASS_FIELD_CACHE.computeIfAbsent(cls, k -> {
            List<Field> all = new ArrayList<>();
            Class<?> current = k;
            while (current != null && current != Object.class) {
                for (Field f : current.getDeclaredFields()) {
                    if ((f.getModifiers() & FIELD_SKIP_MODIFIERS) == 0) {
                        all.add(f);
                    }
                }
                current = current.getSuperclass();
            }
            return all.toArray(new Field[0]);
        });
    }

    /**
     * true if no instance field of this class can be assigned meaning the class is a value type
     * <br>
     * Such objects can only be replaced and never mutated in place
     */
    public static boolean isImmutable(Class<?> cls) {
        if (cls == null || cls.isArray()) return false;
        return IMMUTABLE_CACHE.computeIfAbsent(cls, k -> {
            Field[] fields = getAllFields(k);
            if (fields.length == 0) return true;
            for (Field f : fields) {
                if (!Modifier.isFinal(f.getModifiers())) return false;
            }
            return true;
        });
    }

    public static Stream<Field> fieldStream(Class<?> cls) {
        return Arrays.stream(getAllFields(cls));
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
        while (ad == null && cl.getSuperclass() != null) {
            cl = cl.getSuperclass();
            ad = adapters.get(cl);
        }
        return (Adapter<T, ?>) ad;
    }

    /**
     * Convert the given object into a series of simple values that should be supported by most backend serializers
     *
     * @param in        the object
     * @param useLists  if your serializer uses lists instead of arrays for collections of items
     * @param boxArrays if your serializer requires boxed primitive arrays
     * @return the source object in a simple form
     * @see Adapter#serialize(Object)
     */
    @SuppressWarnings("unchecked")
    public Object serialize(Object in, boolean useLists, boolean boxArrays) {
        if (in == null) return null;
        Class<?> cls = in.getClass();
        if (!isSerializable(in)) return null;

        if (isSimpleObject(in)) {
            return in;
        }

        // instanceof rather than cls.isEnum() so constants with a class body are still packed
        if (in instanceof Enum) {
            Map<String, Object> enumMap = new HashMap<>(2, 1f);
            enumMap.put("class", ((Enum<?>) in).getDeclaringClass().getName());
            enumMap.put("value", ((Enum<?>) in).name());
            return enumMap;
        }

        if (cls.isArray()) {
            return _serializeArray(in, cls.getComponentType(), useLists, boxArrays);
        }
        if (in instanceof Collection) {
            return _serializeCollection((Collection<?>) in, useLists, boxArrays);
        }

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

        Set<Object> seen = IN_PROGRESS.get();
        // already serializing this object further up the stack so drop the cyclic reference
        if (!seen.add(in)) return null;
        try {
            Map<String, Object> cfg = new HashMap<>();
            cfg.put("class", cls.getName());
            _serialize(cls, in, cfg, useLists, boxArrays);
            return cfg;
        } finally {
            seen.remove(in);
            if (seen.isEmpty()) IN_PROGRESS.remove();
        }
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
            out.add(serialize(first, true, boxArrays));
            while (iter.hasNext()) {
                out.add(serialize(iter.next(), true, boxArrays));
            }
            return out;
        } else {
            Object[] out = new Object[c.size()];
            out[0] = serialize(first, false, boxArrays);
            int i = 1;
            while (iter.hasNext()) {
                out[i] = serialize(iter.next(), false, boxArrays);
                i++;
            }
            return out;
        }
    }

    /**
     * Simple object serializer
     * <br>
     * Serializes the non-synthetic and non-transient and non-static fields of the object and its parent classes
     */
    private void _serialize(Class<?> cls, Object value, Map<String, Object> cfg, boolean useLists, boolean boxArrays) {
        for (Field f : cls.getDeclaredFields()) {
            if ((f.getModifiers() & FIELD_SKIP_MODIFIERS) != 0) continue;
            try {
                Object o = MHUtils.setAccessible(f).get(value);
                if (o == value) continue;
                if (o == null) continue;
                if (o instanceof Number && ((Number) o).doubleValue() == 0.0) continue;
                Object s = serialize(o, useLists, boxArrays);
                // null means unserializable or a cycle we broke so do not write it out
                if (s == null) continue;
                cfg.put(f.getName(), s);
            } catch (Throwable e) {
                throw new SerializationException("Failed to serialize object " + value + ": no detail message (potential field access issue, try making " + f + " public?)", e);
            }
        }
        if (cls.getSuperclass() != null) {
            _serialize(cls.getSuperclass(), value, cfg, useLists, boxArrays);
        }
    }

    /**
     * Deserialize the given complex object map
     * <br>
     * The map must contain the class field
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
        if (Enum.class.isAssignableFrom(cls)) {
            Class<?> declaring = cls.isEnum() ? cls : cls.getSuperclass();
            return Enum.valueOf((Class) declaring, (String) in.get("value"));
        }
        Object o = MHUtils.instantiate(cls, true).getOrNull();
        if (o == null) {
            throw new SerializationException("Failed to deserialize object: Failed to instantiate " + cls.getName());
        }
        return _deserialize(in, o, (Class) cls);
    }

    @SuppressWarnings("unchecked")
    private <T> T _deserialize(Map<String, Object> in, @NotNull T theObject, Class<T> cls) {
        // asm: much faster to iterate every field and look it up in the map than the other way around
        fieldStream(cls).forEach(f -> {
            Object value = in.get(f.getName());
            if (value == null) return;
            try {
                MHUtils.setAccessible(f);
                if (value instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) value;
                    f.set(theObject, _deserialize(m, f.getType()));
                } else {
                    Object out = richCast(value, f.getType());
                    f.set(theObject, out);
                }
            } catch (Throwable e) {
                String detail = e.getMessage() != null ? e.getMessage() : "no detail message (potential field access issue?)";
                throw new SerializationException("Failed to deserialize " + cls.getName() + ": " + detail, e);
            }
        });
        return theObject;
    }


    @SuppressWarnings("unchecked")
    public static <T> T overwrite(T self, T input) {
        if (self == null) return null;
        if (input == null) return self;
        if (self == input) return self;
        Class<?> cls = self.getClass();
        if (!cls.isAssignableFrom(input.getClass())) {
            throw new IllegalArgumentException("Cannot merge two objects of different classes: " + cls.getName() + " and " + input.getClass().getName());
        }
        if (isImmutable(cls)) {
            // value types cannot be written through so the caller must replace the reference instead
            throw new SerializationException("Cannot overwrite immutable type " + cls.getName() + " in place");
        }
        if (cls.isArray()) {
            // arrays have no declared fields to iterate so copy elements in place to preserve the reference
            int len = java.lang.reflect.Array.getLength(input);
            if (java.lang.reflect.Array.getLength(self) != len) {
                // cannot resize in place so the caller must replace the reference
                throw new SerializationException("Cannot overwrite array of length " + java.lang.reflect.Array.getLength(self) + " with array of length " + len);
            }
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(input, 0, self, 0, len);
            return self;
        }
        if (self instanceof Collection) {
            Collection<Object> target = (Collection<Object>) self;
            Collection<Object> source = (Collection<Object>) input;
            List<Object> copy = new ArrayList<>(source);
            try {
                target.clear();
                target.addAll(copy);
            } catch (UnsupportedOperationException e) {
                throw new SerializationException("Cannot overwrite unmodifiable collection " + cls.getName() + " in place", e);
            }
            return self;
        }
        if (self instanceof Map) {
            Map<Object, Object> target = (Map<Object, Object>) self;
            Map<Object, Object> source = (Map<Object, Object>) input;
            Map<Object, Object> copy = new LinkedHashMap<>(source);
            try {
                target.clear();
                target.putAll(copy);
            } catch (UnsupportedOperationException e) {
                throw new SerializationException("Cannot overwrite unmodifiable map " + cls.getName() + " in place", e);
            }
            return self;
        }
        fieldStream(cls).forEach(f -> {
            try {
                MHUtils.setAccessible(f);
                Object newIn = f.get(input);
                if (newIn == null) return;
                f.set(self, newIn);
            } catch (Throwable e) {
                String detail = e.getMessage() != null ? e.getMessage() : "no detail message (potential field access issue?)";
                throw new SerializationException("Failed to overwrite " + cls.getName() + ": " + detail, e);
            }
        });

        return self;
    }
}
