/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of Version 3 of the GNU Lesser
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

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * User preferences applied on top of every [Hud] at once
 *
 * Appearance settings pair an override switch with a value: while it is off a HUD keeps its own
 * look, while it is on the global value wins through the `effective...` accessors on [Hud], which
 * HUDs drawing their own content should read instead of the raw properties
 */
object GlobalHudSettings {
    private val _enabled: MutableState<Boolean> = mutableStateOf(true)
    private val _overrideFont: MutableState<Boolean> = mutableStateOf(false)
    private val _font: MutableState<Font> = mutableStateOf(Font.Minecraft)
    private val _overrideTextWeight: MutableState<Boolean> = mutableStateOf(false)
    private val _textWeight: MutableState<Weight> = mutableStateOf(Weight.Regular)
    private val _overrideShowBackground: MutableState<Boolean> = mutableStateOf(false)
    private val _showBackground: MutableState<Boolean> = mutableStateOf(true)
    private val _overrideBackgroundRadius: MutableState<Boolean> = mutableStateOf(false)
    private val _backgroundRadius: MutableState<Float> = mutableStateOf(4f)

    /** Master switch for every custom HUD element; the HUD editor keeps drawing them while it is off */
    @JvmStatic
    var enabled: Boolean
        get() = _enabled.value
        set(value) { _enabled.value = value }

    /** Whether [font] replaces the per-HUD [Hud.font] everywhere */
    @JvmStatic
    var overrideFont: Boolean
        get() = _overrideFont.value
        set(value) { _overrideFont.value = value }

    /** The font forced onto every HUD while [overrideFont] is on */
    @JvmStatic
    var font: Font
        get() = _font.value
        set(value) { _font.value = value }

    /** Whether [textWeight] replaces the per-HUD [Hud.textWeight] everywhere */
    @JvmStatic
    var overrideTextWeight: Boolean
        get() = _overrideTextWeight.value
        set(value) { _overrideTextWeight.value = value }

    /** The Poppins weight forced onto every HUD while [overrideTextWeight] is on */
    @JvmStatic
    var textWeight: Weight
        get() = _textWeight.value
        set(value) { _textWeight.value = value }

    /** Whether [showBackground] replaces the per-HUD [Hud.showBackground] everywhere */
    @JvmStatic
    var overrideShowBackground: Boolean
        get() = _overrideShowBackground.value
        set(value) { _overrideShowBackground.value = value }

    /** Whether HUD backgrounds are drawn while [overrideShowBackground] is on */
    @JvmStatic
    var showBackground: Boolean
        get() = _showBackground.value
        set(value) { _showBackground.value = value }

    /** Whether [backgroundRadius] replaces the per-HUD [Hud.bgRadius] everywhere */
    @JvmStatic
    var overrideBackgroundRadius: Boolean
        get() = _overrideBackgroundRadius.value
        set(value) { _overrideBackgroundRadius.value = value }

    /** The background corner radius forced onto every HUD while [overrideBackgroundRadius] is on */
    @JvmStatic
    var backgroundRadius: Float
        get() = _backgroundRadius.value
        set(value) { _backgroundRadius.value = value }
}
