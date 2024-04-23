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
import org.polyfrost.oneconfig.libs.universal.UMatrixStack
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.namedId
import org.polyfrost.polyui.unit.Vec2

/**
 * [Hud] implementation that uses the old rendering system, with a standard [render] method.
 *
 * **You must** ensure that the [width] and [height] properties accurately reflect the size of the HUD.
 * Note that they are only queried when the HUD is first created, and when the [update] method returns `true`.
 *
 * The [create] method is `open` in case you wish to override it. This is recommended for advanced users only.
 *
 * This class is marked with [ApiStatus.Obsolete] because the PolyUI system should be used for new code.
 * There are currently no plans to remove this, hence it is not considered a warning. This may change in the future.
 */
@ApiStatus.Obsolete(since = "1.0.0")
abstract class LegacyHud : Hud<Drawable>() {
    abstract var width: Float
    abstract var height: Float

    override fun create() = createLegacy()

    abstract fun render(stack: UMatrixStack, x: Float, y: Float)

    /**
     * Wraps the [render] method in a [Drawable] instance, with the [Drawable.size] property delegating to [width] and [height].
     */
    protected fun createLegacy(): Drawable {
        val size = object : Vec2(width, height) {
            override var x: Float
                get() = this@LegacyHud.width
                set(value) {
                    this@LegacyHud.width = value
                }
            override var y: Float
                get() = this@LegacyHud.height
                set(value) {
                    this@LegacyHud.height = value
                }
        }

        return object : Drawable(size = size) {
            override fun preRender() {}

            override fun render() {
                render(UMatrixStack.Compat.get(), x, y)
            }

            override fun postRender() {}

            override fun setup(polyUI: PolyUI): Boolean {
                if (initialized) return false
                initialized = true
                return true
            }
        }.namedId("LegacyHud")
    }
}
