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

package org.polyfrost.oneconfig.api.hud.v1.internal

import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Properties.simple
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.Hud

fun Hud<*>.writeOut(): Tree {
    val tree = Tree.tree()
    val hud = hud
    tree["x"] = simple(hud.x)
    tree["y"] = simple(hud.y)
    tree["scaleX"] = simple(hud.scaleX)
    tree["scaleY"] = simple(hud.scaleY)
    tree["hidden"] = simple(hidden)
    tree["skewX"] = simple(hud.skewX)
    tree["skewY"] = simple(hud.skewY)
    tree["rotation"] = simple(hud.rotation)
    tree["opacity"] = simple(hud.alpha)
    return tree
}

fun Hud<*>.readIn(tree: Tree) {
    val hud = hud
    hud.x = tree["x"].get(0f)
    hud.y = tree["y"].get(0f)
    hud.scaleX = tree["scaleX"].get(1f)
    hud.scaleY = tree["scaleY"].get(1f)
    hidden = tree["hidden"].get(false)
    hud.skewX = tree["skewX"].get(0.0)
    hud.skewY = tree["skewY"].get(0.0)
    hud.rotation = tree["rotation"].get(0.0)
    hud.alpha = tree["opacity"].get(1f)
}

fun <T> Node?.get(def: T) = ((this as? Property<*>)?.get() as? T?) ?: def