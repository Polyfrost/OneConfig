package org.polyfrost.oneconfig.internal.compat

import dev.deftu.omnicore.api.loader.ModInfo
import dev.deftu.omnicore.api.loader.OmniLoader
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.Event
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import java.net.URI

object CompatLoader {
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
        "org.polyfrost.oneconfig",
        "java.lang",
        "net.fabric",
        "net.azureaaron.dandelion.deps.moulconfig",
        "net.azureaaron.dandelion_bp.deps.moulconfig",
        "net.azureaaron.dandelion_bp.impl.moulconfig",
        "moe.nea.firmament.deps.moulconfig",
        "kotlin"
    )

    fun findFirstMod(): ModInfo? {
        forcedModId.get()?.let { forcedId ->
            val forcedMod = OmniLoader.mods.firstOrNull { it.id == forcedId }
            if (forcedMod != null) return forcedMod
        }
        Thread.currentThread().stackTrace.firstOrNull {
            illegalPaths.none { path -> it.className.startsWith(path) }
        }?.let { element ->
            pathFactory.entries.forEach { (key, uri) ->
                val uri = uri(element.className.replace(".", "/") + ".class")
                runCatching {
                    URI.create(uri).toURL().openStream().use {} // throws if unable to open connection
                    return key
                }
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

    fun hasMod(id: String): Boolean = OmniLoader.mods.any { it.id == id }

    private val list: MutableList<Pair<Int, () -> Unit>> = mutableListOf()

    init {
        OmniLoader.mods.forEach { mod ->
            mod.file?.let {
                pathFactory[mod] = it.toUri().toString()::plus
            }
        }

        register<ResourceFinishedLoading> {
            list.sortedBy { (key, _) -> key }.forEach { (_, value) ->
                value()
            }
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