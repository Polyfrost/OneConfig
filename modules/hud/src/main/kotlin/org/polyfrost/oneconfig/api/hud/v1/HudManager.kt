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
import org.polyfrost.compose.node.RootNode
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeHost
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.utils.v1.MHUtils
import java.util.concurrent.CopyOnWriteArrayList

object HudManager {
    internal val LOGGER = LogManager.getLogger("OneConfig/HUD")

    private val hudProviders = HashMap<Class<out Hud>, Hud>()
    private val hudIcons = HashMap<String, String>()
    private val registrationListeners = CopyOnWriteArrayList<Runnable>()
    private var init = false
    private val hiddenHudPaint by lazy { org.jetbrains.skia.Paint().apply { setAlphaf(0.35f) } }

    /**
     * `true` while HUDs are being shown for editing/preview purposes, i.e. while either the HUD editor
     * or the main OneConfig UI is open. HUDs should render example/preview content when this is set.
     */
    @get:JvmName("isEditing")
    val isEditing: Boolean get() = isEditorOpen || isConfigUiOpen

    /** `true` only while the HUD editor screen itself is open. */
    @ApiStatus.Internal
    @Volatile var isEditorOpen = false
        private set

    /** Set by the OneConfig UI screen while it is open, so HUDs behind it still render as previews. */
    @ApiStatus.Internal
    @Volatile @JvmField var isConfigUiOpen = false

    @Volatile @JvmField var guiScreenWidth: Float = 960f
    @Volatile @JvmField var guiScreenHeight: Float = 540f

    @Volatile @JvmField var isDebugScreenVisible: Boolean = false
    @Volatile @JvmField var isTabListVisible: Boolean = false
    @Volatile @JvmField var isGuiScreenOpen: Boolean = false
    @Volatile @JvmField var isChatScreenOpen: Boolean = false

    @Volatile @JvmField var masterHudEnabled: Boolean = true

    /** Mirrors MC's `options.hideGui` (F1). Hides every HUD unless the editor is open. */
    @Volatile @JvmField var isGuiHidden: Boolean = false

    @Volatile @JvmField var overrideShowInScreens: Boolean = false

    @ApiStatus.Internal
    @Volatile @JvmField var targetPixelWidth: Int = 0

    @ApiStatus.Internal
    @Volatile @JvmField var targetPixelHeight: Int = 0

    @ApiStatus.Internal
    @Volatile @JvmField var inWorld: Boolean = true

    @ApiStatus.Internal
    @Volatile @JvmField var pendingSelection: Hud? = null

    private val lastUpdates = HashMap<Hud, Long>()

    private val redrawCacheDisabled = java.lang.Boolean.getBoolean("oneconfig.hud.nocache")

    @Volatile private var contentDirty = true

    @JvmStatic
    fun invalidate() {
        contentDirty = true
    }

    internal var frameId = 0L
        private set
    private var preparedFrameValid = false
    private var lastFrameKey = Long.MIN_VALUE
    private var lastFrameWidth = Float.NaN
    private var lastFrameHeight = Float.NaN
    private var lastFrameScale = Float.NaN

    private val frameOrder = ArrayList<Hud>()

    private var frameGroups: List<HudBackgroundMerge.Group> = emptyList()
    private var lastMergeKey: Int? = null

    private var zOrderCache: List<Hud> = emptyList()
    private var zOrderHuds = arrayOfNulls<Hud>(0)
    private var zOrderBounds = FloatArray(0)

    private const val REGISTRY_ID = "hud-registry.json"
    private const val KNOWN_HUDS = "knownHuds"

    private val knownProviders = LinkedHashSet<String>()
    private var registryTree: Tree? = null

    private fun registryProperty() = Properties.simple(
        KNOWN_HUDS, "Known HUDs",
        "HUD providers which have already been given their default instance.",
        emptyArray<String>(), Array<String>::class.java
    )

    private fun loadRegistry() {
        try {
            val mgr = ConfigManager.active()
            val t = mgr.trees().firstOrNull { it.id == REGISTRY_ID }
                ?: mgr.register(Tree.tree(REGISTRY_ID).put(registryProperty())).get()
            if (t.getProp(KNOWN_HUDS) == null) t.put(registryProperty())
            t.addMetadata("hidden", true)
            registryTree = t
            when (val known = t.getProp(KNOWN_HUDS)?.get()) {
                is Array<*> -> known.forEach { if (it != null) knownProviders.add(it.toString()) }
                is Iterable<*> -> known.forEach { if (it != null) knownProviders.add(it.toString()) }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load HUD registry, HUD deletions may not persist", e)
        }
    }

    private fun saveRegistry() {
        val t = registryTree ?: return
        try {
            t.getProp(KNOWN_HUDS)?.setAs(knownProviders.toTypedArray())
            ConfigManager.active().save(REGISTRY_ID)
        } catch (e: Exception) {
            LOGGER.error("Failed to save HUD registry", e)
        }
    }

    @JvmStatic
    fun markProviderKnown(hud: Hud) {
        if (knownProviders.add(hud::class.java.name)) saveRegistry()
    }

    /**
     * NOTE: NEVER CALL THIS RAW! THERE ARE CERTAIN THINGS THAT MUST BE DONE TO PROPERLY REGISTER AN ACTIVE HUD!
     */
    @ApiStatus.Internal
    val activeInstances = ArrayList<Hud>()

    init {
        Snapshot.registerApplyObserver { _, _ -> contentDirty = true }

        if (java.lang.Boolean.getBoolean("oneconfig.test")) {
            register(object : TextHud.DateTime("Date:", "yyyy-MM-dd") {})
            register(object : TextHud.DateTime("Time:", "HH:mm:ss") {})
            register(object : TextHud("test", "test", Category.COMBAT, "") {
                override fun getText(): String = "mmrp\nmeow"
            })
        }
    }

    /**
     * Adds a listener that is run when a new Hud is registered
     */
    @ApiStatus.Internal
    fun addRegistrationListener(listener: Runnable) {
        registrationListeners.add(listener)
    }

    private fun notifyRegistrationChanged() = registrationListeners.forEach(Runnable::run)

    @JvmStatic
    fun register(hud: Hud) {
        hudProviders[hud::class.java] = hud
        if (hud.updateFrequency() == 0L) LOGGER.warn("update of HUD ${hud.title} is 0, this is not recommended!")
        notifyRegistrationChanged()
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
        notifyRegistrationChanged()
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

    /** The live HUD whose config tree has this [id], used to resolve [Hud.anchorTargetId]. */
    fun instanceById(id: String): Hud? {
        for (it in activeInstances) {
            if (it.tree?.id == id) return it
        }
        return null
    }

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
        // anything hanging off this HUD goes back to being positioned against the screen, staying
        // where it is: the relative position it keeps alongside the anchor is already up to date
        hud.tree?.id?.let { gone ->
            for (it in activeInstances) {
                if (it.anchorTargetId == gone) it.clearAnchor()
            }
        }
        invalidate()
        try { hud.remove() } catch (_: Throwable) {}
        val treeId = hud.tree?.id
        // a HUD which cannot be deleted by the user must never lose its config: without this an
        // errant unregister(delete = true) wipes it from disk and it can never be restored.
        if (delete && !hud.deletable()) {
            LOGGER.warn("refusing to delete the config of ${hud.title}, which is marked as not user-deletable")
        } else if (delete && treeId != null) {
            ConfigManager.active().delete(treeId)
        }
        // back to being a plain provider, so a single-instance HUD can be made again later
        hud.detachTree()
    }

    private fun screenBounds(hud: Hud): FloatArray? {
        val scale = hud.effectiveScale
        val w = if (hud.staticWidth) {
            hud.staticW.takeIf { it > 0f }?.times(scale)
        } else {
            hud.renderedW.takeIf { it > 0f } ?: hud.staticW.takeIf { it > 0f }?.times(scale)
        } ?: return null
        val h = if (hud.staticWidth) {
            hud.staticH.takeIf { it > 0f }?.times(scale)
        } else {
            hud.renderedH.takeIf { it > 0f } ?: hud.staticH.takeIf { it > 0f }?.times(scale)
        } ?: return null
        return floatArrayOf(hud.x, hud.y, w, h)
    }

    private fun encloses(outer: FloatArray, inner: FloatArray): Boolean =
        inner[0] >= outer[0] && inner[1] >= outer[1] &&
            inner[0] + inner[2] <= outer[0] + outer[2] &&
            inner[1] + inner[3] <= outer[1] + outer[3]

    @ApiStatus.Internal
    fun zOrderedInstances(bounds: (Hud) -> FloatArray? = ::screenBounds): List<Hud> {
        val list = activeInstances
        val n = list.size
        if (n <= 1) return list
        val b = arrayOfNulls<FloatArray>(n)
        for (i in 0 until n) b[i] = bounds(list[i])
        val depth = IntArray(n)
        for (i in 0 until n) {
            val bi = b[i] ?: continue
            for (j in 0 until n) {
                if (i == j) continue
                val bj = b[j] ?: continue
                if (encloses(bj, bi) && (!encloses(bi, bj) || j < i)) depth[i]++
            }
        }
        return list.indices.sortedBy { depth[it] }.map { list[it] }
    }

    private fun updateAndAdvance(huds: List<Hud>) {
        for (hud in huds) {
            try {
                updateIfDue(hud)
            } catch (e: Throwable) {
                LOGGER.error("Failed to update HUD ${hud.title}", e)
            }
        }
        PolyComposeHost.frame()
    }

    private fun layout(hud: Hud, screenWidth: Float, screenHeight: Float, scale: Float): RootNode {
        val hudScale = hud.effectiveScale
        val rt = hud.runtime
        rt.layout(screenWidth / scale / hudScale, screenHeight / scale / hudScale)
        val root = rt.root
        val w = root.width * hudScale
        val h = root.height * hudScale
        if (hud.renderedW != w || hud.renderedH != h) {
            Snapshot.withMutableSnapshot {
                hud.renderedW = w
                hud.renderedH = h
            }
        }
        return root
    }

    @ApiStatus.Internal
    fun updateIfDue(hud: Hud) {
        val frequency = hud.updateFrequency()
        if (frequency < 0L || isEditing) {
            hud.update()
            return
        }
        val now = System.nanoTime()
        val last = lastUpdates[hud]
        if (last != null && now - last < frequency) return
        lastUpdates[hud] = now
        hud.update()
    }

    private fun layoutOnce(hud: Hud, screenWidth: Float, screenHeight: Float, scale: Float): RootNode {
        if (hud.lastLayoutFrame == frameId) return hud.runtime.root
        hud.lastLayoutFrame = frameId
        return layout(hud, screenWidth, screenHeight, scale)
    }

    private fun shouldDraw(hud: Hud): Boolean {
        if (!masterHudEnabled && !isEditing) return false
        if (hud is LegacyHudMarker) return false
        if (hud.hidden && !isEditing) return false
        if (isGuiHidden && !isEditing) return false
        if (isDebugScreenVisible && !hud.showInF3) return false
        if (isTabListVisible && !hud.showInTab) return false
        if (!overrideShowInScreens && !isEditing) {
            // chat has its own toggle, so it is never governed by "Show in GUIs"
            if (isChatScreenOpen) {
                if (!hud.showInChat) return false
            } else if (isGuiScreenOpen && !hud.showInScreens) return false
        }
        return true
    }

    @ApiStatus.Internal
    fun beginFrame(screenWidth: Float, screenHeight: Float): Boolean {
        val scale = Platform.compatibility().options().guiScale

        frameId++

        Snapshot.sendApplyNotifications()

        frameOrder.clear()
        var volatileContent = false
        for (hud in orderedForRender()) {
            if (!shouldDraw(hud)) continue
            frameOrder.add(hud)
            if (hud.alwaysRedraw) volatileContent = true
        }

        updateAndAdvance(frameOrder)

        layoutAll(frameOrder, screenWidth, screenHeight, scale)
        updateBackgroundGroups(screenWidth, screenHeight, scale)

        val key = frameKey()
        val keyChanged = key != lastFrameKey ||
            screenWidth != lastFrameWidth || screenHeight != lastFrameHeight || scale != lastFrameScale
        lastFrameKey = key
        lastFrameWidth = screenWidth
        lastFrameHeight = screenHeight
        lastFrameScale = scale

        val neverCache = redrawCacheDisabled || volatileContent || isEditing

        val dirty = contentDirty || keyChanged || neverCache
        contentDirty = false
        preparedFrameValid = dirty
        return dirty
    }

    private fun layoutAll(huds: List<Hud>, screenWidth: Float, screenHeight: Float, scale: Float) {
        for (hud in huds) {
            try {
                layoutOnce(hud, screenWidth, screenHeight, scale)
            } catch (e: Throwable) {
                LOGGER.error("Failed to lay out HUD ${hud.title}", e)
            }
        }
    }

    /**
     * Rebuilds the fused background shapes for HUDs which sit against each other, and tells the HUDs
     * in a fused shape to stop drawing their own background. Cheap on frames where nothing moved:
     * the shapes are only re-traced when a position, size or background style actually changed.
     */
    private fun updateBackgroundGroups(screenWidth: Float, screenHeight: Float, scale: Float) {
        val key = HudBackgroundMerge.layoutKey(frameOrder)
        if (key == lastMergeKey) return
        lastMergeKey = key

        frameGroups = HudBackgroundMerge.computeGroups(frameOrder)
        val merged = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Hud, Boolean>())
        for (group in frameGroups) merged.addAll(group.huds)

        var changed = false
        for (hud in activeInstances) {
            if (hud.bgMerged != (hud in merged)) {
                changed = true
                break
            }
        }
        if (!changed) return

        Snapshot.withMutableSnapshot {
            for (hud in activeInstances) hud.bgMerged = hud in merged
        }
        // the flag is read during composition, so recompose and re-lay out now instead of letting the
        // change land a frame late, which would show a doubled or missing background for one frame
        Snapshot.sendApplyNotifications()
        PolyComposeHost.frame()
        for (hud in frameOrder) hud.lastLayoutFrame = -1L
        layoutAll(frameOrder, screenWidth, screenHeight, scale)
        invalidate()
    }

    private fun frameKey(): Long {
        var key = activeInstances.size.toLong() * 31L + frameOrder.size
        key = key * 31L + (if (isDebugScreenVisible) 1 else 0)
        key = key * 31L + (if (isTabListVisible) 1 else 0)
        key = key * 31L + (if (isGuiScreenOpen) 1 else 0)
        key = key * 31L + (if (isChatScreenOpen) 1 else 0)
        key = key * 31L + (if (isGuiHidden) 1 else 0)
        key = key * 31L + (if (overrideShowInScreens) 1 else 0)
        key = key * 31L + (if (isEditing) 1 else 0)
        key = key * 31L + (if (inWorld) 1 else 0)
        key = key * 31L + targetPixelWidth
        key = key * 31L + targetPixelHeight
        return key
    }

    private fun orderedForRender(): List<Hud> {
        val list = activeInstances
        val n = list.size
        if (n <= 1) return list
        if (zOrderHuds.size < n) {
            zOrderHuds = arrayOfNulls(n)
            zOrderBounds = FloatArray(n * 4)
        }
        var same = zOrderCache.size == n
        for (i in 0 until n) {
            val hud = list[i]
            val b = screenBounds(hud)
            val x = b?.get(0) ?: Float.NaN
            val y = b?.get(1) ?: Float.NaN
            val w = b?.get(2) ?: Float.NaN
            val h = b?.get(3) ?: Float.NaN
            val o = i * 4
            if (same && (zOrderHuds[i] !== hud ||
                    !zOrderBounds[o].sameBound(x) || !zOrderBounds[o + 1].sameBound(y) ||
                    !zOrderBounds[o + 2].sameBound(w) || !zOrderBounds[o + 3].sameBound(h))
            ) {
                same = false
            }
            zOrderHuds[i] = hud
            zOrderBounds[o] = x
            zOrderBounds[o + 1] = y
            zOrderBounds[o + 2] = w
            zOrderBounds[o + 3] = h
        }
        if (same) return zOrderCache
        invalidate()
        zOrderCache = zOrderedInstances()
        return zOrderCache
    }

    private fun Float.sameBound(other: Float): Boolean =
        this == other || (this.isNaN() && other.isNaN())

    @ApiStatus.Internal
    fun prepare(screenWidth: Float, screenHeight: Float) {
        val scale = Platform.compatibility().options().guiScale
        Snapshot.sendApplyNotifications()
        frameId++
        val huds = activeInstances.filterNot { it is LegacyHudMarker }
        updateAndAdvance(huds)
        for (hud in huds) {
            try {
                layoutOnce(hud, screenWidth, screenHeight, scale)
            } catch (e: Throwable) {
                LOGGER.error("Failed to lay out HUD ${hud.title}", e)
            }
        }
    }

    @ApiStatus.Internal
    fun render(ctx: RenderContext, screenWidth: Float, screenHeight: Float) {
        val scale = Platform.compatibility().options().guiScale

        val prepared = preparedFrameValid
        preparedFrameValid = false
        if (!prepared) {
            Snapshot.sendApplyNotifications()
            frameId++
            frameOrder.clear()
            for (hud in orderedForRender()) if (shouldDraw(hud)) frameOrder.add(hud)
            updateAndAdvance(frameOrder)
            layoutAll(frameOrder, screenWidth, screenHeight, scale)
            updateBackgroundGroups(screenWidth, screenHeight, scale)
        }

        ctx.save()
        ctx.scale(scale, scale)

        for (group in frameGroups) {
            try {
                group.draw(ctx)
            } catch (e: Throwable) {
                LOGGER.error("Failed to draw merged HUD background", e)
            }
        }

        for (hud in frameOrder) {
            val hudScale = hud.effectiveScale
            val root = try {
                layoutOnce(hud, screenWidth, screenHeight, scale)
            } catch (e: Throwable) {
                LOGGER.error("Failed to lay out HUD ${hud.title}", e)
                continue
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
        if (isEditorOpen) closeEditor() else openEditor()
    }

    @ApiStatus.Internal
    fun openEditor() {
        // Always (re)assert editing and post OPEN, even if [isEditorOpen] is already true: the flag can
        // get stuck (e.g. the editor screen closed without closeEditor() running), and an early return
        // here would make the "Edit HUD" button silently do nothing. The OPEN handler is responsible
        // for not opening a duplicate editor screen.
        isEditorOpen = true
        EventManager.INSTANCE.post(HudEditorToggleEvent.OPEN)
    }

    @ApiStatus.Internal
    fun closeEditor() {
        if (!isEditorOpen) return
        isEditorOpen = false
        EventManager.INSTANCE.post(HudEditorToggleEvent.CLOSE)
    }

    @ApiStatus.Internal
    fun onEditorScreenRemoved() {
        isEditorOpen = false
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

        loadRegistry()

        ConfigManager.active().gatherAll("huds").forEach { data ->
            try {
                val clsName = data.getProp("hudClass").get() as? String
                    ?: throw IllegalArgumentException("hud tree ${data.id} is missing class name")
                if (clsName.endsWith(".OneConfigHudCompat")) return@forEach
                val cls = Class.forName(clsName, true, loader) as? Class<Hud>
                    ?: throw IllegalArgumentException("$clsName is not a subclass of Hud")
                val h = hudProviders[cls] ?: MHUtils.instantiate(cls, true).getOrThrow()
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
                // only once the instance actually exists: marking the class used on a failed load
                // would both suppress the default instance below and mark the provider known,
                // permanently "deleting" a HUD because of a transient load error.
                used.add(cls)
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

        var registryChanged = false
        for (cls in used) registryChanged = knownProviders.add(cls.name) or registryChanged

        hudProviders.forEach { (cls, h) ->
            if (cls in used) return@forEach
            if (h.isReal) return@forEach
            val known = cls.name in knownProviders
            // A HUD the user cannot delete has no legitimate "deleted" state, so a missing instance
            // always means its config was lost (failed/incomplete write, corrupt file, a launch
            // without the mod, ...). Restore it instead of leaving it stranded in the HUD library.
            val restore = if (h.deletable()) {
                // the user deleted every instance of this HUD; don't resurrect it.
                h.showByDefault() && !known
            } else {
                h.showByDefault() || known
            }
            if (!restore) return@forEach
            if (known && !h.deletable()) {
                LOGGER.warn("HUD ${h.title} cannot be deleted but had no instance; restoring it")
            }
            val (dx, dy) = h.defaultPosition()
            registryChanged = knownProviders.add(cls.name) or registryChanged
            val hud = h.make()
            hud.setAbsolutePosition(dx, dy)
            activeInstances.add(hud)
            hud.setup()
            hud.captureStaticSizeDefaults()
            hud.capturePositionDefaults()
            LOGGER.info("Added HUD ${hud.title} at default position ($dx, $dy)")
        }

        if (registryChanged) saveRegistry()

        LOGGER.info("HUD load took {}ms, loaded {} HUDs from {} providers ({} registered)",
            (System.nanoTime() - now) / 1_000_000.0, i, used.size, hudProviders.size)
    }
}
