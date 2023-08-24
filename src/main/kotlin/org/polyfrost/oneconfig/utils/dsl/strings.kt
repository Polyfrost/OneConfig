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

// Retrocompatibility
@file:JvmName("StringUtilsDSLKt")

package org.polyfrost.oneconfig.utils.dsl

import org.polyfrost.oneconfig.utils.StringUtils

fun String.substringSafe(startIndex: Int, endIndex: Int): String {
    return StringUtils.substringSafe(this, startIndex, endIndex)
}

fun String.substringSafe(startIndex: Int): String {
    return StringUtils.substringSafe(this, startIndex)
}

fun String.isValidSequence(startIndex: Int, endIndex: Int): Boolean {
    return StringUtils.isValidSequence(this, startIndex, endIndex)
}

fun String.nullToEmpty(): String {
    return StringUtils.nullToEmpty(this)
}

fun String.addStringAt(index: Int, string: String): String {
    return StringUtils.addStringAt(this, index, string)
}

fun String.substringIf(startIndex: Int, endIndex: Int, condition: Boolean): String {
    return StringUtils.substringIf(this, startIndex, endIndex, condition)
}

fun String.substringToLastIndexOf(string: String): String {
    return StringUtils.substringToLastIndexOf(this, string)
}

fun String.substringTo(to: Int): String {
    return StringUtils.substringTo(this, to)
}

fun String.substringTo(String: String): String {
    return StringUtils.substringTo(this, String)
}

fun String.substringOrDont(startIndex: Int, endIndex: Int): String {
    return StringUtils.substringOrDont(this, startIndex, endIndex)
}

fun String.substringOrElse(startIndex: Int, endIndex: Int, elseString: String): String {
    return StringUtils.substringOrElse(this, startIndex, endIndex, elseString)
}

fun String.substringOrEmpty(startIndex: Int, endIndex: Int): String {
    return StringUtils.substringOrEmpty(this, startIndex, endIndex)
}
