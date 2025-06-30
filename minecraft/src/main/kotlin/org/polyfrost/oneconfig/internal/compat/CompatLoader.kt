package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.Event
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.internal.ui.OneConfigUI

object CompatLoader {

    private var bypassDelay = false

    val extraCompatConfigs get() = OneConfigUI.extraConfigTrees

    private val list: MutableList<Pair<Int, () -> Unit>> = mutableListOf()
    init {
        register<ResourceFinishedLoading> {
            list.sortedBy { (key) -> key }.forEach { (_, value) -> value() }
        }
    }

    fun requireTranslations(priority: Int = 0, init: () -> Unit) {
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