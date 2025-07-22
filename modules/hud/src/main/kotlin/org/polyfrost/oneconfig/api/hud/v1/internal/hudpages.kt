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

@file:Suppress("UnstableApiUsage")

package org.polyfrost.oneconfig.api.hud.v1.internal

import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.mutable
import org.polyfrost.polyui.color.rgba
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.FontFamily
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.mapToArray
import org.polyfrost.polyui.utils.ref
import kotlin.experimental.or
import kotlin.math.PI

val alignC = Align(main = Align.Main.Center, cross = Align.Cross.Center)
val alignNoPad = Align(pad = Vec2.ZERO)
val alignHudDefault = Align(main = Align.Main.Center, cross = Align.Cross.Center, pad = Vec2(8f, 8f))
val BLACK_HALF = rgba(0, 0, 0, 0.5f)
private val mcFont = FontFamily("Minecraft", "assets/oneconfig/fonts/minecraft")
const val angleSnapMargin = PI / 12.0
const val minMargin = 4f
const val snapMargin = 12f

fun HudsPage(huds: Collection<Hud<*>>): Drawable {
    val hudMap = HashMap<Hud.Category, Drawable>()
    return Group(
        Group(
            HudButton("oneconfig.huds.all").onClick {
                parent[1] = Group(
                    *hudMap.values.toTypedArray(),
                    visibleSize = Vec2(500f, 800f),
                )
            },
            HudButton("oneconfig.huds.pvp").onClick {
                parent[1] = Group(
                    *hudMap.filterValuesByKey { it == Hud.Category.COMBAT }.toTypedArray(),
                    visibleSize = Vec2(500f, 800f),
                )
            },
            HudButton("oneconfig.huds.info").onClick {
                parent[1] = Group(
                    *hudMap.filterValuesByKey { it == Hud.Category.INFO }.toTypedArray(),
                    visibleSize = Vec2(500f, 800f),
                )
            },
            HudButton("oneconfig.huds.player").onClick {
                parent[1] = Group(
                    *hudMap.filterValuesByKey { it == Hud.Category.PLAYER }.toTypedArray(),
                    visibleSize = Vec2(500f, 800f),
                )
            },
            alignment = Align(pad = Vec2(6f, 8f)),
            size = Vec2(452f, 48f)
        ).padded(18f, 0f).named("HudsPageFilterButtons"),
        if (huds.isNotEmpty()) {
            Group(
                children = huds.mapToArray {
                    val preview = it.buildNew()
                    val obj = Block(
                        preview,
                        alignment = alignC,
                    ).withBorder().minimumSize(215f by 80f).withHoverStates().onInit {
                        // #created-with-set-size = true
                        layoutFlags = layoutFlags or 0b00000010
                    }
                    hudMap[it.category] = obj
                    obj
                },
                alignment = Align(pad = Vec2(22f, 22f)),
                size = Vec2(500f, 0f),
                visibleSize = Vec2(500f, 800f),
            )
        } else {
            Text("oneconfig.hudeditor.nothinghere", fontSize = 14f).secondary()
        },
        size = Vec2(500f, 0f),
        alignment = Align(main = Align.Main.SpaceBetween, pad = Vec2.ZERO)
    ).onInit {
        if (huds.isNotEmpty()) {
            polyUI.every(1.seconds) {
                if (!HudManager.panelExists) return@every
                huds.forEach {
                    if (it.update()) it.getBackground()?.recalculate()
                }
            }
        }
    }.named("HudsPage")
}

inline fun <K, reified V> Map<K, V>.filterValuesByKey(predicate: (K) -> Boolean): MutableList<V> {
    val out = mutableListOf<V>()
    for ((key, value) in this) {
        if (predicate(key)) {
            out.add(value)
        }
    }
    return out
}

private fun HudButton(text: String): Block {
    return Button(text = text, fontSize = 14f, font = PolyUI.defaultFonts.medium, padding = Vec2(12f, 8f)).radius(6f).withBorder()
}

fun HudSettingsPage(hud: Hud<*>): Drawable {
    return Group(
        Radiobutton(
            "assets/oneconfig/ico/cog.svg".image() to "oneconfig.hudeditor.settings.title",
            "assets/oneconfig/ico/paintbrush.svg".image() to "oneconfig.hudeditor.designer.title",
        ).onInit { color = polyUI.colors.component.bgDeselected }.onChange { index: Int ->
            if (index == 0) {
                parent[1] = HudVisualizer.get(hud.tree)
            } else {
                parent[1] = makeHudDesigner(hud)
            }
            false
        },
        HudVisualizer.get(hud.tree),
        alignment = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, wrap = Align.Wrap.NEVER),
    ).namedId("HudSettingsPage")
}

private fun makeHudDesigner(hud: Hud<*>): Drawable {
    val isLegacy = hud is LegacyHud
    val theHud = hud.get()
    val bg = hud.getBackground()
    val receiver = bg ?: theHud
    return Group(
        Text("oneconfig.hudeditor.general.title", fontSize = 16f).setFont { medium },
        subheading("oneconfig.hudeditor.padding.title", "oneconfig.hudeditor.padding.info"),
        interactiveAlignment(receiver),
        Group(
            Group(
                DraggingNumericTextInput(icon = "assets/oneconfig/ico/align.svg".image(), suffix = "px", max = 30f, size = Vec2(120f, 32f)).also {
                    it[0].onChange { value: Float ->
                        receiver.alignment = receiver.alignment.copy(pad = Vec2(value, value))
                        receiver.recalculate()
                        false
                    }
                }.titled("oneconfig.hudeditor.padding.between", pad = Vec2(2f, 0f)),
                DraggingNumericTextInput(icon = "assets/oneconfig/ico/maximise.svg".image(), suffix = "px", max = 10f, size = Vec2(120f, 32f)).also {
                    it[0].onChange { value: Float ->
                        (receiver as? Block)?.radius(value)
                        false
                    }
                }.titled("oneconfig.hudeditor.corner.radius", pad = Vec2(2f, 0f)),
                alignment = Align(pad = Vec2(16f, 0f))
            ),
            Group(
                DraggingNumericTextInput(icon = "assets/oneconfig/ico/align.svg".image(), suffix = "px", initialValue = receiver.padding.x, max = 30f, size = Vec2(68f, 32f)).onChange { value: Float ->
                    receiver.padding = receiver.padding.copy(x = value)
                },
                DraggingNumericTextInput(icon = "assets/oneconfig/ico/align.svg".image(), suffix = "px", initialValue = receiver.padding.y, max = 30f, size = Vec2(68f, 32f)).onChange { value: Float ->
                    receiver.padding = receiver.padding.copy(y = value)
                }.also { it[0].rotation = PI / 2 },
                DraggingNumericTextInput(icon = "assets/oneconfig/ico/align.svg".image(), suffix = "px", initialValue = receiver.padding.w, max = 30f, size = Vec2(68f, 32f)).onChange { value: Float ->
                    receiver.padding = receiver.padding.copy(w = value)
                }.also { it[0].rotation = PI },
                DraggingNumericTextInput(icon = "assets/oneconfig/ico/align.svg".image(), suffix = "px", initialValue = receiver.padding.h, max = 30f, size = Vec2(68f, 32f)).onChange { value: Float ->
                    receiver.padding = receiver.padding.copy(h = value)
                }.also { it[0].rotation = PI * 1.5 },
                alignment = Align(main = Align.Main.SpaceBetween, wrap = Align.Wrap.NEVER),
                size = Vec2(308f, 32f)
            ).titled("oneconfig.hudeditor.padding.edges").padded(16f, 12f, 0f, 0f),
            alignment = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, wrap = Align.Wrap.NEVER, pad = Vec2.ZERO)
        ),
        Group(
            Checkbox(size = 18f).onToggle {
                hud.staticWidth = it
            },
            Text("oneconfig.hudeditor.staticwidth").padded(-2f, 0f, 12f, 0f),
            DraggingNumericTextInput(pre = "oneconfig.width", suffix = "px", initialValue = receiver.width, max = 1000f, size = Vec2(128f, 32f)),
            DraggingNumericTextInput(pre = "oneconfig.height", suffix = "px", initialValue = receiver.height, max = 1000f, size = Vec2(128f, 32f)),
            alignment = Align(pad = Vec2(12f, 6f))
        ),
        *(if (bg != null) colorOptions(bg) else arrayOf()),
        Text("oneconfig.hudeditor.component.title", fontSize = 16f).padded(0f, 18f, 0f, 0f).setFont { medium },
        if (isLegacy) {
            Text("oneconfig.hudeditor.cantedit.aslegacy").secondary()
        } else {
            if ((bg?.children?.size ?: 0) > 1) {
                Text("oneconfig.hudeditor.choosesomething").padded(3f, 3f).secondary()
            } else {
                when (theHud) {
                    is Text -> textOptions(theHud)
                    is Block -> Group(*colorOptions(theHud))
                    else -> Text("oneconfig.hudeditor.component.notimplemented").padded(3f, 3f).secondary()
                }
            }
        },
        alignment = Align(cross = Align.Cross.Start),
        size = Vec2(480f, 0f),
    )
}

fun interactiveAlignment(recv: Drawable): Block {
    val short = Block(size = Vec2(9f, 4f)).setPalette { text.secondary }.radius(2f)
    val medium = Block(size = Vec2(14f, 4f)).setPalette { text.secondary }.radius(2f)
    val long = Block(size = Vec2(18f, 4f)).setPalette { text.secondary }.radius(2f)
    val shortBlue = Block(size = Vec2(9f, 4f)).setPalette { brand.fg }.radius(2f)
    val mediumBlue = Block(size = Vec2(14f, 4f)).setPalette { brand.fg }.radius(2f)
    val longBlue = Block(size = Vec2(18f, 4f)).setPalette { brand.fg }.radius(2f)

    val theGrid: Image
    val theLittleBars = Group(
        medium, long, short,
        size = Vec2(20f, 20f),
        alignment = Align(pad = Vec2(2f, 2f))
    ).ignoreLayout()
    val theBlueLittleBars = Group(
        mediumBlue, longBlue, shortBlue,
        size = Vec2(20f, 20f),
        alignment = Align(pad = Vec2(2f, 2f))
    ).ignoreLayout()

    val o = Block(
        Image(
            "assets/oneconfig/hud/align/background.svg".image(),
            children = arrayOf(
                *repeat(9) {
                    Image("assets/oneconfig/hud/align/circle.svg").onHover { _ ->
                        theLittleBars.renders = true
                        needsRedraw = true
                        val mainAlign = when (it) {
                            0, 3, 6 -> Align.Main.Start
                            1, 4, 7 -> Align.Main.Center
                            else -> Align.Main.End
                        }
                        theLittleBars.alignment = Align(main = mainAlign, pad = Vec2(2f, 2f))
                        val bars = theLittleBars.children ?: return@onHover
                        // reorder the bars to give a much clearer visual representation of the cross alignment.
                        when (it) {
                            0, 1, 2 -> bars.set(long, medium, short)
                            3, 4, 5 -> bars.set(medium, long, short)
                            6, 7, 8 -> bars.set(short, medium, long)
                        }
                        theLittleBars.position()
                        theLittleBars.at = this.at + (this.size / 2f) - (theLittleBars.size / 2f)
                    }.onClick { _ ->
                        needsRedraw = true
                        theLittleBars.renders = false
                        val mainAlign = when (it) {
                            0, 3, 6 -> Align.Main.Start
                            1, 4, 7 -> Align.Main.Center
                            else -> Align.Main.End
                        }
                        theBlueLittleBars.alignment = Align(main = mainAlign, pad = Vec2(2f, 2f))
                        val bars = theBlueLittleBars.children ?: return@onClick
                        // reorder the bars to give a much clearer visual representation of the cross alignment.
                        when (it) {
                            0, 1, 2 -> bars.set(longBlue, mediumBlue, shortBlue)
                            3, 4, 5 -> bars.set(mediumBlue, longBlue, shortBlue)
                            6, 7, 8 -> bars.set(shortBlue, mediumBlue, longBlue)
                        }
                        theBlueLittleBars.position()
                        theBlueLittleBars.at = this.at + (this.size / 2f) - (theBlueLittleBars.size / 2f)
                        recv.alignment = indexToAlign(it, recv.alignment)
                    }
                },
                theLittleBars, theBlueLittleBars
            ),
            size = Vec2(90f, 90f),
            alignment = Align(pad = Vec2.ZERO)
        ).onHoverExit {
            theLittleBars.renders = false
            needsRedraw = true
        }.also { theGrid = it },
        alignment = Align(main = Align.Main.Center),
        size = Vec2(125f, 125f)
    ).withBorder()

    theBlueLittleBars.onInit {
        theGrid[alignToIndex(recv.alignment)].accept(Event.Mouse.Clicked)
    }
    return o
}

fun textOptions(text: Text): Drawable {
    var prevWeight: Font.Weight = Font.Weight.Regular
    return Group(
        subheading("oneconfig.hudeditor.text.title", "oneconfig.hudeditor.text.info"),
        Block(
            Text("oneconfig.hudeditor.text.example", fontSize = 16f),
            size = Vec2(476f, 58f),
            alignment = alignC,
        ).withBorder(),
        Dropdown(
            "Poppins", "JetBrains Mono", "Minecraft"
        ).onChange { it: Int ->
            text.font = when (it) {
                1 -> PolyUI.monospaceFont
                2 -> mcFont.get(text.fontWeight, text.italic)
                else -> polyUI.fonts.get(text.fontWeight, text.italic)
            }
            text._parent?.recalculate()
            val ex = (parent.parent[1][0] as? Text) ?: return@onChange false
            ex.font = text.font
            ex.parent.recalculate()
            false
        }.titled("oneconfig.hudeditor.text.font"),
        DraggingNumericTextInput("assets/oneconfig/ico/text-input.svg".image(), initialValue = text.fontSize, min = 1f, size = Vec2(72f, 0f), suffix = "px").also {
            it[0].onChange { value: Float ->
                text.fontSize = value
                text._parent?.recalculate()
                val ex = (parent.parent.parent[1][0] as? Text) ?: return@onChange false
                ex.fontSize = text.fontSize
                ex.parent.recalculate()
                false
            }
        }.titled("oneconfig.hudeditor.text.size"),
        Dropdown(
            "oneconfig.fweight.100",
            "oneconfig.fweight.200",
            "oneconfig.fweight.300",
            "oneconfig.fweight.400",
            "oneconfig.fweight.500",
        ).onChange { it: Int ->
            text.fontWeight = Font.byWeight((it + 1) * 100)
            text._parent?.recalculate()
            val ex = (parent.parent[1][0] as? Text) ?: return@onChange false
            ex.fontWeight = text.fontWeight
            ex.parent.recalculate()
            false
        }.titled("oneconfig.hudeditor.text.weight"),
        Group(
            Block(Image("assets/oneconfig/ico/bold.svg"), alignment = alignNoPad).radius(2f).toggleable(text.fontWeight.value > 500).onToggle {
                if (it) {
                    prevWeight = text.fontWeight
                    text.fontWeight = when (text.fontWeight) {
                        Font.Weight.Thin, Font.Weight.ExtraLight, Font.Weight.Light -> Font.Weight.SemiBold
                        Font.Weight.Regular -> Font.Weight.Bold
                        Font.Weight.Medium -> Font.Weight.ExtraBold
                        else -> text.fontWeight
                    }
                } else {
                    text.fontWeight = prevWeight
                }
            },
            Block(Image("assets/oneconfig/ico/italic.svg"), alignment = alignNoPad).radius(2f).toggleable(text.italic).onToggle { text.italic = it },
            Block(Image("assets/oneconfig/ico/underline.svg"), alignment = alignNoPad).radius(2f).toggleable(text.underline).onToggle { text.underline = it },
            Block(Image("assets/oneconfig/ico/strikethrough.svg"), alignment = alignNoPad).radius(2f).toggleable(text.strikethrough).onToggle { text.strikethrough = it },
        ).titled("oneconfig.hudeditor.text.effects"),
        *colorOptions(text),
        size = Vec2(476f, 0f),
        alignment = Align(pad = Vec2(0f, 8f))
    ).namedId("TextOptions")
}

fun colorOptions(drawable: Drawable) = arrayOf(
    subheading("oneconfig.hudeditor.color.title", "oneconfig.hudeditor.color.info"),
    Group(
        Text("oneconfig.hudeditor.color.fill", fontSize = 14f),
        Block(size = 48f by 24f, color = drawable.color.mutable().also { drawable.color = it }).withBorder(3f).onClick {
            ColorPicker(drawable.color.mutable().ref(), null, null, polyUI)
            false
        },
        if (drawable is Block) Text("oneconfig.hudeditor.color.border", fontSize = 14f) else null,
        if (drawable is Block) Block(size = 48f by 24f, color = drawable.borderColor?.mutable().also { drawable.borderColor = it }).withBorder(3f).onClick {
            val color = (drawable.borderColor ?: polyUI.colors.page.border20).mutable().also { drawable.borderColor = it }
            ColorPicker(color.ref(), null, null, polyUI)
            false
        } else null,
        size = Vec2(476f, 0f),
        alignment = Align(main = Align.Main.SpaceBetween),
    )
)

fun subheading(title: String, desc: String) = Group(
    Text(title).secondary(),
    Image("assets/oneconfig/ico/info.svg".image()).withHoverStates(showClicker = false).addHoverInfo(Text(desc)),
    size = Vec2(476f, 18f),
    alignment = Align(main = Align.Main.SpaceBetween),
)

fun Drawable.titled(title: String, pad: Vec2 = Vec2(2f, 7f)): Drawable {
    return Group(
        Text(title, fontSize = 14f).secondary(),
        this,
        alignment = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, pad = pad),
    )
}

inline fun <reified T> repeat(n: Int, block: (Int) -> T): Array<T> {
    return Array(n) { block(it) }
}

fun <E> MutableList<E>.set(a: E, b: E, c: E): MutableList<E> {
    this.clear()
    this.add(a)
    this.add(b)
    this.add(c)
    return this
}

fun indexToAlign(index: Int, old: Align): Align {
    val main = when (index) {
        0, 3, 6 -> Align.Main.Start
        1, 4, 7 -> Align.Main.Center
        else -> Align.Main.End
    }
    val cross = when (index) {
        0, 1, 2 -> Align.Cross.Start
        3, 4, 5 -> Align.Cross.Center
        else -> Align.Cross.End
    }
    return Align(main = main, cross = cross, mode = old.mode, pad = old.pad, wrap = old.wrap)
}

fun alignToIndex(align: Align): Int {
    val row = when (align.cross) {
        Align.Cross.Start -> 0
        Align.Cross.Center -> 3
        Align.Cross.End -> 6
    }
    val col = when (align.main) {
        Align.Main.Center -> 1
        Align.Main.End -> 2
        else -> 0
    }
    return row + col
}


/// GRAVEYARD ///
/* private fun interactiveAlignment(hud: Hud<*>): Drawable {
    var px = 0f
    var py = 0f
    var s0 = 0.0
    var s1 = 0.0
    var s2 = 0f
    val theHud = hud.get()
    val bg = hud.getBackground()
    val receiver = bg ?: theHud
    return Block(
        Image(
            "assets/oneconfig/hud/align/alignment3.svg".image(),
            alignment = alignC,
            children = arrayOf(
                Image(
                    "assets/oneconfig/hud/align/alignment2.svg".image(),
                    alignment = alignC,
                    children = arrayOf(
                        Block(
                            Image("assets/oneconfig/hud/align/alignment1.svg".image()).withHoverStates(true).setPalette {
                                Colors.Palette(
                                    text.primary.normal,
                                    brand.fg.normal,
                                    brand.fg.pressed,
                                    text.primary.disabled,
                                )
                            },
                            size = 57f by 57f,
                            alignment = alignC,
                        ).also {
                            if (bg != null) it.radii = bg.radii
                        }.withBorder().draggable(withX = false, withY = false)
                            .onDragStart {
                                s0 = receiver.rotation
                            }.onDrag {
                                var rot = s0 + (atan2(((y + height / 2f) - polyUI.mouseY).toDouble(), ((x + width / 2f) - polyUI.mouseX).toDouble()) - PI / 2.0)
                                val low = rot - angleSnapMargin
                                val help = rot + angleSnapMargin
                                if (PI / 2.0 in low..help) {
                                    rot = PI / 2.0
                                } else if (0.0 in low..help) {
                                    rot = 0.0
                                } else if (-PI in low..help) {
                                    rot = -PI
                                } else if (-PI / 2.0 in low..help) {
                                    rot = -PI / 2.0
                                }
                                rotation = rot
                                receiver.rotation = rot
                            }.apply {
                                rotation = receiver.rotation
                            }.events {
                                Event.Mouse.Companion.Pressed then {
                                    this[0].accept(it)
                                }
                                Event.Mouse.Companion.Released then {
                                    this[0].accept(it)
                                }
                                Event.Mouse.Entered then {
                                    this[0].accept(it)
                                }
                                Event.Mouse.Exited then {
                                    this[0].accept(it)
                                }
                            },
                    )
                ).draggable(withX = false, withY = false)
                    .onDragStart {
                        px = polyUI.mouseX
                        py = polyUI.mouseY
                        receiver.let {
                            s0 = it.skewX
                            s1 = it.skewY
                        }
                    }.onDrag {
                        val dx = polyUI.mouseX - px
                        val dy = polyUI.mouseY - py

                        var sx = (s0 + (dx.toDouble() * 0.003)).coerceIn(-(PI / 4.0), PI / 4.0)
                        var sy = (s1 + (dy.toDouble() * 0.003)).coerceIn(-(PI / 4.0), PI / 4.0)
                        if (sx in -(PI / 24.0)..(PI / 24.0)) {
                            sx = 0.0
                        }
                        if (sy in -(PI / 24.0)..(PI / 24.0)) {
                            sy = 0.0
                        }
                        this[0].let {
                            it.skewX = sx
                            it.skewY = sy
                        }
                        receiver.let {
                            it.skewX = sx
                            it.skewY = sy
                        }
                    }.withHoverStates(true).setPalette {
                        Colors.Palette(
                            text.secondary.normal,
                            brand.fg.normal,
                            brand.fg.pressed,
                            text.secondary.disabled,
                        )
                    },
            )
        ).setPalette {
            Colors.Palette(
                text.secondary.disabled,
                brand.fg.disabled,
                brand.fg.disabled,
                text.secondary.disabled,
            )
        }.withHoverStates().draggable(withX = false, withY = false)
            .onDragStart {
                px = polyUI.mouseX
                py = polyUI.mouseY
                val rads = (receiver as? Block)?.radii
                s2 = rads?.get(0) ?: 0f
            }.onDrag {
                val dx = polyUI.mouseX - px
                val dy = polyUI.mouseY - py
                val bgr = (receiver as? Block)?.radii ?: return@onDrag
                val m = (s2 + min(dx, dy) * 0.1f).coerceIn(0f, receiver.height)
                val display = (this[0][0] as Block).radii ?: return@onDrag
                for (i in bgr.indices) {
                    bgr[i] = m
                    display[i] = m
                }
            },
        size = 125f by 125f,
        alignment = alignC,
    ).withBorder()
}
 */