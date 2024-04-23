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

package org.polyfrost.oneconfig.api.config.visualize

import org.polyfrost.oneconfig.api.config.ConfigVisualizer.Companion.strv
import org.polyfrost.oneconfig.api.config.Property
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.events
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.unit.Vec2

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
            ).events {
                Event.Mouse.Clicked(0) then {
                    action.run()
                }
            }
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
                    entries = prop.type.enumConstants.map {
                        it as Enum<*>
                        null to (it::class.java.fields[0].get(it) as? String ?: it.name)
                    }.toTypedArray(),
                )
            } else {
                require(
                    prop.type == java.lang.Integer::class.java || prop.type == Int::class.java,
                ) { "Dropdowns can only be used with enums or integers (offender=${prop.id}, type=${prop.type})" }
                require(options.size >= 2) { "Dropdowns must have at least two options (offender=${prop.id})" }
                return Dropdown(
                    padding = 24f,
                    initial = prop.getAs(),
                    entries = options.map { null to it }.toTypedArray(),
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
            val notFloating = prop.type == java.lang.Integer::class.java || prop.type == java.lang.Long::class.java
                    || prop.type == Int::class.java || prop.type == Long::class.java
            val s =
                TextInput(
                    visibleSize = Vec2(300f, 32f),
                    text = prop.getAs<Number>().toString(),
                ).events {
                    Event.Change.Text() then {
                        if (it.text.isEmpty()) return@then
                        try {
                            val v = it.text.toFloat()
                            if (v < min || v > max) throw NumberFormatException("Out of range")
                            prop.setAs(if (notFloating) v.toInt() else v)
                        } catch (_: NumberFormatException) {
                            // todo show error
                        }
                    }
                }
            return s
        }
    }

    class RadioVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val options: Array<String> = prop.getMetadata("options") ?: emptyArray()
            if (prop.type.isEnum) {
                require(options.isEmpty()) { "Radio button ${prop.id} cannot have options when used with enums" }
                val r =
                    Radiobutton(
                        entries = prop.type.enumConstants.map {
                            it as Enum<*>
                            null to (it::class.java.fields[0].get(it) as? String ?: it.name)
                        }.toTypedArray(),
                        initial = prop.type.enumConstants.indexOf(prop.get()),
                        optionLateralPadding = 20f,
                    ).events {
                        Event.Change.Number() then {
                            prop.setAs(it.amount)
                        }
                    }
                return r
            } else {
                require(
                    prop.type == java.lang.Integer::class.java
                            || prop.type == Int::class.java
                ) { "Radio buttons ${prop.id} can only be used with enum or integer types (type=${prop.type}" }
                require(options.size >= 2) { "Radio button ${prop.id} must have at least two options" }
                return Radiobutton(
                    entries = options.map { null to it }.toTypedArray(),
                    initial = prop.getAs(),
                    optionLateralPadding = 20f,
                ).events {
                    Event.Change.Number() then {
                        prop.setAs(it.amount)
                    }
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
                ).events {
                    Event.Change.Number() then {
                        prop.setAs(it.amount)
                    }
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
            ).events {
                Event.Change.State() then {
                    prop.setAs(it.state)
                }
            }
        }
    }

    class TextVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val placeholder = prop.getMetadata("placeholder") ?: ""
            val s =
                TextInput(
                    placeholder = placeholder,
                    visibleSize = Vec2(300f, 12f),
                    text = prop.getAs(),
                ).events {
                    Event.Change.Text() then {
                        prop.setAs(it.text)
                    }
                }
            return s
        }
    }
}
