/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.oneconfig.ui.pages

import org.polyfrost.oneconfig.api.config.data.Category
import org.polyfrost.oneconfig.api.config.data.Mod
import org.polyfrost.oneconfig.internal.config.ConfigVisualizer
import org.polyfrost.oneconfig.internal.config.ConfigVisualizer.addEventHandler
import org.polyfrost.oneconfig.ui.elements.Card
import org.polyfrost.polyui.component.impl.Button
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.layout.impl.SwitchingLayout
import org.polyfrost.polyui.property.impl.ButtonProperties
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.fastEach

class ModsPage(mods: Collection<Mod>, val owner: SwitchingLayout) {
    val cache = HashMap<Mod, Layout>()
    val noIconProps = object : ButtonProperties() {
        override val verticalPadding: Float
            get() = 8.5f
    }
    val buttonProps = object : ButtonProperties() {
        override val iconTextSpacing: Float
            get() = 10f
    }

    val content: Layout
    val header = FlexLayout(
        at = origin,
        size = 1126.px * 40.px,
        resizesChildren = true,
        drawables = drawables(
            Button(
                properties = noIconProps,
                at = flex(),
                text = "oneconfig.mods".localised(),
                fontSize = 16.px,
            ),
            *Category.values().map {
                val icon = if (it.iconPath != null) PolyImage(it.iconPath, 17f, 17f) else null
                Button(
                    properties = if (icon == null) noIconProps else null,
                    fontSize = 16.px,
                    at = flex(),
                    text = it.name.localised(),
                    left = icon,
                )
            }.toTypedArray(),
        ),
    )

    init {
        content = FlexLayout(
            resizesChildren = true,
            at = 0.px * 40.px,
            size = 1136.px * 0.px,
            gap = Gap(12.px, 12.px),
            drawables = mods.map { mod ->
                val c = Card(
                    at = flex(),
                    mod = mod,
                )
                c.children[0].addEventHandler(MouseClicked(0)) {
                    if (!c.enabled) return@addEventHandler false
                    owner.switch(cache.getOrPut(mod) { ConfigVisualizer.create(owner.layout, mod.config) })
                    true
                }
                c.children[1].addEventHandler(MouseClicked(0)) {
                    c.enabled = !c.enabled
                    true
                }
                return@map c
            }.toTypedArray(),
        ).scrolling(1126.px * 600.px)
    }

    val self: Layout = object :
        PixelLayout(
            at = origin,
            size = 1126.px * 600.px,
            drawables = drawables(
                header,
                content,
            ),
        ),
        Page {
        override fun filter(query: String, search: Any.(String) -> Boolean) {
            children[1].components.fastEach {
                if (it !is Card) return@fastEach
                it.exists = it.title.string.run { search(query) }
            }
            children[1].calculateBounds()
        }
    }

    fun open() {
        owner.switch(self)
    }
}
