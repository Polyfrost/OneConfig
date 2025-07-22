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
import org.polyfrost.polyui.color.mutable
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.mapToArray
import org.polyfrost.polyui.utils.ref
import kotlin.jvm.java

/**
 * Visualizers are procedures that take a property, and return a drawable that represents it.
 */
fun interface Visualizer {
    fun visualize(prop: Property<*>): Drawable

    class ButtonVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val text = prop.getMetadata<String>("text")?.strv()
            val action: Runnable? = prop.getMetadata<Runnable>("runnable") ?: prop.getAs<Runnable?>()
            require(action != null) { "Button property $prop is missing a runnable, set it with either the metadata key 'runnable' or the property value" }
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
            val s = Block(color = prop.getAs(), size = Vec2(58f, 32f)).withBorder(3f, color = { page.border20 })
                .onClick { ColorPicker(prop.getAs<PolyColor.Mutable>().ref(), null, null, polyUI); true }
            prop.addCallback {
                s.color = it as PolyColor
                false
            }
            return s
        }
    }

    class DropdownVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val options: Array<String> = prop.getMetadata("options") ?: emptyArray()
            if (prop.type.isEnum || prop.type.superclass?.isEnum == true) {
                require(options.isEmpty()) { "Dropdowns should not have options when used with enums (offender=${prop.id})" }
                val constants = if (prop.type.isEnum) prop.type.enumConstants else prop.type.superclass.enumConstants
                val index = constants.indexOf(prop.get())
                val s = Dropdown(
                    optPadding = 24f,
                    initial = index,
                    entries = constants.mapToArray {
                        it as Enum<*>
                        null to (it::class.java.fields[0].get(it) as? String ?: it.name)
                    },
                ).onChange { i: Int ->
                    prop.setAs(constants[i])
                    false
                }
                // todo setback not supported currently on dropdowns
                return s
            } else {
                require(prop.type == Int::class.java) { "Dropdowns can only be used with enums or integers (offender=${prop.id}, type=${prop.type})" }
                require(options.size >= 2) { "Dropdowns must have at least two options (offender=${prop.id})" }
                val s = Dropdown(
                    optPadding = 24f,
                    initial = prop.getAs(),
                    entries = options.mapToArray { null to it },
                ).onChange { i: Int ->
                    prop.setAs(i)
                    false
                }
                return s
            }
        }
    }

    class KeybindVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            return Block(
                Image("assets/oneconfig/ico/keyboard.svg".image(), at = Vec2(7f, 7f)).ignoreLayout(),
                Text(prop.getAs<KeyBinder.Bind>().keysToString()),
                size = Vec2(230f, 32f),
                alignment = Align(main = Align.Content.Center),
            ).onInit {
                polyUI.keyBinder?.add(prop.getAs())
            }.withHoverStates().onClick {
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
            // todo
        }
    }

    class DraggableListVisualizer : Visualizer {
        override fun visualize(prop: Property<*>) = Group(size = Vec2.ONE).onInit {
            // todo
        }
    }

    class MultiSelectDropdownVisualizer : Visualizer {
        override fun visualize(prop: Property<*>) = Group(size = Vec2.ONE).onInit {
            // todo
        }
    }

    class NumberVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val unit = prop.getMetadata<String>("unit")
            val min = prop.getMetadata<Float>("min") ?: 0f
            val max = prop.getMetadata<Float>("max") ?: 100f
            val integral = prop.type == Int::class.java || prop.type == Long::class.java
            val placeholder = prop.getMetadata<String>("placeholder") ?: if (integral) "${min.toInt()}-${max.toInt()}" else "$min-$max"
            var dodge = false
            val s = BoxedTextInput(
                placeholder = placeholder,
                image = "assets/oneconfig/ico/text.svg".image(),
                size = Vec2(200f, 32f),
                initialValue = prop.getAs<Number>().toString(),
                post = unit
            ).apply {
                (this[1][0] as TextInput).numeric(min, max, integral).on(Event.Change.Number) {
                    dodge = true
                    prop.setAs(if (integral) it.amount.toInt() else it.amount.toFloat())
                }
            }
            prop.addCallback {
                if (!dodge) (s[1][0] as TextInput).text = it.toString()
                dodge = false
                false
            }
            return s
        }
    }

    class RadioVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val options: Array<String> = prop.getMetadata("options") ?: emptyArray()
            var dodge = false
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
                        dodge = true
                        prop.setAs(values[amount])
                        false
                    }
                prop.addCallback {
                    if (!dodge) r.setRadiobuttonEntry(values.indexOf(it as Enum<*>))
                    dodge = false
                    false
                }
                return r
            } else {
                require(prop.type == Int::class.java) { "Radio buttons ${prop.id} can only be used with enum or integer types (type=${prop.type}" }
                require(options.size >= 2) { "Radio button ${prop.id} must have at least two options" }
                val r = Radiobutton(
                    entries = options.mapToArray { null to it },
                    initial = prop.getAs(),
                    optionLateralPadding = 20f,
                ).onChange { amount: Int ->
                    dodge = true
                    prop.setAs(amount)
                    false
                }
                prop.addCallback {
                    if (!dodge) r.setRadiobuttonEntry(it as Int)
                    dodge = false
                    false
                }
                return r
            }
        }
    }

    class SliderVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val min = prop.getMetadata<Float>("min") ?: 0f
            val max = prop.getMetadata<Float>("max") ?: 100f
            var dodge = false
            // todo stepped
            val s =
                Slider(
                    min = min,
                    max = max,
                    length = 200f,
                    initialValue = prop.getAs<Number>().toFloat().coerceAtLeast(min),
                    integral = prop.type == Int::class.java || prop.type == Long::class.java,
                ).onChange { amount: Float ->
                    dodge = true
                    if (prop.type == Int::class.java) prop.setAs(amount.toInt()) else prop.setAs(amount)
                    false
                }
            prop.addCallback {
                if (!dodge) s.setSliderValue((it as Number).toFloat(), min, max, false)
                dodge = false
                false
            }
            return s
        }
    }

    class SwitchVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val state = prop.getAs<Boolean>()
            var dodge = false
            val s = Switch(
                lateralStretch = 2f,
                size = 21f,
                state = state,
            ).onChange { new: Boolean ->
                dodge = true
                prop.setAs(new)
                false
            }
            prop.addCallback {
                if (!dodge) s.toggle(it as Boolean)
                dodge = false
                false
            }
            return s
        }
    }

    class CheckboxVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val state = prop.getAs<Boolean>()
            var dodge = false
            val s = Checkbox(
                size = 24f,
                state = state,
            ).onChange { new: Boolean ->
                dodge = true
                prop.setAs(new)
                false
            }
            prop.addCallback {
                if (!dodge) s.toggle(it as Boolean)
                dodge = false
                false
            }
            return s
        }
    }

    class TextVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val placeholder = prop.getMetadata("placeholder") ?: "polyui.textinput.placeholder"
            val validate = prop.getMetadata<String?>("validate")
            val regex = if (validate != null) Regex(validate) else null
            var dodge = false
            val s = BoxedTextInput(
                image = "assets/oneconfig/ico/text.svg".image(),
                placeholder = placeholder,
                size = Vec2(200f, 32f),
                initialValue = prop.getAs(),
            ).onChange { text: String ->
                if (regex != null && !regex.matches(text)) {
                    shake()
                    return@onChange true
                }
                dodge = true
                prop.setAs(text)
                false
            }
            if (validate != null) s.addHoverInfo(Text("Must match regex: $validate"))
            prop.addCallback {
                if (!dodge) (s[1][0] as TextInput).text = it as String
                dodge = false
                false
            }
            return s
        }
    }
}
