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

import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.utils.v1.MHUtils

object HudManager {
    internal val LOGGER = LogManager.getLogger("OneConfig/HUD")

    private val hudProviders = HashMap<Class<out Hud>, Hud>()
    private val hudIcons = HashMap<String, String>()
    private var init = false
    private val hiddenHudPaint = org.jetbrains.skia.Paint().apply { setAlphaf(0.35f) }

    @get:JvmName("isEditing")
    var isEditing = false
        private set

    @Volatile @JvmField var guiScreenWidth: Float = 960f
    @Volatile @JvmField var guiScreenHeight: Float = 540f

    @Volatile @JvmField var isDebugScreenVisible: Boolean = false
    @Volatile @JvmField var isTabListVisible: Boolean = false
    @Volatile @JvmField var isGuiScreenOpen: Boolean = false

    @Volatile @JvmField var overrideShowInScreens: Boolean = false

    @ApiStatus.Internal
    @Volatile @JvmField var pendingSelection: Hud? = null

    private val lastUpdates = HashMap<Hud, Long>()

    /**
     * NOTE: NEVER CALL THIS RAW! THERE ARE CERTAIN THINGS THAT MUST BE DONE TO PROPERLY REGISTER AN ACTIVE HUD!
     */
    @ApiStatus.Internal
    val activeInstances = ArrayList<Hud>()

    init {
        register(object : TextHud.DateTime("Date:", "yyyy-MM-dd") {
            override fun defaultPosition() = 0f to 0f
        })
        register(object : TextHud.DateTime("Time:", "HH:mm:ss") {
            override fun defaultPosition() = 0f to 0f
        })
        if (java.lang.Boolean.getBoolean("oneconfig.test")) {
            register(object : TextHud("test", "test", Category.COMBAT, "") {
                override fun defaultPosition() = 0f to 0f
                override fun getText(): String = "mmrp\nmeow"
            })
        }
    }

    @JvmStatic
    fun register(hud: Hud) {
        hudProviders[hud::class.java] = hud
        if (hud.updateFrequency() == 0L) LOGGER.warn("update of HUD ${hud.title} is 0, this is not recommended!")
    }

    @JvmStatic
    fun register(hud: Hud, configId: String) {
        hud.configId = configId
        register(hud)
    }

    /**
     * Registers a HUD under [configId] and associates a menu [icon] with that id, used for the
     * HUD library's per-mod icon column. [icon] is either an OC icon name (e.g. `"combat"`,
     * resolved under `assets/oneconfig/ico/`) or a classpath/absolute resource path
     * (e.g. `"/assets/yourmod/icon.svg"`). Lets HUD-only mods set a menu icon without
     * registering a matching [org.polyfrost.oneconfig.api.config.v1.Config].
     */
    @JvmStatic
    fun register(hud: Hud, configId: String, icon: String) {
        hudIcons[configId] = icon
        register(hud, configId)
    }

    /** The menu icon associated with [configId] via [register], or `null` if none was set. */
    fun iconFor(configId: String): String? = hudIcons[configId]

    @JvmStatic
    fun register(vararg huds: Hud) {
        for (hud in huds) register(hud)
    }

    fun providers(): Collection<Hud> = hudProviders.values

    fun <T : Hud> unregister(hud: T, removeActiveInstances: Boolean = false, delete: Boolean = false): ArrayList<T>? {
        hudProviders.remove(hud::class.java)
        if (!removeActiveInstances) return null
        val out = ArrayList<T>(10.coerceAtMost(activeInstances.size))
        val iter = activeInstances.iterator()
        while (iter.hasNext()) {
            val it = iter.next()
            if (it::class.java == hud::class.java) {
                iter.remove()
                disposeHud(it, delete)
                @Suppress("UNCHECKED_CAST")
                out.add(it as T)
            }
        }
        return out
    }

    fun <T : Hud> getHudsOfType(hudClass: Class<T>): ArrayList<T> {
        val out = ArrayList<T>(10.coerceAtMost(activeInstances.size))
        for (it in activeInstances) {
            if (it::class.java == hudClass) {
                @Suppress("UNCHECKED_CAST")
                out.add(it as T)
            }
        }
        return out
    }

    fun getProvider(hudClass: Class<out Hud>): Hud? = hudProviders[hudClass]

    fun toggleAllHuds(hud: Hud, hidden: Boolean) {
        hudProviders[hud::class.java]?.hidden = hidden
        for (it in activeInstances) {
            if (it::class.java == hud::class.java) it.hidden = hidden
        }
    }

    fun removeHud(hud: Hud, delete: Boolean = false) {
        require(hud.isReal) { "Tried to remove a non-real HUD - use unregister() instead." }
        activeInstances.remove(hud)
        disposeHud(hud, delete)
    }

    private fun disposeHud(hud: Hud, delete: Boolean) {
        hud._runtime?.dispose()
        hud._runtime = null
        lastUpdates.remove(hud)
        try { hud.remove() } catch (_: Throwable) {}
        if (delete) ConfigManager.active().delete(hud.tree.id)
    }

    @ApiStatus.Internal
    fun render(ctx: RenderContext, screenWidth: Float, screenHeight: Float) {
        val scale = Platform.compatibility().options().guiScale

        Snapshot.sendApplyNotifications()

        ctx.save()
        ctx.scale(scale, scale)

        for (hud in activeInstances) {
            if (hud is LegacyHudMarker) continue
            if (hud.hidden && !isEditing) continue
            if (isDebugScreenVisible && !hud.showInF3) continue
            if (isTabListVisible && !hud.showInTab) continue
            if (isGuiScreenOpen && !hud.showInScreens && !overrideShowInScreens) continue

            hud.update()

            val hudScale = hud.effectiveScale
            val rt = hud.runtime
            rt.frame(screenWidth / scale / hudScale, screenHeight / scale / hudScale)
            val root = rt.root
            Snapshot.withMutableSnapshot {
                hud.renderedW = root.width * hudScale
                hud.renderedH = root.height * hudScale
            }
            ctx.save()
            ctx.translate(hud.x, hud.y)
            if (hudScale != 1f) ctx.scale(hudScale, hudScale)
            if (hud.hidden && isEditing) {
                ctx.canvas.saveLayer(null, hiddenHudPaint)
                root.render(ctx)
                ctx.canvas.restore()
            } else {
                root.render(ctx)
            }
            ctx.restore()
        }

        ctx.restore()
    }

    @ApiStatus.Internal
    fun toggleEditor() {
        if (isEditing) closeEditor() else openEditor()
    }

    @ApiStatus.Internal
    fun openEditor() {
        // Always (re)assert editing and post OPEN, even if [isEditing] is already true: the flag can
        // get stuck (e.g. the editor screen closed without closeEditor() running), and an early return
        // here would make the "Edit HUD" button silently do nothing. The OPEN handler is responsible
        // for not opening a duplicate editor screen.
        isEditing = true
        EventManager.INSTANCE.post(HudEditorToggleEvent.OPEN)
    }

    @ApiStatus.Internal
    fun closeEditor() {
        if (!isEditing) return
        isEditing = false
        EventManager.INSTANCE.post(HudEditorToggleEvent.CLOSE)
    }

    @ApiStatus.Internal
    fun onEditorScreenRemoved() {
        isEditing = false
    }

    @Suppress("UNCHECKED_CAST")
    @ApiStatus.Internal
    fun initialize() {
        if (init) throw IllegalStateException("HudManager.initialize() called twice!")
        init = true
        LOGGER.info("Initializing HUD...")
        val now = System.nanoTime()
        val loader = HudManager::class.java.classLoader
        val used = HashSet<Class<Hud>>(hudProviders.size)
        val failed = HashMap<String, Int>(8)
        var i = 0

        ConfigManager.active().gatherAll("huds").forEach { data ->
            try {
                val clsName = data.getProp("hudClass").get() as? String
                    ?: throw IllegalArgumentException("hud tree ${data.id} is missing class name")
                val cls = Class.forName(clsName, true, loader) as? Class<Hud>
                    ?: throw IllegalArgumentException("$clsName is not a subclass of Hud")
                val h = hudProviders[cls] ?: MHUtils.instantiate(cls, true).getOrThrow()
                used.add(cls)
                val hud = h.make(data)
                val sec = data.getProp("section")?.getAs<Section?>()
                if (sec != null) {
                    hud.section = sec
                    hud.relativeX = data.getProp("relativeX")?.getAs<Number?>()?.toFloat() ?: 0f
                    hud.relativeY = data.getProp("relativeY")?.getAs<Number?>()?.toFloat() ?: 0f
                } else {
                    val absX = data.getProp("x")?.getAs<Number?>()?.toFloat() ?: 0f
                    val absY = data.getProp("y")?.getAs<Number?>()?.toFloat() ?: 0f
                    hud.setAbsolutePosition(absX, absY)
                }
                activeInstances.add(hud)
                hud.setup()
                hud.captureStaticSizeDefaults()
                hud.capturePositionDefaults()
                i++
            } catch (e: ClassNotFoundException) {
                val cls = e.message?.substringAfter(':')?.trim() ?: "unknown"
                failed[cls] = failed.getOrDefault(cls, 0) + 1
            } catch (e: Exception) {
                LOGGER.error("Failed to load HUD from ${data.id}", e)
            }
        }

        if (failed.isNotEmpty()) {
            LOGGER.warn("Failed to load HUDs from ${failed.size} providers (mods may have been removed):")
            failed.forEach { (cls, count) -> LOGGER.warn("  $cls: $count HUDs") }
        }

        hudProviders.forEach { (cls, h) ->
            if (cls in used) return@forEach
            val (dx, dy) = h.defaultPosition()
            if (dx <= 0f && dy <= 0f) return@forEach
            val hud = h.make()
            hud.setAbsolutePosition(dx, dy)
            activeInstances.add(hud)
            hud.setup()
            hud.captureStaticSizeDefaults()
            hud.capturePositionDefaults()
            LOGGER.info("Added HUD ${hud.title} at default position ($dx, $dy)")
        }

        eventHandler { _: ScreenOpenEvent -> }

        LOGGER.info("HUD load took {}ms, loaded {} HUDs from {} providers ({} registered)",
            (System.nanoTime() - now) / 1_000_000.0, i, used.size, hudProviders.size)
    }
}
