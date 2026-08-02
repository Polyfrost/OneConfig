package org.polyfrost.oneconfig.api.ui.v1

import org.apache.logging.log4j.LogManager
import java.util.ServiceLoader

/**
 * Utility for opening the OneConfig UI.
 *
 * A route is any destination registered in the OneConfig navigation graph, such as a page a mod added itself.
 * Opening on a route that is not registered logs an error and falls back to the mods page.
 *
 * Kotlin:
 * ```
 * OneConfigUI.open()
 * OneConfigUI.open(MyModRoute)
 * ```
 *
 * Java:
 * ```
 * OneConfigUI.open();
 * OneConfigUI.open(MyModRoute.INSTANCE);
 * ```
 */
object OneConfigUI {
    private val LOGGER = LogManager.getLogger("OneConfig/UI")

    /**
     * Bridge into the OneConfig UI screen if present.
     */
    private val bridge: OneConfigUIBridge? by lazy {
        try {
            ServiceLoader.load(OneConfigUIBridge::class.java, OneConfigUIBridge::class.java.classLoader)
                .iterator().let { if (it.hasNext()) it.next() else null }
        } catch (t: Throwable) {
            LOGGER.warn("Failed to load OneConfigUIBridge; the OneConfig UI cannot be opened", t)
            null
        }
    }

    /**
     * Opens the OneConfig UI on [route], or on the page set by the user's opening behavior when null.
     *
     * [route] must be a destination registered in the OneConfig navigation graph; an unregistered route falls
     * back to the mods page. If the UI is already open, it navigates to [route] in place.
     */
    @JvmStatic
    @JvmOverloads
    fun open(route: Any? = null) {
        val bridge = bridge
        if (bridge == null) {
            LOGGER.warn("Cannot open the OneConfig UI: no OneConfigUIBridge is available")
            return
        }
        bridge.open(route)
    }

    /**
     * Builds the OneConfig UI screen opened on [route] without showing it, for callers that set the screen
     * themselves. Returns null when the OneConfig UI is unavailable.
     *
     * [route] must be a destination registered in the OneConfig navigation graph; an unregistered route falls
     * back to the mods page. When null, the screen opens on the page set by the user's opening behavior.
     */
    @JvmStatic
    @JvmOverloads
    fun createScreen(route: Any? = null): Any? = bridge?.createScreen(route)
}
