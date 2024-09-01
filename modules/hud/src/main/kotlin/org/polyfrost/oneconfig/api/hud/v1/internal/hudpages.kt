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
private val mcFont = FontFamily("Minecraft", "assets/oneconfig/fonts/minecraft")
const val angleSnapMargin = PI / 12.0
const val minMargin = 4f
const val snapMargin = 12f

fun HudsPage(huds: Collection<Hud<*>>): Drawable {
    return Group(
        Group(
            HudButton("oneconfig.huds.all"),
            HudButton("oneconfig.huds.pvp"),
            HudButton("oneconfig.huds.info"),
            HudButton("oneconfig.huds.player"),
            alignment = Align(pad = Vec2(6f, 8f)),
            visibleSize = Vec2(500f, 48f)
        ),
        if (huds.isNotEmpty()) {
            Group(
                children = huds.mapToArray {
                    val preview = it.buildNew()
                    Block(
                        preview,
                        alignment = alignC,
                    ).withBoarder().minimumSize(215f by 80f).withStates().onInit {
                        // #created-with-set-size = true
                        layoutFlags = layoutFlags or 0b00000010
                    }
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
                        it.get().parent.recalculate()
                    }
                }
            }
        }
    }.named("HudsPage")
}

private fun HudButton(text: String): Block {
    return Button(text = text, fontSize = 14f, font = PolyUI.defaultFonts.medium, padding = Vec2(12f, 8f)).radius(6f).withBoarder()
}

fun createInspectionsScreen(hud: Hud<*>): Drawable {
    return Group(
        Radiobutton(
            "assets/oneconfig/ico/paintbrush.svg".image() to "oneconfig.hudeditor.designer.title",
            "assets/oneconfig/ico/cog.svg".image() to "oneconfig.hudeditor.settings.title",
        ).onInit { color = polyUI.colors.component.bgDeselected }.onChange { index: Int ->
            if (index == 0) {
                parent[1] = createDesigner(hud)
            } else {
                parent[1] = createSettings(hud)
            }
            false
        },
        createDesigner(hud),
        visibleSize = Vec2(500f, 800f),
        alignment = Align(cross = Align.Cross.Start),
    )
}

private fun createSettings(hud: Hud<*>): Drawable {
    return HudVisualizer().get(hud.tree)
}

private fun createDesigner(hud: Hud<*>): Drawable {
    val isLegacy = hud is LegacyHud
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
                ).minimumSize(70f by 32f).titled("oneconfig.hudeditor.padding.mode.main"),
                Dropdown(
                    "oneconfig.align.start", "oneconfig.align.center", "oneconfig.align.end",
                ).minimumSize(70f by 32f).titled("oneconfig.hudeditor.padding.mode.cross"),
                BoxedTextInput("assets/oneconfig/ico/info.svg".image(), placeholder = "8", size = Vec2(72f, 0f), post = "px").titled("oneconfig.hudeditor.padding.main"),
                BoxedTextInput("assets/oneconfig/ico/info.svg".image(), placeholder = "6", size = Vec2(72f, 0f), post = "px").titled("oneconfig.hudeditor.padding.cross"),
                size = Vec2(328f, 0f),
            ),
        ),
        *colorOptions(hud.get().parent as Drawable),
        Text("oneconfig.hudeditor.component.title", fontSize = 16f).padded(0f, 18f, 0f, 0f).setFont { medium },
        if (isLegacy) {
            Text("oneconfig.hudeditor.cantedit.aslegacy").secondary()
        } else {
            if ((hud.get().parent.children?.size ?: 0) > 1) {
                Text("oneconfig.hudeditor.choosesomething").padded(3f, 3f).secondary()
            } else {
                when (hud.get()) {
                    is Text -> textOptions(hud.get() as Text)
                    is Block -> Group(*colorOptions(hud.get()))
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
                            Image("assets/oneconfig/hud/align/alignment1.svg".image()).withStates(true).setPalette {
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
                            it.radii = (hud.get().parent as Block).radii
                        }.withBoarder().draggable(withX = false, withY = false)
                            .onDragStart {
                                s0 = (hud.get().parent as Drawable).rotation
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
                                (hud.get().parent as Drawable).rotation = rot
                            }.apply {
                                rotation = (hud.get().parent as Drawable).rotation
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
                        hud.get().parent.let {
                            it as Drawable
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
                        hud.get().parent.let {
                            it as Drawable
                            it.skewX = sx
                            it.skewY = sy
                        }
                    }.withStates(true).setPalette {
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
        }.withStates().draggable(withX = false, withY = false)
            .onDragStart {
                px = polyUI.mouseX
                py = polyUI.mouseY
                val rads = (hud.get().parent as? Block)?.radii
                s2 = rads?.get(0) ?: 0f
            }.onDrag {
                val dx = polyUI.mouseX - px
                val dy = polyUI.mouseY - py
                val bg = (hud.get().parent as Block)
                val bgr = bg.radii ?: return@onDrag
                val m = (s2 + min(dx, dy) * 0.1f).coerceIn(0f, bg.height)
                val display = (this[0][0] as Block).radii ?: return@onDrag
                for (i in bgr.indices) {

                    bgr[i] = m
                    display[i] = m
                }
            },
        size = 125f by 125f,
        alignment = alignC,
    ).withBoarder()
}

fun textOptions(text: Text): Drawable {
    return Group(
        subheading("oneconfig.hudeditor.text.title", "oneconfig.hudeditor.text.info"),
        Block(
            Text("oneconfig.hudeditor.text.example", fontSize = 16f),
            size = Vec2(476f, 58f),
            alignment = alignC,
        ).withBoarder(),
        Dropdown(
            "Poppins", "JetBrains Mono", "Minecraft"
        ).onChange { it: Int ->
            text.font = when (it) {
                1 -> PolyUI.monospaceFont
                2 -> mcFont.get(text.fontWeight, text.italic)
                else -> polyUI.fonts.get(text.fontWeight, text.italic)
            }
            text.parent.recalculate()
            val ex = (parent.parent[1][0] as? Text) ?: return@onChange false
            ex.font = text.font
            ex.parent.recalculate()
            false
        }.titled("oneconfig.hudeditor.text.font"),
        BoxedTextInput("assets/oneconfig/ico/info.svg".image(), placeholder = "1-100", initialValue = text.fontSize.toString(), size = Vec2(72f, 0f), post = "px")
            .apply {
                (this[1][0] as TextInput).numeric(1f, 100f).on(Event.Change.Number) { (it) ->
                    text.fontSize = it.toFloat()
                    text.parent.recalculate()
                    val ex = (parent.parent.parent[1][0] as? Text) ?: return@on true
                    ex.fontSize = text.fontSize
                    ex.parent.recalculate()
                    true
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
            "oneconfig.fweight.600",
            "oneconfig.fweight.700",
            "oneconfig.fweight.800",
            "oneconfig.fweight.900"
        ).onChange { it: Int ->
            text.fontWeight = Font.byWeight((it + 1) * 100)
            text.parent.recalculate()
            val ex = (parent.parent[1][0] as? Text) ?: return@onChange false
            ex.fontWeight = text.fontWeight
            ex.parent.recalculate()
            false
        }.titled("oneconfig.hudeditor.text.weight"),
        Radiobutton(
            "assets/oneconfig/ico/info.svg".image(),
            "assets/oneconfig/ico/info.svg".image(),
            "assets/oneconfig/ico/info.svg".image(),
            optionLateralPadding = 2f,
            optionVerticalPadding = 2f,
        ).onChange { it: Int ->
            false
        }.titled("oneconfig.hudeditor.text.effects"),
        *colorOptions(text),
        size = Vec2(476f, 0f),
        alignment = Align(pad = Vec2(0f, 8f))
    ).namedId("TextOptions")
}

fun colorOptions(drawable: Drawable) = arrayOf(
    subheading("oneconfig.hudeditor.color.title", "oneconfig.hudeditor.color.info"),
    Group(
        Text("oneconfig.hudeditor.color.fill", fontSize = 14f),
        Block(size = 48f by 24f, color = drawable.color.mutable().also { drawable.color = it }).withBoarder(3f).onClick {
            ColorPicker(drawable.color.mutable().ref(), null, null, polyUI)
            false
        },
        if (drawable is Block) Text("oneconfig.hudeditor.color.border", fontSize = 14f) else null,
        if (drawable is Block) Block(size = 48f by 24f, color = drawable.borderColor?.mutable().also { drawable.borderColor = it }).withBoarder(3f).onClick {
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
    Image("assets/oneconfig/ico/info.svg".image()).withStates(showClicker = false).addHoverInfo(Text(desc)),
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
