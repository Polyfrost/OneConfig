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

package org.polyfrost.oneconfig.api.config.visualize

import org.polyfrost.oneconfig.api.config.Property
import org.polyfrost.oneconfig.api.config.annotations.Color
import org.polyfrost.oneconfig.api.config.annotations.Text
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.property.impl.TextInputProperties
import org.polyfrost.polyui.unit.origin
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.unit.times

fun interface Visualizer {
    fun visualize(prop: Property<*>): Component

    companion object {
        val switchOn = "oneconfig.switch.enabled".localised()
        val switchOff = "oneconfig.switch.disabled".localised()
        fun <A> getAnnotation(prop: Property<*>): A {
            return prop.getMetadata<A>("annotation")!!
        }
    }

    class ButtonVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val a = getAnnotation<org.polyfrost.oneconfig.api.config.annotations.Button>(prop)
            val action = prop.getMetadata<Runnable>("runnable") ?: prop.getAs()
            return Button(
                at = origin,
                size = 300.px * 34.px,
                text = a.text.localised(),
                events = {
                    MouseClicked(0) to {
                        action.run()
                        true
                    }
                },
            )
        }
    }

    class ColorVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val a = getAnnotation<Color>(prop)
            return Block(at = origin, size = origin)
        }
    }

    class DropdownVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val a = getAnnotation<org.polyfrost.oneconfig.api.config.annotations.Dropdown>(prop)
            if (prop.type.isEnum) {
                require(a.options.isEmpty()) { "Dropdowns cannot have options when used with enums" }
                val index = prop.type.enumConstants.indexOf(prop.get())
                return Dropdown(
                    at = origin,
                    size = 300.px * 32.px,
                    default = index,
                    entries = Dropdown.from(prop.type),
                )
            } else {
                require(prop.type == java.lang.Integer::class.java) { "Dropdowns can only be used with enums or integers" }
                require(a.options.size >= 2) { "Dropdowns must have at least two options" }
                return Dropdown(
                    at = origin,
                    size = 300.px * 32.px,
                    default = prop.getAs(),
                    entries = Dropdown.from(a.options),
                )
            }
        }
    }

    class KeybindVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            return Keybind(
                at = origin,
                size = 300.px * 32.px,
                bind = prop.getAs(),
            )
        }
    }

    class NumberVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val a = getAnnotation<org.polyfrost.oneconfig.api.config.annotations.Number>(prop)
            val notFloating = prop.type == java.lang.Integer::class.java || prop.type == java.lang.Long::class.java
            val s = TextInput(
                properties = TextInputProperties.numberProperties(!notFloating, a.min, a.max),
                initialText = prop.getAs<Number>().toString().localised(),
                placeholder = a.placeholder.localised(),
                hint = a.unit.ifEmpty { null }?.localised(),
                at = origin,
                size = 300.px * 32.px,
                events = {
                    TextInput.ChangedEvent() to event@{
                        if (it.value.isEmpty()) return@event
                        val v = it.value.toFloat()
                        prop.setAs(if (notFloating) v.toInt() else v)
                    }
                },
            )
            return s
        }
    }

    class RadioVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val a = getAnnotation<org.polyfrost.oneconfig.api.config.annotations.RadioButton>(prop)
            if (prop.type.isEnum) {
                require(a.options.isEmpty()) { "Radio buttons cannot have options when used with enums" }
                val r = RadioButton(
                    at = origin,
                    size = 300.px * 32.px,
                    values = prop.type.enumConstants,
                    onChange = {
                        prop.setAs(it)
                    },
                )
                r.set(prop.get())
                return r
            } else {
                require(prop.type == java.lang.Integer::class.java) { "Radio buttons can only be used with enums or integers" }
                require(a.options.size >= 2) { "Radio buttons must have at least two options" }
                return RadioButton(
                    at = origin,
                    size = 300.px * 32.px,
                    values = a.options,
                    defaultIndex = prop.getAs(),
                    onChange = {
                        prop.setAs(this.selectedIndex)
                    },
                )
            }
        }
    }

    class SliderVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val a = getAnnotation<org.polyfrost.oneconfig.api.config.annotations.Slider>(prop)
            // todo steps and lil boi inside the slider head
            val notFloating = prop.type == java.lang.Integer::class.java || prop.type == java.lang.Long::class.java
            val s = Slider(
                at = origin,
                size = 300.px * 32.px,
                min = a.min,
                max = a.max,
                events = {
                    Slider.ChangedEvent() to {
                        prop.setAs(if (notFloating) it.value.toInt() else it.value)
                    }
                },
            )
            s.value = prop.getAs<Number>().toFloat()
            return s
        }
    }

    class SwitchVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val state = prop.getAs<Boolean>()
            return Switch(
                at = origin,
                switchSize = 40.px * 20.px,
                enabled = state,
                label = if (state) switchOn else switchOff,
                onSwitch = {
                    prop.setAs(it)
                    label = if (it) switchOn else switchOff
                },
            )
        }
    }

    class TextVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val a = getAnnotation<Text>(prop)
            val s = TextInput(
                at = origin,
                size = 300.px * 32.px,
                initialText = prop.getAs<String>().localised(),
                placeholder = a.placeholder.localised(),
                events = {
                    TextInput.ChangedEvent() to {
                        prop.setAs(it.value)
                    }
                },
            )
            return s
        }
    }
}
