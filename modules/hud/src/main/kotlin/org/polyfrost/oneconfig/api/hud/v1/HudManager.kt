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

import dev.deftu.omnicore.api.client.render.OmniResolution
import dev.deftu.omnicore.api.client.screen.OmniScreen
import dev.deftu.omnicore.api.loader.OmniLoader
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.api.hud.v1.internal.*
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.UIManager
import org.polyfrost.oneconfig.utils.v1.MHUtils
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.PolyColor.Constants.TRANSPARENT
import org.polyfrost.polyui.color.rgba
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Group
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Fade
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.image
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object HudManager {
    internal val LOGGER = LogManager.getLogger("OneConfig/HUD")
    private val hudProviders = HashMap<Class<out Hud<*>>, Hud<*>>()
    private val snapLineColor = rgba(170, 170, 170, 0.8f)

    private var init = false

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

    /**
     * Whether the HUD editor panel is currently open, i.e. collapsed or expanded.
     */
    @get:JvmName("isPanelOpen")
    var panelOpen = false
        private set

    /**
     * Whether the HUD editor is currently on the screen, i.e. `HudCore.editing` in V0.
     */
    @JvmStatic
    @get:JvmName("isEditing")
    var isEditing = false
        private set

    var useGuiScale = true
        set(value) {
            if (field == value) return
            field = value
            val scaleFactor = if (value) OmniResolution.scaleFactor.toFloat() else 1f
            setAllHudsScaleFactor(scaleFactor)
        }

    init {
        register(TextHud.DateTime("Date:", "yyyy-MM-dd"))
        register(TextHud.DateTime("Time:", "HH:mm:ss"))
        register(TextHud.Simple("", "Text Hud", ""))
        if (OmniLoader.isDevelopment) register(TextHud.DownKeys())
    }

    @ApiStatus.Internal
    lateinit var panel: Drawable
        private set

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    inline val polyUI get() = UIManager.INSTANCE.defaultInstance

    /**
     * **VERY** internal list of active HUD instances. Do not modify this list directly. Please. Like, seriously.
     */
    @ApiStatus.Internal
    val activeInstances = ArrayList<Hud<*>>()

    /**
     * Register a HUD provider to the system. This does not add an instance of the HUD to the screen; it only makes it available in the HUD picker.
     */
    @JvmStatic
    fun register(hud: Hud<*>) {
        hudProviders[hud::class.java] = hud
        if (hud.updateFrequency() == 0L) LOGGER.warn("update of HUD ${hud.title} is 0, this is not recommended!")
    }

    @JvmStatic
    fun register(vararg huds: Hud<*>) {
        for (hud in huds) {
            register(hud)
        }
    }

    /**
     * Remove a HUD type from the registered providers. If [removeActiveInstances] is true, all active instances of this HUD will be removed from the screen; and they will be returned.
     * If [delete] is true, the config entries of the removed HUDs will be deleted as well, meaning they won't be restored on next load.
     *
     * **The [delete] operation is permanent and CANNOT be undone.** To temporarily hide a HUD, use [toggleAllHuds] instead.
     */
    fun <T : Drawable> unregister(hud: Hud<T>, removeActiveInstances: Boolean = false, delete: Boolean = false): ArrayList<Hud<T>>? {
        hudProviders.remove(hud::class.java)
        if (removeActiveInstances) {
            val out = ArrayList<Hud<T>>(10.coerceAtMost(activeInstances.size))
            activeInstances.fastEach {
                if (it::class.java == hud::class.java) {
                    removeHud(it, delete)
                    @Suppress("UNCHECKED_CAST")
                    out.add(it as Hud<T>)
                }
            }
            return out
        } else return null
    }

    /**
     * Return all active HUD instances of this type. To get the "provider" HUD (the one in the picker screen), use [getProvider].
     */
    fun <T : Drawable> getHudsOfType(hudClass: Class<Hud<T>>): ArrayList<Hud<T>> {
        val out = ArrayList<Hud<T>>(10.coerceAtMost(activeInstances.size))
        activeInstances.fastEach {
            if (it::class.java == hudClass) {
                @Suppress("UNCHECKED_CAST")
                out.add(it as Hud<T>)
            }
        }
        return out
    }

    /**
     * Get the "provider" HUD of this type, which is the one in the picker screen. To get active instances, use [getHudsOfType].
     */
    fun getProvider(hudClass: Class<out Hud<*>>): Hud<*>? = hudProviders[hudClass]

    /**
     * Hide/unhide **all** the huds of this type on the screen, and remove the HUD from the HUD picker temporarily until the inverse is called.
     *
     * To permanently remove a HUD, use [removeHud].
     * To permanently unregister a HUD type, use [unregister].
     */
    fun toggleAllHuds(hud: Hud<*>, state: Boolean) {
        val provider = hudProviders[hud::class.java] ?: return
        activeInstances.fastEach {
            if (it::class.java == hud::class.java) {
                it.hidden = state
            }
        }
        provider.hidden = state
    }

    /**
     * Remove a HUD instance from the screen. If [delete] is true, the HUD's config will be deleted as well, meaning it won't be restored on next load.
     *
     * **The [delete] operation is permanent and CANNOT be undone.** To temporarily hide a HUD, use [toggleAllHuds] instead.
     */
    fun removeHud(hud: Hud<*>, delete: Boolean = false) {
        require(hud.isReal) { "Tried to remove a non-real HUD - use unregister() for this case. You might also be calling this method too early!" }
        polyUI.master.removeChild(hud.getBackground() ?: hud.get(), recalculate = false)
        polyUI.removeExecutor(hud.tree.getMetadata("updateTicker"))
        
        try {
            hud.remove() // allow HUD to clean up resources
        } catch (_: Throwable) {
        }

        if (delete) ConfigManager.active().delete(hud.tree.id)
    }

    @Suppress("UNCHECKED_CAST")
    @ApiStatus.Internal
    fun initialize() {
        if (init) throw IllegalStateException("HudManager.initialize() called twice!")
        init = true
        polyUI.translator.addDelegate("assets/oneconfig/hud")
        LOGGER.info("Initializing HUD...")
        val now = System.nanoTime()
        Runtime.getRuntime().addShutdownHook(Thread {
            LOGGER.info("Saving size lock as ${polyUI.size}")
            ConfigManager.internal().folder.resolve("size.lock").writeText(polyUI.size.value.toString())
        })
        val sizeFile = ConfigManager.internal().folder.resolve("size.lock")
        val size = Vec2(if (sizeFile.exists()) sizeFile.readText().toLongOrNull() ?: 0L else 0L)
        val prevSize: Vec2
        if (size.isPositive) {
            LOGGER.info("Found a size to restore: $size")
            prevSize = polyUI.size
            polyUI.resize(size.x, size.y)
            polyUI.window?.pixelRatio = Platform.screen().pixelRatio()
        } else {
            LOGGER.warn("Failed to read previous size from size.lock: HUD positions may be inaccurate. If this is first start, you may ignore this message.")
            prevSize = Vec2.ZERO
        }

        // todo use for inspections
//        it.master.onClick { (x, y) ->
//            val obj = polyUI.inputManager.rayCheckUnsafe(this, x, y) ?: return@onClick false
//            return@onClick false
//        }
        val loader = HudManager::class.java.classLoader
        val used = HashSet<Class<Hud<*>>>(hudProviders.size)
        val failed = HashMap<String, Int>(8)
        var i = 0
        ConfigManager.active().gatherAll("huds").forEach { data ->
            try {
                val clsName = data.getProp("hudClass").get() as? String ?: throw IllegalArgumentException("hud tree ${data.id} is missing class name, will be ignored")
                val cls = Class.forName(clsName, true, loader) as? Class<Hud<*>> ?: throw IllegalArgumentException("hud class $clsName is not a subclass of org.polyfrost.oneconfig.api.v1.hud.Hud, will be ignored")
                // asm: the documentation of Hud states that code should not be run in the constructor
                // so, we are fine to (potentially) malloc the HUD here
                val h = hudProviders.getOrPut(cls) { MHUtils.instantiate(cls, true).getOrThrow() }
                used.add(cls)
                val hud = h.make(data)
                activeInstances.add(hud)
                val bg = hud.build()
                polyUI.master.addChild(bg, recalculate = false)
                val x = data.getProp("x")?.getAs<Number?>()?.toFloat() ?: 0f
                val y = data.getProp("y")?.getAs<Number?>()?.toFloat() ?: 0f
                val it = hud.get()
                // asm: place it so that the relative position to bg is maintained
                bg.x = x - (it.x - bg.x)
                bg.y = y - (it.y - bg.y)
                it.x = x
                it.y = y
                i++
            } catch (e: ClassNotFoundException) {
                val cls = e.message?.substringAfter(':') ?: "unknown"
                failed[cls] = failed.getOrDefault(cls, 0) + 1
            } catch (e: Exception) {
                LOGGER.error("Failed to load HUD from ${data.id}", e)
            }
        }
        if (failed.isNotEmpty()) {
            LOGGER.warn("Failed to load HUDs from ${failed.size} providers as they weren't found: (maybe the mods were removed?)")
            failed.forEach { (cls, amount) -> LOGGER.warn("  $cls: $amount HUDs") }
        }
        if (prevSize.isPositive) {
            polyUI.resize(prevSize.x, prevSize.y)
        }
        polyUI.window?.pixelRatio = Platform.screen().pixelRatio()
        LOGGER.info("successfully loaded {} HUDs from {} providers (total {} registered providers)", i, used.size, hudProviders.size)
        hudProviders.forEach { (cls, h) ->
            if (cls in used) return@forEach
            val default = h.defaultPosition()
            if (!default.isPositive) return@forEach
            val hud = h.make()
            val theHud = hud.build()
            theHud.x = default.x
            theHud.y = default.y
            activeInstances.add(hud)
            polyUI.master.addChild(theHud, recalculate = false)
            LOGGER.info("Added HUD {} to {} (default)", hud.title, default)
        }

        // asm: we will check and set update the gui scale of each HUD when a screen changes.
        // this means that we will catch 99% of cases when the GUI scale changes,
        // as most often this will occur in a screen. It is more reliable than listening to the option change directly
        // as it is a public int so would be impossible to listen to find every change.
        eventHandler { (screen): ScreenOpenEvent ->
            if (useGuiScale) setAllHudsScaleFactor(OmniResolution.scaleFactor.toFloat())
            // in the case the screen crashes or doesn't exit cleanly, we force it to close here
            // as otherwise it can end up being stuck open. seems to be confined to modern only. see #505.
            if(screen == null && isEditing) editorClose()
        }

        LOGGER.info("HUD load took {}ms", (System.nanoTime() - now) / 1_000_000.0)
    }

    private fun setAllHudsScaleFactor(scaleFactor: Float) {
        activeInstances.fastEach {
            if (it !is LegacyHud) {
                val drawable = it.getBackground() ?: it.get()
                drawable.scaleX = scaleFactor
                drawable.scaleY = scaleFactor
            }
        }
    }

    @ApiStatus.Internal
    fun getWithEditor(): OmniScreen {
        if (!::panel.isInitialized) {
            panel = makePanel()
            polyUI.master.addChild(panel, recalculate = false)
            panel.renders = false
        }
        toggleHudPicker()
        activeInstances.fastEach {
            it.hidden = false
        }
        val o = UIManager.INSTANCE.createPolyUIScreen(polyUI, 0f, 0f, false, true) { editorClose() }
        // asm: ensure that the panel is the correct height.
//        panel[0].height = polyUI.size.y - 32f
        return o
    }

    @ApiStatus.Internal
    fun openHudEditor(hud: Hud<*>) {
        panel[0][3] = HudSettingsPage(hud)
        if (!panelOpen) toggle()
    }

    private fun editorClose() {
        toggleHudPicker()
        polyUI.unfocus()
        removeMenu()
        ConfigManager.active().saveAll()
    }

    /**
     * Expand or collapse the HUD editor panel, so move it on or off screen.
     */
    @ApiStatus.Internal
    fun toggle() {
        panelOpen = !panelOpen
        removeMenu()
        val pg = panel
        if (!panelOpen) {
            Move(pg, polyUI.size.x - 32f, pg.y, false, Animations.Default.create(0.2.seconds)).add()
            Fade(pg, 0.8f, false, Animations.Default.create(0.2.seconds)).add()
        } else {
            Move(pg, polyUI.size.x - pg.width - 8f, pg.y, false, Animations.Default.create(0.2.seconds)).add()
            pg.alpha = 1f
            pg.prioritize()
        }
    }

    /**
     * Open or close the HUD editor.
     */
    @ApiStatus.Internal
    fun toggleHudPicker() {
        val pg = panel
        if (panelOpen) {
            toggle()
        }
        pg.prioritize()
        pg.renders = true
        if (isEditing) {
            Fade(pg, 0f, false, Animations.Default.create(0.2.seconds)) {
                renders = false
            }.add()
            EventManager.INSTANCE.post(HudEditorToggleEvent.CLOSE)
        } else {
            pg.alpha = 0f
            Fade(pg, 1f, false, Animations.Default.create(0.2.seconds)).add()
            pg.x = polyUI.size.x - 32f
            toggle()
            EventManager.INSTANCE.post(HudEditorToggleEvent.OPEN)
        }
        isEditing = !isEditing
    }

    internal fun canAutoOpen(): Boolean = !polyUI.master.hasChildIn(polyUI.size.x - panel.width - 34f, 0f, panel.width, polyUI.size.y)

    private fun makePanel(): Drawable {
        val hudsPage = HudsPage(hudProviders.values)
        return Group(
            Block(
                Block(
                    size = Vec2(32f, 1048f),
                    alignment = alignC,
                ).named("CloseArea").withHoverStates().ignoreLayout().setPalette(
                    Colors.Palette(
                        TRANSPARENT,
                        PolyColor.Gradient(rgba(0, 0, 0, 0.7f), TRANSPARENT),
                        PolyColor.Gradient(rgba(0, 0, 0, 0.8f), TRANSPARENT),
                        TRANSPARENT,
                    )
                ).events {
                    Event.Mouse.Entered then {
                        if (!panelOpen) toggle()
                        else {
                            val panel = panel
                            Move(panel, polyUI.size.x - panel.width, panel.y, false, Animations.Default.create(0.2.seconds)).add()
                        }
                    }
                    Event.Mouse.Exited then {
                        val panel = panel
                        if (panelOpen) {
                            Move(panel, polyUI.size.x - panel.width - 8f, panel.y, false, Animations.Default.create(0.2.seconds)).add()
                        }
                    }
                    Event.Mouse.Companion.Clicked then {
                        toggle()
                    }
                },
                Group(
                    Image("assets/oneconfig/ico/left-arrow.svg".image(), size = Vec2(18f, 18f)).setDestructivePalette().withHoverStates().onClick {
                        if (parent.parent[3] !== hudsPage) {
                            parent.parent[3] = hudsPage
                        } else {
                            Platform.screen().close()
                        }
                    },
//                    BoxedTextInput(placeholder = "oneconfig.search.placeholder", image = "assets/oneconfig/ico/search.svg".image(), size = Vec2(256f, 32f)),
                    alignment = Align(pad = Vec2(24f, 0f)),
                    size = Vec2(500f, 32f),
                ),
                Text("oneconfig.hudeditor.title", fontSize = 24f).padded(24f, 0f).setFont { semiBold },
                hudsPage,
                size = Vec2(500f, 1048f),
                alignment = Align(line = Align.Line.Start, pad = Vec2(0f, 16f)),
            ).setPalette { page.bg }.withBorder().apply {
                addOperation {
                    if (polyUI.mouseDown) {
                        if (slinex != -1f) polyUI.renderer.line(slinex, 0f, slinex, polyUI.size.y, snapLineColor, 1f)
                        if (sliney != -1f) polyUI.renderer.line(0f, sliney, polyUI.size.x, sliney, snapLineColor, 1f)
                    } else {
                        slinex = -1f
                        sliney = -1f
                    }
                }
            },
            size = Vec2(0f, 1080f)
        ).also {
            it.rawRescalePosition = true
            it.rawRescaleSize = true
        }
    }
}
