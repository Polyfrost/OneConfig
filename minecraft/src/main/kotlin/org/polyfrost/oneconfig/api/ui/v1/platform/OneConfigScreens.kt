@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import net.minecraft.client.gui.screens.Screen
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
object OneConfigScreens {
    private val LOGGER = LogManager.getLogger("OneConfig/Screens")

    private class Guard(val owner: Any, val allow: (Screen?) -> Boolean)

    private val guards = CopyOnWriteArrayList<Guard>()

    @JvmStatic
    fun display(screen: Screen?) {
        Platform.screen().display(screen, 0)
    }

    @JvmStatic
    fun current(): Screen? = Platform.screen().current<Screen?>()

    @JvmStatic
    fun preventReplacement(owner: Any, allow: (next: Screen?) -> Boolean): AutoCloseable {
        ReplacementHandler.install()
        val guard = Guard(owner, allow)
        guards += guard
        return AutoCloseable { guards.remove(guard) }
    }

    internal fun allows(next: Screen?): Boolean {
        for (guard in guards) {
            if (next === guard.owner) continue
            val allowed = try {
                guard.allow(next)
            } catch (t: Throwable) {
                LOGGER.error("Screen guard of {} failed; letting the screen change through", guard.owner, t)
                true
            }
            if (!allowed) return false
        }
        return true
    }

    private object ReplacementHandler : EventHandler<ScreenOpenEvent>() {
        private val installed = AtomicBoolean(false)

        fun install() {
            if (installed.compareAndSet(false, true)) register()
        }

        override fun handle(event: ScreenOpenEvent): Boolean {
            if (!allows(event.getScreen<Screen?>())) event.cancelled = true
            return false
        }

        override fun getEventClass(): Class<ScreenOpenEvent> = ScreenOpenEvent::class.java

        override fun getPriority(): Int = Int.MAX_VALUE
    }
}
