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

/**
 * Utility class for casting between primitive array types.
 */
public final class ArrayCastUtils {
    private ArrayCastUtils() {
    }

    public static Object unboxNumberArray(Object[] array, Class<?> type) {
        type = WrappingUtils.getUnwrapped(type);
        if (type == int.class) {
            return i2i(array);
        } else if (type == long.class) {
            return l2l(array);
        } else if (type == short.class) {
            return s2s(array);
        } else if (type == byte.class) {
            return b2b(array);
        } else if (type == char.class) {
            return c2c(array);
        } else if (type == float.class) {
            return f2f(array);
        } else if (type == double.class) {
            return d2d(array);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static int[] i2i(Object[] arr) {
        int[] ret = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = ((Number) arr[i]).intValue();
        }
        return ret;
    }

    public static int[] s2i(short[] arr) {
        int[] ret = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }

    public static int[] l2i(long[] arr) {
        int[] ret = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = (int) arr[i];
        }
        return ret;
    }

    public static int[] b2i(byte[] arr) {
        int[] ret = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }

    public static int[] c2i(char[] arr) {
        int[] ret = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }


    public static float[] f2f(Object[] arr) {
        float[] ret = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = ((Number) arr[i]).floatValue();
        }
        return ret;
    }

    public static float[] d2f(double[] arr) {
        float[] ret = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = (float) arr[i];
        }
        return ret;
    }

    public static double[] d2d(Object[] arr) {
        double[] ret = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = ((Number) arr[i]).doubleValue();
        }
        return ret;
    }

    public static double[] f2d(float[] arr) {
        double[] ret = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }

    public static long[] l2l(Object[] arr) {
        long[] ret = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = ((Number) arr[i]).longValue();
        }
        return ret;
    }

    public static short[] s2s(Object[] arr) {
        short[] ret = new short[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = ((Number) arr[i]).shortValue();
        }
        return ret;
    }

    public static byte[] b2b(Object[] arr) {
        byte[] ret = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = ((Number) arr[i]).byteValue();
        }
        return ret;
    }

    public static char[] c2c(Object[] arr) {
        char[] ret = new char[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = (char) arr[i];
        }
        return ret;
    }

    public static long[] i2l(int[] arr) {
        long[] ret = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }

    public static long[] s2l(short[] arr) {
        long[] ret = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }

    public static long[] b2l(byte[] arr) {
        long[] ret = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }

    public static long[] c2l(char[] arr) {
        long[] ret = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i];
        }
        return ret;
    }

}
