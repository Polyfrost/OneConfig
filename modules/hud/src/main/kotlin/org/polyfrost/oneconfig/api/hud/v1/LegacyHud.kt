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

import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.render.OmniResolution
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.UIManager
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2

/**
 * [Hud] implementation that uses the old rendering system.
 *
 * **You must** ensure that the [width] and [height] properties accurately reflect the size of the HUD.
 * Note that they are only queried when the HUD is first created, and when the [update] method returns `true`.
 *
 * The [create] method is `open` in case you wish to override it. This is recommended for advanced users only.
 *
 * This class is marked with [ApiStatus.Obsolete] because the PolyUI system should be used for new code.
 * There are currently no plans to remove this, hence it is not considered a warning.
 */
@ApiStatus.Obsolete(since = "1.0.0")
abstract class LegacyHud(id: String, title: String, category: Category) : Hud<Drawable>(id, title, category) {
    abstract var width: Float
    abstract var height: Float

    /**
     * Support for complex hybrid legacy HUDs with PolyUI children is currently experimental. Please report any issues you find.
     */
    override fun create(): Drawable = createLegacy()

    /**
     * Render your HUD. Note that [x] and [y], unlike the normal render methods of other HUD classes, are scaled to **Minecraft's coordinate space**
     * instead of as raw screen coordinates. This ensures that, when using methods such as those of the WorldRenderer, they show up correctly.
     * As such, **it is important to multiply your [width] and [height] with the [scaleX] and [scaleY] parameters to ensure accurate rendering.**
     *
     * **Note:** This method is called every frame, so you should not perform any heavy calculations here.
     */
    abstract fun renderLegacy(renderer: Renderer, ctx: OmniRenderingContext, x: Float, y: Float, scaleX: Float, scaleY: Float, drawable: Drawable)

    @ApiStatus.Experimental
    protected fun createLegacy(
        vararg children: Component? = arrayOf(),
        alignment: Align = AlignDefault,
        size: Vec2 = Vec2.ZERO,
        focusable: Boolean = false
    ) = LegacyHudComponent(hud = this, children = children, alignment = alignment, size = size, focusable = focusable)

    @Suppress("SENSELESS_COMPARISON")
    open class LegacyHudComponent(
        private val hud: LegacyHud,
        vararg children: Component? = arrayOf(),
        alignment: Align = AlignDefault,
        size: Vec2 = Vec2.ZERO,
        focusable: Boolean = false
    ) :
        Drawable(children = children, alignment = alignment, size = size, focusable = focusable) {

        override var width: Float
            get() = hud.width
            set(value) {
                hud.width = value
            }

        override var height: Float
            get() = hud.height
            set(value) {
                hud.height = value
            }

        override fun render() {
            val scale = if (HudManager.useGuiScale) 1f else Platform.screen().pixelRatio() / OmniResolution.scaleFactor.toFloat()
            hud.renderLegacy(polyUI.renderer, UIManager.INSTANCE.renderingContext, x * scale, y * scale, scaleX * scale, scaleY * scale, this)
        }
    }
}
