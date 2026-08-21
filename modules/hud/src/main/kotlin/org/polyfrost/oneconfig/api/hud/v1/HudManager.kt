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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

@Suppress("DEPRECATION")
private fun Throwable.isFatalHudFailure(): Boolean = this is VirtualMachineError || this is ThreadDeath

object HudManager {
    internal val LOGGER = LogManager.getLogger("OneConfig/HUD")

    private val hudProviders = HashMap<Class<out Hud>, Hud>()
    private val hudIcons = HashMap<String, String>()
    private val registrationListeners = CopyOnWriteArrayList<Runnable>()

    var revision by mutableIntStateOf(0)
        private set

    private var init = false
    private const val UI_THREAD_TIMEOUT_SECONDS = 30L
    @Volatile private var profileReloadDispatcher = Consumer<Runnable> { it.run() }
    private data class ProfileReload(
        val profile: String,
        val saveCurrent: Boolean,
        val trees: Collection<Tree>? = null,
    )
    private val pendingProfileReload = AtomicReference<ProfileReload?>(null)
    private val profileChangeListener = object : ConfigManager.ProfileChangeListener {
        override fun onProfileChanged(newProfile: String) {
            pendingProfileReload.set(
                ProfileReload(newProfile, saveCurrent = false, trees = gatherHudTrees())
            )
            applyPendingProfileReload()
        }
    }
    private val hiddenHudPaint by lazy { org.jetbrains.skia.Paint().apply { setAlphaf(0.35f) } }

    /**
     * `true` while HUDs are being shown for editing or preview
     *
     * That is while either the HUD editor or the main OneConfig UI is open
     *
     * HUDs should render example content when this is set
     */
    @get:JvmName("isEditing")
    val isEditing: Boolean get() = isEditorOpen || isConfigUiOpen

    /** `true` only while the HUD editor screen itself is open */
    @ApiStatus.Internal
    @Volatile var isEditorOpen = false
        private set

    /** Set by the OneConfig UI screen while it is open so HUDs behind it still render as previews */
    @ApiStatus.Internal
    @Volatile @JvmField var isConfigUiOpen = false

    @Volatile @JvmField var guiScreenWidth: Float = 960f
    @Volatile @JvmField var guiScreenHeight: Float = 540f

    @Volatile @JvmField var isDebugScreenVisible: Boolean = false
    @Volatile @JvmField var isTabListVisible: Boolean = false
    @Volatile @JvmField var isGuiScreenOpen: Boolean = false
    @Volatile @JvmField var isChatScreenOpen: Boolean = false

    @Volatile @JvmField var masterHudEnabled: Boolean = true

    /**
     * Mirrors MC's `options.hideGui` (F1)
     *
     * Hides every HUD unless the editor is open
     */
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

    @ApiStatus.Internal
    @Volatile @JvmField var pendingAdd: Hud? = null

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

    /** [frameOrder] plus the hidden HUDs which still contribute their background to a fused shape */
    private val layoutOrder = ArrayList<Hud>()

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
            t.addMetadata(ConfigManager.PROFILE_LOCAL_METADATA, true)
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
     * NOTE NEVER CALL THIS RAW
     *
     * THERE ARE CERTAIN THINGS THAT MUST BE DONE TO PROPERLY REGISTER AN ACTIVE HUD
     */
    @ApiStatus.Internal
    val activeInstances = ArrayList<Hud>()

    private class DateTestHud : TextHud.DateTime("Date:", "yyyy-MM-dd", title = "Date")

    private class TimeTestHud : TextHud.DateTime("Time:", "HH:mm:ss", title = "Time")

    private class TextTestHud : TextHud("test", "test", Category.COMBAT, "") {
        override fun getText(): String = "mmrp\nmeow"
    }

    init {
        Snapshot.registerApplyObserver { _, _ -> contentDirty = true }

        if (java.lang.Boolean.getBoolean("oneconfig.test")) {
            register(DateTestHud())
            register(TimeTestHud())
            register(TextTestHud())
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
        revision++
        // Providers are commonly registered by later InitializationEvent handlers, after the
        // manager has already performed its first load. Coalesce those registrations into one
        // render-thread reload so default and persisted HUD instances appear on the first launch.
        if (init && activeInstances.none { it::class.java == hud::class.java }) {
            pendingProfileReload.compareAndSet(
                null,
                ProfileReload(
                    ConfigManager.activeProfile(),
                    saveCurrent = true,
                ),
            )
        }
        if (hud.updateFrequency() == 0L) LOGGER.warn("update of HUD ${hud.title} is 0, this is not recommended!")
        notifyRegistrationChanged()
    }

    @JvmStatic
    fun register(hud: Hud, configId: String) {
        hud.configId = configId
        register(hud)
    }

    /**
     * Registers a HUD under [configId] and associates a menu [icon] with that id
     *
     * The icon is used for the HUD library per-mod icon column
     *
     * [icon] is either an OC icon name such as `"combat"` resolved under `assets/oneconfig/ico/`
     * or a classpath or absolute resource path such as `"/assets/yourmod/icon.svg"`
     *
     * Lets HUD-only mods set a menu icon without registering a matching
     * [org.polyfrost.oneconfig.api.config.v1.Config]
     */
    @JvmStatic
    fun register(hud: Hud, configId: String, icon: String) {
        hudIcons[configId] = icon
        register(hud, configId)
    }

    /** The menu icon associated with [configId] via [register] or `null` if none was set */
    fun iconFor(configId: String): String? = hudIcons[configId]

    @JvmStatic
    fun register(vararg huds: Hud) {
        for (hud in huds) register(hud)
    }

    fun providers(): Collection<Hud> = hudProviders.values

    fun <T : Hud> unregister(hud: T, removeActiveInstances: Boolean = false, delete: Boolean = false): ArrayList<T>? {
        if (hudProviders.remove(hud::class.java) != null) {
            revision++
            notifyRegistrationChanged()
        }
        if (!removeActiveInstances) return null
        val out = ArrayList<T>(10.coerceAtMost(activeInstances.size))
        val iter = activeInstances.iterator()
        while (iter.hasNext()) {
            val it = iter.next()
            if (it::class.java == hud::class.java) {
                iter.remove()
                disposeHudLogging(it, delete)
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

    /** The live HUD whose config tree has this [id] used to resolve [Hud.anchorTargetId] */
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
        disposeHudLogging(hud, delete)
    }

    private fun disposeHudLogging(hud: Hud, delete: Boolean) {
        try {
            disposeHud(hud, delete)
        } catch (failure: Throwable) {
            if (failure.isFatalHudFailure()) throw failure
            LOGGER.error("Failed to dispose HUD ${hud.title}", failure)
        }
    }

    private fun disposeHud(hud: Hud, delete: Boolean) {
        val treeId = hud.tree?.id
        var failure: Throwable? = null
        fun cleanup(action: () -> Unit) {
            try {
                action()
            } catch (next: Throwable) {
                val first = failure
                if (first == null) failure = next else first.addSuppressed(next)
            }
        }

        // A user DisposableEffect is allowed to run while the composition is disposed. Even if it
        // fails, finish detaching the HUD so a profile switch cannot leave instances from two
        // profiles active at the same time.
        cleanup { hud._runtime?.dispose() }
        hud._runtime = null
        lastUpdates.remove(hud)
        // anything hanging off this HUD goes back to screen positioning and stays where it is
        // because the relative position kept alongside the anchor is already up to date
        treeId?.let { gone ->
            for (it in activeInstances) {
                if (it.anchorTargetId == gone) cleanup { it.clearAnchor() }
            }
        }
        for (it in activeInstances) {
            if (it.mergeLinkX?.parent === hud || it.mergeLinkY?.parent === hud) cleanup { it.clearMergeLink() }
        }
        lastMergeKey = null
        cleanup { invalidate() }
        try { hud.remove() } catch (_: Throwable) {}
        // a HUD the user cannot delete must never lose its config because an errant
        // unregister(delete = true) would wipe it from disk with no way to restore it
        if (delete && !hud.deletable()) {
            LOGGER.warn("refusing to delete the config of ${hud.title}, which is marked as not user-deletable")
        } else if (delete && treeId != null) {
            cleanup { ConfigManager.active().delete(treeId) }
        }
        // back to being a plain provider so a single-instance HUD can be made again later
        cleanup { hud.detachTree() }
        failure?.let { throw it }
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
        if (hud.hidden && !isEditing) return false
        return isShown(hud)
    }

    private fun keepsBackgroundOnly(hud: Hud): Boolean =
        hud.hidden && !isEditing && hud.keepsHiddenBackground && isShown(hud)

    /** Everything [shouldDraw] checks apart from the HUD's own hidden flag */
    private fun isShown(hud: Hud): Boolean {
        if (hud is LegacyHudMarker) return false
        if (isEditing) return true
        if (!masterHudEnabled) return false
        if (isGuiHidden) return false
        if (isDebugScreenVisible && !hud.showInF3) return false
        if (isTabListVisible && !hud.showInTab) return false
        if (!overrideShowInScreens) {
            // chat has its own toggle so it is never governed by "Show in GUIs"
            if (isChatScreenOpen) {
                if (!hud.showInChat) return false
            } else if (isGuiScreenOpen && !hud.showInScreens) return false
        }
        return true
    }

    private fun collectFrameOrder(): Boolean {
        frameOrder.clear()
        layoutOrder.clear()
        var volatileContent = false
        for (hud in orderedForRender()) {
            if (shouldDraw(hud)) {
                frameOrder.add(hud)
                if (hud.alwaysRedraw) volatileContent = true
            } else if (keepsBackgroundOnly(hud)) {
                layoutOrder.add(hud)
                if (hud.bgChroma) volatileContent = true
            }
        }
        layoutOrder.addAll(0, frameOrder)
        return volatileContent
    }

    @ApiStatus.Internal
    fun beginFrame(screenWidth: Float, screenHeight: Float): Boolean {
        drainProfileReload()
        val scale = Platform.compatibility().options().guiScale

        frameId++

        Snapshot.sendApplyNotifications()

        val volatileContent = collectFrameOrder()

        updateAndAdvance(layoutOrder)

        layoutAll(layoutOrder, screenWidth, screenHeight, scale)
        updateBackgroundGroups(layoutOrder, screenWidth, screenHeight, scale)

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
     * Rebuilds the fused background shapes for HUDs which sit against each other
     *
     * Tells the HUDs in a fused shape to stop drawing their own background
     *
     * Cheap on frames where nothing moved because the shapes are only re-traced
     * when a position or size or background style actually changed
     */
    private fun updateBackgroundGroups(huds: List<Hud>, screenWidth: Float, screenHeight: Float, scale: Float) {
        val key = HudBackgroundMerge.layoutKey(huds)
        if (key == lastMergeKey) return
        lastMergeKey = key
        // the fused outline can change without anything else asking for a redraw
        // for example when a hidden HUD which keeps its background resizes
        // so the cached frame is no longer good
        invalidate()

        val mergeable = if (mergeExclusions.isEmpty()) huds else huds.filter { hud -> !isMergeExcluded(hud) }
        frameGroups = HudBackgroundMerge.computeGroups(mergeable)
        val merged = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Hud, Boolean>())
        for (group in frameGroups) merged.addAll(group.huds)
        updateMergeLinks()

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
        // the flag is read during composition so recompose and re-lay out now
        // instead of letting the change land a frame late
        // which would show a doubled or missing background for one frame
        Snapshot.sendApplyNotifications()
        PolyComposeHost.frame()
        for (hud in huds) hud.lastLayoutFrame = -1L
        layoutAll(huds, screenWidth, screenHeight, scale)
        invalidate()
    }

    private var mergeExclusions: List<Hud> = emptyList()

    /**
     * HUDs kept out of merging for now
     *
     * Used by the editor while the user pulls one out of a fused shape
     *
     * An excluded HUD draws its own background again and stops dragging its neighbours along
     * so it comes away cleanly and re-merges wherever it is dropped
     */
    @ApiStatus.Internal
    @JvmStatic
    fun setMergeExclusions(huds: Collection<Hud>) {
        val next = huds.toList()
        if (next.size == mergeExclusions.size && next.indices.all { next[it] === mergeExclusions[it] }) return
        mergeExclusions = next
        lastMergeKey = null
        invalidate()
    }

    private fun isMergeExcluded(hud: Hud): Boolean = mergeExclusions.any { it === hud }

    /** Every HUD fused into the same background shape as [hud] including [hud] itself */
    @ApiStatus.Internal
    @JvmStatic
    fun mergeGroupOf(hud: Hud): List<Hud> =
        frameGroups.firstOrNull { group -> group.huds.any { it === hud } }?.huds ?: listOf(hud)

    /**
     * Holds every HUD in a fused shape against the neighbour it touches at the point where the two meet
     * so a HUD which grows keeps the shape flush
     *
     * These links live only as long as the merge does
     *
     * A HUD which stops being merged is let go and stays where it was left
     */
    private fun updateMergeLinks() {
        val linkedX = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Hud, Boolean>())
        val linkedY = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Hud, Boolean>())
        Snapshot.withMutableSnapshot {
            for (group in frameGroups) {
                for (link in group.links) {
                    link.child.applyMergeLink(link.parent, link.childPoint, link.parentPoint, link.axis)
                    (if (link.axis == MergeAxis.X) linkedX else linkedY).add(link.child)
                }
            }
            for (hud in activeInstances) {
                hud.clearMergeLinks(clearX = hud !in linkedX, clearY = hud !in linkedY)
            }
        }
    }

    /**
     * Paints the fused backgrounds of merged HUDs in gui units
     *
     * Used by renderers which draw the HUD trees themselves (the editor viewport)
     * instead of going through [render]
     *
     * The HUDs in a fused shape do not draw their own background
     * so without this they would show up bare
     */
    @ApiStatus.Internal
    fun drawMergedBackgrounds(ctx: RenderContext) {
        for (group in frameGroups) {
            try {
                group.draw(ctx)
            } catch (e: Throwable) {
                LOGGER.error("Failed to draw merged HUD background", e)
            }
        }
    }

    private fun frameKey(): Long {
        var key = activeInstances.size.toLong() * 31L + frameOrder.size
        key = key * 31L + layoutOrder.size
        key = key * 31L + (if (isDebugScreenVisible) 1 else 0)
        key = key * 31L + (if (isTabListVisible) 1 else 0)
        key = key * 31L + (if (isGuiScreenOpen) 1 else 0)
        key = key * 31L + (if (isChatScreenOpen) 1 else 0)
        key = key * 31L + (if (isGuiHidden) 1 else 0)
        key = key * 31L + (if (overrideShowInScreens) 1 else 0)
        key = key * 31L + (if (masterHudEnabled) 1 else 0)
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
        drainProfileReload()
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
        updateBackgroundGroups(huds, screenWidth, screenHeight, scale)
    }

    @ApiStatus.Internal
    fun render(ctx: RenderContext, screenWidth: Float, screenHeight: Float) {
        val scale = Platform.compatibility().options().guiScale

        val prepared = preparedFrameValid
        preparedFrameValid = false
        if (!prepared) {
            Snapshot.sendApplyNotifications()
            frameId++
            collectFrameOrder()
            updateAndAdvance(layoutOrder)
            layoutAll(layoutOrder, screenWidth, screenHeight, scale)
            updateBackgroundGroups(layoutOrder, screenWidth, screenHeight, scale)
        }

        ctx.save()
        ctx.scale(scale, scale)

        drawMergedBackgrounds(ctx)

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
        // always re-assert editing and post OPEN even when isEditorOpen is already true because
        // the flag can get stuck and an early return would make the Edit HUD button do nothing
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
        // a drag interrupted by the editor closing must not leave a HUD unable to merge
        setMergeExclusions(emptyList())
    }

    @ApiStatus.Internal
    fun initialize() {
        if (init) throw IllegalStateException("HudManager.initialize() called twice!")
        ConfigManager.active()
        init = true
        ConfigManager.addProfileChangeListener(profileChangeListener)
        LOGGER.info("Initializing HUD...")
        loadFromActiveProfile()
    }

    @ApiStatus.Internal
    fun setProfileReloadDispatcher(dispatcher: Consumer<Runnable>) {
        profileReloadDispatcher = dispatcher
    }

    private fun teardownForProfile(profile: String) {
        val kept = ArrayList<Hud>(activeInstances.size)
        for (hud in ArrayList(activeInstances)) {
            if (!hud.profileLocalTree) {
                kept.add(hud)
                continue
            }
            activeInstances.remove(hud)
            try {
                disposeHud(hud, delete = false)
            } catch (failure: Throwable) {
                if (failure.isFatalHudFailure()) throw failure
                LOGGER.error("Failed to dispose HUD ${hud.title} while switching profiles", failure)
            }
        }
        knownProviders.clear()
        registryTree = null
        zOrderCache = emptyList()
        preparedFrameValid = false
        lastMergeKey = null
        frameOrder.clear()
        layoutOrder.clear()
        frameGroups = emptyList()
        setMergeExclusions(emptyList())
        pendingSelection = null
        pendingAdd = null
        LOGGER.info("Reloading HUDs for profile '{}' ({} wrapped HUDs kept)", profile, kept.size)
    }

    private fun restoreProviderDefaults() {
        for (provider in hudProviders.values) {
            if (!provider.profileLocalTree) continue
            try {
                provider.restoreCapturedDefaults()
            } catch (failure: Throwable) {
                if (failure.isFatalHudFailure()) throw failure
                LOGGER.error("Failed to restore defaults for HUD ${provider.title}", failure)
            }
        }
    }

    private fun drainProfileReload() {
        val reload = pendingProfileReload.getAndSet(null) ?: return
        try {
            synchronized(ConfigManager::class.java) {
                if (reload.saveCurrent && ConfigManager.activeProfile() == reload.profile) {
                    ConfigManager.active().saveAll()
                }
            }
            teardownForProfile(reload.profile)
            synchronized(ConfigManager::class.java) {
                restoreProviderDefaults()
                loadFromActiveProfile(reload.trees)
            }
            revision++
            invalidate()
        } catch (e: Throwable) {
            if (e.isFatalHudFailure()) throw e
            LOGGER.error("Failed to reload HUDs for profile '{}'", reload.profile, e)
        }
    }

    /** Applies a queued profile change on the UI thread and waits for it to finish. */
    private fun applyPendingProfileReload() {
        if (pendingProfileReload.get() == null) return
        ConfigManager.dispatchAndWait(
            profileReloadDispatcher,
            ::drainProfileReload,
            TimeUnit.SECONDS.toNanos(UI_THREAD_TIMEOUT_SECONDS),
            "the HUD profile reload",
        )
    }

    private fun gatherHudTrees(): Collection<Tree> = try {
        ConfigManager.active().gatherAll("huds")
    } catch (e: Throwable) {
        if (e.isFatalHudFailure()) throw e
        LOGGER.error("Failed to read HUDs from the active profile", e)
        emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadFromActiveProfile(prefetched: Collection<Tree>? = null) {
        val now = System.nanoTime()
        val loader = HudManager::class.java.classLoader
        val used = HashSet<Class<Hud>>(hudProviders.size)
        val failedProviders = HashSet<Class<Hud>>()
        val failed = HashMap<String, Int>(8)
        var i = 0

        fun rollback(candidate: Hud?) {
            if (candidate == null) return
            val treeId = candidate.tree?.id
            if (treeId != null) {
                try {
                    ConfigManager.active().unregister(treeId)
                } catch (failure: Throwable) {
                    LOGGER.error("Failed to untrack broken HUD tree $treeId", failure)
                }
            }
            activeInstances.remove(candidate)
            try {
                disposeHud(candidate, delete = false)
            } catch (failure: Throwable) {
                candidate.detachTree()
                LOGGER.error("Failed to dispose broken HUD ${candidate.title}", failure)
            }
        }

        loadRegistry()

        (prefetched ?: gatherHudTrees()).forEach { data ->
            var candidate: Hud? = null
            var providerClass: Class<Hud>? = null
            try {
                val clsName = data.getProp("hudClass").get() as? String
                    ?: throw IllegalArgumentException("hud tree ${data.id} is missing class name")
                if (clsName.endsWith(".OneConfigHudCompat")) return@forEach
                val cls = Class.forName(clsName, true, loader) as? Class<Hud>
                    ?: throw IllegalArgumentException("$clsName is not a subclass of Hud")
                providerClass = cls
                val h = hudProviders[cls] ?: MHUtils.instantiate(cls, true).getOrThrow()
                // A previous HUD instance may still own this ID when the same backend is reloaded.
                // Drop only the in-memory binding; make() will load the unchanged file into the new HUD.
                ConfigManager.active().unregister(data.id)
                val hud = h.make(data)
                candidate = hud
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
                used.add(cls)
                i++
            } catch (e: ClassNotFoundException) {
                rollback(candidate)
                providerClass?.let(failedProviders::add)
                val cls = e.message?.substringAfter(':')?.trim() ?: "unknown"
                failed[cls] = failed.getOrDefault(cls, 0) + 1
            } catch (e: Throwable) {
                rollback(candidate)
                providerClass?.let(failedProviders::add)
                if (e.isFatalHudFailure()) throw e
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
            var candidate: Hud? = null
            try {
                if (cls in used || cls in failedProviders) return@forEach
                if (h.isReal) return@forEach
                val known = cls.name in knownProviders
                val deletable = h.deletable()
                // A HUD the user cannot delete has no legitimate "deleted" state, so a missing
                // instance always means its config was lost. Restore it instead of stranding it.
                val restore = if (deletable) {
                    h.showByDefault() && !known
                } else {
                    h.showByDefault() || known
                }
                if (!restore) return@forEach
                if (known && !deletable) {
                    LOGGER.warn("HUD ${h.title} cannot be deleted but had no instance; restoring it")
                }
                val (dx, dy) = h.defaultPosition()
                val hud = h.make()
                candidate = hud
                hud.setAbsolutePosition(dx, dy)
                activeInstances.add(hud)
                hud.setup()
                hud.captureStaticSizeDefaults()
                hud.capturePositionDefaults()
                registryChanged = knownProviders.add(cls.name) or registryChanged
                LOGGER.info("Added HUD ${hud.title} at default position ($dx, $dy)")
            } catch (e: Throwable) {
                rollback(candidate)
                if (e.isFatalHudFailure()) throw e
                LOGGER.error("Failed to add default HUD ${h.title}", e)
            }
        }

        if (registryChanged) saveRegistry()

        LOGGER.info("HUD load took {}ms, loaded {} HUDs from {} providers ({} registered)",
            (System.nanoTime() - now) / 1_000_000.0, i, used.size, hudProviders.size)
    }
}
