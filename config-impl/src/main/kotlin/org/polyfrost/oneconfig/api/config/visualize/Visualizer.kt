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

package org.polyfrost.oneconfig.api.config.visualize

import org.polyfrost.oneconfig.api.config.Property
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
        val click = "oneconfig.click".localised()
    }

    class ButtonVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val text = prop.getMetadata<String>("text")?.ifEmpty { null }
            val action = prop.getMetadata<Runnable>("runnable") ?: prop.getAs()
            return Button(
                at = origin,
                size = 300.px * 34.px,
                text = text?.localised() ?: click,
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
            return Block(at = origin, size = origin)
        }
    }

    class DropdownVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Component {
            val options: Array<String> = prop.getMetadata("options") ?: emptyArray()
            if (prop.type.isEnum) {
                require(options.isEmpty()) { "Dropdowns cannot have options when used with enums" }
                val index = prop.type.enumConstants.indexOf(prop.get())
                return Dropdown(
                    at = origin,
                    size = 300.px * 32.px,
                    default = index,
                    entries = Dropdown.from(prop.type),
                )
            } else {
                require(prop.type == java.lang.Integer::class.java) { "Dropdowns can only be used with enums or integers" }
                require(options.size >= 2) { "Dropdowns must have at least two options" }
                return Dropdown(
                    at = origin,
                    size = 300.px * 32.px,
                    default = prop.getAs(),
                    entries = Dropdown.from(options),
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
            val placeholder = prop.getMetadata<String>("placeholder") ?: "0"
            val unit = prop.getMetadata<String>("unit")
            val min = prop.getMetadata<Float>("min") ?: 0f
            val max = prop.getMetadata<Float>("max") ?: 100f
            val notFloating = prop.type == java.lang.Integer::class.java || prop.type == java.lang.Long::class.java
            val s = TextInput(
                properties = TextInputProperties.numberProperties(!notFloating, min, max),
                initialText = prop.getAs<Number>().toString().localised(),
                placeholder = placeholder.localised(),
                hint = unit?.localised(),
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
            val options: Array<String> = prop.getMetadata("options") ?: emptyArray()
            if (prop.type.isEnum) {
                require(options.isEmpty()) { "Radio buttons cannot have options when used with enums" }
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
                require(options.size >= 2) { "Radio buttons must have at least two options" }
                return RadioButton(
                    at = origin,
                    size = 300.px * 32.px,
                    values = options,
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
            val min = prop.getMetadata<Float>("min") ?: 0f
            val max = prop.getMetadata<Float>("max") ?: 100f
            // todo steps and lil boi inside the slider head
            val notFloating = prop.type == java.lang.Integer::class.java || prop.type == java.lang.Long::class.java
            val s = Slider(
                at = origin,
                size = 300.px * 32.px,
                min = min,
                max = max,
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
            val placeholder = prop.getMetadata("placeholder") ?: ""
            val s = TextInput(
                at = origin,
                size = 300.px * 32.px,
                initialText = prop.getAs<String>().localised(),
                placeholder = placeholder.localised(),
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
