package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.Event
import org.polyfrost.oneconfig.api.event.v1.events.FramebufferRenderEvent
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.polyfrost.oneconfig.internal.ui.compose.opengl.resyncTextureBindCache
import java.net.URI
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

object CompatLoader {
    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/Compat")

    private val forcedModId = ThreadLocal<String?>()

    private var bypassDelay = false

    private val pathFactory: MutableMap<ModInfo, (String) -> String> = mutableMapOf()

    val nativeLoadedConfigs = mutableListOf<String>()

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
        "net.azureaaron.dandelion",
        "com.odtheking.odin",
        "de.hysky.skyblocker",
        "co.stellarskys.stella",
        "uk.co.hexeption.apec",
        "moe.nea.firmament.deps.moulconfig",
        "io.github.notenoughupdates.moulconfig",
        "dev.tr7zw.trender",
        "dev.tr7zw.transition",
        "kotlin",
        "kotlinx",
        "androidx.compose",
        "org.jetbrains.compose",
        "org.jetbrains.skia",
        "org.jetbrains.skiko",
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

    fun hasMod(id: String): Boolean = ModInfo.loadedMods.any { it.id == id }

    private const val MOD_MENU_CLASS = "com.terraformersmc.modmenu.ModMenu"

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
        if (!SkiaCtx.isVulkanMode) runCatching { resyncTextureBindCache() }
        runCatching { warmup() }.onFailure { LOGGER.warn("Config screen warmup failed", it) }
    }

    private val list: MutableList<Pair<Int, () -> Unit>> = mutableListOf()

    init {
        ModInfo.loadedMods.forEach { mod ->
            mod.file?.let {
                pathFactory[mod] = it.toUri().toString()::plus
            }
        }

        register<ResourceFinishedLoading> {
            val pending = list.sortedBy { (key, _) -> key }
            list.clear()
            pending.forEach { (_, value) -> value() }
        }
    }

    fun requireTranslations(priority: Int = 0, skip: Boolean = false, init: () -> Unit) {
        if (!skip) markFirstModAsSkip()
        if (bypassDelay) {
            init()
            return
        }
        list.add(priority to init)
    }

    private inline fun <reified T> register(noinline runnable: () -> Unit) where T : Event {
        EventManager.register(T::class.java) { _ ->
            bypassDelay = true
            runnable.invoke()
        }
    }
}