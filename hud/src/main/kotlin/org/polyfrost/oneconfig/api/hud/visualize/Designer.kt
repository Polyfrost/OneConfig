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

package org.polyfrost.oneconfig.api.hud.visualize

import org.polyfrost.oneconfig.api.hud.Hud
import org.polyfrost.oneconfig.api.hud.elements.InferringCComponent
import org.polyfrost.oneconfig.api.hud.properties.ICComponentProperties
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.layout.impl.SwitchingLayout
import org.polyfrost.polyui.property.PropertyManager
import org.polyfrost.polyui.property.impl.TextInputProperties
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.flex
import org.polyfrost.polyui.unit.origin
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.unit.times
import org.polyfrost.polyui.utils.fastEach

object Designer {
    @JvmStatic
    fun design(hud: Hud, owner: Layout): Layout {
        val switcher = SwitchingLayout(
            at = 24.px * 74.px,
            size = 500.px * 1000.px,
        )
        val propertyManager = PropertyManager(owner.polyUI)
        propertyManager.addPropertyType(InferringCComponent::class, ICComponentProperties())
        val designerPage by lazy {
            FlexLayout(
                propertyManager = propertyManager,
                at = origin,
                drawables = createUnits(hud.self),
            )
        }
        val settingsPage by lazy {
            FlexLayout(
                propertyManager = propertyManager,
                at = origin,
                drawables = createUnits(hud.self),
            )
        }

        val out = PixelLayout(
            at = origin,
            size = 500.px * 1000.px,
            drawables = drawables(
                Button(
                    at = 24.px * 12.px,
                    left = PolyImage("paintbrush.svg", 18f, 18f),
                    text = "oneconfig.hudeditor.designer".localised(),
                    size = 106.px * 30.px,
                ),
                Button(
                    at = 136.px * 12.px,
                    left = PolyImage("spanner.svg", 18f, 18f),
                    text = "oneconfig.hudeditor.settings".localised(),
                    size = 129.px * 30.px,
                ),
                switcher,
            ),
        )
        owner.add(out)
        switcher.switch(designerPage)
        return out
    }

    fun createUnits(cmp: InferringCComponent): Array<out ContainingComponent> {
        val out = ArrayList<ContainingComponent>(5)
        val pxHint = "oneconfig.hint.px".localised()
        val numHint = "0".localised()
        out.add(
            createUnit(
                "oneconfig.hudeditor.options",
                drawables = arrayOf(
                    Text(
                        at = 0.px * 30.px,
                        text = "oneconfig.hudeditor.enabled".localised(),
                        fontSize = 14.px,
                    ),
                    Switch(
                        at = 425.px * 30.px,
                        switchSize = 40.px * 20.px,
                    ),
                    Text(
                        at = 0.px * 60.px,
                        text = "oneconfig.hudeditor.dimensions".localised(),
                        fontSize = 14.px,
                    ),
                    TextInput(
                        TextInputProperties.floatingNumber,
                        at = 0.px * 80.px,
                        size = 128.px * 30.px,
                        hint = pxHint,
                        placeholder = numHint,
                        title = "oneconfig.hudeditor.width".localised(),
                    ),
                    TextInput(
                        TextInputProperties.floatingNumber,
                        at = 140.px * 80.px,
                        size = 128.px * 30.px,
                        hint = pxHint,
                        placeholder = numHint,
                        title = "oneconfig.hudeditor.height".localised(),
                    ),
                ),
            ),
        )

        cmp.children.fastEach {
            when (it) {
//                is Block -> {
//                    out.add(createBlockUnit(it))
//                }
//
//                is Text -> {
//                    out.add(createTextUnit(it))
//                }
//
//                is Image -> {
//                    out.add(createImageUnit(it))
//                }
            }
        }

        return out.toTypedArray()
    }

    fun createBlockUnit(block: Block): ContainingComponent {
        val out = InferringCComponent(at = flex(), children = arrayOf())
        out.addComponents(
            Text(
                text = "oneconfig.hudeditor.padding".localised(),
                at = 24.px * 0.px,
                fontSize = 14.px,
            ),
            TextInput(
                properties = TextInputProperties.floatingNumber,
                at = 260.px * 0.px,
                size = 94.px * 32.px,
                hint = "oneconfig.hint.px".localised(),
                image = PolyImage("padding-vertical.svg"),
            ),
            TextInput(
                properties = TextInputProperties.floatingNumber,
                at = 260.px * 0.px,
                size = 94.px * 32.px,
                hint = "oneconfig.hint.px".localised(),
                image = PolyImage("padding-horizontal.svg"),
            ),
            Text(
                text = "oneconfig.hudeditor.radius".localised(),
                at = 24.px * 30.px,
                fontSize = 14.px,
            ),
            Slider(
                at = 150.px * 30.px,
                size = 225.px * 30.px,
            ),
        )
        return out
    }

    fun createImageUnit(image: Image): ContainingComponent {
        val out = InferringCComponent(at = flex(), children = arrayOf())
        return out
    }

    fun createTextUnit(text: Text): ContainingComponent {
        val out = InferringCComponent(at = flex(), children = arrayOf())
        return out
    }

    fun createUnit(title: String, vararg drawables: Component): ContainingComponent {
        val out = InferringCComponent(
            at = flex(),
            children = arrayOf(
                Text(
                    at = 0.px * 0.px,
                    text = title.localised(),
                    fontSize = 12.px,
                ),
                Image(
                    at = 432.px * 0.px,
                    image = PolyImage("info.svg", 18f, 18f),
                ),
            ),
        )
        out.addComponents(*drawables)
        return out
    }
}
