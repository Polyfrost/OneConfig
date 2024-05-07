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

public final class StringUtils {
    public static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private StringUtils() {
    }

    /**
     * generate a random string of length 16, using {@link #ALPHANUMERIC} as the source.
     */
    public static String randomString() {
        return randomString(ALPHANUMERIC, 16);
    }

    /**
     * generate a random string of the given length, using {@link #ALPHANUMERIC} as the source.
     */
    public static String randomString(int length) {
        return randomString(ALPHANUMERIC, length);
    }

    /**
     * generate a random string of the given length, using the given source.
     */
    public static String randomString(String source, int length) {
        if (length < 1) throw new IllegalArgumentException("length must be greater than 0");
        StringBuilder sb = new StringBuilder(length);
        while (length-- != 0) {
            int idx = (int) (Math.random() * source.length());
            sb.append(source.charAt(idx));
        }
        return sb.toString();
    }
}
