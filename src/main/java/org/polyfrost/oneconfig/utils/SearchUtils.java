/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Based on <a href="https://www.baeldung.com/java-levenshtein-distance">https://www.baeldung.com/java-levenshtein-distance</a>
 */
public class SearchUtils {

    public static boolean isSimilar(String s1, String s2) {
        return isSimilar(s1, s2, 2);
    }

    public static boolean isSimilar(String s1, String s2, int searchDistance) {
        s1 = s1.toLowerCase(Locale.ENGLISH);
        s2 = s2.toLowerCase(Locale.ENGLISH);
        if (s1.length() <= searchDistance) {
            return s1.contains(s2);
        }
        boolean similar = false;
        for (String a : StringUtils.split(s1)) {
            similar = a.contains(s2) || StringUtils.getLevenshteinDistance(a, s2) <= searchDistance;
            if (similar) break;
        }
        return similar || s1.contains(s2) || StringUtils.getLevenshteinDistance(s1, s2) <= searchDistance;
    }
}
