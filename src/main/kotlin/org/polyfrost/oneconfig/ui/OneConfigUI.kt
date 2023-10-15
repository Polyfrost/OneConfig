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

import org.polyfrost.oneconfig.api.config.ConfigManager
import org.polyfrost.oneconfig.ui.elements.ColorPicker
import org.polyfrost.oneconfig.ui.pages.ChangelogsPage
import org.polyfrost.oneconfig.ui.pages.ModsPage
import org.polyfrost.oneconfig.ui.pages.Page
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.LightTheme
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.event.MouseEntered
import org.polyfrost.polyui.event.MouseExited
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.Modifiers
import org.polyfrost.polyui.input.Modifiers.Companion.mods
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.layout.impl.SwitchingLayout
import org.polyfrost.polyui.property.impl.BlockProperties
import org.polyfrost.polyui.property.impl.ButtonProperties
import org.polyfrost.polyui.property.impl.ImageProperties
import org.polyfrost.polyui.property.impl.TextProperties
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.origin
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.unit.times
import org.polyfrost.polyui.utils.radii
import org.polyfrost.polyui.utils.rgba

fun create(): org.polyfrost.oneconfig.ui.screen.PolyUIScreen {
    val poppinsMed = TextProperties { fonts.medium }

    val content = SwitchingLayout(
        at = 274.px * 66.px,
        size = 1126.px * 640.px,
        makesScrolling = true,
    )

    val modsPage = ModsPage(ConfigManager.INSTANCE.trees(), content)
    val changelogsPage = ChangelogsPage(content)

    val slideyBoi = Block(
        properties = BlockProperties(cornerRadii = 7f.radii(), outlineThickness = 2f),
        at = 24.px * 100.px,
        size = 225.px * 33.px,
        acceptInput = false,
    )
    val headerText = Text(
        properties = TextProperties { fonts.medium },
        text = "oneconfig".localised(),
        fontSize = 24.px,
        at = 98.px * 22.px,
    )

    val sidebarProperties = SidebarProperties().addEventHandler(MouseClicked(0)) {
        slideyBoi.moveTo(at, Animations.EaseOutExpo, 0.25.seconds)
        headerText.string = (this as Button).text!!.string
        true
    } as SidebarProperties

    var theme = false

    val sidebar = PixelLayout(
        at = origin,
        size = 274.px * 700.px,
        drawables = drawables(
            slideyBoi,
            Image(
                image = PolyImage("oneconfig.png"),
                at = 53.px * 23.px,
            ),
            Text(
                properties = poppinsMed,
                text = "oneconfig.sidebar.title.mods".localised(),
                at = 30.px * 78.px,
                fontSize = 12.px,
            ),
            Button(
                properties = sidebarProperties,
                at = 24.px * 100.px,
                left = PolyImage("settings.svg", 18f, 18f),
                text = "oneconfig.mods".localised(),
                fontSize = 14.px,
                size = 225.px * 33.px,
                events = {
                    MouseClicked(0) to {
                        modsPage.open()
                        false
                    }
                },
            ),
            Button(
                properties = sidebarProperties,
                at = 24.px * 139.px,
                left = PolyImage("profiles.svg", 18f, 18f),
                text = "oneconfig.profiles".localised(),
                fontSize = 14.px,
                size = 225.px * 33.px,
            ),
            Button(
                properties = sidebarProperties,
                at = 24.px * 178.px,
                left = PolyImage("keyboard.svg", 18f, 18f),
                text = "oneconfig.keybinds".localised(),
                fontSize = 14.px,
                size = 225.px * 33.px,
            ),
            Text(
                properties = poppinsMed,
                text = "oneconfig.sidebar.title.personalization".localised(),
                at = 30.px * 230.px,
                fontSize = 12.px,
            ),
            Button(
                properties = sidebarProperties,
                at = 24.px * 253.px,
                left = PolyImage("paintbrush.svg", 18f, 18f),
                text = "oneconfig.themes".localised(),
                fontSize = 14.px,
                size = 225.px * 33.px,
                events = {
                    MouseClicked(0) to {
//                        content.switch(themes)
                        false
                    }
                },
            ),
            Button(
                properties = sidebarProperties,
                at = 24.px * 292.px,
                left = PolyImage("cog.svg", 18f, 18f),
                text = "oneconfig.preferences".localised(),
                fontSize = 14.px,
                size = 225.px * 33.px,
                events = {
                    MouseClicked(0) to {
//                        content.switch(preferences)
                        false
                    }
                },
            ),
            Text(
                properties = poppinsMed,
                text = "oneconfig.sidebar.title.oneconfig".localised(),
                at = 30.px * 344.px,
                fontSize = 12.px,
            ),
            Button(
                properties = sidebarProperties,
                at = 24.px * 367.px,
                left = PolyImage("refresh.svg", 18f, 18f),
                text = "oneconfig.changelog".localised(),
                fontSize = 14.px,
                size = 225.px * 33.px,
                events = {
                    MouseClicked(0) to {
                        changelogsPage.open()
                        false
                    }
                },
            ),
            Button(
                properties = sidebarProperties,
                at = 24.px * 406.px,
                left = PolyImage("text.svg", 18f, 18f),
                text = "oneconfig.feedback".localised(),
                fontSize = 14.px,
                size = 225.px * 33.px,
            ),
            Button(
                properties = SidebarProperties().addEventHandler(MouseClicked(0)) {
                    polyUI.colors = if (!theme) LightTheme() else DarkTheme()
                    theme = !theme
                    true
                } as SidebarProperties,
                at = 24.px * 649.px,
                left = PolyImage("hud.svg", 18f, 18f),
                text = "oneconfig.edithud".localised(),
                fontSize = 14.px,
                size = 225.px * 33.px,
            ),
        ),
    )

    val back = Image(
        properties = ImageProperties(true),
        image = PolyImage("left-arrow.svg", 18f, 18f),
        at = 22.px * 24.px,
    )
    val forward = Image(
        properties = ImageProperties(true),
        image = PolyImage("right-arrow.svg", 18f, 18f),
        at = 56.px * 24.px,
        events = {
            MouseClicked(0) to {
                polyUI.master.add(ColorPicker(at = 100.px * 100.px, color = rgba(242, 100, 231)))
                true
            }
        },
    )

    val header = PixelLayout(
        at = 274.px * 0.px,
        size = 1126.px * 64.px,
        drawables = drawables(
            back,
            forward,
            headerText,
            Image(
                properties = ImageProperties(true),
                image = PolyImage("bell.svg", 18f, 18f),
                at = 738.px * 24.px,
            ),
            Image(
                image = PolyImage("img.png", 24f, 24f),
                at = 772.px * 21.px,
            ),
            SearchField(
                at = 812.px * 17.px,
                size = 256.px * 32.px,
                image = PolyImage("search.svg", 16f, 16f),
                fontSize = 12.px,
                input = listOf(),
                events = {
                    TextInput.ChangedEvent() to {
                        (content.current as? Page)?.filter(it.value, this.properties.searchAlgorithm)
                        true
                    }
                },
            ),
            Image(
                image = PolyImage("close.svg", 18f, 18f),
                at = 1084.px * 24.px,
                events = {
                    MouseEntered to {
                        recolor(properties.colors.state.danger.hovered, Animations.EaseOutExpo, 0.25.seconds)
                        polyUI.cursor = Cursor.Clicker
                    }
                    MouseExited to {
                        recolor(properties.colors.text.primary.normal, Animations.EaseOutExpo, 0.25.seconds)
                        polyUI.cursor = Cursor.Pointer
                    }
                    MouseClicked(0) to {
                        polyUI.window.close()
                    }
                },
            ),
        ),
    )



    return org.polyfrost.oneconfig.ui.screen.PolyUIScreen(
        1400f, 700f,
        null, {
            it.settings.debug = false
            back.disabled = true
            modsPage.open()
            it.keyBinder?.add(
                KeyBinder.Bind('P', mods = mods(Modifiers.LCONTROL)) {
                    it.debugPrint()
                    true
                },
            )
        },
        Block(properties = BlockProperties.backgroundBlock, at = origin, size = 1400.px * 700.px), sidebar, header, content
    )
}

class SidebarProperties : ButtonProperties() {
    override val hasBackground = false
    override val palette: Colors.Palette
        get() = colors.text.primary
    override val recolorsAll = true
}
