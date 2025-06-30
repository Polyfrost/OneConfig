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
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.mutable
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
import kotlin.math.atan2
import kotlin.math.min

val alignC = Align(main = Align.Main.Center, cross = Align.Cross.Center)
val alignNoPad = Align(pad = Vec2.ZERO)
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
                    *hudMap.filterKeys { it == Hud.Category.COMBAT }.values.toTypedArray(),
                    visibleSize = Vec2(500f, 800f),
                )
            },
            HudButton("oneconfig.huds.info").onClick {
                parent[1] = Group(
                    *hudMap.filterKeys { it == Hud.Category.INFO }.values.toTypedArray(),
                    visibleSize = Vec2(500f, 800f),
                )
            },
            HudButton("oneconfig.huds.player").onClick {
                parent[1] = Group(
                    *hudMap.filterKeys { it == Hud.Category.PLAYER }.values.toTypedArray(),
                    visibleSize = Vec2(500f, 800f),
                )
            },
            alignment = Align(pad = Vec2(6f, 8f)),
            visibleSize = Vec2(500f, 48f)
        ),
        if (huds.isNotEmpty()) {
            Group(
                children = huds.mapToArray {
                    val preview = it.buildNew()
                    val obj = Block(
                        preview,
                        alignment = alignC,
                    ).withBorder(2f) { page.border10 }.minimumSize(215f by 80f).withHoverStates().onInit {
                        // #created-with-set-size = true
                        layoutFlags = layoutFlags or 0b00000010
                    }
                    hudMap[it.category()] = obj
                    obj
                },
                visibleSize = Vec2(500f, 800f),
            )
        } else {
            Text("oneconfig.hudeditor.nothinghere", fontSize = 14f).secondary()
        },
        size = Vec2(500f, 0f),
    ).onInit {
        if (huds.isNotEmpty()) {
            polyUI.every(1.seconds) {
                if (!HudManager.panelExists) return@every
                huds.forEach {
                    if (it.update()) {
                        it.getBackground()?.recalculate()
                    }
                }
            }
        }
    }.named("HudsPage")
}

private fun HudButton(text: String): Block {
    return Button(text = text, fontSize = 14f, font = PolyUI.defaultFonts.medium, padding = Vec2(12f, 8f)).radius(6f).withBorder()
}

fun createInspectionsScreen(hud: Hud<*>): Drawable {
    return Group(
        Radiobutton(
            "assets/oneconfig/ico/cog.svg".image() to "oneconfig.hudeditor.settings.title",
            "assets/oneconfig/ico/paintbrush.svg".image() to "oneconfig.hudeditor.designer.title",
        ).onInit { color = polyUI.colors.component.bgDeselected }.onChange { index: Int ->
            if (index == 0) {
                parent[1] = createSettings(hud)
            } else {
                parent[1] = createDesigner(hud)
            }
            false
        },
        createSettings(hud),
        visibleSize = Vec2(500f, 800f),
        alignment = Align(cross = Align.Cross.Start),
    )
}

private fun createSettings(hud: Hud<*>): Drawable {
    return HudVisualizer().get(hud.tree)
}

private fun createDesigner(hud: Hud<*>): Drawable {
    val isLegacy = hud is LegacyHud
    val theHud = hud.get()
    val bg = hud.getBackground()
    val receiver = bg ?: theHud
    return Group(
        Text("oneconfig.hudeditor.general.title", fontSize = 16f).setFont { medium },
        subheading("oneconfig.hudeditor.padding.title", "oneconfig.hudeditor.padding.info"),
        Group(
            interactiveAlignment(hud),
            Group(
                Dropdown(
                    "oneconfig.align.start",
                    "oneconfig.align.center",
                    "oneconfig.align.end",
                    "oneconfig.align.spacebetween",
                    "oneconfig.align.spaceevenly",
                ).minimumSize(70f by 32f).titled("oneconfig.hudeditor.padding.mode.main").onChange { index: Int ->
                    val a = receiver.alignment
                    receiver.alignment = Align(Align.Main.entries[index], a.cross, a.mode, a.pad, a.maxRowSize)
                    false
                },
                Dropdown(
                    "oneconfig.align.start", "oneconfig.align.center", "oneconfig.align.end",
                ).minimumSize(70f by 32f).titled("oneconfig.hudeditor.padding.mode.cross").onChange { index: Int ->
                    val a = receiver.alignment
                    receiver.alignment = Align(a.main, Align.Cross.entries[index], a.mode, a.pad, a.maxRowSize)
                    false
                },
                BoxedNumericInput("assets/oneconfig/ico/info.svg".image(), initialValue = receiver.alignment.pad.x, size = Vec2(72f, 0f), post = "px").also {
                    it[0].onChange { value: Float ->
                        val a = receiver.alignment
                        receiver.alignment = Align(a.main, a.cross, a.mode, Vec2(value, a.pad.y), a.maxRowSize)
                        false
                    }
                }.titled("oneconfig.hudeditor.padding.main"),
                BoxedNumericInput("assets/oneconfig/ico/info.svg".image(), initialValue = receiver.alignment.pad.y, size = Vec2(72f, 0f), post = "px").also {
                    it[0].onChange { value: Float ->
                        val a = receiver.alignment
                        receiver.alignment = Align(a.main, a.cross, a.mode, Vec2(a.pad.x, value), a.maxRowSize)
                        false
                    }
                }.titled("oneconfig.hudeditor.padding.cross"),
                size = Vec2(328f, 0f),
            ),
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
        visibleSize = Vec2(480f, 800f),
    )
}

private fun interactiveAlignment(hud: Hud<*>): Drawable {
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
        BoxedNumericInput("assets/oneconfig/ico/info.svg".image(), initialValue = text.fontSize, min = 1f, size = Vec2(72f, 0f), post = "px").also {
            it[0].onChange { value: Float ->
                text.fontSize = value
                text._parent?.recalculate()
                val ex = (parent.parent.parent[1][0] as? Text) ?: return@onChange false
                ex.fontSize = text.fontSize
                ex.parent.recalculate()
                false
            }
        }.titled("oneconfig.hudeditor.text.size"),
        Radiobutton(
            "assets/oneconfig/ico/info.svg".image(),
            "assets/oneconfig/ico/info.svg".image(),
            "assets/oneconfig/ico/info.svg".image(),
            optionLateralPadding = 2f,
            optionVerticalPadding = 2f,
        ).onChange { it: Int ->
            false
        }.titled("oneconfig.align"),
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
            Block(Image("assets/oneconfig/ico/info.svg"), alignment = alignNoPad).radius(2f).toggleable(text.fontWeight.value > 500).onToggle {
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
            Block(Image("assets/oneconfig/ico/info.svg"), alignment = alignNoPad).radius(2f).toggleable(text.italic).onToggle { text.italic = it },
            Block(Image("assets/oneconfig/ico/info.svg"), alignment = alignNoPad).radius(2f).toggleable(text.strikethrough).onToggle { text.strikethrough = it },
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

fun Drawable.titled(title: String): Drawable {
    return Group(
        Text(title).secondary(),
        this,
        alignment = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, pad = Vec2(2f, 3f)),
    )
}
