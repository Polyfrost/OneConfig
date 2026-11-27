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

package org.polyfrost.oneconfig.api.hud.v1

import org.jetbrains.annotations.ApiStatus

/**
 * Decides where the HUDs of a [HudPreset] sit relative to one another
 *
 * It only works out positions from sizes so the library can preview a preset without placing it
 */
@ApiStatus.Experimental
public fun interface HudPresetLayout {
    /** The position of each of [huds] in gui units, in the same order, given the [sizes] they will have */
    public fun positions(huds: List<Hud>, sizes: List<Pair<Float, Float>>): List<Pair<Float, Float>>
}

/**
 * Arranges the HUDs of a group made from a [HudPreset], typically by anchoring them to one another
 */
@ApiStatus.Experimental
public fun interface HudPresetArrangement {
    /** Rearranges [huds], which line up with [HudPreset.members] and are `null` where the group has no such HUD */
    public fun arrange(huds: List<Hud?>)
}

/** A named way of arranging the HUDs of a [HudPreset] that the user can pick in the settings of its group */
@ApiStatus.Experimental
public class HudPresetAction @JvmOverloads constructor(
    /** Identifies the action within its preset */
    public val id: String,
    /** The name shown to the user, which is looked up as a translation key first */
    public val name: String,
    public val arrangement: HudPresetArrangement,
    /** What the action does, shown as a tooltip */
    public val description: String? = null,
)

/**
 * A set of HUDs the library adds to the screen together as one group
 *
 * Two presets are equal when they have the same [id]
 */
@ApiStatus.Experimental
public class HudPreset internal constructor(
    public val id: String,
    public val name: String,
    public val members: List<Class<out Hud>>,
    /** The config the preset is listed under in the library, or the one of its first member when `null` */
    public val configId: String?,
    /** The icon shown on its library card, or a default one when `null` */
    public val icon: String?,
    /** Where the members sit, or each member's [Hud.defaultPosition] when `null` */
    public val layout: HudPresetLayout?,
    /** The arrangements the user can pick in the settings of a group made from this preset */
    public val actions: List<HudPresetAction>,
    /** The action applied when the preset is added, or `null` to leave the members where [layout] puts them */
    public val defaultAction: HudPresetAction?,
) {
    override fun equals(other: Any?): Boolean = other is HudPreset && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "HudPreset($id)"
}
