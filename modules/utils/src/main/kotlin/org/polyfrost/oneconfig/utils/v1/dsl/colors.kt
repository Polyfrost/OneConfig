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

package org.polyfrost.oneconfig.utils.v1.dsl

import org.polyfrost.oneconfig.utils.v1.ColorUtils

/**
 * Get the red component of the given RGBA value.
 *
 * @see ColorUtils.getRed
 */
fun Int.getRed() = ColorUtils.getRed(this)

/**
 * Get the green component of the given RGBA value.
 *
 * @see ColorUtils.getGreen
 */
fun Int.getGreen() = ColorUtils.getGreen(this)

/**
 * Get the blue component of the given RGBA value.
 *
 * @see ColorUtils.getBlue
 */
fun Int.getBlue() = ColorUtils.getBlue(this)

/**
 * Get the alpha component of the given RGBA value.
 *
 * @see ColorUtils.getAlpha
 */
fun Int.getAlpha() = ColorUtils.getAlpha(this)

/**
 * Return the color with the given red component.
 *
 * @see ColorUtils.setRed
 */
fun Int.setRed(red: Int) = ColorUtils.setRed(this, red)

/**
 * Return the color with the given green component.
 *
 * @see ColorUtils.setGreen
 */
fun Int.setGreen(green: Int) = ColorUtils.setGreen(this, green)

/**
 * Return the color with the given blue component.
 *
 * @see ColorUtils.setBlue
 */
fun Int.setBlue(blue: Int) = ColorUtils.setBlue(this, blue)

/**
 * Return the color with the given alpha component.
 *
 * @see ColorUtils.setAlpha
 */
fun Int.setAlpha(alpha: Int) = ColorUtils.setAlpha(this, alpha)