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

package org.polyfrost.oneconfig.utils.v1;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.polyfrost.oneconfig.utils.v1.ArrayCastUtils.*;

/**
 * Various utilities for wrapping and unboxing of arrays, lists, etc.
 * see {@link ArrayCastUtils} for some more info.
 */
public class WrappingUtils {
    private static final Map<Class<?>, Class<?>> prim2Wrapper;
    private static final Map<Class<?>, Class<?>> wrapper2Prim;

    static {
        // asm: identity map can be used as class objects are singleton
        Map<Class<?>, Class<?>> m = new IdentityHashMap<>(8);
        m.put(double.class, Double.class);
        m.put(long.class, Long.class);
        m.put(float.class, Float.class);
        m.put(int.class, Integer.class);
        m.put(char.class, Character.class);
        m.put(byte.class, Byte.class);
        m.put(boolean.class, Boolean.class);
        m.put(short.class, Short.class);
        prim2Wrapper = m;
        wrapper2Prim = reverse(m);
    }

    private static <K, V> Map<V, K> reverse(Map<K, V> map) {
        Map<V, K> reversed = new IdentityHashMap<>(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            reversed.put(entry.getValue(), entry.getKey());
        }
        return reversed;
    }

    /**
     * Return the wrapped class for the given primitive class.
     * If this class is not a primitive, it will return the same class.
     */
    public static Class<?> getWrapped(Class<?> cls) {
        return prim2Wrapper.getOrDefault(cls, cls);
    }

    /**
     * Return the primitive class for the given wrapped class.
     * If this class is not a wrapper, it will return the same class.
     */
    public static Class<?> getUnwrapped(Class<?> cls) {
        return wrapper2Prim.getOrDefault(cls, cls);
    }

    /**
     * Method that will attempt to get around the 'quirks' of casting involving primitives in java.
     * <br>
     * This method will do the following (note that these actions are effectively chained where applicable, so a {@code List<Integer>} can become a long[] if needed):
     * <ul>
     *     <li> Convert Number classes to the target type (e.g. Float into Double)</li>
     *     <li> Turn Object[] arrays into their actual types, by inspecting the type of the first element</li>
     *     <li> Turn {@code Collection<?>} into their array counterparts</li>
     *     <li> unbox boxed arrays (e.g. Integer[] into int[])</li>
     *     <li> cast array content to the given type if possible</li>
     *     <li> convert between common primitve array types, e.g. int[] into long[]</li>
     * </ul>
     */
    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    public static <T> T richCast(Object in, Class<T> target) {
        if (in == null) return null;
        Class<?> cls = in.getClass();
        if (cls == target || cls.isInstance(target)) return (T) in;
        if (in instanceof Number) {
            Class<?> cp = getUnwrapped(target);
            if (cp == double.class) return (T) (Double) ((Number) in).doubleValue();
            if (cp == long.class) return (T) (Long) ((Number) in).longValue();
            if (cp == float.class) return (T) (Float) ((Number) in).floatValue();
            if (cp == int.class) return (T) (Integer) ((Number) in).intValue();
            if (cp == short.class) return (T) (Short) ((Number) in).shortValue();
            if (cp == byte.class) return (T) (Byte) ((Number) in).byteValue();
        }
        if (target.isArray()) {
            Class<?> tType = target.getComponentType();
            Class<?> cType = cls.getComponentType();
            if (cType == null) {
                if (in instanceof Collection) {
                    in = ((Collection<?>) in).toArray();
                    cType = in.getClass().getComponentType();
                } else {
                    throw new IllegalArgumentException("cannot convert non-array/collection to array");
                }
            } else if (Array.getLength(in) == 0) return (T) Array.newInstance(tType, 0);
            if (cType == Object.class) {
                Object[] arr = (Object[]) in;
                if (arr.length > 0) cType = arr[0].getClass();
                Object arr2 = Array.newInstance(cType, arr.length);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(arr, 0, arr2, 0, arr.length);
                in = arr2;
            }
            if (cType == tType) {
                // same type, just return the input
                return (T) in;
            }
            if (tType.isPrimitive()) {
                if (!cType.isPrimitive()) {
                    if (Number.class.isAssignableFrom(getWrapped(tType))) {
                        return (T) unboxNumberArray((Object[]) in, tType);
                    }
                    Object[] arr = (Object[]) in;
                    Object a = Array.newInstance(tType, arr.length);
                    for (int i = 0; i < arr.length; i++) {
                        Array.set(a, i, arr[i]);
                    }
                    return (T) a;
                }
                if (tType == int.class) {
                    // long, short, byte, char -> int
                    if (cType == long.class) return (T) l2i((long[]) in); // LOSSY //
                    if (cType == short.class) return (T) s2i((short[]) in);
                    if (cType == byte.class) return (T) b2i((byte[]) in);
                    if (cType == char.class) return (T) c2i((char[]) in);
                } else if (tType == double.class) {
                    // float -> double
                    if (cType == float.class) return (T) f2d((float[]) in);
                } else if (tType == float.class) {
                    // double -> float
                    if (cType == double.class) return (T) d2f((double[]) in); // LOSSY //
                } else if (tType == long.class) {
                    // int, short, byte, char -> long
                    if (cType == int.class) return (T) i2l((int[]) in);
                    if (cType == short.class) return (T) s2l((short[]) in);
                    if (cType == byte.class) return (T) b2l((byte[]) in);
                    if (cType == char.class) return (T) c2l((char[]) in);
                }
                throw new ClassCastException("inconvertible array types: " + cType.getSimpleName() + " -> " + tType.getSimpleName());
            }
        }
        return (T) in;
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
}
