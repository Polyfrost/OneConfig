/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package org.polyfrost.polyui.test

import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.LightTheme
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.property.PropertyManager
import org.polyfrost.polyui.property.impl.BlockProperties
import org.polyfrost.polyui.property.impl.TextProperties
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.radii
import kotlin.random.Random

object Test {
    @JvmField
    val moon = PolyImage("moon.svg")

    @JvmField
    val sun = PolyImage("sun.svg")

    @JvmField
    val brand = BlockProperties.brand(4f.radii())

    @JvmField
    val success = BlockProperties.successBlock

    @JvmField
    val warning = BlockProperties.warningBlock

    @JvmField
    val danger = BlockProperties.dangerBlock

    @JvmField
    val text = TextProperties { fonts.medium }

    @JvmStatic
    fun create(): Array<out Drawable> {
        return drawables(
            create(origin, false).also {
                it.add(
                    Text(
                        text = "polyfrost.copyright".localised(),
                        at = 24.px * 475.px,
                        fontSize = 10.px
                    ),
                    Image(
                        image = PolyImage("polyfrost.png"),
                        at = 24.px * 24.px
                    )
                )
            },
            create(400.px * 0.px, true).also {
                it.propertyManager = PropertyManager(DarkTheme())
                it.getComponent<Button>(2).leftImage!!.image = sun
                it.getComponent<Text>(1).initialText = "text.dark".localised()
            }
        )
    }

    fun blocks(amount: Int = 40): Array<Block> {
        return Array(amount) {
            block()
        }
    }

    fun block() = Block(
        properties = prop(),
        at = flex(),
        size = (Random.nextFloat() * 100f + 32f).px * 32.px
    )

    fun prop() = when (Random.Default.nextInt(4)) {
        0 -> brand
        1 -> success
        2 -> warning
        else -> danger
    }

    fun create(at: Point<Unit>, default: Boolean): PixelLayout {
        var t = default
        return PixelLayout(
            acceptInput = false,
            at = at,
            drawables = drawables(
                Block(
                    properties = BlockProperties.backgroundBlock,
                    at = origin,
                    size = 400.px * 500.px,
                    acceptInput = false
                ),
                Text(
                    properties = text,
                    text = "text.light".localised(),
                    fontSize = 20.px,
                    at = 24.px * 65.px
                ),
                Button(
                    left = moon,
                    at = 24.px * 111.px,
                    events = {
                        MouseClicked(0) to {
                            if (t) {
                                this.layout.changeColors(LightTheme())
                                this.layout.getComponent<Text>(1).initialText = "text.light".localised()
                                this.leftImage!!.image = moon
                            } else {
                                this.layout.changeColors(DarkTheme())
                                this.layout.getComponent<Text>(1).initialText = "text.dark".localised()
                                this.leftImage!!.image = sun
                            }
                            t = !t
                        }
                    }
                ),
                Button(
                    text = "button.text".localised("simple"),
                    fontSize = 13.px,
                    left = PolyImage("face-wink.svg"),
                    at = 68.px * 111.px
                ),
                Switch(
                    at = 266.px * 113.px,
                    switchSize = 64.px * 32.px
                ),
                Checkbox(
                    at = 336.px * 113.px,
                    size = 32.px * 32.px
                ),
                Dropdown(
                    at = 24.px * 159.px,
                    size = 352.px * 32.px,
                    entries = Dropdown.from(SlideDirection.values())
                ),
                TextInput(
                    at = 24.px * 203.px,
                    size = 352.px * 32.px
                ),
                FlexLayout(
                    at = 24.px * 247.px,
                    drawables = blocks(),
                    wrap = 348.px
                ).scrolling(348.px * 117.px),
                Button(
                    left = PolyImage("shuffle.svg"),
                    text = "button.randomize".localised(),
                    at = 24.px * 380.px,
                    events = {
                        MouseClicked(0) to {
                            this.layout.getLayout<FlexLayout>(0).shuffle()
                        }
                    }
                ),
                Button(
                    left = PolyImage("plus.svg"),
                    at = 320.px * 380.px,
                    events = {
                        MouseClicked(0) to {
                            this.layout.getLayout<FlexLayout>(0).add(block())
                        }
                    }
                ),
                Button(
                    left = PolyImage("minus.svg"),
                    at = 355.px * 380.px,
                    events = {
                        MouseClicked(0) to {
                            val l = this.layout.getLayout<FlexLayout>(0)
                            l.removeNow(l.flexDrawables.last())
                        }
                    }
                ),
                Block(
                    properties = BlockProperties.brandBlock,
                    at = 24.px * 430.px,
                    size = 85.px * 32.px
                ).draggable(),
                Block(
                    at = 113.px * 430.px,
                    size = 85.px * 32.px
                ),
                Block(
                    at = 202.px * 430.px,
                    size = 85.px * 32.px
                ),
                Block(
                    at = 291.px * 430.px,
                    size = 85.px * 32.px
                )
            )
        )
    }
}