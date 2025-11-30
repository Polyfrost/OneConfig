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

import dev.deftu.omnicore.api.client.input.OmniInputs
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer.Companion.strv
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.asMutable
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.State
import org.polyfrost.polyui.input.PolyBind
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Align.Wrap
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.mapToArray
import org.polyfrost.polyui.utils.setNumber
import org.polyfrost.polyui.utils.toString
import java.lang.ref.WeakReference
import java.util.function.Predicate
import kotlin.math.roundToInt

/**
 * Visualizers are procedures that take a property, and return a drawable that represents it.
 */
@Suppress("UNCHECKED_CAST")
fun interface Visualizer {
    fun visualize(prop: Property<*>): Drawable

    fun <T> Property<T>.toState(): State<T> {
        val state = State<T>(getAs())
        var dodge = false

        val stateRef = WeakReference(state)
        var callback: Predicate<T>? = null
        callback = Predicate<T> {
            if (!dodge) {
                dodge = true
                val ret = stateRef.get()?.set(it) ?: run {
                    removeCallback(callback!!)
                    false
                }
                dodge = false
                ret
            } else false
        }
        addCallback(callback)

        state.weaklyListen(this) {
            if (!dodge) {
                dodge = true
                setAs(it)
                dodge = false
            }
        }
        return state
    }

    fun Property<Enum<*>>.toEnumState(): State<Int> {
        val state = State(get()?.ordinal ?: 0)
        var dodge = false

        val stateRef = WeakReference(state)
        var callback: Predicate<Enum<*>>? = null
        callback = Predicate<Enum<*>> {
            if (!dodge) {
                val st = stateRef.get()
                if (st == null) {
                    removeCallback(callback!!)
                    false
                } else {
                    dodge = true
                    val ret = st.set(it.ordinal)
                    dodge = false
                    ret
                }
            } else false
        }
        addCallback(callback)

        state.weaklyListen(this) {
            if (!dodge) {
                dodge = true
                setAs(this.type.enumConstants[it] as Enum<*>)
                dodge = false
            }
        }
        return state
    }

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
                prop.setAsReferential(p.asMutable())
            }
            prop as Property<PolyColor.Mutable>
            val state = prop.toState()
            val out = Block(color = state.value, size = Vec2(58f, 32f)).withBorder(3f, color = { page.border20 })
            out.onClick { ColorPicker(state, polyUI, attachedDrawable = out); true }
            return out
        }
    }

    class DropdownVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val options = prop.getMetadata<Array<String>>("options")
            return if (prop.type.isEnum || prop.type.superclass?.isEnum == true) {
                require(options.isNullOrEmpty()) { "Dropdowns should not have options when used with enums (offender=${prop.id})" }
                val constants = if (prop.type.isEnum) prop.type.enumConstants else prop.type.superclass.enumConstants
                prop as Property<Enum<*>>
                Dropdown(
                    optPadding = 24f,
                    state = prop.toEnumState(),
                    entries = constants.mapToArray {
                        it as Enum<*>
                        null to (it::class.java.fields[0].get(it) as? String ?: it.name)
                    },
                )
            } else {
                require(prop.type == Int::class.java) { "Dropdowns can only be used with enums or integers (offender=${prop.id}, type=${prop.type})" }
                require(options != null) { "Dropdown integer properties must have options specified in their metadata (offender=${prop.id})" }
                require(options.size >= 2) { "Dropdowns must have at least two options (offender=${prop.id})" }
                prop as Property<Int>
                Dropdown(
                    optPadding = 24f,
                    state = prop.toState(),
                    entries = options.mapToArray { null to it },
                )
            }
        }
    }

    class KeybindVisualizer : Visualizer {
        private val KEY_MAPPER: ((Int) -> String) = { OmniInputs.getDisplayName(it) }

        @Suppress("UnstableApiUsage")
        override fun visualize(prop: Property<*>): Drawable {
            return Block(
                Image("assets/oneconfig/ico/keyboard.svg".image(), at = Vec2(7f, 7f)),
                Text(prop.getAs<PolyBind>().keysToString("oneconfig.keybinds.none", KEY_MAPPER)),
                size = Vec2(230f, 32f),
                alignment = Align(main = Align.Content.Center, wrap = Wrap.NEVER),
            ).onInit {
                polyUI.keyBinder?.add(prop.getAs())
            }.withHoverStates().onClick {
                val bind = prop.getAs<PolyBind>()
                val image = this[0] as Image
                val text = this[1] as Text
                text.text = "oneconfig.keybinds.recording"
                image.color = polyUI.colors.state.danger.pressed
                recalculate()
                polyUI.keyBinder?.record(bind) {
                    text.text = bind.keysToString("oneconfig.keybinds.none")
                    if (it == null) shake()
                    image.color = polyUI.colors.text.primary.normal
                    recalculate()
                    needsRedraw = true
                }
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
            val placeholder = prop.getMetadata<String>("placeholder") ?: "${min.toString(dps = 2)}-${max.toString(dps = 2)}"
            prop as Property<out Number>
            val state = prop.toState()
            val s = BoxedTextInput(
                placeholder = placeholder,
                image = "assets/oneconfig/ico/text.svg".image(),
                size = Vec2(200f, 32f),
                value = state,
                post = unit
            )
            s.getTextFromBoxedTextInput().numeric(min, max, state)
            return s
        }
    }

    class RadioVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val options: Array<String>? = prop.getMetadata("options")
            if (prop.type.isEnum) {
                val values = prop.type.enumConstants
                var field = prop.type::class.java.fields.firstOrNull()
                if (field?.type == String::class.java) field = null
                require(options.isNullOrEmpty()) { "Radio button ${prop.id} cannot have options when used with enums" }
                prop as Property<Enum<*>>
                return Radiobutton(
                        entries = values.mapToArray {
                            it as Enum<*>
                            null to (field?.get(it) as? String ?: it.name)
                        },
                        state = prop.toEnumState(),
                        optionLateralPadding = 20f,
                    )
            } else {
                require(prop.type == Int::class.java) { "Radio buttons ${prop.id} can only be used with enum or integer types (type=${prop.type}" }
                require(options != null) { "Radio button ${prop.id} integer properties must have options specified in their metadata" }
                require(options.size >= 2) { "Radio button ${prop.id} must have at least two options" }
                prop as Property<Int>
                return Radiobutton(
                    entries = options.mapToArray { null to it },
                    state = prop.toState(),
                    optionLateralPadding = 20f,
                )
            }
        }
    }

    class SliderVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val min = prop.getMetadata<Float>("min") ?: 0f
            val max = prop.getMetadata<Float>("max") ?: 100f
            val stepAmount = prop.getMetadata<Float>("step") ?: 0f
            val nsteps = if (stepAmount > 0f) ((max - min) / stepAmount).roundToInt() else 0
            prop as Property<out Number>
            val state = prop.toState()

            val f = state.value.toFloat()
            if (f.isNaN() || f.isInfinite()) {
                println("Warning: Slider property ${prop.id} has invalid value $f, resetting to $min")
                state.setNumber(min)
            } else state.setNumber(f.coerceIn(min, max))

            return Slider(
                    min = min,
                    max = max,
                    length = 200f,
                    steps = nsteps,
                    state = state
                )
        }
    }

    class SwitchVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            prop as Property<Boolean>
            val s = Switch(
                lateralStretch = 2f,
                size = 21f,
                state = prop.toState(),
            )
            return s
        }
    }

    class CheckboxVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            prop as Property<Boolean>
            val s = Checkbox(
                size = 24f,
                state = prop.toState(),
            )
            return s
        }
    }

    class TextVisualizer : Visualizer {
        override fun visualize(prop: Property<*>): Drawable {
            val placeholder = prop.getMetadata("placeholder") ?: "polyui.textinput.placeholder"
            val regexString = prop.getMetadata<String?>("regex")
            val regex = regexString?.let { Regex(it) }
            val validate = prop.getMetadata<Predicate<String>>("validate")
            prop as Property<String>
            val state = prop.toState()
            val s = BoxedTextInput(
                image = "assets/oneconfig/ico/text.svg".image(),
                placeholder = placeholder,
                //size = Vec2(200f, 32f),
                value =  state,
            ).onChange(state) { text: String ->
                if (validate != null && !validate.test(text)) {
                    shake(); return@onChange true
                }
                if (regex != null && !regex.matches(text)) {
                    shake(); return@onChange true
                }
                false
            }
            if (regexString != null) s.addHoverInfo(Text("Must match regex: $regexString"))
            return s
        }
    }
}
