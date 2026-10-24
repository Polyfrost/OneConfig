package org.polyfrost.oneconfig.api.ui.v1.keybind

import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.platform.v1.Platform

object TextInputFocus {
    private val NO_SCREEN = Any()

    private val holders = Collections.synchronizedMap(WeakHashMap<Any, WeakReference<Any>>())

    @JvmStatic
    val isTyping: Boolean
        get() {
            if (holders.isEmpty()) return false
            val current = currentScreen()
            synchronized(holders) {
                return holders.values.any { it.get() === current }
            }
        }

    @JvmStatic
    fun acquire(token: Any) {
        holders[token] = WeakReference(currentScreen())
    }

    @JvmStatic
    fun release(token: Any) {
        holders.remove(token)
    }

    @JvmStatic
    @ApiStatus.Internal
    fun clear() {
        holders.clear()
    }

    private fun currentScreen(): Any = Platform.screen().current<Any?>() ?: NO_SCREEN
}
