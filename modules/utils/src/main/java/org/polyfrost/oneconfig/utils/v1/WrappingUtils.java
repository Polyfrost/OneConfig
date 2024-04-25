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

import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
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
     * Method, that will take the given Object[] or Collection<?> and return the corresponding primitive array.
     * If the given object is not an array or collection, it will return the same object.
     * <br><b>Note that if the input is a Collection and it is empty, an Object[0] will be returned.</b>
     */
    public static Object unbox(Object in) {
        if (in instanceof Object[]) {
            Class<?> cls = getUnwrapped(in.getClass().getComponentType());
            Object[] arr = (Object[]) in;
            if (cls.isPrimitive()) {
                if (cls == int.class) return ipopa(arr);
                if (cls == boolean.class) return zpopa(arr);
                if (cls == float.class) return fpopa(arr);
                if (cls == long.class) return lpopa(arr);
                if (cls == double.class) return dpopa(arr);
                if (cls == char.class) return cpopa(arr);
                if (cls == byte.class) return bpopa(arr);
                if (cls == short.class) return spopa(arr);
            }
            return in;
        }
        if (in instanceof Collection) {
            Collection<?> coll = (Collection<?>) in;
            if (coll.isEmpty()) {
                return new Object[0];
            }
            Iterator<?> it = coll.iterator();
            Object f = it.next();
            Class<?> cls = getUnwrapped(f.getClass());
            if (!cls.isPrimitive()) return in;
            int size = coll.size();
            if (cls == int.class) return ipopi(it, size, (int) f);
            if (cls == boolean.class) return zpopi(it, size, (boolean) f);
            if (cls == float.class) return fpopi(it, size, (float) f);
            if (cls == long.class) return lpopi(it, size, (long) f);
            if (cls == double.class) return dpopi(it, size, (double) f);
            if (cls == char.class) return cpopi(it, size, (char) f);
            if (cls == byte.class) return bpopi(it, size, (byte) f);
            if (cls == short.class) return spopi(it, size, (short) f);
        }
        return in;
    }

    /**
     * Take the given primitive array and return the corresponding Object[].
     *
     * @throws IllegalArgumentException if the given object is not an array
     */
    public static Object[] box(Object in) {
        Class<?> type = in.getClass().getComponentType();
        if (type == null) throw new IllegalArgumentException("must be array type");
        if (!type.isPrimitive()) return (Object[]) in;
        if (type == int.class) return iwrap((int[]) in);
        if (type == boolean.class) return zwrap((boolean[]) in);
        if (type == float.class) return fwrap((float[]) in);
        if (type == long.class) return lwrap((long[]) in);
        if (type == double.class) return dwrap((double[]) in);
        if (type == char.class) return cwrap((char[]) in);
        if (type == byte.class) return bwrap((byte[]) in);
        if (type == short.class) return swrap((short[]) in);
        throw new InternalError("wow, a void[]? congratulations");
    }

    /**
     * Take the given primitive array and return a List of the corresponding wrapper objects.
     *
     * @throws IllegalArgumentException if the given object is not an array
     */
    public static Collection<?> boxToList(Object in) {
        return Arrays.asList(box(in));
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
