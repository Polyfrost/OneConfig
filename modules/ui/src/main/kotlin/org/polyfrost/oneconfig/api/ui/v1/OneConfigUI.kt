@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1

import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.page.ModConfigTarget
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigDestination
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPage

/**
 * Utility for opening the OneConfig UI
 *
 * A route is any destination registered in the OneConfig navigation graph such as a page a mod added itself
 *
 * Opening on a route that is not registered logs an error and falls back to the mods page
 *
 * Kotlin
 * ```
 * OneConfigUI.open()
 * OneConfigUI.open(MyModRoute)
 * ```
 *
 * Java
 * ```
 * OneConfigUI.open();
 * OneConfigUI.open(MyModRoute.INSTANCE);
 * ```
 */
public object OneConfigUI {
    private val LOGGER = LogManager.getLogger("OneConfig/UI")

    /**
     * Bridge into the OneConfig UI screen if present
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

    private val warnedRouteTypes = ConcurrentHashMap.newKeySet<Class<*>>()

    @JvmStatic
    public fun open() {
        show(null)
    }

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun open(destination: OneConfigDestination? = null) {
        show(routeOf(destination))
    }

    @ExperimentalOneConfigUiApi
    @JvmStatic
    @JvmOverloads
    public fun open(page: OneConfigPage, @Suppress("UNUSED_PARAMETER") argument: Any? = null) {
        open(OneConfigDestination.Custom(page))
    }

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun open(pageId: String) {
        open(OneConfigDestination.Page(pageId))
    }

    /**
     * Opens the OneConfig UI on [route] or on the page set by the user's opening behavior when null
     *
     * [route] must be a destination registered in the OneConfig navigation graph
     *
     * An unregistered route falls back to the mods page
     *
     * If the UI is already open it navigates to [route] in place
     */
    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    public fun open(route: Any? = null) {
        show(normalizeRoute(route))
    }

    private fun show(route: Any?) {
        val bridge = bridge
        if (bridge == null) {
            LOGGER.warn("Cannot open the OneConfig UI: no OneConfigUIBridge is available")
            return
        }
        bridge.open(route)
    }

    private fun routeOf(destination: OneConfigDestination?): Any? = when (destination) {
        null -> null
        is OneConfigDestination.Page -> destination.id
        is OneConfigDestination.ModConfig -> destination.target
        is OneConfigDestination.Custom -> destination.page
    }

    private fun normalizeRoute(route: Any?): Any? = when (route) {
        null, is String, is OneConfigPage, is ModConfigTarget -> route
        is OneConfigDestination -> routeOf(route)
        else -> {
            if (warnedRouteTypes.add(route.javaClass)) {
                LOGGER.warn("Cannot open the OneConfig UI on {} because {} is not a OneConfigDestination; opening the default page instead", route, route.javaClass.name)
            }
            null
        }
    }

    /**
     * Builds the OneConfig UI screen opened on [route] without showing it for callers that set the screen
     * themselves
     *
     * Returns null when the OneConfig UI is unavailable
     *
     * [route] must be a destination registered in the OneConfig navigation graph
     *
     * An unregistered route falls back to the mods page
     *
     * When null the screen opens on the page set by the user's opening behavior
     */
    @JvmStatic
    @JvmOverloads
    public fun createScreen(route: Any? = null): Any? = bridge?.createScreen(normalizeRoute(route))

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun isOneConfigScreen(screen: Any?): Boolean = bridge?.isOneConfigScreen(screen) ?: false
}
