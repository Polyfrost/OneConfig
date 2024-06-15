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

package org.polyfrost.oneconfig.api.config.v1

import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer.Companion.strv
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.component.onChange
import org.polyfrost.polyui.component.onClick
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.mapToArray

/**
 * Visualizers are procedures that take a property, and return a drawable that represents it.
 */
fun interface Visualizer {
    fun visualize(prop: Property<*>): Drawable

    class ButtonVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val text = prop.getMetadata<String>("text")?.strv()
            val action = prop.getMetadata<Runnable>("runnable") ?: prop.getAs()
            return Button(
                size = Vec2(300f, 32f),
                text = text ?: "oneconfig.button.default",
            ).onClick { action.run() }
        }
    }

    class ColorVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            return Block()
        }
    }

    class DropdownVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val options: Array<String> = prop.getMetadata("options") ?: emptyArray()
            if (prop.type.isEnum) {
                require(options.isEmpty()) { "Dropdowns should not have options when used with enums (offender=${prop.id})" }
                val index = prop.type.enumConstants.indexOf(prop.get())
                return Dropdown(
                    padding = 24f,
                    initial = index,
                    entries = prop.type.enumConstants.mapToArray {
                        it as Enum<*>
                        null to (it::class.java.fields[0].get(it) as? String ?: it.name)
                    },
                )
            } else {
                require(prop.type == Int::class.java) { "Dropdowns can only be used with enums or integers (offender=${prop.id}, type=${prop.type})" }
                require(options.size >= 2) { "Dropdowns must have at least two options (offender=${prop.id})" }
                return Dropdown(
                    padding = 24f,
                    initial = prop.getAs(),
                    entries = options.mapToArray { null to it },
                )
            }
        }
    }

    class KeybindVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
//            val bind: KeyBinder.Bind = prop.getAs()
            return Block(
                size = Vec2(300f, 32f),
            )
        }
    }

    class NumberVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
//            val placeholder = prop.getMetadata<String>("placeholder") ?: "0"
//            val unit = prop.getMetadata<String>("unit")
            val min = prop.getMetadata<Float>("min") ?: 0f
            val max = prop.getMetadata<Float>("max") ?: 100f
            val notFloating = prop.type == Int::class.java || prop.type == Long::class.java
            val s =
                TextInput(
                    visibleSize = Vec2(300f, 32f),
                    text = prop.getAs<Number>().toString(),
                ).onChange { text: String ->
                    if (text.isEmpty()) return@onChange false
                    try {
                        val v = text.toFloat()
                        if (v < min || v > max) throw NumberFormatException("Out of range")
                        prop.setAs(if (notFloating) v.toInt() else v)
                        false
                    } catch (_: NumberFormatException) {
                        // todo show error
                        true
                    }

                }
            return s
        }
    }

    class RadioVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val options: Array<String> = prop.getMetadata("options") ?: emptyArray()
            if (prop.type.isEnum) {
                val values = prop.type.enumConstants
                var field = prop.type::class.java.fields.firstOrNull()
                if (field?.type == String::class.java) field = null
                require(options.isEmpty()) { "Radio button ${prop.id} cannot have options when used with enums" }
                val r =
                    Radiobutton(
                        entries = values.mapToArray {
                            it as Enum<*>
                            null to (field?.get(it) as? String ?: it.name)
                        },
                        initial = values.indexOf(prop.get()),
                        optionLateralPadding = 20f,
                    ).onChange { amount: Int ->
                        prop.setAs(values[amount])
                        false
                    }
                return r
            } else {
                require(prop.type == Int::class.java) { "Radio buttons ${prop.id} can only be used with enum or integer types (type=${prop.type}" }
                require(options.size >= 2) { "Radio button ${prop.id} must have at least two options" }
                return Radiobutton(
                    entries = options.mapToArray { null to it },
                    initial = prop.getAs(),
                    optionLateralPadding = 20f,
                ).onChange { amount: Int ->
                    prop.setAs(amount)
                    false
                }
            }
        }
    }

    class SliderVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val min = prop.getMetadata<Float>("min") ?: 0f
            val max = prop.getMetadata<Float>("max") ?: 100f
            // todo stepped
            val s =
                Slider(
                    min = min,
                    max = max,
                    initialValue = prop.getAs<Number>().toFloat(),
                ).onChange { amount: Int ->
                    prop.setAs(amount)
                    false
                }
            return s
        }
    }

    class SwitchVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val state = prop.getAs<Boolean>()
            return Switch(
                lateralStretch = 2f,
                size = 21f,
                state = state,
            ).onChange { new: Boolean ->
                prop.setAs(new)
                false
            }
        }
    }

    class CheckboxVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val state = prop.getAs<Boolean>()
            return Checkbox(
                size = 24f,
                state = state,
            ).onChange { new: Boolean ->
                prop.setAs(new)
                false
            }
        }
    }

    class TextVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val placeholder = prop.getMetadata("placeholder") ?: ""
            val s =
                TextInput(
                    placeholder = placeholder,
                    visibleSize = Vec2(200f, 12f),
                    text = prop.getAs(),
                ).onChange { text: String ->
                    prop.setAs(text)
                    false
                }
            return s
        }
    }
}
