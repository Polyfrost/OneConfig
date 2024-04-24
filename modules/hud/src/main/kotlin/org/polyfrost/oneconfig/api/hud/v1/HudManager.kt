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

package org.polyfrost.oneconfig.api.hud.v1

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.backend.impl.FileBackend
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.HudRenderEvent
import org.polyfrost.oneconfig.api.event.v1.events.ResizeEvent
import org.polyfrost.oneconfig.api.hud.v1.internal.HudsPage
import org.polyfrost.oneconfig.api.hud.v1.internal.alignC
import org.polyfrost.oneconfig.api.hud.v1.internal.build
import org.polyfrost.oneconfig.api.hud.v1.internal.createInspectionsScreen
import org.polyfrost.oneconfig.api.ui.v1.LwjglManager
import org.polyfrost.oneconfig.api.ui.v1.screen.PolyUIScreen
import org.polyfrost.universal.UResolution
import org.polyfrost.oneconfig.utils.v1.GuiUtils
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.PolyColor.Companion.TRANSPARENT
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.DrawableOp
import org.polyfrost.polyui.operations.Fade
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.property.Settings
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.LinkedList
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.ref
import org.polyfrost.polyui.utils.rgba
import org.slf4j.LoggerFactory
import kotlin.math.PI

object HudManager {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/HUD")
    lateinit var backend: FileBackend
    private val huds = LinkedList<Hud<out Drawable>>()
    private val snapLineColor = rgba(170, 170, 170, 0.8f)

    /**
     * the vertical line x position used for snapping.
     * Do not set this value.
     */
    @ApiStatus.Internal
    var slinex = -1f

    /**
     * the horizontal line y position used for snapping.
     * Do not set this value.
     */
    @ApiStatus.Internal
    var sliney = -1f
    var open = false
        private set

    private var exists = false

    init {
        register(TextHud.DateTime("Date:", "yyyy-MM-dd"))
        register(TextHud.DateTime("Time:", "HH:mm:ss"))
        register(TextHud.Field("", "Text"))
    }

    var hudsPage = HudsPage(huds)

    val panel = Block(
        size = Vec2(500f, 1048f),
        children = arrayOf(
            Group(
                Image("assets/oneconfig/ico/left-arrow.svg".image()).setDestructivePalette().withStates().onClick {
                    if (parent!!.parent!![2] !== hudsPage) {
                        parent!!.parent!![2] = hudsPage
                    } else {
                        GuiUtils.closeScreen()
                    }
                },
                Block(
                    children = arrayOf(
                        Image("assets/oneconfig/ico/search.svg".image()),
                        TextInput(placeholder = "oneconfig.search.placeholder"),
                    ),
                    size = Vec2(256f, 32f),
                ).withBoarder().withCursor(Cursor.Text).onClick {
                    polyUI.focus(this[1])
                },
                alignment = Align(main = Align.Main.SpaceBetween, padding = Vec2.ZERO),
                size = Vec2(468f, 32f),
            ),
            Text("oneconfig.hudeditor.title", fontSize = 24f, font = PolyUI.defaultFonts.medium).onClick {
                ColorPicker(rgba(32, 53, 41).toAnimatable().ref(), mutableListOf(), mutableListOf(), polyUI)
            },
            hudsPage,
        ),
        alignment = Align(cross = Align.Cross.Start, padding = Vec2(24f, 17f)),
    ).events {
        Event.Lifetime.Added then {
            addChild(
                Block(
                    size = Vec2(32f, 1048f),
                    alignment = alignC,
                    children = arrayOf(Image("assets/oneconfig/ico/right-arrow.svg".image()).setAlpha(0.1f)),
                ).named("CloseArea").withStates().setPalette(
                    Colors.Palette(
                        TRANSPARENT,
                        PolyColor.Gradient(
                            rgba(100, 100, 100, 0.4f),
                            TRANSPARENT,
                            type = PolyColor.Gradient.Type.LeftToRight,
                        ),
                        PolyColor.Gradient(
                            rgba(100, 100, 100, 0.3f),
                            TRANSPARENT,
                            type = PolyColor.Gradient.Type.LeftToRight,
                        ),
                        TRANSPARENT,
                    ),
                ).events {
                    Event.Mouse.Entered then {
                        Fade(this[0], 1f, false, Animations.EaseInOutQuad.create(0.08.seconds)).add()
                    }
                    Event.Mouse.Exited then {
                        Fade(this[0], 0.1f, false, Animations.EaseInOutQuad.create(0.08.seconds)).add()
                    }
                    Event.Mouse.Clicked(0) then {
                        // asm: makes close button easier to use
                        if (polyUI.mouseY < 40f) {
                            false
                        } else {
                            toggle()
                            true
                        }
                    }
                },
                reposition = false,
            )
        }
    }.also {
        object : DrawableOp(it) {
            override fun apply() {
                if (self.polyUI.mouseDown) {
                    if (slinex != -1f) self.renderer.line(slinex, 0f, slinex, self.polyUI.size.y, snapLineColor, 1f)
                    if (sliney != -1f) self.renderer.line(0f, sliney, self.polyUI.size.x, sliney, snapLineColor, 1f)
                } else {
                    slinex = -1f
                    sliney = -1f
                }
            }

            override fun unapply() = false
        }.add()
    }

    init {
        eventHandler { (w, h): ResizeEvent ->
            polyUI.resize(w.toFloat(), h.toFloat())
        }.register()
        eventHandler { _: HudRenderEvent ->
            if (!exists) polyUI.render()
        }.register()
    }

    val polyUI: PolyUI = PolyUI(
        LwjglManager.INSTANCE.renderer,
        size = 1920f by 1080f,
        settings = Settings().apply {
            cleanupAfterInit = false
            debug = false
        }
    ).also {
        it.master.rawResize = true
        it.resize(UResolution.windowWidth.toFloat(), UResolution.windowHeight.toFloat())
    }

    fun getWithEditor(): PolyUIScreen {
        return PolyUIScreen(polyUI.also {
            toggleHudPicker()
            exists = true
        }).closeCallback(this::editorClose)
    }

    private fun editorClose() {
        toggleHudPicker()
        exists = false
    }


    @JvmStatic
    fun register(hud: Hud<out Drawable>) {
        huds.add(hud)
    }

    @JvmStatic
    fun register(vararg huds: Hud<out Drawable>) {
        HudManager.huds.addAll(huds)
    }

    fun loadInAll() {
        polyUI.master.children?.fastEach {
            if (it !== panel) polyUI.master.children?.remove(it)
        }
        backend = FileBackend(ConfigManager.active().folder.resolve("huds"))
        backend.gatherAll().forEach { tree ->
            val it: Hud<*> = tree.getProp("hud")?.getAs() ?: return@forEach
            polyUI.master.addChild(it.build(), false)
        }
    }

    fun openHudEditor(hud: Hud<out Drawable>) {
        if (!open) toggle()
        panel[2] = createInspectionsScreen(hud)
    }

    fun toggle() {
        open = !open
        val pg = panel
        val arrow = pg.children!!.last()[0] as Image
        if (!open) {
            Move(pg, polyUI.size.x - 32f, pg.y, false, Animations.EaseInOutExpo.create(0.2.seconds)).add()
            Fade(pg, 0.8f, false, Animations.EaseInOutExpo.create(0.2.seconds)).add()
            arrow.rotation = PI
        } else {
            panel.y = UResolution.windowHeight.toFloat() / 2f - panel.height / 2f
            Move(pg, polyUI.size.x - pg.width - 8f, pg.y, false, Animations.EaseInOutExpo.create(0.2.seconds)).add()
            arrow.rotation = 0.0
            pg.alpha = 1f
            pg.prioritize()
        }
    }

    fun toggleHudPicker() {
        val pg = panel
        if (open) {
            toggle()
        }
        // first open
        if (pg.parent == null) {
            val sx = polyUI.size.x / 1920f
            val sy = polyUI.size.y / 1080f
            polyUI.master.addChild(
                pg, reposition = false,
            )
            pg.rescale(sx, sy, true)
        } else {
            pg.prioritize()
            pg.renders = true
        }
        if (exists) {
            Fade(pg, 0f, false, Animations.EaseInOutQuad.create(0.2.seconds)) {
                renders = false
            }.add()
            // remove scale blob
            polyUI.focus(null)
        } else {
            pg.alpha = 0f
            Fade(pg, 1f, false, Animations.EaseInOutQuad.create(0.2.seconds)).add()
            pg.x = polyUI.size.x - 32f
            toggle()
        }
    }

    fun canAutoOpen(): Boolean = !polyUI.master.hasChildIn(polyUI.size.x - panel.width - 34f, 0f, panel.width, polyUI.size.y)
}
