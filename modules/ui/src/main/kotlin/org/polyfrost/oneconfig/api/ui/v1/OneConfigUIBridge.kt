package org.polyfrost.oneconfig.api.ui.v1

/**
 * Platform bridge that builds and shows the OneConfig UI screen
 */
interface OneConfigUIBridge {
    /** Creates the OneConfig UI screen opened on [route] or on the configured opening page when null */
    fun createScreen(route: Any?): Any

    /** Shows the OneConfig UI on [route] navigating in place when it is already open */
    fun open(route: Any?)
}
