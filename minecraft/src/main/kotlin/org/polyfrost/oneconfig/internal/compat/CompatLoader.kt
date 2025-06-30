package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.Event
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading

object CompatLoader {

    private var bypassDelay = false

    fun requireTranslations(init: () -> Unit) {
        if (bypassDelay) {
            init()
            return
        }
        register<ResourceFinishedLoading>(init)
    }

    private inline fun <reified T> register(noinline runnable: () -> Unit) where T : Event {
        EventManager.register(T::class.java) { _ ->
            bypassDelay = true
            runnable.invoke()
        }
    }
}