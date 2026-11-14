package org.polyfrost.oneconfig.api.ui.v1

import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPage

/**
 * Platform bridge that builds and shows the OneConfig UI screen
 */
public interface OneConfigUIBridge {
    /** Creates the OneConfig UI screen opened on [route] or on the configured opening page when null */
    public fun createScreen(route: Any?): Any

    /** Shows the OneConfig UI on [route] navigating in place when it is already open */
    public fun open(route: Any?)

    public fun isOneConfigScreen(screen: Any?): Boolean = false

    @ExperimentalOneConfigUiApi
    public fun openPage(page: OneConfigPage, argument: Any?) {
        open(page)
    }

    @ExperimentalOneConfigUiApi
    public fun takePageArgument(): Any? = null

    @ExperimentalOneConfigUiApi
    public fun ensureInitialized() {}
}
