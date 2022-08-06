/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.renderer.font.Font
import cc.polyfrost.oneconfig.utils.TextUtils

/**
 * Wraps the given [String] to the given [maxWidth].
 * @see TextUtils.wrapText
 */
fun String.wrap(vg: Long, maxWidth: Float, fontSize: Number, font: Font) =
    TextUtils.wrapText(vg, this, maxWidth, fontSize.toFloat(), font)

/**
 * Wraps the given [String] to the given [maxWidth].
 * @see wrap
 */
fun Long.wrap(text: String, maxWidth: Float, fontSize: Number, font: Font) =
    TextUtils.wrapText(this, text, maxWidth, fontSize.toFloat(), font)

/**
 * Wraps the given [String] to the given [maxWidth].
 * @see wrap
 */
fun VG.wrap(text: String, maxWidth: Float, fontSize: Number, font: Font) =
    TextUtils.wrapText(instance, text, maxWidth, fontSize.toFloat(), font)
