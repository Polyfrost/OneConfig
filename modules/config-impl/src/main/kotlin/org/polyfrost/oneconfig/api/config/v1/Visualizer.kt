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
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.notify.Notifications
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.mapToArray
import org.polyfrost.polyui.utils.mutable
import org.polyfrost.polyui.utils.ref

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
            val p = prop.getAs<PolyColor>()
            if (p !is PolyColor.Mutable) {
                prop.setAsReferential(p.mutable())
            }
            return Block(color = prop.getAs(), size = Vec2(58f, 32f)).withBoarder(3f, color = { page.border20 }).onClick { ColorPicker(prop.getAs<PolyColor.Mutable>().ref(), null, null, polyUI); true }
        }
    }

    class DropdownVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val options: Array<String> = prop.getMetadata("options") ?: emptyArray()
            if (prop.type.isEnum) {
                require(options.isEmpty()) { "Dropdowns should not have options when used with enums (offender=${prop.id})" }
                val index = prop.type.enumConstants.indexOf(prop.get())
                return Dropdown(
                    optPadding = 24f,
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
                    optPadding = 24f,
                    initial = prop.getAs(),
                    entries = options.mapToArray { null to it },
                )
            }
        }
    }

    class KeybindVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            return Block(
                Image("assets/oneconfig/ico/keyboard.svg".image(), at = Vec2(7f, 7f)).ignoreLayout(),
                Text(prop.getAs<KeyBinder.Bind>().keysToString()),
                size = Vec2(230f, 32f),
                alignment = Align(main = Align.Main.Center),
            ).onInit {
                polyUI.keyBinder?.add(prop.getAs())
            }.withStates().onClick {
                val bind = prop.getAs<KeyBinder.Bind>()
                val image = this[0] as Image
                val text = this[1] as Text
                text.text = "oneconfig.recording"
                image.color = polyUI.colors.state.danger.pressed
                recalculate()
                polyUI.keyBinder?.record(bind.durationNanos, callback = {
                    if (it != null) {
                        prop.setAs(it)
                        polyUI.keyBinder?.remove(bind)
                        polyUI.keyBinder?.add(it)
                        text.text = it.keysToString()
                    } else {
                        shake()
                        text.text = bind.keysToString()
                    }
                    image.color = polyUI.colors.text.primary.normal
                    recalculate()
                    needsRedraw = true
                }, bind.action)
                false
            }
        }
    }

    class InfoVisualizer : Visualizer {
        override fun visualize(prop: Property<*>) = Group(size = Vec2.ONE).onInit {
            val type = prop.getMetadata<Notifications.Type>("type") ?: Notifications.Type.Info
            val title = prop.getMetadata<String>("title")?.strv() ?: Notifications.Type.Info.name
            val icon = prop.getMetadata<String>("icon") ?: Notifications.Type.Info.icon
            if (type != Notifications.Type.Info) {
                if (title == Notifications.Type.Info.name) {
                    (parent[0][1][0] as Text).text = type.name
                }
                if (icon == Notifications.Type.Info.icon) {
                    (parent[0][0] as Image).image = type.icon.image()
                }
            }
            val colors = polyUI.colors
            when (type) {
                Notifications.Type.Info -> {
                    (parent[0][0] as Drawable).palette = colors.brand.fg
                }

                Notifications.Type.Warning -> {
                    (parent[0][0] as Drawable).palette = colors.state.warning
                }

                Notifications.Type.Error -> {
                    (parent[0][0] as Drawable).palette = colors.state.danger
                }

                Notifications.Type.Success -> {
                    (parent[0][0] as Drawable).palette = colors.state.success
                }
            }
        }
    }

    class NumberVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val unit = prop.getMetadata<String>("unit")
            val min = prop.getMetadata<Float>("min") ?: -10f
            val max = prop.getMetadata<Float>("max") ?: 100f
            val integral = prop.type == Int::class.java || prop.type == Long::class.java
            val placeholder = prop.getMetadata<String>("placeholder") ?: if (integral) "${min.toInt()}-${max.toInt()}" else "$min-$max"
            val s = BoxedTextInput(
                placeholder = placeholder,
                image = "assets/oneconfig/ico/text.svg".image(),
                size = Vec2(200f, 32f),
                initialValue = prop.getAs<Number>().toString(),
                post = unit
            ).apply {
                (this[1][0] as TextInput).numeric(min, max, integral).on(Event.Change.Number) {
                    prop.setAs(if (integral) it.amount.toInt() else it.amount.toFloat())
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
                    length = 200f,
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
            val placeholder = prop.getMetadata("placeholder") ?: "polyui.textinput.placeholder"
            val s = BoxedTextInput(
                image = "assets/oneconfig/ico/text.svg".image(),
                placeholder = placeholder,
                size = Vec2(200f, 32f),
                initialValue = prop.getAs(),
            ).onChange { text: String ->
                prop.setAs(text)
                false
            }
            return s
        }
    }
}
