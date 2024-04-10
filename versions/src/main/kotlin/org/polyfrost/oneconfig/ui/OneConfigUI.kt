/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.ui

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.ConfigManager
import org.polyfrost.oneconfig.api.hud.HudManager
import org.polyfrost.oneconfig.libs.universal.UScreen
import org.polyfrost.oneconfig.ui.pages.FeedbackPage
import org.polyfrost.oneconfig.ui.pages.ModsPage
import org.polyfrost.oneconfig.ui.pages.ThemesPage
import org.polyfrost.oneconfig.ui.screen.PolyUIScreen
import org.polyfrost.polyui.PolyUI
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
import org.polyfrost.polyui.utils.radii
import org.polyfrost.polyui.utils.rgba


object OneConfigUI {
    @ApiStatus.Internal
    var username: String = ""

    lateinit var ui: Drawable


    fun create(): PolyUIScreen {
        val vertical = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical)

        return PolyUIScreen(
            null, null, null, null, null, rgba(21, 21, 21),
            1920f by 1080f, 1400f by 700f,
            Group(
                size = Vec2(273f, 700f),
                alignment = Align(mode = Align.Mode.Vertical, padding = Vec2(12f, 16f)),
                children = arrayOf(
                    Block(
                        size = Vec2(225f, 32f),
                    ).hide().afterParentInit {
                        renders = true
                        val modsBtn = parent!![2][1]
                        Move(this, modsBtn.x, modsBtn.y, false).add()
                    },
                    Image("oneconfig.svg".image()).named("Logo"),
                    Group(
                        alignment = vertical,
                        children = arrayOf(
                            Text("oneconfig.sidebar.title.options", fontSize = 11f).setPalette { text.secondary },
                            SidebarButton(
                                "settings.svg".image(),
                                "oneconfig.mods",
                            ).onClick { openPage(ModsPage(ConfigManager.INSTANCE.trees()), "oneconfig.mods") },
                            SidebarButton(
                                "profiles.svg".image(),
                                "oneconfig.profiles",
                            ).disable().addHoverInfo("this feature is experimental and is coming soon!"),
                            SidebarButton("keyboard.svg".image(), "oneconfig.keybinds").disable(),
                        ),
                    ),
                    Group(
                        alignment = vertical,
                        children = arrayOf(
                            Text("oneconfig.sidebar.title.personal", fontSize = 11f).setPalette { text.secondary },
                            SidebarButton("paintbrush.svg".image(), "oneconfig.themes", label("oneconfig.soon")).onClick {
                                openPage(ThemesPage(), "oneconfig.themes")
                            }.disable(),
                            SidebarButton("cog.svg".image(), "oneconfig.preferences"),
                        ),
                    ),
                    Group(
                        alignment = vertical,
                        children = arrayOf(
                            Text("oneconfig.sidebar.title.extra", fontSize = 11f).setPalette { text.secondary },
                            SidebarButton(
                                "refresh.svg".image(),
                                "oneconfig.changelog",
                            ),//.onClick { openPage(ChangelogPage(NewsManager.getNews()), "oneconfig.changelog") },
                            SidebarButton(
                                "text.svg".image(),
                                "oneconfig.feedback",
                                label("oneconfig.beta"),
                            ).onClick { openPage(FeedbackPage(), "oneconfig.feedback") },
                        ),
                    ),
                    Spacer(size = Vec2(200f, 170f)),
                    SidebarButton0("hud.svg".image(), "oneconfig.edithud").onClick {
                        UScreen.displayScreen(PolyUIScreen(HudManager.polyUI))
                    },
                ),
            ).named("Sidebar"),
            Group(
                size = Vec2(1127f, 700f),
                alignment = Align(padding = Vec2.ZERO),
                children = arrayOf(
                    Group(
                        size = Vec2(1130f, 64f),
                        alignment = Align(main = Align.Main.SpaceBetween),
                        children = arrayOf(
                            Group(
                                alignment = Align(padding = Vec2(16f, 8f)),
                                children = arrayOf(
                                    Image("left-arrow.svg".image()).named("Back").disable(),
                                    Image("right-arrow.svg".image()).named("Forward").disable(),
                                    Text(
                                        "oneconfig.mods",
                                        font = PolyUI.defaultFonts.medium,
                                        fontSize = 24f,
                                    ).named("Current"),
                                ),
                            ).named("Controls"),
                            Group(
                                alignment = Align(padding = Vec2(24f, 8f)),
                                children = arrayOf(
                                    Group(
                                        alignment = Align(padding = Vec2(16f, 8f)),
                                        children = arrayOf(
                                            Image("cloud.svg".image()),
                                            Image(
                                                "bell.svg".image(),
                                            ),//.onClick { showNotifications(polyUI, NotificationsManager.getNotifications()) },
                                            Image(
                                                PolyImage(
                                                    "https://mc-heads.net/avatar/$username/24",
                                                    type = PolyImage.Type.Raster,
                                                ),
                                                radii = 6f.radii(),
                                            ).named("ProfileImage").onInit {
                                                if (image.errored) {
                                                    PolyUI.LOGGER.warn(
                                                        "mc-heads.net did not return a valid head texture, using Steve",
                                                    )
//                                                    renderer.initImage(steveHead)
//                                                    image = steveHead
                                                }
                                            }.withBoarder(
                                                rgba(255, 255, 255, 0.14f),
                                                width = 1f,
                                            ).addHoverInfo(username.ifEmpty { "null" }),
                                        ),
                                    ),
                                    Block(
                                        size = Vec2(256f, 32f),
                                        alignment = Align(padding = Vec2(10f, 8f)),
                                        children = arrayOf(
                                            Image("search.svg".image()),
                                            TextInput(
                                                placeholder = "oneconfig.search.placeholder",
                                                visibleSize = Vec2(210f, 12f),
                                            ),
                                        ),
                                    ).named("SearchField"),
                                    Image(
                                        "close.svg".image(),
                                    ).named("Close").onClick { UScreen.displayScreen(null) }.withStates().setDestructivePalette(),
                                ),
                            ),
                        ),
                    ).named("Header"),
                    ModsPage(ConfigManager.INSTANCE.trees()),
                ),
            )
        ).also {
            ui = it.polyUI!!.master
        }
    }

    fun openPage(page: Drawable, name: String) {
        val title = ui[1][0][0][2] as Text
        val translated = ui.polyUI.translator.translate(name)
        if (title.text == translated.string) return
        @Suppress("deprecation_error")
        title._translated = translated
        ui[1][1] = page
    }

    fun label(text: String): Drawable {
        return Block(
            alignment = Align(main = Align.Main.Center),
            size = Vec2(54f, 18f),
            children = arrayOf(Text(text, font = PolyUI.defaultFonts.bold)),
        ).setPalette { brand.fg }
    }


    val sidebarBtnAlign = Align(padding = Vec2(16f, 6f))

    fun SidebarButton(image: PolyImage, text: String, extra: Drawable? = null): Group {
        return SidebarButton0(image, text, extra).events {
            Event.Mouse.Clicked(0) then { _ ->
                val it = parent!!.parent!![0]
                Move(it, this.x, this.y, false, Animations.EaseOutQuad.create(0.15.seconds)).add()
                false
            }
        }
    }

    fun SidebarButton0(image: PolyImage, text: String, extra: Drawable? = null): Group {
        return Group(
            size = Vec2(225f, 33f),
            alignment = sidebarBtnAlign,
            children = arrayOf(
                Image(image),
                Text(text, fontSize = 14f),
                extra,
            ),
        ).namedId("SidebarButton").apply {
            addEventHandler(Event.Mouse.Entered) {
                Recolor(this[1], this[1].palette.hovered, Animations.EaseInOutQuad.create(0.08.seconds)).add()
                polyUI.cursor = Cursor.Clicker
                false
            }
            addEventHandler(Event.Mouse.Exited) {
                Recolor(this[1], this[1].palette.normal, Animations.EaseInOutQuad.create(0.08.seconds)).add()
                polyUI.cursor = Cursor.Pointer
                false
            }
            addEventHandler(Event.Mouse.Pressed(0)) {
                Recolor(this[1], this[1].palette.pressed, Animations.EaseInOutQuad.create(0.08.seconds)).add()
                false
            }
            addEventHandler(Event.Mouse.Released(0)) {
                Recolor(this[1], this[1].palette.hovered, Animations.EaseInOutQuad.create(0.08.seconds)).add()
                false
            }
        }
    }
}
