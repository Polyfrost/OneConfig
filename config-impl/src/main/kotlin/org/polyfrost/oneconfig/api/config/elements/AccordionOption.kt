/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.api.config.elements

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.event.MouseEntered
import org.polyfrost.polyui.event.MouseExited
import org.polyfrost.polyui.event.MouseMoved
import org.polyfrost.polyui.input.PolyText
import org.polyfrost.polyui.property.impl.BlockProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.radii
import org.polyfrost.polyui.utils.rgba
import org.polyfrost.polyui.utils.truncate
import kotlin.math.max
import kotlin.math.min

class AccordionOption(
    at: Vec2<Unit>,
    title: PolyText,
    desc: PolyText?,
    val option: Component,
) : ContainingComponent(AccordianOptionProperties, at, 534.px * 32.px, children = arrayOf()) {
    val title = Text(at = origin, fontSize = 16.px, initialText = title, acceptInput = false)
    val delay = 1.seconds
    val description = if (desc != null) Text(at = origin, fontSize = 12.px, initialText = desc, acceptInput = false, size = 300.px * 0.px) else null
    override val properties
        get() = super.properties as AccordianOptionProperties
    private var time = 0L
    private var counting = false
    private var open = false

    init {
        this.description?.alpha = 0f
        addComponents(this.title, option)
        (AccordianOptionProperties.lateralPadding as? Unit.Dynamic)?.set(this.size!!.a)
        events {
            MouseMoved to {
                if (this@AccordionOption.description != null) counting = this.title.isInside(polyUI.mouseX, polyUI.mouseY)
                time = 0L
            }
            MouseEntered to {
                if (this@AccordionOption.description != null) counting = this.title.isInside(polyUI.mouseX, polyUI.mouseY)
            }
            MouseExited to {
                counting = false
                time = 0L
                false
            }
            MouseClicked(0) to {
                time = 0L
                false
            }
        }
    }

    override fun preRender(deltaTimeNanos: Long) {
        super.preRender(deltaTimeNanos)
        if (description != null) {
            if (counting) time += deltaTimeNanos
            if (time > delay) {
                if (!open) {
                    description.alpha = 0.01f
                    description.x = min(max(0f, (polyUI.mouseX - trueX - description.width / 2f)), this.width - description.width)
                    description.y = polyUI.mouseY - trueY - description.height - 18f
                    description.fadeTo(1f, Animations.EaseOutExpo, 0.5.seconds)
                    open = true
                }
            } else if (open) {
                description.fadeTo(0f, Animations.EaseOutExpo, 0.5.seconds)
                open = false
            }
        }
    }

    override fun render() {
        super.render()
        if (description?.alpha != 0f) {
            description?.let {
                renderer.globalAlpha(it.alpha)
                renderer.rect(it.x - 12f, it.y - 12f, it.width + 24f, it.height + 20f, properties.colors.component.bg.hovered, 8f)
                renderer.resetGlobalAlpha()
                it.preRender(polyUI.delta)
                it.render()
                it.postRender()
            }
        }
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        description?.onColorsChanged(colors)
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        description?.rescale(scaleX, scaleY)
    }

    override var consumesHover: Boolean
        get() = false
        set(value) {}

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        description?.let {
            it.layout = this.layout
            it.setup(renderer, polyUI)
            it.calculateBounds()
        }
    }

    override fun calculateBounds() {
        super.calculateBounds()
        title.x = AccordianOptionProperties.lateralPadding.px
        title.y = this.height / 2f - title.height / 2f
        option.x = this.width - option.width - AccordianOptionProperties.lateralPadding.px
        option.y = this.height / 2f - option.height / 2f
        title.string = title.string.truncate(renderer, title.font, title.fontSize, 200f)
    }

    object AccordianOptionProperties : BlockProperties() {
        override val palette = Colors.Palette(rgba(32, 39, 45, 0.8f), Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)

        open val lateralPadding: Unit = 24.px
        override val cornerRadii = 8f.radii()
    }
}
