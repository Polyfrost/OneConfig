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

package org.polyfrost.oneconfig.ui.elements

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.PolyColor as Color
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.property.impl.BlockProperties
import org.polyfrost.polyui.property.impl.SliderProperties
import org.polyfrost.polyui.property.impl.TextInputProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.radii
import org.polyfrost.polyui.utils.rgba

class ColorPicker(
    at: Vec2<Unit>,
    private val hasAlpha: Boolean = false,
    var color: Color,
    private val favorites: Array<Color> = emptyArray(),
    private val onColorChange: (ColorPicker.(Color) -> Unit)? = null,
) : PixelLayout(at = at, size = 296.px * 400.px) {

    object Properties {
        val white = rgba(255, 255, 255)
        val gradient1 = Color.Gradient(
            white,
            Color.TRANSPARENT,
            Color.Gradient.Type.LeftToRight,
        )
        val gradient2 = Color.Gradient(
            Color.TRANSPARENT,
            rgba(0, 0, 0, 1f),
            Color.Gradient.Type.TopToBottom,
        )
    }

    init {
        addEventHandler(Added) {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            scaleTo(1f, 1f, Animations.EaseOutExpo, 0.5.seconds)
            fadeTo(1f, Animations.EaseOutExpo, 0.3.seconds)
        }
        val hexIn = TextInput(
            at = 84.px * 268.px,
            size = 83.px * 32.px,
            placeholder = "#FFFFFF".localised(),
            initialText = Color.hexOf(color).localised(),
            properties = ColorInputProperties(sanitizationFunction = {
                try {
                    Color.from(it)
                    true
                } catch (e: Exception) {
                    false
                }
            }),
            events = {
                TextInput.ChangedEvent() to {
                    this@ColorPicker.color.take(Color.from(it.value))
                    true
                }
            },
        )
        val alphaIn = TextInput(
            at = 176.px * 268.px,
            size = 73.px * 32.px,
            hint = "%".localised(),
            placeholder = "100".localised(),
            initialText = (color.alpha * 100f).toInt().toString().localised(),
            properties = ColorInputProperties(sanitizationFunction = {
                try {
                    val i = it.toFloat()
                    i in 0f..100f
                } catch (e: Exception) {
                    false
                }
            }),
            events = {
                TextInput.ChangedEvent() to {
                    this@ColorPicker.color.alpha = it.value.toFloat() / 100f
                }
                FocusedEvent.Lost to {
                    if (hexIn.focused) return@to
                    hexIn.txt = Color.hexOf(this@ColorPicker.color)
                }
            },
        )
        hexIn.addEventHandler(FocusedEvent.Lost) {
            alphaIn.txt = (this@ColorPicker.color.alpha * 100f).toInt().toString()
        }
        color = color.toAnimatable()
        color as Color.Animated
        add(
            Block(
                properties = BlockProperties(cornerRadii = 20f.radii(), outlineThickness = 1f),
                at = origin,
                size = size!!,
                acceptInput = false,
            ),
            Dropdown(
                at = 12.px * 12.px,
                size = 200.px * 32.px,
                entries = arrayOf(Dropdown.Entry("polyui.colorpicker.solid"), Dropdown.Entry("polyui.colorpicker.gradient"), Dropdown.Entry("polyui.colorpicker.chroma")),
                default = when (color) {
                    is Color.Chroma -> 2
                    is Color.Gradient -> 1
                    else -> 0
                },
            ),
            Image(
                at = 264.px * 20.px,
                image = PolyImage("close.svg", 16f, 16f),
                events = {
                    MouseEntered to {
                        recolor(properties.colors.state.danger.hovered, Animations.EaseOutExpo, 0.25.seconds)
                        polyUI.cursor = Cursor.Clicker
                    }
                    MouseExited to {
                        recolor(properties.colors.text.primary.normal, Animations.EaseOutExpo, 0.25.seconds)
                        polyUI.cursor = Cursor.Pointer
                    }
                    MouseClicked(0) to {
                        this@ColorPicker.layout!!.remove(this@ColorPicker)
                        wantRedraw()
                    }
                },
            ),
            ColorBox(
                at = 12.px * 56.px,
                color = color as Color.Animated,
                events = {
                    Slider.ChangedEvent() to {
                        if (hexIn.focused) return@to
                        hexIn.txt = Color.hexOf(this@ColorPicker.color)
                    }
                },
            ),
            ColorSlider(
                at = 228.px * 56.px,
                size = 16.px * 200.px,
                min = 0f,
                max = 0.999f,
                tex = PolyImage("hue.png"),
                ccolor = color as Color.Animated,
                alphaMode = false,
                events = {
                    Slider.ChangedEvent() to {
                        this@ColorPicker.color.hue = it.value
                        if (hexIn.focused) return@to
                        hexIn.txt = Color.hexOf(this@ColorPicker.color)
                    }
                },
            ),
            ColorSlider(
                at = 260.px * 56.px,
                size = 16.px * 200.px,
                min = 0f,
                tex = PolyImage("alpha.png"),
                ccolor = color as Color.Animated,
                max = 1f,
                alphaMode = true,
                events = {
                    Slider.ChangedEvent() to {
                        this@ColorPicker.color.alpha = 1f - it.value
                        alphaIn.txt = (this@ColorPicker.color.alpha * 100f).toInt().toString()
                        if (hexIn.focused) return@to
                        hexIn.txt = Color.hexOf(this@ColorPicker.color)
                    }
                },
            ),
            Dropdown(
                at = 12.px * 268.px,
                size = 66.px * 32.px,
                entries = arrayOf(Dropdown.Entry("polyui.colorpicker.hex"), Dropdown.Entry("polyui.colorpicker.rgb")),
            ),
            hexIn,
            alphaIn,
            Button(
                at = 258.px * 268.px,
                size = 32.px * 32.px,
                left = PolyImage("dropper.svg", 16f, 16f),
            ),
            Button(
                at = 12.px * 312.px,
                size = 32.px * 32.px,
                left = PolyImage("favorite.svg", 16f, 16f),
            ),
            Button(
                at = 12.px * 356.px,
                size = 32.px * 32.px,
                left = PolyImage("paintbrush.svg", 16f, 16f),
            ),
        )
    }

    override fun onInitComplete() {
        if (!hasAlpha) {
            components[5].disabled = true
            components[8].disabled = true
        }
        super.onInitComplete()
    }

    class ColorInputProperties(sanitizationFunction: (String) -> Boolean) : TextInputProperties(sanitizationFunction = sanitizationFunction) {
        override val outlineColor: Color
            get() = colors.page.border5
        override val outlineThickness = 1f.px
    }

    class ColorBox(
        at: Vec2<Unit>,
        color: Color.Animated,
        events: EventDSL<ColorBox>.() -> kotlin.Unit = {},
    ) : Block(BlockProperties(), at = at, size = 200.px * 198.px, acceptInput = true, events = events as EventDSL<Block>.() -> kotlin.Unit) {
        private var cx = 0f
            set(value) {
                val value = value.coerceIn(-6f, width - 6f)
                field = value
                color.saturation = (value + 6f) / width
            }
        private var cy = 0f
            set(value) {
                val value = value.coerceIn(-6f, height - 6f)
                field = value
                color.brightness = 1f - (value + 6f) / height
            }
        private var dragging = false

        init {
            this.color = color
        }

        override fun setup(renderer: Renderer, polyUI: PolyUI) {
            val c = color
            super.setup(renderer, polyUI)
            set(c)
            cornerRadii = 8f.radii()
        }

        fun set(color: Color.Animated) {
            this.color = color
            cx = color.saturation * width
            cy = (1f - color.brightness) * height
        }

        override fun accept(event: Event): Boolean {
            if (event is MousePressed) {
                if (event.button == 0) {
                    dragging = true
                }
            }
            if (event is MouseReleased) {
                dragging = false
            }
            return super.accept(event)
        }

        override fun render() {
            if (!polyUI.mouseDown && dragging) {
                dragging = false
            }
            if (dragging) {
                cx = polyUI.mouseX - trueX - 6f
                cy = polyUI.mouseY - trueY - 6f
                accept(Slider.ChangedEvent())
            }
            if (color.dirty) color.argb
            val s = color.saturation
            val b = color.brightness
            val a = color.alpha
            color.saturation = 1f
            color.brightness = 1f
            color.alpha = 1f
            super.render()
            color.saturation = s
            color.brightness = b
            color.alpha = a
            color.dirty = false
            // how will they know? they will never know
            renderer.rect(x, y, width, height + 1f, Properties.gradient1, cornerRadii)
            renderer.rect(x, y, width, height + 2f, Properties.gradient2, cornerRadii)
            renderer.hollowRect(x + cx, y + cy, 12f, 12f, Properties.white, 2f, 6f)
        }
    }

    class ColorSlider(
        at: Vec2<Unit>,
        size: Vec2<Unit>,
        private val tex: PolyImage? = null,
        private val ccolor: Color.Animated,
        private val alphaMode: Boolean,
        min: Float,
        max: Float,
        events: EventDSL<ColorSlider>.() -> kotlin.Unit = {},
    ) : Slider(ColorSliderProperties, at = at, size = size, min = min, max = max, events = events as EventDSL<Slider>.() -> kotlin.Unit) {
        private val theColor = if (alphaMode) Color.Gradient(ccolor, Color.TRANSPARENT, Color.Gradient.Type.TopToBottom) else null
        override fun render() {
            doDrag()
            val barRadius = width / 2f
            if (tex != null) {
                if (!alphaMode) value = ccolor.hue
                renderer.image(tex, x, y, width, height, barRadius)
            }
            if (theColor != null) {
                val color = ccolor
                val alpha = color.alpha
                value = 1f - alpha
                color.alpha = 1f
                renderer.rect(x, y, width, height, theColor, barRadius)
                color.alpha = alpha
                color.dirty = false
            }
            renderer.hollowRect(x, y + bitMain, width, width, Properties.white, 2f, barRadius)
        }

        override fun calculateBounds() {
            super.calculateBounds()
            barThickness = height
            barCross = y + height / 2f
        }

        object ColorSliderProperties : SliderProperties() {
            override val setInstantly: Boolean
                get() = true

            init {
                eventHandlers.clear()
            }
        }
    }
}
