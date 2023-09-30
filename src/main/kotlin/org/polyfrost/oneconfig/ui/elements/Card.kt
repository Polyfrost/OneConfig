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

package org.polyfrost.oneconfig.ui.elements

import org.polyfrost.oneconfig.api.config.Config
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.property.impl.BlockProperties
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.fastEachIndexed

@Suppress("UNCHECKED_CAST")
class Card(
    at: Vec2<Unit>,
    private val configData: Config,
    events: EventDSL<Card>.() -> kotlin.Unit = {},
) : ContainingComponent(
    properties = CardProperties,
    at = at,
    size = 256.px * 140.px,
    children = arrayOf(
        Block(
            properties = CardProperties.backgroundProps,
            at = origin,
            size = 256.px * 104.px,
        ),
        Block(
            properties = CardProperties.barProps,
            at = 0.px * 104.px,
            size = 256.px * 36.px,
        ),
    ),
    events = events as EventDSL<ContainingComponent>.() -> kotlin.Unit,
) {
    var enabled: Boolean
        get() = configData.enabled
        set(value) {
            configData.enabled = value
        }
    val icon = if (configData.icon != null) Image(image = configData.icon, at = origin, acceptInput = false) else null
    val title = Text(at = origin, initialText = configData.name, acceptInput = false, fontSize = 16.px)
    override val properties
        get() = super.properties as CardProperties

    init {
        addComponents(icon, title)
    }

    override fun calculateBounds() {
        super.calculateBounds()
        if (icon != null) {
            icon.x = width / 2f - icon.width / 2f
            icon.y = 104f / 2f - icon.height / 2f
        }
        title.x = if (configData.favorite) CardProperties.lateralPadding else width / 2f - title.width / 2f
        title.y = 104f + 36f / 2f - title.height / 2f
        configData.data.fastEach {
            it.width = 16f
            it.height = 16f
        }
    }

    override fun render() {
        super.render()
        if (configData.data.size != 0) {
            val height = configData.data.size * 16f + (configData.data.size + 1) * 6f
            renderer.rect(width - 37f, 6f, 28f, height, CardProperties.bgColor, 14f)
            configData.data.fastEachIndexed { i, it ->
                renderer.image(it, width - 37f + 6f, 6f + 6f + i * 22f)
            }
        }
    }
    object CardProperties : BlockProperties() {
        val backgroundProps = BlockProperties(withStates = true, cornerRadii = floatArrayOf(12f, 12f, 0f, 0f))
        val barProps = brand(cornerRadii = floatArrayOf(0f, 0f, 12f, 12f))
        val lateralPadding = 12f
        val bgColor get() = backgroundProps.colors.component.bg.normal
    }
}
