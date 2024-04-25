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

import java.util.Iterator;

/**
 * Utility class for casting arrays.
 * <br>
 * There are three types of methods for each primitive type in Java:
 * <ul>
 *     <li>*popa: Converts an Object array to a primitive array.</li>
 *     <li>*popi: Converts an Iterator to a primitive array.</li>
 *     <li>*wrap: Converts a primitive array to an Object array.</li>
 * </ul>
 */
public final class ArrayCastUtils {
    private ArrayCastUtils() {}
    
    public static int[] ipopa(Object[] in) {
        int[] out = new int[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (int) in[i];
        }
        return out;
    }

    public static int[] ipopi(Iterator<?> in, int sz, int first) {
        int[] out = new int[sz];
        out[0] = first;
        for (int i = 1; i < sz; i++) {
            out[i] = (Integer) in.next();
        }
        return out;
    }

    public static Integer[] iwrap(int[] in) {
        Integer[] out = new Integer[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }
        return out;
    }

    public static long[] lpopa(Object[] in) {
        long[] out = new long[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (long) in[i];
        }
        return out;
    }

    public static long[] lpopi(Iterator<?> in, int sz, long first) {
        long[] out = new long[sz];
        out[0] = first;
        for (int i = 1; i < sz; i++) {
            out[i] = (Long) in.next();
        }
        return out;
    }

    public static Long[] lwrap(long[] in) {
        Long[] out = new Long[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }
        return out;
    }

    public static float[] fpopa(Object[] in) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (float) in[i];
        }
        return out;
    }

    public static float[] fpopi(Iterator<?> in, int sz, float first) {
        float[] out = new float[sz];
        out[0] = first;
        for (int i = 1; i < sz; i++) {
            out[i] = (Float) in.next();
        }
        return out;
    }

    public static Float[] fwrap(float[] in) {
        Float[] out = new Float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }
        return out;
    }

    public static double[] dpopa(Object[] in) {
        double[] out = new double[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (double) in[i];
        }
        return out;
    }

    public static double[] dpopi(Iterator<?> in, int sz, double first) {
        double[] out = new double[sz];
        out[0] = first;
        for (int i = 1; i < sz; i++) {
            out[i] = (Double) in.next();
        }
        return out;
    }

    public static Double[] dwrap(double[] in) {
        Double[] out = new Double[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }
        return out;
    }

    public static char[] cpopa(Object[] in) {
        char[] out = new char[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (char) in[i];
        }
        return out;
    }

    public static char[] cpopi(Iterator<?> in, int sz, char first) {
        char[] out = new char[sz];
        out[0] = first;
        for (int i = 1; i < sz; i++) {
            out[i] = (Character) in.next();
        }
        return out;
    }

    public static Character[] cwrap(char[] in) {
        Character[] out = new Character[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }
        return out;
    }

    public static byte[] bpopa(Object[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (byte) in[i];
        }
        return out;
    }

    public static byte[] bpopi(Iterator<?> in, int sz, byte first) {
        byte[] out = new byte[sz];
        out[0] = first;
        for (int i = 1; i < sz; i++) {
            out[i] = (Byte) in.next();
        }
        return out;
    }

    public static Byte[] bwrap(byte[] in) {
        Byte[] out = new Byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }
        return out;
    }

    public static boolean[] zpopa(Object[] in) {
        boolean[] out = new boolean[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (boolean) in[i];
        }
        return out;
    }

    public static boolean[] zpopi(Iterator<?> in, int sz, boolean first) {
        boolean[] out = new boolean[sz];
        out[0] = first;
        for (int i = 1; i < sz; i++) {
            out[i] = (Boolean) in.next();
        }
        return out;
    }

    public static Boolean[] zwrap(boolean[] in) {
        Boolean[] out = new Boolean[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }
        return out;
    }

    public static short[] spopa(Object[] in) {
        short[] out = new short[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (short) in[i];
        }
        return out;
    }

    public static short[] spopi(Iterator<?> in, int sz, short first) {
        short[] out = new short[sz];
        out[0] = first;
        for (int i = 1; i < sz; i++) {
            out[i] = (Short) in.next();
        }
        return out;
    }

    public static Short[] swrap(short[] in) {
        Short[] out = new Short[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }
        return out;
    }
}
