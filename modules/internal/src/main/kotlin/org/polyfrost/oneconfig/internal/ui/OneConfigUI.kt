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

import dev.deftu.omnicore.client.OmniClientPlayer
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.OCPolyUIBuilder
import org.polyfrost.oneconfig.internal.ui.pages.FeedbackPage
import org.polyfrost.oneconfig.internal.ui.pages.ModsPage
import org.polyfrost.oneconfig.internal.ui.pages.ThemesPage
import org.polyfrost.oneconfig.internal.ui.pages.TreeSource
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.animate.SetAnimation
import org.polyfrost.polyui.color.rgba
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.Cursor
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Modifiers
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.operations.Recolor
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.image

object OneConfigUI {
    // Should only be used for native compat trees that require custom on click methods
    @ApiStatus.Internal
    val extraConfigTrees: MutableList<Tree> = mutableListOf()

    private val playerHead = PolyImage(
        "https://mc-heads.net/avatar/${OmniClientPlayer.name}/24",
        type = PolyImage.Type.Raster,
    )
    private val searchNoneFound = Text("oneconfig.search.nonefound", fontSize = 16f)

    private lateinit var ui: Drawable
    private var window: Any? = null

    private fun collectTrees(): Map<TreeSource, Set<Tree>> {
        val result = mutableMapOf<TreeSource, MutableSet<Tree>>()

        for (tree in ConfigManager.active().trees()) {
            if (tree.id == "oneconfig" || tree.title == "OneConfig") {
                continue // skip our own tree
            }

            result.getOrPut(TreeSource.CONFIG, ::mutableSetOf).add(tree)
        }

        for (validItem in Platform.compatibility().validTrees) {
            if (
                result.values.flatten().any { it.id.equals(validItem.id, ignoreCase = true) } ||
                result.values.flatten().any { it.title.equals(validItem.name, ignoreCase = true) }
            ) {
                continue
            }

            val tree = Tree.tree(validItem.id)
            tree.title = validItem.name
            result.getOrPut(TreeSource.COMMAND, ::mutableSetOf).add(tree)
        }

        for (tree in extraConfigTrees) {
            result.getOrPut(TreeSource.COMPAT, ::mutableSetOf).add(tree)
        }

        return result
    }

    @JvmOverloads
    fun open(initialScreen: Component = ModsPage(collectTrees())) {
        if (window == null) {
            val builder = OCPolyUIBuilder.create()
                .blurs()
                .atResolution(1920f, 1080f)
                .backgroundColor {
                    colors.page.fg.normal
                }.size(1400f, 700f) as OCPolyUIBuilder
            builder.translatorDelegate("assets/oneconfig")
            builder.onClose { _ ->
                for (t in ConfigManager.active().trees()) {
                    ConfigManager.active().save(t)
                }
            }

            val searchField: TextInput
            val (polyUI, win) = builder.makeAndOpenWithRef(
                Block(
                    Block(
                        size = Vec2(225f, 32f),
                    ).withBorder(1f) { page.border5 }.ignoreLayout().afterParentInit(Int.MAX_VALUE) {
                        // move to mod button
                        this.at = parent[3].at
                    },
                    Image("assets/oneconfig/brand/oneconfig.svg".image()).named("Logo").padded(29f, 0f, 0f, 0f),
                    Text("oneconfig.sidebar.title.options", fontSize = 11f).setPalette { text.secondary }.padded(0f, 24f, 0f, 0f),
                    SidebarButton(
                        "assets/oneconfig/ico/settings.svg".image(),
                        "oneconfig.mods",
                    ).onClick { openPage(ModsPage(collectTrees()), "oneconfig.mods") },
                    SidebarButton(
                        "assets/oneconfig/ico/profiles.svg".image(),
                        "oneconfig.profiles",
                    ).disable().addHoverInfo(Text("this feature is experimental and is coming soon!")),
                    SidebarButton("assets/oneconfig/ico/keyboard.svg".image(), "oneconfig.keybinds").disable(),
                    Text("oneconfig.sidebar.title.personal", fontSize = 11f).setPalette { text.secondary }.padded(0f, 12f, 0f, 0f),
                    SidebarButton("assets/oneconfig/ico/paintbrush.svg".image(), "oneconfig.themes", label("oneconfig.soon")).onClick {
                        openPage(ThemesPage(), "oneconfig.themes")
                    }.disable(),
                    SidebarButton("assets/oneconfig/ico/cog.svg".image(), "oneconfig.preferences").onClick {
                        openPage(ConfigVisualizer.INSTANCE.get(ConfigManager.active().get("oneconfig.json")), "oneconfig.preferences")
                    },
                    Text("oneconfig.sidebar.title.extra", fontSize = 11f).setPalette { text.secondary }.padded(0f, 12f, 0f, 0f),
                    SidebarButton("assets/oneconfig/ico/refresh.svg".image(), "oneconfig.changelog"),
                    SidebarButton(
                        "assets/oneconfig/ico/text.svg".image(), "oneconfig.feedback"
                    ).onClick { openPage(FeedbackPage(), "oneconfig.feedback") },
                    SidebarButton0(
                        "assets/oneconfig/ico/hud.svg".image(),
                        "oneconfig.edithud",
                        label("oneconfig.beta")
                    ).onClick {
                        Platform.screen().display(HudManager.getWithEditor())
                    }.padded(0f, 210f, 0f, 0f),
                    size = Vec2(273f, 700f),
                    alignment = Align(cross = Align.Content.Start, mode = Align.Mode.Vertical, line = Align.Line.Start, padBetween = Vec2(6f, 8f), padEdges = Vec2(24f, 24f), wrap = Align.Wrap.NEVER),
                    radii = floatArrayOf(16f, 0f, 16f, 0f)
                ).setPalette { page.bg }.onInit { Recolor(this, palette.hovered).add() }.withBorder { page.border5 }.named("Sidebar"),
                Group(
                    Group(
                        Group(
                            Image("assets/oneconfig/ico/left-arrow.svg".image()).named("Back").disable(),
                            Image("assets/oneconfig/ico/right-arrow.svg".image()).named("Forward").disable(),
                            Text(
                                "oneconfig.mods",
                                fontSize = 24f,
                            ).setFont { semiBold }.named("Current"),
                            alignment = Align(pad = Vec2(16f, 8f), wrap = Align.Wrap.NEVER),
                        ).named("Controls"),
                        Group(
                            Group(
                                Group(
                                    Image("assets/oneconfig/ico/bell.svg".image()),
                                    Image(playerHead, size = Vec2(24f, 24f)).radius(6f).named("ProfileImage").withBorder(
                                        rgba(255, 255, 255, 0.2f),
                                        width = 1f,
                                    ).addHoverInfo(Text(OmniClientPlayer.name.ifEmpty { "Steve" })),
                                    alignment = Align(pad = Vec2(16f, 8f)),
                                ),
                                Block(
                                    Image("assets/oneconfig/ico/search.svg".image()),
                                    TextInput(
                                        placeholder = "oneconfig.search.placeholder",
                                        visibleSize = Vec2(210f, 12f),
                                    ).onChange { text: String ->
                                        if (text.length > 2) {
                                            val scale = Vec2(polyUI.size.x / polyUI.iSize.x, polyUI.size.y / polyUI.iSize.y)
                                            val search = Group(children = ConfigVisualizer.INSTANCE.getMatching(text).toTypedArray(), visibleSize = Vec2(1130f, 635f) * scale)
                                            if (search.children?.size == 0) search.children?.add(searchNoneFound)
                                            search.setup(polyUI)
                                            openPage(search, "oneconfig.search")
                                        } else {
                                            openPage(ModsPage(collectTrees()), "oneconfig.mods")
                                        }

                                        false
                                    }.also { searchField = it },
                                    size = Vec2(256f, 32f),
                                    alignment = Align(pad = Vec2(10f, 8f)),
                                ).withBorder(1f) { page.border5 }.named("SearchField"),
                                alignment = Align(pad = Vec2(16f, 4f))
                            ),
                            Image(
                                "assets/oneconfig/ico/close.svg".image(),
                            ).named("Close").onClick {
                                Platform.screen().close()
                            }.withHoverStates().setDestructivePalette(),
                            alignment = Align(pad = Vec2(24f, 4f)),
                        ),
                        size = Vec2(1130f, 64f),
                        alignment = Align(main = Align.Content.SpaceBetween),
                    ).named("Header"),
                    initialScreen,
                    size = Vec2(1127f, 700f),
                    alignment = Align(line = Align.Line.Start, pad = Vec2.ZERO),
                ),
            )
            polyUI.keyBinder?.add(KeyBinder.Bind(char = 'F', mods = Modifiers(KeyModifiers.CONTROL)) {
                polyUI.focus(searchField)
                false
            })
            ui = polyUI.master
            window = win
            searchNoneFound.setup(polyUI)
            (ui as Block).let {
                it.radius(16f)
                it.borderWidth = 1f
                it.borderColor = polyUI.colors.page.border10
            }
        } else {
            Platform.screen().display(window)
            if (ui[1][1] != initialScreen) {
                openPage(initialScreen, "oneconfig.mods")
            }
        }
    }

    fun toggleDebug() {
        if (::ui.isInitialized) {
            ui.polyUI.settings.debug = !ui.polyUI.settings.debug
        }
    }

    fun openPage(page: Component, name: String) {
        val title = ui[1][0][0][2] as Text
        val translated = ui.polyUI.translator.translate(name)
        title.text = translated.string
        val prev = ui[1][1]
        ui[1].set(prev, page, SetAnimation.SlideLeft)
    }

    fun label(text: String) = Block(
        Text(text).setFont { bold },
        alignment = Align(main = Align.Content.Center),
        size = Vec2(54f, 18f),
    ).setPalette { brand.fg }.radius(9f)

    fun invalidateCache() {
        window = null
    }


    private val sidebarBtnAlign = Align(pad = Vec2(16f, 6f))

    fun SidebarButton(image: PolyImage, text: String, extra: Drawable? = null) =
        SidebarButton0(image, text, extra).onClick { _ ->
            val it = parent[0]
            Move(it, this.x, this.y, false, Animations.Default.create(0.15.seconds)).add()
            false
        }

    fun SidebarButton0(image: PolyImage, text: String, extra: Drawable? = null) =
        Group(
            Image(image),
            Text(text, fontSize = 14f).onInit { fontWeight = Font.Weight.Regular },
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
