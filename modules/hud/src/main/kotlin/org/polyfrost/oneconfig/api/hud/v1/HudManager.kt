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

@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.node.RootNode
import org.polyfrost.compose.render.CapturingRenderer
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeHost
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.utils.v1.MHUtils
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

private fun Throwable.isFatalHudFailure(): Boolean = this is VirtualMachineError

public object HudManager {
    internal val LOGGER = LogManager.getLogger("OneConfig/HUD")

    private val hudProviders = HashMap<Class<out Hud>, Hud>()
    private val unsupportedHuds: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val pendingUnsupported: MutableSet<Class<out Hud>> = ConcurrentHashMap.newKeySet()
    private val hudIcons = HashMap<String, String>()
    private val registrationListeners = CopyOnWriteArrayList<Runnable>()

    public var revision: Int by mutableIntStateOf(0)
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
    private const val HIDDEN_HUD_ALPHA = 0.35f
    private const val VOLATILE_HITS = 60

    /**
     * `true` while HUDs are being shown for editing or preview
     *
     * That is while either the HUD editor or the main OneConfig UI is open
     *
     * HUDs should render example content when this is set
     */
    @get:JvmName("isEditing")
    public val isEditing: Boolean get() = isEditorOpen || isConfigUiOpen

    /** `true` only while the HUD editor screen itself is open */
    @Volatile public var isEditorOpen: Boolean = false
        private set

    /** `true` while OneConfig draws vanilla HUD elements off-screen to measure them for the HUD editor */
    @Volatile @JvmField public var isSamplingVanilla: Boolean = false

    /** Set by the OneConfig UI screen while it is open so HUDs behind it still render as previews */
    @Volatile @JvmField public var isConfigUiOpen: Boolean = false

    @ExperimentalOneConfigUiApi
    @Volatile @JvmField public var partialTick: Float = 1f

    @Volatile @JvmField public var guiScreenWidth: Float = 960f
    @Volatile @JvmField public var guiScreenHeight: Float = 540f

    @Volatile @JvmField public var layoutRefWidth: Float = 0f

    @Volatile @JvmField public var layoutRefHeight: Float = 0f

    @get:ApiStatus.Experimental
    @JvmStatic
    public val layoutReferenceWidth: Float get() = layoutRefWidth

    @get:ApiStatus.Experimental
    @JvmStatic
    public val layoutReferenceHeight: Float get() = layoutRefHeight

    @Volatile private var loadingLayout = false

    @Volatile @JvmField internal var systemReposition = false

    @ExperimentalOneConfigUiApi
    @get:JvmName("isSystemPosition")
    @JvmStatic
    public val isSystemPosition: Boolean get() = systemReposition

    internal fun noteLayoutArranged() {
        if (loadingLayout || systemReposition) return
        val w = guiScreenWidth
        val h = guiScreenHeight
        if (w > 0f) layoutRefWidth = w
        if (h > 0f) layoutRefHeight = h
    }

    @Volatile @JvmField public var isDebugScreenVisible: Boolean = false
    @Volatile @JvmField public var isTabListVisible: Boolean = false
    @Volatile @JvmField public var isGuiScreenOpen: Boolean = false
    @Volatile @JvmField public var isChatScreenOpen: Boolean = false

    /**
     * Mirrors MC's `options.hideGui` (F1)
     *
     * Hides every HUD unless the editor is open
     */
    @Volatile @JvmField public var isGuiHidden: Boolean = false

    @Volatile @JvmField public var overrideShowInScreens: Boolean = false

    @ApiStatus.Internal
    @Volatile @JvmField public var targetPixelWidth: Int = 0

    @ApiStatus.Internal
    @Volatile @JvmField public var targetPixelHeight: Int = 0

    @ApiStatus.Internal
    @Volatile @JvmField public var inWorld: Boolean = true

    @ApiStatus.Internal
    @Volatile @JvmField public var pendingSelection: Hud? = null

    @ApiStatus.Internal
    @Volatile @JvmField public var pendingAdd: Hud? = null

    private var wasEditing = false

    private val showingPreviews get() = isConfigUiOpen || isEditorOpen

    @ApiStatus.Internal
    public val previewRevision: MutableIntState = mutableIntStateOf(0)

    @ApiStatus.Internal
    public val renderRevision: MutableIntState = mutableIntStateOf(0)

    /** Compose cannot see changes to [activeInstances] so bump this whenever a HUD is added or removed */
    @ApiStatus.Internal
    public val instancesRevision: MutableIntState = mutableIntStateOf(0)

    @ApiStatus.Internal
    public val editorOpenRevision: MutableIntState = mutableIntStateOf(0)

    /** Compose cannot see writes to [guiScreenWidth] and [guiScreenHeight] so this is bumped when either changes */
    @ApiStatus.Internal
    public val screenRevision: MutableIntState = mutableIntStateOf(0)

    private var notedScreenW = Float.NaN
    private var notedScreenH = Float.NaN

    private fun noteScreenSize() {
        if (guiScreenWidth == notedScreenW && guiScreenHeight == notedScreenH) return
        notedScreenW = guiScreenWidth
        notedScreenH = guiScreenHeight
        screenRevision.intValue++
    }

    private val redrawCacheDisabled = java.lang.Boolean.getBoolean("oneconfig.hud.nocache")

    @Volatile private var contentDirty = true
    private var relaidOut = false

    @JvmStatic
    public fun invalidate() {
        drawEpoch++
        contentDirty = true
    }

    // watches the snapshot state read while drawing so a change to it repaints the cached frame
    private val drawReadObserver = SnapshotStateObserver { it() }.also { it.start() }
    private val onDrawStateChanged: (Any) -> Unit = { invalidate() }
    private val onHudDrawStateChanged: (Any) -> Unit = { scope ->
        (scope as Hud).drawReads++
        contentDirty = true
    }

    internal class HudDraw(
        val runtime: PolyComposeRuntime,
        val revision: Long,
        val reads: Int,
        val epoch: Int,
        val editing: Boolean,
        val key: Any,
        val captured: Any,
        val missFrame: Long,
        var volatile: Boolean,
    ) {
        var hits = 0
    }

    private var drawnHuds = ArrayList<Hud>()
    private var nextDrawnHuds = ArrayList<Hud>()
    private var renderId = 0L
    private var drawEpoch = 0
    private var framePos = FloatArray(0)
    private var frameRedraw = BooleanArray(0)
    private var frameVolatile = BooleanArray(0)
    private var frameSize = FloatArray(0)
    private var mergedDraw: Triple<List<HudBackgroundMerge.Group>, Any, Any>? = null

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

    private val prepareOrder = ArrayList<Hud>()

    private val backgroundOrder = ArrayList<Hud>()

    private val frameDrawn = IdentityHashMap<Hud, Boolean>()

    private var frameGroups: List<HudBackgroundMerge.Group> = emptyList()
    private var lastMergeKey: Int? = null

    private val renderable = ArrayList<Hud>()
    private var zOrderCache: List<Hud> = emptyList()
    private var zOrderFrame = Long.MIN_VALUE
    private var zOrderHuds = arrayOfNulls<Hud>(0)
    private var zOrderBounds = FloatArray(0)
    private var zOrderDrawn = BooleanArray(0)
    private var zOrderStamps = IntArray(0)
    private val boundsScratch = FloatArray(4)

    private const val REGISTRY_ID = "hud-registry.json"
    private const val KNOWN_HUDS = "knownHuds"
    private const val KNOWN_GROUPS = "knownGroups"

    private val knownProviders = LinkedHashSet<String>()
    private val knownGroups = LinkedHashSet<String>()
    private var registryTree: Tree? = null

    private fun registryProperty() = Properties.simple(
        KNOWN_HUDS, "Known HUDs",
        "HUD providers which have already been given their default instance.",
        emptyArray<String>(), Array<String>::class.java
    )

    private fun knownGroupsProperty() = Properties.simple(
        KNOWN_GROUPS, "Known HUD groups",
        "Default HUD groups which have already been applied once.",
        emptyArray<String>(), Array<String>::class.java
    )

    private fun loadRegistry() {
        try {
            val mgr = ConfigManager.active()
            val t = mgr.trees().firstOrNull { it.id == REGISTRY_ID }
                ?: mgr.register(Tree.tree(REGISTRY_ID).put(registryProperty()).put(knownGroupsProperty())).get()
            if (t.getProp(KNOWN_HUDS) == null) t.put(registryProperty())
            if (t.getProp(KNOWN_GROUPS) == null) t.put(knownGroupsProperty())
            t.addMetadata("hidden", true)
            t.addMetadata(ConfigManager.PROFILE_LOCAL_METADATA, true)
            registryTree = t
            readInto(t.getProp(KNOWN_HUDS)?.get(), knownProviders)
            readInto(t.getProp(KNOWN_GROUPS)?.get(), knownGroups)
        } catch (e: Exception) {
            LOGGER.error("Failed to load HUD registry, HUD deletions may not persist", e)
        }
    }

    private fun readInto(stored: Any?, into: MutableSet<String>) {
        when (stored) {
            is Array<*> -> stored.forEach { if (it != null) into.add(it.toString()) }
            is Iterable<*> -> stored.forEach { if (it != null) into.add(it.toString()) }
        }
    }

    internal fun isGroupKnown(id: String): Boolean = id in knownGroups

    internal fun markGroupKnown(id: String) {
        if (knownGroups.add(id)) saveRegistry()
    }

    private fun saveRegistry() {
        val t = registryTree ?: return
        try {
            t.getProp(KNOWN_HUDS)?.setAs(knownProviders.toTypedArray())
            t.getProp(KNOWN_GROUPS)?.setAs(knownGroups.toTypedArray())
            ConfigManager.active().save(REGISTRY_ID)
        } catch (e: Exception) {
            LOGGER.error("Failed to save HUD registry", e)
        }
    }

    @JvmStatic
    public fun markProviderKnown(hud: Hud) {
        if (knownProviders.add(hud::class.java.name)) saveRegistry()
    }

    /**
     * NOTE NEVER CALL THIS RAW
     *
     * THERE ARE CERTAIN THINGS THAT MUST BE DONE TO PROPERLY REGISTER AN ACTIVE HUD
     */
    public val activeInstances: ArrayList<Hud> = ArrayList<Hud>()

    @ApiStatus.Experimental
    @JvmStatic
    public fun instances(): List<Hud> = activeInstances.toList()

    private class DateTestHud : TextHud.DateTime("Date:", "yyyy-MM-dd", title = "Date")

    private class TimeTestHud : TextHud.DateTime("Time:", "HH:mm:ss", title = "Time")

    private class TextTestHud : TextHud("test", "test", Category.COMBAT, "") {
        override fun getText(): String = "mmrp\nmeow"
    }

    init {
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
    public fun addRegistrationListener(listener: Runnable) {
        registrationListeners.add(listener)
    }

    private fun notifyRegistrationChanged() = registrationListeners.forEach(Runnable::run)

    @JvmStatic
    public fun register(hud: Hud) {
        unsupportedHuds.remove(hud::class.java.name)
        HudStyles.captureBuiltIn(hud)
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
    public fun register(vararg huds: Hud) {
        for (hud in huds) register(hud)
    }

    @JvmStatic
    public fun register(hud: Hud, configId: String) {
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
    public fun register(hud: Hud, configId: String, icon: String) {
        hudIcons[configId] = icon
        register(hud, configId)
    }

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun register(hud: Hud, configId: String? = null, icon: String? = null, supported: () -> Boolean = { true }) {
        afterInit {
            val ok = try {
                supported()
            } catch (e: Throwable) {
                if (e.isFatalHudFailure()) throw e
                LOGGER.error("Failed to check whether HUD ${hud.title} is supported, it will not be registered", e)
                false
            }
            if (!ok) {
                if (hud::class.java !in hudProviders) {
                    unsupportedHuds.add(hud::class.java.name)
                    pendingUnsupported.add(hud::class.java)
                }
                LOGGER.info("HUD ${hud.title} is not supported here and was not registered")
                return@afterInit
            }
            val id = configId ?: hud.configId
            when {
                id != null && icon != null -> register(hud, id, icon)
                id != null -> register(hud, id)
                else -> {
                    if (icon != null) LOGGER.warn("HUD ${hud.title} has no config id, so its icon $icon was not used")
                    register(hud)
                }
            }
        }
    }

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun registerLater(factory: () -> Hud) {
        afterInit {
            val hud = try {
                factory()
            } catch (e: Throwable) {
                if (e.isFatalHudFailure()) throw e
                LOGGER.error("Failed to create a HUD registered with registerLater", e)
                return@afterInit
            }
            register(hud)
        }
    }

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun registerLater(configId: String?, icon: String? = null, supported: () -> Boolean = { true }, factory: () -> Hud) {
        afterInit {
            val hud = try {
                factory()
            } catch (e: Throwable) {
                if (e.isFatalHudFailure()) throw e
                LOGGER.error("Failed to create a HUD registered with registerLater", e)
                return@afterInit
            }
            register(hud, configId, icon, supported)
        }
    }

    private val pendingRegistrations = ArrayList<() -> Unit>()
    private var registrationsOpen = false

    private fun afterInit(action: () -> Unit) {
        synchronized(pendingRegistrations) {
            if (!registrationsOpen) {
                pendingRegistrations.add(action)
                return
            }
        }
        action()
    }

    private fun runPendingRegistrations() {
        while (true) {
            val batch = synchronized(pendingRegistrations) {
                registrationsOpen = true
                if (pendingRegistrations.isEmpty()) return
                ArrayList(pendingRegistrations).also { pendingRegistrations.clear() }
            }
            for (action in batch) {
                try {
                    action()
                } catch (e: Throwable) {
                    if (e.isFatalHudFailure()) throw e
                    LOGGER.error("Failed to run a deferred HUD registration", e)
                }
            }
        }
    }

    /** The menu icon associated with [configId] via [register] or `null` if none was set */
    public fun iconFor(configId: String): String? = hudIcons[configId]

    public fun providers(): Collection<Hud> = hudProviders.values

    public fun <T : Hud> unregister(hud: T, removeActiveInstances: Boolean = false, delete: Boolean = false): ArrayList<T>? {
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
                instancesRevision.intValue++
                disposeHudLogging(it, delete)
                @Suppress("UNCHECKED_CAST")
                out.add(it as T)
            }
        }
        return out
    }

    public fun <T : Hud> getHudsOfType(hudClass: Class<T>): ArrayList<T> {
        val out = ArrayList<T>(10.coerceAtMost(activeInstances.size))
        for (it in activeInstances) {
            if (it::class.java == hudClass) {
                @Suppress("UNCHECKED_CAST")
                out.add(it as T)
            }
        }
        return out
    }

    public fun hasHudOfType(hudClass: Class<out Hud>): Boolean = activeInstances.any { it::class.java == hudClass }

    public fun getProvider(hudClass: Class<out Hud>): Hud? = hudProviders[hudClass]

    /** The live HUD whose config tree has this [id] used to resolve [Hud.anchorTargetId] */
    public fun instanceById(id: String): Hud? {
        val instances = activeInstances
        for (i in instances.indices) {
            val hud = instances[i]
            if (hud.tree?.id == id) return hud
        }
        return null
    }

    public fun toggleAllHuds(hud: Hud, hidden: Boolean) {
        hudProviders[hud::class.java]?.hidden = hidden
        for (it in activeInstances) {
            if (it::class.java == hud::class.java) it.hidden = hidden
        }
    }

    public fun removeHud(hud: Hud, delete: Boolean = false) {
        if (!hud.isReal) LOGGER.warn("Removing HUD ${hud.title}, which has no config tree")
        if (activeInstances.remove(hud)) instancesRevision.intValue++
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
        hud.lastDraw = null
        drawReadObserver.clear(hud)
        hud.lastUpdate = Long.MIN_VALUE
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
        notShowing.remove(hud)
        if (delete) cleanup { dropSuppressionsOf(hud) }
        cleanup { invalidate() }
        try { hud.remove() } catch (_: Throwable) {}
        // a HUD the user cannot delete must never lose its config because an errant
        // unregister(delete = true) would wipe it from disk with no way to restore it
        if (delete && !hud.deletable()) {
            LOGGER.warn("refusing to delete the config of ${hud.title}, which is marked as not user-deletable")
        } else if (delete && treeId != null) {
            cleanup { ConfigManager.active().delete(treeId) }
            cleanup { CompatSnapshots.untrack(treeId) }
            cleanup { hud.groupPath = "" }
        }
        // back to being a plain provider so a single-instance HUD can be made again later
        cleanup { hud.detachTree() }
        failure?.let { throw it }
    }

    internal fun bodyBounds(hud: Hud): FloatArray? = screenBounds(hud)

    private fun screenBounds(hud: Hud): FloatArray? {
        val out = FloatArray(4)
        return if (fillBounds(hud, out, 0)) out else null
    }

    internal fun bodyBounds(hud: Hud, out: FloatArray): Boolean = fillBounds(hud, out, 0)

    private fun fillBounds(hud: Hud, out: FloatArray, offset: Int): Boolean {
        val scale = hud.frameEffectiveScale
        val w = if (hud.staticWidth) {
            val s = hud.staticW
            if (s > 0f) s * scale else return false
        } else {
            val r = hud.renderedW
            if (r > 0f) {
                r
            } else {
                val s = hud.staticW
                if (s > 0f) s * scale else return false
            }
        }
        val h = if (hud.staticWidth) {
            val s = hud.staticH
            if (s > 0f) s * scale else return false
        } else {
            val r = hud.renderedH
            if (r > 0f) {
                r
            } else {
                val s = hud.staticH
                if (s > 0f) s * scale else return false
            }
        }
        out[offset] = hud.frameX
        out[offset + 1] = hud.frameY
        out[offset + 2] = w
        out[offset + 3] = h
        return true
    }

    private fun encloses(outer: FloatArray, inner: FloatArray): Boolean =
        inner[0] >= outer[0] && inner[1] >= outer[1] &&
            inner[0] + inner[2] <= outer[0] + outer[2] &&
            inner[1] + inner[3] <= outer[1] + outer[3]

    private fun encloses(b: FloatArray, outer: Int, inner: Int): Boolean =
        b[inner] >= b[outer] && b[inner + 1] >= b[outer + 1] &&
            b[inner] + b[inner + 2] <= b[outer] + b[outer + 2] &&
            b[inner + 1] + b[inner + 3] <= b[outer + 1] + b[outer + 3]

    @ApiStatus.Internal
    public fun zOrderedInstances(bounds: (Hud) -> FloatArray? = ::screenBounds): List<Hud> = zOrdered(activeInstances, bounds)

    private var pointerHuds = arrayOfNulls<Hud>(0)
    private var pointerStamps = IntArray(0)
    private var pointerHas = BooleanArray(0)
    private var pointerBounds = FloatArray(0)
    private var pointerOrderHas = BooleanArray(0)
    private var pointerOrderBounds = FloatArray(0)
    private var pointerCount = -1
    private var pointerCache: List<Hud> = emptyList()
    private val pointerScratch = FloatArray(4)

    internal fun pointerOrder(): List<Hud> {
        val list = activeInstances
        val n = list.size
        if (pointerHuds.size < n) {
            pointerHuds = arrayOfNulls(n)
            pointerStamps = IntArray(n)
            pointerHas = BooleanArray(n)
            pointerBounds = FloatArray(n * 4)
            pointerOrderHas = BooleanArray(n)
            pointerOrderBounds = FloatArray(n * 4)
            pointerCount = -1
        }
        var same = pointerCount == n
        pointerCount = -1
        val b = pointerScratch
        val bounds = pointerBounds
        for (i in 0 until n) {
            val hud = list[i]
            val stamp = if (hud is LegacyHudMarker) -1 else hud.geometryStamp
            if (same && pointerHuds[i] === hud && stamp != -1 && pointerStamps[i] == stamp) continue
            val has = fillBounds(hud, b, 0)
            pointerStamps[i] = stamp
            val o = i * 4
            if (same && (pointerHuds[i] !== hud || pointerHas[i] != has || has && (
                    !bounds[o].sameBound(b[0]) || !bounds[o + 1].sameBound(b[1]) ||
                    !bounds[o + 2].sameBound(b[2]) || !bounds[o + 3].sameBound(b[3])))
            ) {
                same = false
            }
            pointerHuds[i] = hud
            pointerHas[i] = has
            if (has) {
                bounds[o] = b[0]
                bounds[o + 1] = b[1]
                bounds[o + 2] = b[2]
                bounds[o + 3] = b[3]
            }
        }
        if (same) {
            pointerCount = n
            return pointerCache
        }
        val has = pointerHas
        val depth = IntArray(n)
        for (i in 0 until n) {
            if (!has[i]) continue
            val oi = i * 4
            for (j in 0 until n) {
                if (i == j || !has[j]) continue
                val oj = j * 4
                if (encloses(bounds, oj, oi) && (!encloses(bounds, oi, oj) || j < i)) depth[i]++
            }
        }
        var max = 0
        for (i in 0 until n) if (depth[i] > max) max = depth[i]
        val out = ArrayList<Hud>(n)
        val orderHas = pointerOrderHas
        val orderBounds = pointerOrderBounds
        for (d in 0..max) {
            for (i in 0 until n) {
                if (depth[i] != d) continue
                val k = out.size
                out.add(list[i])
                orderHas[k] = has[i]
                val o = i * 4
                val ok = k * 4
                orderBounds[ok] = bounds[o]
                orderBounds[ok + 1] = bounds[o + 1]
                orderBounds[ok + 2] = bounds[o + 2]
                orderBounds[ok + 3] = bounds[o + 3]
            }
        }
        pointerCache = out
        pointerCount = n
        return out
    }

    internal fun pointerBody(index: Int, out: FloatArray): Boolean {
        if (index >= pointerCache.size || !pointerOrderHas[index]) return false
        val o = index * 4
        val bounds = pointerOrderBounds
        out[0] = bounds[o]
        out[1] = bounds[o + 1]
        out[2] = bounds[o + 2]
        out[3] = bounds[o + 3]
        return true
    }

    private fun zOrdered(list: List<Hud>, bounds: (Hud) -> FloatArray?): List<Hud> {
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
        return byDepth(list, depth, n)
    }

    private fun zOrderedByBounds(list: List<Hud>, drawnOnly: Boolean): List<Hud> {
        val n = list.size
        if (n <= 1) return list
        val b = FloatArray(n * 4)
        val has = BooleanArray(n)
        for (i in 0 until n) {
            val hud = list[i]
            if (drawnOnly && hud !in frameDrawn) continue
            has[i] = fillBounds(hud, b, i * 4)
        }
        val depth = IntArray(n)
        for (i in 0 until n) {
            if (!has[i]) continue
            val oi = i * 4
            for (j in 0 until n) {
                if (i == j || !has[j]) continue
                val oj = j * 4
                if (encloses(b, oj, oi) && (!encloses(b, oi, oj) || j < i)) depth[i]++
            }
        }
        return byDepth(list, depth, n)
    }

    private fun byDepth(list: List<Hud>, depth: IntArray, n: Int): List<Hud> {
        var max = 0
        for (i in 0 until n) if (depth[i] > max) max = depth[i]
        val out = ArrayList<Hud>(n)
        for (d in 0..max) {
            for (i in 0 until n) if (depth[i] == d) out.add(list[i])
        }
        return out
    }

    private fun updateAndAdvance() {
        var providerContent = false
        if (showingPreviews) for (hud in hudProviders.values) {
            val stamp = hud.contentStamp
            updateIfDue(hud)
            if (hud.contentStamp != stamp) providerContent = true
        }
        if (PolyComposeHost.frameWithReport()) {
            contentDirty = true
            if (showingPreviews) renderRevision.intValue++
        }
        if (showingPreviews) {
            PolyComposeHost.previews.frame(notify = false)
            if (PolyComposeHost.previews.appliedChange || providerContent) previewRevision.intValue++
        }
    }

    private fun layout(hud: Hud, screenWidth: Float, screenHeight: Float, scale: Float): RootNode {
        val hudScale = hud.frameEffectiveScale
        val rt = hud.runtime
        val changed = rt.layoutIfChanged(screenWidth / scale / hudScale, screenHeight / scale / hudScale)
        if (changed) relaidOut = true
        val root = rt.root
        if (changed || hudScale != hud.sizedScale || hud.stateRevision != hud.sizedRevision || hud.hasExternalGeometry) {
            val w = root.width * hudScale
            val h = root.height * hudScale
            if (hud.renderedW != w || hud.renderedH != h) {
                contentDirty = true
                Snapshot.withMutableSnapshot {
                    hud.renderedW = w
                    hud.renderedH = h
                }
            }
            hud.sizedW = w
            hud.sizedH = h
            hud.sizedScale = hudScale
            hud.sizedRevision = hud.stateRevision
        }
        hud.settlePin()
        return root
    }

    @ApiStatus.Internal
    @InternalOneConfigUiApi
    public fun updateIfDue(hud: Hud) {
        val frequency = hud.updateFrequency()
        if (frequency >= 0L) {
            val now = System.nanoTime()
            val last = hud.lastUpdate
            if (last != Long.MIN_VALUE && now - last < frequency) return
            hud.lastUpdate = now
        }
        try {
            hud.update()
            hud.updateFailing = false
        } catch (e: Throwable) {
            // HUDs update every frame by default, so only log until the next successful update
            if (!hud.updateFailing) LOGGER.error("Failed to update HUD ${hud.title}", e)
            hud.updateFailing = true
        }
    }

    private fun layoutOnce(hud: Hud, screenWidth: Float, screenHeight: Float, scale: Float): RootNode {
        if (hud.lastLayoutFrame == frameId) return hud.runtime.root
        hud.lastLayoutFrame = frameId
        return layout(hud, screenWidth, screenHeight, scale)
    }

    private fun shouldDraw(hud: Hud): Boolean {
        if (!isEditing && (hud.frameHidden || isSuppressed(hud))) return false
        return isShown(hud)
    }

    private fun keepsBackgroundOnly(hud: Hud): Boolean =
        !isEditing && hud.frameHidden && !isSuppressed(hud) && hud.keepsHiddenBackground && isShown(hud)

    private class Suppression(owner: Hud, val target: String, val reason: String) : AutoCloseable {
        private val ownerRef = WeakReference(owner)
        val owner: Hud? get() = ownerRef.get()

        override fun close() {
            if (suppressions.remove(this)) invalidate()
        }
    }

    private val suppressions = CopyOnWriteArrayList<Suppression>()

    @Volatile private var suppressed: Map<Hud, List<String>> = emptyMap()

    internal fun addSuppression(owner: Hud, otherHudId: String, reason: String): AutoCloseable {
        val suppression = Suppression(owner, normaliseHudId(otherHudId), reason)
        suppressions.add(suppression)
        invalidate()
        return suppression
    }

    private fun normaliseHudId(id: String): String = id.trim().removeSuffix(".json").removePrefix("huds/")

    private fun hasHudId(hud: Hud, id: String): Boolean =
        normaliseHudId(hud.id) == id || hud.tree?.id?.let(::normaliseHudId) == id ||
            hud.configId?.let(::normaliseHudId) == id

    private fun isActive(hud: Hud): Boolean = activeInstances.any { it === hud }

    private val frameClaims = ArrayList<Suppression>()

    private val notShowing: MutableSet<Hud> = Collections.newSetFromMap(IdentityHashMap())

    private fun isOwnerShown(hud: Hud): Boolean = !hud.frameHidden && isShown(hud) && hud !in notShowing

    private fun refreshSuppressed() {
        if (suppressions.isEmpty()) {
            var any = false
            val instances = activeInstances
            for (i in instances.indices) {
                if (instances[i].overridesReplacesIds) {
                    any = true
                    break
                }
            }
            if (!any) {
                if (suppressed.isNotEmpty()) {
                    suppressed = emptyMap()
                    invalidate()
                }
                return
            }
        }
        val claims = frameClaims
        claims.clear()
        suppressions.removeIf { it.owner == null }
        for (s in suppressions) {
            val owner = s.owner ?: continue
            if (!isActive(owner) || !isOwnerShown(owner)) continue
            claims.add(s)
        }
        for (hud in activeInstances) {
            if (!hud.overridesReplacesIds || hud.hidden || !isOwnerShown(hud)) continue
            val ids = try {
                hud.replacesIds
            } catch (e: Throwable) {
                if (e.isFatalHudFailure()) throw e
                LOGGER.error("Failed to read the HUDs that ${hud.title} replaces", e)
                continue
            }
            for (id in ids) claims.add(Suppression(hud, normaliseHudId(id), hud.title))
        }
        val next: Map<Hud, List<String>> = if (claims.isEmpty()) {
            emptyMap()
        } else {
            val incoming = IdentityHashMap<Hud, MutableList<Suppression>>()
            for (hud in activeInstances) {
                for (claim in claims) {
                    if (claim.owner !== hud && hasHudId(hud, claim.target)) {
                        incoming.getOrPut(hud) { ArrayList(1) }.add(claim)
                    }
                }
            }
            claims.clear()
            val effective = IdentityHashMap<Hud, Boolean>()
            val resolving = Collections.newSetFromMap(IdentityHashMap<Hud, Boolean>())
            fun isEffective(hud: Hud): Boolean {
                effective[hud]?.let { return it }
                if (!resolving.add(hud)) return true
                val shown = incoming[hud]?.none { claim -> claim.owner?.let(::isEffective) == true } != false
                resolving.remove(hud)
                effective[hud] = shown
                return shown
            }
            val map = IdentityHashMap<Hud, MutableList<String>>()
            for (hud in activeInstances) {
                if (isEffective(hud)) continue
                val reasons = ArrayList<String>(1)
                for (claim in incoming[hud] ?: continue) {
                    if (claim.owner?.let(::isEffective) == true) reasons.add(claim.reason)
                }
                map[hud] = reasons
            }
            map
        }
        if (!sameSuppressions(next, suppressed)) {
            suppressed = next
            invalidate()
        }
    }

    private fun sameSuppressions(a: Map<Hud, List<String>>, b: Map<Hud, List<String>>): Boolean {
        if (a.size != b.size) return false
        for ((hud, reasons) in a) {
            if (b[hud] != reasons) return false
        }
        return true
    }

    private fun dropSuppressionsOf(hud: Hud) {
        if (suppressions.removeIf { it.owner === hud }) invalidate()
    }

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun isSuppressed(hud: Hud): Boolean = suppressed.containsKey(hud)

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun suppressionReasons(hud: Hud): List<String> = suppressed[hud]?.toList() ?: emptyList()

    @Volatile private var previewSessionId = 0

    internal val previewSession: Int get() = previewSessionId

    /** Everything [shouldDraw] checks apart from the HUD's own hidden flag */
    private fun isShown(hud: Hud): Boolean {
        if (isEditing) return true
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

    private fun liveRedraw(hud: Hud): Boolean = !hud.redrawOverridden && lastDraw(hud) != null

    private fun lastDraw(hud: Hud): HudDraw? = if (hud.lastDrawRender == renderId - 1) hud.lastDraw else null

    private fun stored(hud: Hud, draw: HudDraw) {
        hud.lastDraw = draw
        hud.lastDrawRender = renderId
        drawnHuds.add(hud)
    }

    private var liveChroma = false

    private fun collectFrameOrder(): Boolean {
        frameOrder.clear()
        layoutOrder.clear()
        frameDrawn.clear()
        var volatileContent = false
        val instances = activeInstances
        var drawnKey = instances.size.toLong()
        for (i in instances.indices) {
            val hud = instances[i]
            if (hud is LegacyHudMarker) continue
            val visible = shouldDraw(hud)
            val backgroundOnly = !visible && keepsBackgroundOnly(hud)
            if (visible || backgroundOnly) {
                updateIfDue(hud)
                val show = isEditing || try {
                    hud.shouldShow()
                } catch (e: Throwable) {
                    LOGGER.error("Failed to check whether HUD ${hud.title} should show", e)
                    true
                }
                if (!isEditing) {
                    if (show) notShowing.remove(hud) else notShowing.add(hud)
                }
                if (!show) {
                    hud.setVisible(false)
                    continue
                }
                frameDrawn[hud] = visible
                drawnKey = drawnKey * 31L + System.identityHashCode(hud) * 2L + (if (visible) 1L else 0L)
            }
            hud.setVisible(visible)
        }
        val writes = hudStateWrites.get()
        val guiScale = prepareScale
        geometrySkip = geometryStable && !isEditing && writes == geometryWrites && drawnKey == geometryDrawn &&
            guiScreenWidth == geometryW && guiScreenHeight == geometryH && guiScale == geometryScale && polledUnchanged()
        geometryWrites = writes
        geometryDrawn = drawnKey
        geometryW = guiScreenWidth
        geometryH = guiScreenHeight
        geometryScale = guiScale
        if (geometrySkip) zOrderFrame = frameId
        val ordered = if (geometrySkip) zOrderCache else orderedForRender()
        val backgroundOnly = backgroundOrder
        backgroundOnly.clear()
        for (i in ordered.indices) {
            val hud = ordered[i]
            val visible = frameDrawn[hud] ?: continue
            if (visible) {
                frameOrder.add(hud)
                if (hud.frameAlwaysRedraw && !(liveChroma && !isEditing && liveRedraw(hud))) volatileContent = true
            } else {
                backgroundOnly.add(hud)
                if (hud.bgChroma) volatileContent = true
            }
        }
        for (i in frameOrder.indices) layoutOrder.add(frameOrder[i])
        for (i in backgroundOnly.indices) layoutOrder.add(backgroundOnly[i])
        backgroundOnly.clear()
        return volatileContent
    }

    private inline fun prepareFrame(
        screenWidth: Float,
        screenHeight: Float,
        selectHuds: () -> List<Hud>,
        afterPreparation: Runnable? = null,
    ): Float {
        drainProfileReload()
        drainUnsupported()
        noteScreenSize()
        if (init) HudGroups.applyPendingDefaults()
        migratePositions(screenWidth, screenHeight)
        val scale = Platform.compatibility().options().guiScale
        prepareScale = scale

        frameId++

        // opening or closing the editor swaps between preview and live content, so every HUD gets
        // one immediate update on the edge instead of waiting out its remaining interval
        if (wasEditing != isEditing) {
            wasEditing = isEditing
            previewSessionId++
            for (hud in activeInstances) hud.lastUpdate = Long.MIN_VALUE
        }
        refreshSuppressed()

        geometrySkip = false
        val huds = selectHuds()
        updateAndAdvance()
        relaidOut = false
        layoutAll(huds, screenWidth, screenHeight, scale)
        if (relaidOut) {
            contentDirty = true
            if (showingPreviews) renderRevision.intValue++
        }
        updateBackgroundGroups(huds, screenWidth, screenHeight, scale)

        // Atlas rendering can invalidate HUD content after composition has reconciled icon handles.
        // Run it before consuming contentDirty so those changes are drawn in this frame.
        afterPreparation?.run()
        return scale
    }

    @ApiStatus.Internal
    @InternalOneConfigUiApi
    @JvmOverloads
    public fun beginFrame(screenWidth: Float, screenHeight: Float, beforeDirtyCheck: Runnable? = null, alwaysRender: Boolean = false, liveChroma: Boolean = false): Boolean {
        var volatileContent = false
        this.liveChroma = liveChroma
        val scale = prepareFrame(screenWidth, screenHeight, {
            volatileContent = collectFrameOrder()
            layoutOrder
        }, beforeDirtyCheck)
        if (liveChroma && anyChromaGroup()) volatileContent = true

        val key = frameKey()
        val keyChanged = key != lastFrameKey ||
            screenWidth != lastFrameWidth || screenHeight != lastFrameHeight || scale != lastFrameScale
        lastFrameKey = key
        lastFrameWidth = screenWidth
        lastFrameHeight = screenHeight
        lastFrameScale = scale

        val neverCache = redrawCacheDisabled || volatileContent

        val dirty = contentDirty || keyChanged || neverCache
        contentDirty = false
        preparedFrameValid = dirty || alwaysRender
        return dirty
    }

    private fun anyChromaGroup(): Boolean {
        val groups = frameGroups
        for (i in groups.indices) if (groups[i].chroma) return true
        return false
    }

    private fun layoutAll(huds: List<Hud>, screenWidth: Float, screenHeight: Float, scale: Float) {
        for (i in huds.indices) {
            val hud = huds[i]
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
        val writes = hudStateWrites.get()
        val skip = geometrySkip && lastMergeKey != null && writes == mergeWrites
        mergeWrites = writes
        if (skip) return
        var stable = lastMergeKey != null && huds.size == mergeCheckHuds.size
        if (mergeCheckHuds.size != huds.size) {
            mergeCheckHuds = arrayOfNulls(huds.size)
            mergeCheckStamps = IntArray(huds.size)
        }
        for (i in huds.indices) {
            val hud = huds[i]
            val stamp = hud.geometryStamp
            if (stamp == -1 || mergeCheckHuds[i] !== hud || mergeCheckStamps[i] != stamp) stable = false
            mergeCheckHuds[i] = hud
            mergeCheckStamps[i] = stamp
        }
        if (stable) return
        val key = HudBackgroundMerge.layoutKey(huds)
        if (key == lastMergeKey) return
        lastMergeKey = key
        // the fused outline can change without anything else asking for a redraw
        // for example when a hidden HUD which keeps its background resizes
        // so the cached frame is no longer good
        contentDirty = true

        val mergeable = if (mergeExclusions.isEmpty()) huds else huds.filter { hud -> !isMergeExcluded(hud) }
        val refW = if (layoutRefWidth > 0f) layoutRefWidth else screenWidth
        val refH = if (layoutRefHeight > 0f) layoutRefHeight else screenHeight
        frameGroups = HudBackgroundMerge.computeGroups(mergeable, refW, refH)
        if (releaseStaleMergeLinks()) frameGroups = HudBackgroundMerge.computeGroups(mergeable, refW, refH)
        val merged = Collections.newSetFromMap(IdentityHashMap<Hud, Boolean>())
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
        contentDirty = true
    }

    private var mergeExclusions: List<Hud> = emptyList()
    internal var prepareScale = Float.NaN
        private set
    private var geometrySkip = false
    private var geometryStable = false
    private var geometryWrites = -1
    private var geometryDrawn = 0L
    private var geometryW = Float.NaN
    private var geometryH = Float.NaN
    private var geometryScale = Float.NaN
    private val polled = ArrayList<Hud>()
    private var polledStamps = IntArray(0)
    private var mergeWrites = -1
    private var mergeCheckHuds = arrayOfNulls<Hud>(0)
    private var mergeCheckStamps = IntArray(0)

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
    public fun setMergeExclusions(huds: Collection<Hud>) {
        val next = huds.toList()
        if (next.size == mergeExclusions.size && next.indices.all { next[it] === mergeExclusions[it] }) return
        mergeExclusions = next
        invalidate()
        // unlink right away so moving an excluded HUD never drags its neighbours with it
        Snapshot.withMutableSnapshot {
            for (hud in activeInstances) {
                val excluded = isMergeExcluded(hud)
                hud.clearMergeLinks(
                    clearX = excluded || hud.mergeLinkX?.parent?.let(::isMergeExcluded) == true,
                    clearY = excluded || hud.mergeLinkY?.parent?.let(::isMergeExcluded) == true,
                )
            }
        }
        lastMergeKey = null
        invalidate()
    }

    private fun isMergeExcluded(hud: Hud): Boolean = mergeExclusions.any { it === hud }

    /** Every HUD fused into the same background shape as [hud] including [hud] itself */
    @ApiStatus.Internal
    @JvmStatic
    public fun mergeGroupOf(hud: Hud): List<Hud> =
        frameGroups.firstOrNull { group -> group.huds.any { it === hud } }?.huds ?: listOf(hud)

    @ApiStatus.Internal
    @JvmStatic
    public fun isMergeCandidate(hud: Hud): Boolean = HudBackgroundMerge.canMerge(hud)

    @ApiStatus.Internal
    @JvmStatic
    public fun backgroundStylesMatch(a: Hud, b: Hud): Boolean = HudBackgroundMerge.canFuse(a, b)

    /**
     * Holds every HUD in a fused shape against the neighbour it touches at the point where the two meet
     * so a HUD which grows keeps the shape flush
     *
     * These links live only as long as the merge does
     *
     * A HUD which stops being merged is let go and stays where it was left
     */
    private fun updateMergeLinks() {
        Snapshot.withMutableSnapshot {
            for (group in frameGroups) {
                for (link in group.links) {
                    link.child.applyMergeLink(link.parent, link.childPoint, link.parentPoint, link.axis)
                }
            }
            releaseStaleMergeLinks()
            for (hud in activeInstances) hud.keepMergedPosition()
        }
    }

    private fun releaseStaleMergeLinks(): Boolean {
        val linkedX = Collections.newSetFromMap(IdentityHashMap<Hud, Boolean>())
        val linkedY = Collections.newSetFromMap(IdentityHashMap<Hud, Boolean>())
        for (group in frameGroups) {
            for (link in group.links) (if (link.axis == MergeAxis.X) linkedX else linkedY).add(link.child)
        }
        var released = false
        Snapshot.withMutableSnapshot {
            for (hud in activeInstances) {
                val clearX = hud !in linkedX && hud.mergeLinkX != null
                val clearY = hud !in linkedY && hud.mergeLinkY != null
                if (!clearX && !clearY) continue
                hud.releaseMergeLinks(clearX, clearY)
                released = true
            }
        }
        return released
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
    @InternalOneConfigUiApi
    public fun drawMergedBackgrounds(ctx: RenderContext) {
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
        // one HUD hiding while another appears must not leave the cached frame in place
        val order = layoutOrder
        for (i in order.indices) key = key * 31L + System.identityHashCode(order[i])
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
        // legacy and compat HUDs draw through their own renderers, and reading their size or hidden
        // flag can mean measuring text or going through reflection, so keep them out of this pass
        geometryStable = false
        val list = renderable
        list.clear()
        for (hud in activeInstances) if (hud !is LegacyHudMarker) list.add(hud)
        val n = list.size
        if (n <= 1) return list
        if (zOrderHuds.size < n) {
            zOrderHuds = arrayOfNulls(n)
            zOrderBounds = FloatArray(n * 4)
            zOrderDrawn = BooleanArray(n)
            zOrderStamps = IntArray(n)
        }
        var same = zOrderCache.size == n
        val b = boundsScratch
        for (i in 0 until n) {
            val hud = list[i]
            val drawn = hud in frameDrawn
            val stamp = if (drawn) hud.geometryStamp else 0
            if (same && zOrderHuds[i] === hud && zOrderDrawn[i] == drawn && stamp != -1 && zOrderStamps[i] == stamp) continue
            zOrderDrawn[i] = drawn
            zOrderStamps[i] = stamp
            val bounded = drawn && fillBounds(hud, b, 0)
            val x = if (bounded) b[0] else Float.NaN
            val y = if (bounded) b[1] else Float.NaN
            val w = if (bounded) b[2] else Float.NaN
            val h = if (bounded) b[3] else Float.NaN
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
        var stable = true
        polled.clear()
        for (i in 0 until n) {
            if (!zOrderDrawn[i]) continue
            val hud = list[i]
            if (hud.geometryStateOnly) continue
            if (hud.geometryPollable) polled.add(hud) else stable = false
        }
        if (polledStamps.size < polled.size) polledStamps = IntArray(polled.size)
        for (i in polled.indices) polledStamps[i] = polled[i].geometryStamp
        geometryStable = stable
        zOrderFrame = frameId
        if (same) return zOrderCache
        contentDirty = true
        zOrderCache = zOrderedByBounds(list, true)
        return zOrderCache
    }

    private fun polledUnchanged(): Boolean {
        for (i in polled.indices) {
            val stamp = polled[i].geometryStamp
            if (stamp == -1 || stamp != polledStamps[i]) return false
        }
        return true
    }

    private fun Float.sameBound(other: Float): Boolean =
        this == other || (this.isNaN() && other.isNaN())

    @ApiStatus.Internal
    @InternalOneConfigUiApi
    public fun prepare(screenWidth: Float, screenHeight: Float) {
        prepareFrame(screenWidth, screenHeight, {
            prepareOrder.clear()
            val instances = activeInstances
            for (i in instances.indices) {
                val hud = instances[i]
                val visible = hud !is LegacyHudMarker
                hud.setVisible(visible)
                if (visible) prepareOrder.add(hud)
            }
            for (i in prepareOrder.indices) updateIfDue(prepareOrder[i])
            prepareOrder
        })
    }

    @ApiStatus.Internal
    @InternalOneConfigUiApi
    @JvmOverloads
    public fun render(ctx: RenderContext, screenWidth: Float, screenHeight: Float, volatileCtx: RenderContext? = null) {
        val prepared = preparedFrameValid
        preparedFrameValid = false
        val scale = if (prepared) {
            Platform.compatibility().options().guiScale
        } else {
            prepareFrame(screenWidth, screenHeight, {
                collectFrameOrder()
                layoutOrder
            })
        }

        ctx.save()
        ctx.scale(scale, scale)

        val cr = ctx.renderer as? CapturingRenderer
        val cacheNodes = ctx.cacheNodes
        ctx.cacheNodes = cr != null
        val previous = drawnHuds
        drawnHuds = nextDrawnHuds
        drawnHuds.clear()
        val framePositions = !isEditing
        if (framePositions) {
            if (framePos.size < frameOrder.size * 3) {
                framePos = FloatArray(frameOrder.size * 3)
                frameRedraw = BooleanArray(frameOrder.size)
                frameVolatile = BooleanArray(frameOrder.size)
            }
            for (i in frameOrder.indices) {
                val hud = frameOrder[i]
                framePos[i * 3] = hud.frameX
                framePos[i * 3 + 1] = hud.frameY
                framePos[i * 3 + 2] = hud.frameEffectiveScale
                frameRedraw[i] = hud.frameAlwaysRedraw
            }
        }
        val split = volatileCtx != null && cr != null && volatileCtx.renderer is CapturingRenderer && framePositions && splitLayers()
        if (split) {
            volatileCtx.save()
            volatileCtx.scale(scale, scale)
        }
        val volatileCacheNodes = volatileCtx?.cacheNodes ?: false
        volatileCtx?.cacheNodes = split
        val splitCtx = if (split) volatileCtx else null
        if (framePositions) {
            mergedCtx = ctx
            mergedCr = cr
            try {
                drawReadObserver.observeReads(this, onDrawStateChanged, mergedBackgroundsBlock)
            } finally {
                mergedCtx = null
                mergedCr = null
            }
            drawFrameHuds(ctx, cr, splitCtx, true, screenWidth, screenHeight, scale)
        } else {
            drawReadObserver.observeReads(this, onDrawStateChanged) {
                drawMergedBackgroundsCached(ctx, cr)
                drawFrameHuds(ctx, cr, splitCtx, false, screenWidth, screenHeight, scale)
            }
        }
        for (i in previous.indices) {
            val hud = previous[i]
            if (hud.lastDrawRender != renderId) drawReadObserver.clear(hud)
        }
        nextDrawnHuds = previous
        renderId++

        ctx.cacheNodes = cacheNodes
        ctx.restore()
        volatileCtx?.cacheNodes = volatileCacheNodes
        if (split) volatileCtx.restore()
    }

    private var mergedCtx: RenderContext? = null
    private var mergedCr: CapturingRenderer? = null

    private val mergedBackgroundsBlock: () -> Unit = {
        val ctx = mergedCtx
        if (ctx != null) drawMergedBackgroundsCached(ctx, mergedCr)
    }

    private fun drawMergedBackgroundsCached(ctx: RenderContext, cr: CapturingRenderer?) {
        if (cr == null || anyChromaGroup()) {
            drawMergedBackgrounds(ctx)
        } else {
            val cached = mergedDraw
            if (cached != null && cached.first === frameGroups && cr.sameState(cached.second) && cr.valid(cached.third)) {
                cr.append(cached.third)
            } else {
                val key = cr.stateKey()
                val mark = cr.mark()
                drawMergedBackgrounds(ctx)
                mergedDraw = Triple(frameGroups, key, cr.since(mark))
            }
        }
    }

    private fun drawFrameHuds(
        ctx: RenderContext,
        cr: CapturingRenderer?,
        splitCtx: RenderContext?,
        framePositions: Boolean,
        screenWidth: Float,
        screenHeight: Float,
        scale: Float,
    ) {
        val splitCr = splitCtx?.renderer as? CapturingRenderer
        try {
            for (i in frameOrder.indices) {
                val hud = frameOrder[i]
                val hudCtx: RenderContext
                val hudCr: CapturingRenderer?
                if (splitCtx != null && frameVolatile[i]) {
                    hudCtx = splitCtx
                    hudCr = splitCr
                } else {
                    hudCtx = ctx
                    hudCr = cr
                }
                val hudScale = if (framePositions) framePos[i * 3 + 2] else hud.effectiveScale
                val clickPass = HudClickInput.passStart(hud)
                val root = try {
                    layoutOnce(hud, screenWidth, screenHeight, scale)
                } catch (e: Throwable) {
                    LOGGER.error("Failed to lay out HUD ${hud.title}", e)
                    continue
                }
                hudCtx.save()
                if (framePositions) hudCtx.translate(framePos[i * 3], framePos[i * 3 + 1]) else hudCtx.translate(hud.x, hud.y)
                if (hudScale != 1f) hudCtx.scale(hudScale, hudScale)
                val rt = hud.runtimeOrNull
                val redraw = if (framePositions) frameRedraw[i] else hud.alwaysRedraw
                if (hudCr == null || rt == null || redraw && hud.redrawOverridden) {
                    HudClickInput.recorded(hud, clickPass)
                    drawHud(hud, root, hudCtx)
                } else {
                    val cached = lastDraw(hud)
                    if (cached != null && cached.runtime === rt && cached.revision == rt.revision && cached.reads == hud.drawReads &&
                        cached.epoch == drawEpoch && cached.editing == isEditing && hudCr.sameState(cached.key) && hudCr.valid(cached.captured)
                    ) {
                        hudCr.append(cached.captured)
                        stored(hud, cached)
                        if (cached.volatile && ++cached.hits > VOLATILE_HITS) cached.volatile = false
                    } else {
                        val key = hudCr.stateKey()
                        val reads = hud.drawReads
                        val chroma = PolyColor.chromaReads
                        val mark = hudCr.mark()
                        HudClickInput.recorded(hud, clickPass)
                        drawReadObserver.observeReads(hud, onHudDrawStateChanged) { drawHud(hud, root, hudCtx) }
                        val captured = hudCr.since(mark)
                        val volatile = cached != null && (cached.volatile || cached.missFrame == frameId - 1)
                        if (!redraw || PolyColor.chromaReads == chroma) stored(hud, HudDraw(rt, rt.revision, reads, drawEpoch, isEditing, key, captured, frameId, volatile))
                    }
                }
                hudCtx.restore()
            }
        } finally {
            HudClickInput.passEnd()
        }
    }

    private fun splitLayers(): Boolean {
        var any = false
        for (i in frameOrder.indices) {
            val hud = frameOrder[i]
            val v = frameRedraw[i] && !liveRedraw(hud) || lastDraw(hud)?.volatile == true
            frameVolatile[i] = v
            if (v) any = true
        }
        if (!any) return false
        if (frameSize.size < frameOrder.size * 2) frameSize = FloatArray(frameOrder.size * 2)
        for (i in frameOrder.indices) {
            val hud = frameOrder[i]
            val external = hud.hasExternalGeometry
            frameSize[i * 2] = if (external) hud.renderedW else hud.sizedW
            frameSize[i * 2 + 1] = if (external) hud.renderedH else hud.sizedH
        }
        for (i in frameOrder.indices) {
            if (!frameVolatile[i]) continue
            for (j in i + 1 until frameOrder.size) {
                if (frameVolatile[j]) continue
                if (framePos[i * 3] < framePos[j * 3] + frameSize[j * 2] && framePos[j * 3] < framePos[i * 3] + frameSize[i * 2] &&
                    framePos[i * 3 + 1] < framePos[j * 3 + 1] + frameSize[j * 2 + 1] && framePos[j * 3 + 1] < framePos[i * 3 + 1] + frameSize[i * 2 + 1]
                ) return false
            }
        }
        return true
    }

    private fun drawHud(hud: Hud, root: RootNode, ctx: RenderContext) {
        if (isEditing && (hud.hidden || isSuppressed(hud))) {
            ctx.saveLayerAlpha(HIDDEN_HUD_ALPHA)
            root.render(ctx)
            ctx.restore()
        } else {
            root.render(ctx)
        }
    }

    @ApiStatus.Internal
    public fun toggleEditor() {
        if (isEditorOpen) closeEditor() else openEditor()
    }

    @ApiStatus.Internal
    public fun openEditor() {
        // always re-assert editing and post OPEN even when isEditorOpen is already true because
        // the flag can get stuck and an early return would make the Edit HUD button do nothing
        isEditorOpen = true
        EventManager.INSTANCE.post(HudEditorToggleEvent.OPEN)
    }

    @ApiStatus.Internal
    public fun closeEditor() {
        if (!isEditorOpen) return
        isEditorOpen = false
        EventManager.INSTANCE.post(HudEditorToggleEvent.CLOSE)
    }

    /**
     * The editor screen has been installed, which also happens when Minecraft removes it and
     * replaces it with itself, clearing the flag while the editor stays open
     */
    @ApiStatus.Internal
    public fun onEditorScreenAdded() {
        if (!isEditorOpen) openEditor()
    }

    @ApiStatus.Internal
    public fun onEditorScreenRemoved() {
        val wasOpen = isEditorOpen
        isEditorOpen = false
        // a drag interrupted by the editor closing must not leave a HUD unable to merge
        setMergeExclusions(emptyList())

        for (hud in activeInstances) {
            hud.unpinCorner()
            hud.settlePin()
        }

        if (wasOpen) EventManager.INSTANCE.post(HudEditorToggleEvent.SCREEN_REMOVED)
    }

    @ApiStatus.Internal
    public fun initialize() {
        if (init) throw IllegalStateException("HudManager.initialize() called twice!")
        ConfigManager.active()
        runPendingRegistrations()
        init = true
        ConfigManager.addProfileChangeListener(profileChangeListener)
        LOGGER.info("Initializing HUD...")
        loadFromActiveProfile()
    }

    @ApiStatus.Internal
    public fun setProfileReloadDispatcher(dispatcher: Consumer<Runnable>) {
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
            instancesRevision.intValue++
            try {
                disposeHud(hud, delete = false)
            } catch (failure: Throwable) {
                if (failure.isFatalHudFailure()) throw failure
                LOGGER.error("Failed to dispose HUD ${hud.title} while switching profiles", failure)
            }
        }
        knownProviders.clear()
        knownGroups.clear()
        registryTree = null
        zOrderCache = emptyList()
        zOrderFrame = Long.MIN_VALUE
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

    private fun drainUnsupported() {
        if (pendingUnsupported.isEmpty()) return
        val classes = pendingUnsupported.toList()
        pendingUnsupported.removeAll(classes.toSet())
        for (cls in classes) {
            if (cls in hudProviders || cls.name !in unsupportedHuds) continue
            for (hud in activeInstances.filter { it.javaClass == cls }) {
                LOGGER.info("Removing loaded HUD ${hud.title} because it is not supported here")
                removeHud(hud, delete = false)
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

    private fun migratePositions(screenWidth: Float, screenHeight: Float) {
        if (screenWidth <= 0f || screenHeight <= 0f) return
        val gridW = screenWidth / GRID_SIZE
        val gridH = screenHeight / GRID_SIZE
        var migrated = false
        for (hud in activeInstances) {
            if (hud.posSchema >= POS_SCHEMA) continue
            hud.relativeX *= gridW
            hud.relativeY *= gridH
            hud.posSchema = POS_SCHEMA
            migrated = true
        }
        if (!migrated) return
        layoutRefWidth = screenWidth
        layoutRefHeight = screenHeight
        invalidate()
    }

    private fun loadFromActiveProfile(prefetched: Collection<Tree>? = null) {
        val now = System.nanoTime()
        loadingLayout = true
        layoutRefWidth = 0f
        layoutRefHeight = 0f
        try {
            loadHuds(now, prefetched)
        } finally {
            loadingLayout = false
        }
    }

    private fun migrateLoaded(hud: Hud, saved: Tree) {
        try {
            if (hud.migrateLoaded(saved)) hud.tree?.let { ConfigManager.active().save(it) }
        } catch (e: Throwable) {
            if (e.isFatalHudFailure()) throw e
            LOGGER.error("Failed to migrate saved data for HUD ${hud.title}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadHuds(now: Long, prefetched: Collection<Tree>?) {
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
            if (activeInstances.remove(candidate)) instancesRevision.intValue++
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
                // mods (e.g. VanillaHUD) pre-seed settings without hudClass; the default HUD of that ID adopts them below
                val clsName = data.getProp("hudClass")?.get() as? String ?: return@forEach
                if (clsName.endsWith(".OneConfigHudCompat")) return@forEach
                if (clsName in unsupportedHuds) return@forEach
                val cls = Class.forName(clsName, true, loader) as? Class<Hud>
                    ?: throw IllegalArgumentException("$clsName is not a subclass of Hud")
                providerClass = cls
                val h = hudProviders[cls] ?: MHUtils.instantiate(cls, true).getOrThrow()
                // A previous HUD instance may still own this ID when the same backend is reloaded.
                // Drop only the in-memory binding; make() will load the unchanged file into the new HUD.
                ConfigManager.active().unregister(data.id)
                val savedSchema = data.getProp("posSchema")?.getAs<Number?>()?.toInt() ?: 0
                var made: Hud? = null
                val failures = Tree.collectFailures { made = h.make(data) }
                val hud = made ?: throw IllegalStateException("HUD from ${data.id} was not created")
                candidate = hud
                if (failures.isNotEmpty()) LOGGER.warn("Reset {} unreadable value(s) of HUD {} to defaults: {}", failures.size, hud.title, failures)
                val sec = data.getProp("section")?.getAs<Section?>()
                if (sec != null) {
                    hud.section = sec
                    val rx = data.getProp("relativeX")?.getAs<Number?>()?.toFloat() ?: 0f
                    val ry = data.getProp("relativeY")?.getAs<Number?>()?.toFloat() ?: 0f
                    hud.relativeX = rx
                    hud.relativeY = ry
                    hud.posSchema = savedSchema
                } else {
                    val absX = data.getProp("x")?.getAs<Number?>()?.toFloat() ?: 0f
                    val absY = data.getProp("y")?.getAs<Number?>()?.toFloat() ?: 0f
                    hud.setAbsolutePosition(absX, absY)
                }
                migrateLoaded(hud, data)
                activeInstances.add(hud)
                instancesRevision.intValue++
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
                val (dx, dy) = h.defaultPosition() ?: (10f to 10f)
                val hud = h.make()
                candidate = hud
                hud.setAbsolutePosition(dx, dy)
                activeInstances.add(hud)
                instancesRevision.intValue++
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

        try {
            HudGroups.applyDefaults()
        } catch (e: Throwable) {
            if (e.isFatalHudFailure()) throw e
            LOGGER.error("Failed to apply default HUD groups", e)
        }

        LOGGER.info("HUD load took {}ms, loaded {} HUDs from {} providers ({} registered)",
            (System.nanoTime() - now) / 1_000_000.0, i, used.size, hudProviders.size)
    }
}
