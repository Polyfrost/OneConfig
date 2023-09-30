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
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.input.PolyText
import org.polyfrost.polyui.property.impl.BlockProperties
import org.polyfrost.polyui.property.impl.BlockProperties.Companion.withStates
import org.polyfrost.polyui.property.impl.TextProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.radii
import org.polyfrost.polyui.utils.truncate
import kotlin.math.max

class Option(
    icon: PolyImage? = null,
    title: PolyText,
    desc: PolyText?,
    val option: Component,
    // 22x34
) : ContainingComponent(properties = OptionProperties, at = flex(endRowAfter = true), size = null, children = arrayOf()) {
    private val image = if (icon != null) Image(at = origin, image = icon, acceptInput = false) else null
    val title = Text(properties = TextProperties { fonts.medium }.withStates(), at = origin, initialText = title, acceptInput = true, fontSize = (if (desc == null) 18f else 16f).px)
    val description = if (desc != null) Text(at = origin, initialText = desc, acceptInput = false, fontSize = 12.px) else null
    override val properties
        get() = super.properties as OptionProperties

    init {
        if (icon != null && (icon.width != 32f || icon.height != 32f)) {
            icon.width = 32f
            icon.height = 32f
        }
        addComponents(image, this.title, this.description, option)
        (OptionProperties.lateralPadding as? Unit.Dynamic)?.set(this.size!!.a)
    }

    override var consumesHover: Boolean
        get() = false
        set(value) {}

    override fun calculateSize(): Size<Unit> {
        var mh = 0f
        children.fastEach {
            mh = max(mh, it.y + it.height)
        }
        return 1068.px * max(64f, mh + 12f).px
    }

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        children.fastEach {
            if (it !is Text) return@fastEach
            it.string = it.string.truncate(renderer, it.font, it.fontSize, 500f)
        }
    }

    override fun calculateBounds() {
        super.calculateBounds()
        title.x = 24f
        if (description != null) {
            description.x = 24f
            description.y = 39f
            title.y = 15f
        } else {
            title.y = 32f - title.height / 2f
        }
        if (image != null) {
            image.x = 24f
            image.y = 16f
            title.x += 48f
            if (description != null) description.x += 48f
        }
        val optX = this.width - option.width - OptionProperties.lateralPadding.px
        val optY = 32f - option.height / 2f
        option.x = optX
        option.y = optY
    }

    override fun render() {
        if (children.size > 4) clipDrawables()
        if (properties.outlineThickness != 0f) {
            renderer.hollowRect(0f, 0f, width, height, properties.outlineColor, properties.outlineThickness, OptionProperties.cornerRadii)
        }
        renderer.rect(0f, 0f, width, height, color, OptionProperties.cornerRadii)
        super.render()
    }

    object OptionProperties : BlockProperties() {
        override val palette get() = colors.component.bg

        open val lateralPadding: Unit = 32.px
        override val cornerRadii = 8f.radii()
    }
}
