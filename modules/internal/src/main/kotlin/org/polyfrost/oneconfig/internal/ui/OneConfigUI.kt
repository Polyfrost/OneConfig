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

@file:JvmName("OneConfigUI")

package org.polyfrost.oneconfig.internal.ui

import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.PolyUIBuilder
import org.polyfrost.oneconfig.internal.ui.pages.FeedbackPage
import org.polyfrost.oneconfig.internal.ui.pages.ModsPage
import org.polyfrost.oneconfig.internal.ui.pages.ThemesPage
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.operations.Recolor
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.rgba

object OneConfigUI {
    private val playerHead = PolyImage(
        "https://mc-heads.net/avatar/${Platform.player().playerName ?: "Steve"}/24",
        type = PolyImage.Type.Raster,
    ).also {
        it.size = (24f by 24f)
    }
    private val searchNoneFound = Text("oneconfig.search.nonefound", fontSize = 16f)
    private val search = Group(searchNoneFound, visibleSize = Vec2(1130f, 635f))

    lateinit var ui: Drawable


    fun open() {
        val builder = PolyUIBuilder.builder().blurs().backgroundColor(rgba(21, 21, 21)).atResolution(1920f, 1080f).size(1400f, 700f)
        builder.translatorDelegate("assets/oneconfig")
        builder.onClose { _ ->
            for (t in ConfigManager.active().trees()) {
                ConfigManager.active().save(t)
            }
        }
        builder.makeAndOpen(
            Group(
                Block(
                    size = Vec2(225f, 32f),
                ).ignoreLayout().afterParentInit {
                    val modsBtn = parent[3]
                    Move(this, modsBtn.x, modsBtn.y, false).add()
                },
                Image("assets/oneconfig/brand/oneconfig.svg".image()).named("Logo"),
                Text("oneconfig.sidebar.title.options", fontSize = 11f).setPalette { text.secondary }.padded(0f, 24f, 0f, 0f),
                SidebarButton(
                    "assets/oneconfig/ico/settings.svg".image(),
                    "oneconfig.mods",
                ).onClick { openPage(ModsPage(ConfigManager.active().trees()), "oneconfig.mods") },
                SidebarButton(
                    "assets/oneconfig/ico/profiles.svg".image(),
                    "oneconfig.profiles",
                ).addHoverInfo(Text("this feature is experimental and is coming soon!")),
                SidebarButton("assets/oneconfig/ico/keyboard.svg".image(), "oneconfig.keybinds"),
                Text("oneconfig.sidebar.title.personal", fontSize = 11f).setPalette { text.secondary }.padded(0f, 12f, 0f, 0f),
                SidebarButton("assets/oneconfig/ico/paintbrush.svg".image(), "oneconfig.themes", label("oneconfig.soon")).onClick {
                    openPage(ThemesPage(), "oneconfig.themes")
                },
                SidebarButton("assets/oneconfig/ico/cog.svg".image(), "oneconfig.preferences"),
                Text("oneconfig.sidebar.title.extra", fontSize = 11f).setPalette { text.secondary }.padded(0f, 12f, 0f, 0f),
                SidebarButton(
                    "assets/oneconfig/ico/refresh.svg".image(),
                    "oneconfig.changelog",
                ),
                SidebarButton(
                    "assets/oneconfig/ico/text.svg".image(),
                    "oneconfig.feedback",
                    label("oneconfig.beta"),
                ).onClick { openPage(FeedbackPage(), "oneconfig.feedback") },
                SidebarButton0("assets/oneconfig/ico/hud.svg".image(), "oneconfig.edithud").onClick {
                    Platform.screen().display(HudManager.getWithEditor())
                }.padded(0f, 200f, 0f, 0f),
                size = Vec2(273f, 700f),
                alignment = Align(mode = Align.Mode.Vertical, pad = Vec2(6f, 8f)),
            ).named("Sidebar"),
            Group(
                Group(
                    Group(
                        Image("assets/oneconfig/ico/left-arrow.svg".image()).named("Back").disable(), Image("assets/oneconfig/ico/right-arrow.svg".image()).named("Forward").disable(),
                        Text(
                            "oneconfig.mods",
                            fontSize = 24f,
                        ).setFont { medium }.named("Current"),
                        alignment = Align(pad = Vec2(16f, 8f)),
                    ).named("Controls"),
                    Group(
                        Group(
                            Image("assets/oneconfig/ico/cloud.svg".image()),
                            Image(
                                "assets/oneconfig/ico/bell.svg".image(),
                            ),
                            Image(playerHead).radius(6f).named("ProfileImage").withBoarder(
                                rgba(255, 255, 255, 0.14f),
                                width = 1f,
                            ).addHoverInfo(Text(Platform.player().playerName.ifEmpty { "Steve" })),
                            alignment = Align(pad = Vec2(16f, 8f)),
                        ),
                        Block(
                            Image("assets/oneconfig/ico/search.svg".image()),
                            TextInput(
                                placeholder = "oneconfig.search.placeholder",
                                visibleSize = Vec2(210f, 12f),
                            ).onChange { text: String ->
                                if (text.length > 2) {
                                    search.children?.clear()
                                    search.children?.addAll(ConfigVisualizer.INSTANCE.getMatching(text))
                                    if (search.children?.size == 0) search.children?.add(searchNoneFound)
                                    if (ui[1][1] !== search) openPage(search, "oneconfig.search")
                                    search.recalculate()
                                } else if(ui[1][1] === search) {
                                    openPage(ModsPage(ConfigManager.active().trees()), "oneconfig.mods")
                                }
                                false
                            },
                            size = Vec2(256f, 32f),
                            alignment = Align(pad = Vec2(10f, 8f)),
                        ).named("SearchField"),
                        Image(
                            "assets/oneconfig/ico/close.svg".image(),
                        ).named("Close").onClick {
                        }.withStates().setDestructivePalette(),
                        alignment = Align(pad = Vec2(24f, 8f)),
                    ),
                    size = Vec2(1130f, 64f),
                    alignment = Align(main = Align.Main.SpaceBetween),
                ).named("Header"),
                ModsPage(ConfigManager.active().trees()),
                size = Vec2(1127f, 700f),
                alignment = Align(cross = Align.Cross.Start, pad = Vec2.ZERO),
            ),
        ).also {
            ui = it.master
            searchNoneFound.setup(it)
            (ui as Block).radius(8f)
        }
    }

    fun openPage(page: Drawable, name: String) {
        val title = ui[1][0][0][2] as Text
        val translated = ui.polyUI.translator.translate(name)
        title.text = translated.string
        ui[1][1] = page
    }

    fun label(text: String): Drawable {
        return Block(
            Text(text).setFont { bold },
            alignment = Align(main = Align.Main.Center),
            size = Vec2(54f, 18f),
        ).setPalette { brand.fg }
    }


    private val sidebarBtnAlign = Align(pad = Vec2(16f, 6f))

    fun SidebarButton(image: PolyImage, text: String, extra: Drawable? = null): Group {
        return SidebarButton0(image, text, extra).onClick { _ ->
            val it = parent[0]
            Move(it, this.x, this.y, false, Animations.Default.create(0.15.seconds)).add()
            false
        }
    }

    fun SidebarButton0(image: PolyImage, text: String, extra: Drawable? = null): Group {
        return Group(
            Image(image),
            Text(text, fontSize = 14f),
            extra,
            size = Vec2(225f, 33f),
            alignment = sidebarBtnAlign,
        ).namedId("SidebarButton").apply {
            on(Event.Mouse.Entered) {
                Recolor(this[1], this[1].palette.hovered, Animations.Default.create(0.08.seconds)).add()
                polyUI.cursor = Cursor.Clicker
                false
            }
            on(Event.Mouse.Exited) {
                Recolor(this[1], this[1].palette.normal, Animations.Default.create(0.08.seconds)).add()
                polyUI.cursor = Cursor.Pointer
                false
            }
            on(Event.Mouse.Pressed) {
                Recolor(this[1], this[1].palette.pressed, Animations.Default.create(0.08.seconds)).add()
                false
            }
            on(Event.Mouse.Released) {
                Recolor(this[1], this[1].palette.hovered, Animations.Default.create(0.08.seconds)).add()
                false
            }
        }
    }
}
