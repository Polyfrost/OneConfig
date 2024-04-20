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

package org.polyfrost.oneconfig.api.hud.internal

import org.polyfrost.oneconfig.api.hud.Hud
import org.polyfrost.oneconfig.api.hud.LegacyHud
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.LinkedList
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.radii
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min

val alignC = Align(main = Align.Main.Center, cross = Align.Cross.Center)
const val angleSnapMargin = PI / 12.0
const val minMargin = 4f
const val snapMargin = 12f

fun HudsPage(huds: LinkedList<Hud<out Drawable>>): Drawable {
    return Group(
        Group(
            HudButton("oneconfig.all"),
            HudButton("oneconfig.pvp"),
            HudButton("oneconfig.huds.info"),
            HudButton("oneconfig.huds.player"),
            alignment = Align(main = Align.Main.SpaceBetween),
        ),
        if (huds.isNotEmpty()) {
            Group(
                children = huds.map {
                    val preview = it.buildNew()
                    val size = Vec2(if (preview.width > 200f) 452f else 215f, if (preview.height > 70f) 0f else 80f)
                    Block(
                        alignment = alignC,
                        size = size,
                        children = arrayOf(preview),
                    ).withBoarder().withStates()
                }.toTypedArray(),
                visibleSize = Vec2(452f, 800f),
            )
        } else {
            Text("oneconfig.hudeditor.nothinghere", fontSize = 14f, wrap = 300f).secondary()
        },
        visibleSize = Vec2(476f, 800f),
        alignment = Align(cross = Align.Cross.Start, padding = Vec2.ZERO),
    ).onInit {
        if (huds.isNotEmpty()) {
            polyUI.every(1.seconds) {
                if (parent?.renders != true) return@every
                huds.fastEach {
                    if (it.update()) {
                        it.get().parent?.recalculateChildren()
                    }
                }
            }
        }
    }.namedId("HudsPage")
}

private fun HudButton(text: String): Block {
    return Button(text = text, fontSize = 14f, font = PolyUI.defaultFonts.medium, radii = 6f.radii(), padding = Vec2(12f, 8f)).withBoarder()
}

fun createInspectionsScreen(hud: Hud<out Drawable>): Drawable {
    val isLegacy = hud is LegacyHud
    return Group(
        Text("oneconfig.hudeditor.general.title", font = PolyUI.defaultFonts.medium, fontSize = 16f),
        Group(
            Text("oneconfig.hudeditor.padding.title").secondary(),
            Image("assets/oneconfig/ico/info.svg".image()).withStates(showClicker = false).addHoverInfo("oneconfig.hudeditor.padding.info"),
            size = Vec2(450f, 18f),
            alignment = Align(main = Align.Main.SpaceBetween, padding = Vec2.ZERO),
        ),
        Group(
            interactiveAlignment(hud),
            Group(
                Dropdown(
                    entries = arrayOf(
                        "oneconfig.align.start",
                        "oneconfig.align.center",
                        "oneconfig.align.end",
                        "oneconfig.align.spacebetween",
                        "oneconfig.align.spaceevenly",
                    ),
                ).titled("oneconfig.hudeditor.padding.mode.main"),
                Dropdown(
                    padding = 32f,
                    entries = arrayOf("oneconfig.align.start", "oneconfig.align.center", "oneconfig.align.end"),
                ).titled("oneconfig.hudeditor.padding.mode.cross"),
                BoxedTextInput("assets/oneconfig/ico/info.svg".image(), placeholder = "8px", size = Vec2(140f, 32f)).titled("oneconfig.hudeditor.padding.main"),
                BoxedTextInput("assets/oneconfig/ico/info.svg".image(), placeholder = "6px", size = Vec2(140f, 32f)).titled("oneconfig.hudeditor.padding.cross"),
                size = Vec2(375f, 0f),
                alignment = Align(main = Align.Main.SpaceBetween, padding = Vec2.ZERO),
            ),
        ),
        Text("oneconfig.hudeditor.component.title"),
        textOptions(hud.get() as Text),
        Group(
            if (isLegacy) {
                Text("oneconfig.hudeditor.cantedit.aslegacy")
            } else {
                Text("oneconfig.hudeditor.choosesomething")
            },
        ),
        alignment = Align(maxRowSize = 1, cross = Align.Cross.Start),
        visibleSize = Vec2(500f, 800f),
    )
}

private fun interactiveAlignment(hud: Hud<out Drawable>): Drawable {
    var px = 0f
    var py = 0f
    var s0 = 0.0
    var s1 = 0.0
    var s2 = 0f
    return Block(
        size = 125f by 125f,
        children = arrayOf(
            Image("assets/oneconfig/ico/align/alignment2.svg".image(), at = 19f by 19f).draggable(
                withX = false, withY = false,
                onStart = {
                    px = polyUI.mouseX
                    py = polyUI.mouseY
                    hud.get().parent?.let {
                        s0 = it.skewX
                        s1 = it.skewY
                    }
                },
                onDrag = {
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
                    this.parent?.get(2)?.let {
                        it.skewX = sx
                        it.skewY = sy
                    }
                    hud.get().parent?.let {
                        it.skewX = sx
                        it.skewY = sy
                    }
                },
            ).withStates().setPalette {
                Colors.Palette(
                    text.secondary.normal,
                    brand.fg.normal,
                    brand.fg.pressed,
                    text.secondary.disabled,
                )
            }.afterParentInit {
                val bg = hud.get().parent ?: return@afterParentInit
                this.parent?.get(2)?.let {
                    it.skewX = bg.skewX
                    it.skewY = bg.skewY
                }
            },
            Image("assets/oneconfig/ico/align/alignment3.svg".image()).setPalette {
                Colors.Palette(
                    text.secondary.disabled,
                    brand.fg.disabled,
                    brand.fg.disabled,
                    text.secondary.disabled,
                )
            }.withStates().draggable(
                withX = false, withY = false,
                onStart = {
                    px = polyUI.mouseX
                    py = polyUI.mouseY
                    val rads = (hud.get().parent as? Block)?.radii
                    s2 = rads?.get(0) ?: 0f
                },
                onDrag = {
                    val dx = polyUI.mouseX - px
                    val dy = polyUI.mouseY - py
                    val bg = (hud.get().parent as? Block) ?: return@draggable
                    val m = (s2 + min(dx, dy) * 0.1f).coerceIn(0f, bg.height)
                    val display = (this.parent?.get(2) as? Block)?.radii ?: return@draggable
                    for (i in 0..3) {
                        bg.radii[i] = m
                        display[i] = m
                    }
                },
            ).onInit {
                val p = (parent?.get(2) as? Block)?.radii ?: return@onInit
                val bg = (hud.get().parent as? Block)?.radii ?: return@onInit
                for (i in 0..3) {
                    p[i] = bg[i]
                }
            },
            Block(
                size = 57f by 57f,
                children = arrayOf(
                    Image(
                        "assets/oneconfig/ico/align/alignment1.svg".image(),
                    ),
                ),
                alignment = alignC,
            ).withBoarder().withStates().draggable(
                withX = false, withY = false,
                onStart = {
                    s0 = hud.get().parent?.rotation ?: 0.0
                },
                onDrag = {
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
                    hud.get().parent?.rotation = rot
                },
            ).apply {
                rotation = hud.get().parent?.rotation ?: 0.0
            },
        ),
        alignment = alignC,
    ).withBoarder()
}

fun textOptions(text: Text): Drawable {
    return Group(
        Group(
            Text("oneconfig.hudeditor.text.title").secondary(),
            Image("assets/oneconfig/ico/info.svg".image()).addHoverInfo("oneconfig.hudeditor.text.description"),
            alignment = Align(main = Align.Main.SpaceBetween, padding = Vec2.ZERO),
            size = Vec2(450f, 18f),
        ),
        Block(
            size = Vec2(452f, 58f),
            children = arrayOf(
                Text("oneconfig.hudeditor.text.example", fontSize = 16f),
            ),
            alignment = alignC,
        ),
        Dropdown(
            entries = arrayOf(
                "Poppins",
                "JetBrains Mono",
                "Minecraft",
            ),
        ).onChange { it: Int ->
            text.font = when (it) {
                1 -> PolyUI.monospaceFont
                // 2 -> mc
                else -> PolyUI.defaultFonts.regular
            }
            text.parent?.recalculate()
            val ex = (parent?.parent?.get(1)?.get(0) as? Text) ?: return@onChange false
            ex.font = text.font
            ex.parent?.recalculateChildren()
            false
        }.titled("oneconfig.hudeditor.text.font"),
        BoxedTextInput("assets/oneconfig/ico/info.svg".image(), "12px", text.fontSize.toString()).titled("oneconfig.hudeditor.text.size"),
        Radiobutton(
            entries = arrayOf(
                "assets/oneconfig/ico/info.svg".image(),
                "assets/oneconfig/ico/info.svg".image(),
                "assets/oneconfig/ico/info.svg".image(),
            ),
            optionLateralPadding = 2f,
            optionVerticalPadding = 2f,
        ).onChange { it: Int ->
            false
        }.titled("oneconfig.align"),
        Dropdown(
            entries = arrayOf(
                "oneconfig.fweight.100",
                "oneconfig.fweight.200",
                "oneconfig.fweight.300",
                "oneconfig.fweight.400",
                "oneconfig.fweight.500",
                "oneconfig.fweight.600",
                "oneconfig.fweight.700",
                "oneconfig.fweight.800",
                "oneconfig.fweight.900",
            ),
        ).onChange { it: Int ->
            text.fontWeight = Font.byWeight((it + 1) * 100)
            text.parent?.recalculate()
            val ex = (parent?.parent?.get(1)?.get(0) as? Text) ?: return@onChange false
            ex.fontWeight = text.fontWeight
            ex.parent?.recalculateChildren()
            false
        }.titled("oneconfig.hudeditor.text.weight"),
        Radiobutton(
            entries = arrayOf(
                "assets/oneconfig/ico/info.svg".image(),
                "assets/oneconfig/ico/info.svg".image(),
                "assets/oneconfig/ico/info.svg".image(),
            ),
            optionLateralPadding = 2f,
            optionVerticalPadding = 2f,
        ).onChange { it: Int ->
            false
        }.titled("oneconfig.hudeditor.text.effects"),
        size = Vec2(375f, 0f),
    ).namedId("TextOptions")
}

fun Drawable.titled(title: String): Drawable {
    return Group(
        Text(title).secondary(),
        this,
        alignment = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, padding = Vec2(2f, 3f)),
    )
}
