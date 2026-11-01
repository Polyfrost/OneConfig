package org.polyfrost.oneconfig.internal.compat

import java.net.URI
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.Event
import org.polyfrost.oneconfig.api.event.v1.events.FramebufferRenderEvent
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.GpuBackend
import org.polyfrost.oneconfig.internal.ui.compose.opengl.resyncTextureBindCache

object CompatLoader {
    private val LOGGER = LogManager.getLogger("OneConfig/Compat")

    private val forcedModId = ThreadLocal<String?>()

    private var bypassDelay = false

    private val pathFactory: MutableMap<ModInfo, (String) -> String> = mutableMapOf()

    // concurrent as compat hooks may fire on other threads while ConfigEntrypoints un-marks a failed mod
    val nativeLoadedConfigs: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // list of packages that contain known configs/ignored paths
    private val illegalPaths = listOf(
        "com.terraformersmc.modmenu",
        "com.teamresourceful.resourcefulconfig",
        "com.teamresourceful.resourcefulconfigkt",
        "dev.isxander.yacl3",
        "me.shedaniel",
        "org.polyfrost.oneconfig",
        "java.lang",
        "net.fabric",
        "net.ornithemc.osl",
        "net.azureaaron.dandelion",
        "com.odtheking.odin",
        "de.hysky.skyblocker",
        "co.stellarskys.stella",
        "moe.nea.firmament.deps.moulconfig",
        "io.github.notenoughupdates.moulconfig",
        "com.skysoft.deps.softconfig",
        "dev.tr7zw.trender",
        "net.uku3lig.ukulib",
        "io.github.axolotlclient.AxolotlClientConfig.",
        "dev.tr7zw.transition",
        "kotlin",
        "kotlinx",
        "androidx.compose",
        "org.jetbrains.compose",
        "org.polyfrost.compose",
        "org.polyfrost.polyui"
    )

    private val ownerByClassName = ConcurrentHashMap<String, Optional<ModInfo>>()

    fun findFirstMod(): ModInfo? {
        forcedModId.get()?.let { forcedId ->
            val forcedMod = Platform.compatibility().mods.firstOrNull { it.id == forcedId }
            if (forcedMod != null) return forcedMod
        }
        val callerClass = callerClassName() ?: return null
        return ownerByClassName
            .computeIfAbsent(callerClass) { Optional.ofNullable(resolveOwner(it)) }
            .orElse(null)
    }

    private fun callerClassName(): String? =
        Thread.currentThread().stackTrace.firstOrNull { element ->
            illegalPaths.none { path -> element.className.startsWith(path) }
        }?.className

    private fun resolveOwner(className: String): ModInfo? {
        val resourcePath = className.replace(".", "/") + ".class"
        pathFactory.entries.forEach { (mod, uri) ->
            runCatching {
                URI.create(uri(resourcePath)).toURL().openStream().use {} // throws if unable to open
                return mod
            }
        }
        return null
    }

    fun findModByClass(cls: Class<*>): ModInfo? = ownerByClassName
        .computeIfAbsent(cls.name) { Optional.ofNullable(resolveOwner(it)) }
        .orElse(null)

    fun markFirstModAsSkip() {
        val mod = findFirstMod()
        if (mod == null) return
        nativeLoadedConfigs.add(mod.id)
    }

    fun withForcedModId(modId: String?, block: () -> Unit) {
        if (modId == null) {
            block()
            return
        }
        val prev = forcedModId.get()
        forcedModId.set(modId)
        try {
            block()
        } finally {
            forcedModId.set(prev)
        }
    }

    /**
     * Whether an extraction for this mod should not run, as its config entrypoint replaces it (queueing [retry] in
     * case the entrypoint fails) or another compat already loaded its config
     *
     * Compat that handles [nativeLoadedConfigs] itself, such as refreshing an already loaded config or always
     * re-registering its own tree, calls [ConfigEntrypoints.skipExtraction] directly instead and keeps its own check
     */
    fun skipExtraction(modId: String?, key: Any, retry: Runnable): Boolean =
        ConfigEntrypoints.skipExtraction(modId, key, retry) || (modId != null && modId in nativeLoadedConfigs)

    fun hasMod(id: String): Boolean = ModInfo.loadedMods.any { it.id == id }

    private const val MOD_MENU_CLASS = "com.terraformersmc.modmenu.ModMenu"

    // the tree metadata holding the opener from originalScreenOpener, read by the "open original" button
    const val OPEN_ORIGINAL_SCREEN = "open_original_screen"

    fun originalScreenOpener(modId: String): Runnable? {
        val hasScreen = runCatching {
            Class.forName(MOD_MENU_CLASS)
                .getMethod("hasConfigScreen", String::class.java)
                .invoke(null, modId) as? Boolean
        }.getOrNull() ?: false
        if (!hasScreen) return null
        return Runnable {
            runCatching {
                val parent = Platform.screen().current<Any?>()
                val method = Class.forName(MOD_MENU_CLASS).methods.firstOrNull {
                    it.name == "getConfigScreen" && it.parameterCount == 2
                } ?: return@runCatching
                var screen: Any? = null
                withForcedModId(modId) {
                    screen = method.invoke(null, modId, parent)
                }
                screen?.let { Platform.screen().display(it) }
            }
        }
    }

    private val screenWarmups = ConcurrentLinkedDeque<() -> Unit>()
    private val screenWarmupScheduled = AtomicBoolean(false)

    fun queueScreenWarmup(block: () -> Unit) {
        screenWarmups.add(block)
        if (!screenWarmupScheduled.compareAndSet(false, true)) return
        EventManager.register(FramebufferRenderEvent.End::class.java) { _ -> runNextScreenWarmup() }
    }

    private fun runNextScreenWarmup() {
        val warmup = screenWarmups.poll() ?: return
        if (!GpuBackend.isVulkan) runCatching { resyncTextureBindCache() }
        runCatching { warmup() }.onFailure { LOGGER.warn("Config screen warmup failed", it) }
    }

    private val list: MutableList<Pair<Int, () -> Unit>> = mutableListOf()

    // guarded by list, as the two events may fire on different threads
    private var resourcesLoaded = false
    private var entrypointsDecided = false

    init {
        ModInfo.loadedMods.forEach { mod ->
            mod.file?.let {
                pathFactory[mod] = it.toUri().toString()::plus
            }
        }

        // filtered so a mod whose entrypoint already failed is never marked again
        nativeLoadedConfigs.addAll(ConfigEntrypoints.providerModIds().filter(ConfigEntrypoints::provides))
    }

    /**
     * Called by OneConfig whenever resources finish loading, rather than from a listener registered here, as this
     * object may only be created after the first load, which on 1.8.9 happens during init
     */
    fun onResourcesLoaded() {
        synchronized(list) { resourcesLoaded = true }
        runPending()
    }

    /**
     * Called once [ConfigEntrypoints] decided which mods keep their configs, which compat waits for as
     * Mod Menu would otherwise skip a mod whose entrypoint later fails
     */
    fun onEntrypointsDecided() {
        synchronized(list) { entrypointsDecided = true }
        runPending()
    }

    // resources usually finish loading after the entrypoints are decided, but on 1.8.9 they load during init
    // the compat itself runs outside the lock so it never holds it while calling into mods
    private fun runPending() {
        var first = false
        val pending = synchronized(list) {
            if (!resourcesLoaded || !entrypointsDecided) return
            first = !bypassDelay
            bypassDelay = true
            list.sortedBy { (key, _) -> key }.also { list.clear() }
        }
        // logged once so a startup where the compat never ran is easy to spot
        if (first) LOGGER.info("Running {} queued config compat(s)", pending.size)
        // one failing compat must not drop the rest, which were already taken off the list
        pending.forEach { (_, value) ->
            runCatching(value).onFailure { LOGGER.error("Failed to load a queued config compat", it) }
        }
    }

    fun requireTranslations(priority: Int = 0, skip: Boolean = false, init: () -> Unit) {
        if (!skip) markFirstModAsSkip()
        // checked under the lock so an init added while runPending swaps the list is not lost
        val queued = synchronized(list) {
            if (!bypassDelay) list.add(priority to init)
            !bypassDelay
        }
        if (!queued) init()
    }
}