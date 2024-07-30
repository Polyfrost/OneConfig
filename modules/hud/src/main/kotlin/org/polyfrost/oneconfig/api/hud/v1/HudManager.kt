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

import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.hud.v1.internal.HudsPage
import org.polyfrost.oneconfig.api.hud.v1.internal.alignC
import org.polyfrost.oneconfig.api.hud.v1.internal.build
import org.polyfrost.oneconfig.api.hud.v1.internal.createInspectionsScreen
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.UIManager
import org.polyfrost.oneconfig.utils.v1.MHUtils
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.PolyColor.Constants.TRANSPARENT
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.ComponentOp
import org.polyfrost.polyui.operations.Fade
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.rgba
import kotlin.math.PI

object HudManager {
    internal val LOGGER = LogManager.getLogger("OneConfig/HUD")
    private val hudProviders = HashMap<Class<out Hud<out Drawable>>, Hud<out Drawable>>()
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

    @ApiStatus.Internal
    var panelOpen = false
        private set

    @ApiStatus.Internal
    var panelExists = false
        private set

    private val hudsPage = HudsPage(hudProviders.values)

    val panel = Block(
        Block(
            Image("assets/oneconfig/ico/right-arrow.svg").setAlpha(0.1f),
            size = Vec2(32f, 1048f),
            alignment = alignC,
        ).named("CloseArea").withStates().ignoreLayout().setPalette(
            Colors.Palette(
                TRANSPARENT,
                PolyColor.Gradient(rgba(100, 100, 100, 0.4f), TRANSPARENT),
                PolyColor.Gradient(rgba(100, 100, 100, 0.3f), TRANSPARENT),
                TRANSPARENT,
            )
        ).events {
            Event.Mouse.Entered then {
                Fade(this[0], 1f, false, Animations.Default.create(0.08.seconds)).add()
            }
            Event.Mouse.Exited then {
                Fade(this[0], 0.1f, false, Animations.Default.create(0.08.seconds)).add()
            }
            Event.Mouse.Companion.Clicked then {
                toggle()
            }
        },
        Group(
            Image("assets/oneconfig/ico/left-arrow.svg").setDestructivePalette().withStates().onClick {
                if (parent.parent[3] !== hudsPage) {
                    parent.parent[3] = hudsPage
                } else {
                    Platform.screen().close()
                }
            },
            Block(
                Image("assets/oneconfig/ico/search.svg"),
                TextInput(placeholder = "oneconfig.search.placeholder", visibleSize = Vec2(220f, 12f)),
                size = Vec2(256f, 32f),
            ).withBoarder().withCursor(Cursor.Text).onClick {
                polyUI.focus(this[1])
            },
            alignment = Align(main = Align.Main.SpaceBetween, pad = Vec2(12f, 6f)),
            size = Vec2(500f, 32f),
        ),
        Text("oneconfig.hudeditor.title", fontSize = 24f).padded(16f, 0f).setFont { semiBold },
        hudsPage,
        size = Vec2(500f, 1048f),
        alignment = Align(cross = Align.Cross.Start, pad = Vec2(0f, 18f)),
    ).also {
        it.rawResize = true
        object : ComponentOp<Drawable>(it) {
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

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    inline val polyUI get() = UIManager.INSTANCE.defaultInstance


    @JvmStatic
    fun register(hud: Hud<out Drawable>) {
        hudProviders[hud::class.java] = hud
    }

    @JvmStatic
    fun register(vararg huds: Hud<out Drawable>) {
        for (hud in huds) {
            register(hud)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @ApiStatus.Internal
    fun initialize() {
        polyUI.translator.addDelegate("assets/oneconfig/hud")
        polyUI.master.addChild(panel, recalculate = false)
        panel.clipped = false
        // todo use for inspections
//        it.master.onClick { (x, y) ->
//            val obj = polyUI.inputManager.rayCheckUnsafe(this, x, y) ?: return@onClick false
//            return@onClick false
//        }
        val used = HashSet<Class<out Hud<out Drawable>>>(hudProviders.size)
        ConfigManager.active().gatherAll("huds").forEach { data ->
            try {
                val clsName = data.getProp("hudClass").get() as? String ?: throw IllegalArgumentException("hud tree ${data.id} is missing class name, will be ignored")
                val cls = Class.forName(clsName) as? Class<Hud<out Drawable>> ?: throw IllegalArgumentException("hud class $clsName is not a subclass of org.polyfrost.oneconfig.api.v1.hud.Hud, will be ignored")
                // asm: the documentation of Hud states that code should not be run in the constructor
                // so, we are fine to (potentially) malloc the HUD here
                // note that this is stored in a map separate to the loaded hud list.
                // we don't want to register a HUD class ourselves, as it may lead to wierd scenarios when mods are removed.
                val h = hudProviders.getOrPut(cls) { MHUtils.instantiate(cls, true).getOrThrow() }
                used.add(cls)
                val hud = h.make(data)
                val theHud = hud.build()
                polyUI.master.addChild(theHud, recalculate = false)
                LOGGER.info("Loaded HUD ${hud.title()} from ${data.id}")
                val x: Float = data.getProp("x").getAs()
                val y: Float = data.getProp("y").getAs()
                theHud.x = x - (hud.get().x - theHud.x)
                theHud.y = y - (hud.get().y - theHud.y)
            } catch (e: Exception) {
                LOGGER.error("Failed to load HUD from ${data.id}", e)
            }
        }
        hudProviders.forEach { (cls, h) ->
            if (used.contains(cls)) return@forEach
            val default = h.defaultPosition()
            if (!default.isPositive) return@forEach
            val hud = h.make()
            val theHud = hud.build()
            theHud.x = default.x
            theHud.y = default.y
            polyUI.master.addChild(theHud, recalculate = false)
            LOGGER.info("Adding HUD {} to {} (default)", hud.title(), default)
        }
    }

    @ApiStatus.Internal
    fun getWithEditor(): Any {
        toggleHudPicker()
        return UIManager.INSTANCE.createPolyUIScreen(polyUI, 0f, 0f, false, true) { editorClose() }
    }

    @ApiStatus.Internal
    fun openHudEditor(hud: Hud<out Drawable>) {
        if (!panelOpen) toggle()
        panel[3] = createInspectionsScreen(hud)
    }

    private fun editorClose() {
        toggleHudPicker()
        ConfigManager.active().saveAll()
    }

    @ApiStatus.Internal
    fun toggle() {
        panelOpen = !panelOpen
        val pg = panel
        val arrow = pg[0][0] as Image
        if (!panelOpen) {
            Move(pg, polyUI.size.x - 32f, pg.y, false, Animations.Default.create(0.2.seconds)).add()
            Fade(pg, 0.8f, false, Animations.Default.create(0.2.seconds)).add()
            arrow.rotation = PI
        } else {
            Move(pg, polyUI.size.x - pg.width - 8f, pg.y, false, Animations.Default.create(0.2.seconds)).add()
            arrow.rotation = 0.0
            pg.alpha = 1f
            pg.prioritize()
        }
    }

    @ApiStatus.Internal
    fun toggleHudPicker() {
        val pg = panel
        if (panelOpen) {
            toggle()
        }
        pg.prioritize()
        pg.clipped = true
        if (panelExists) {
            Fade(pg, 0f, false, Animations.Default.create(0.2.seconds)) {
                clipped = false
            }.add()
            // remove scale blob
            polyUI.inputManager.focus(null)
        } else {
            pg.alpha = 0f
            Fade(pg, 1f, false, Animations.Default.create(0.2.seconds)).add()
            pg.x = polyUI.size.x - 32f
            toggle()
        }
        panelExists = !panelExists
    }

    internal fun canAutoOpen(): Boolean = !polyUI.master.hasChildIn(polyUI.size.x - panel.width - 34f, 0f, panel.width, polyUI.size.y)
}
