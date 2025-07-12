package org.polyfrost.oneconfig.internal.compat

import dev.deftu.omnicore.common.OmniLoader
import kotlinx.coroutines.NonCancellable.key
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.Event
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.internal.ui.OneConfigUI
import java.net.URI

object CompatLoader {

    private var bypassDelay = false

    private val pathFactory: MutableMap<OmniLoader.ModInfo, (String) -> String> = mutableMapOf()

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
        "net.azureaaron.dandelion",
        "kotlin"
    )

    fun findFirstMod(): OmniLoader.ModInfo? {
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
        findFirstMod()?.let { nativeLoadedConfigs.add(it.id)}
    }

    val extraCompatConfigs get() = OneConfigUI.extraConfigTrees

    private val list: MutableList<Pair<Int, () -> Unit>> = mutableListOf()

    init {
        OmniLoader.loadedMods.forEach { mod ->
            mod.file?.let {
                pathFactory.put(mod, it.toUri().toString()::plus)
            }
        }

        register<ResourceFinishedLoading> {
            list.sortedBy { (key) -> key }.forEach { (_, value) ->
                println(key)
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