package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.Event
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent

object CompatLoader {

    fun delay(init: () -> Unit) {
        register<ScreenOpenEvent>(init)
    }

    inline fun <reified T> register(noinline runnable: () -> Unit) where T : Event {
        EventManager.register(T::class.java, { _->
            runnable.invoke()
        })

    }
}