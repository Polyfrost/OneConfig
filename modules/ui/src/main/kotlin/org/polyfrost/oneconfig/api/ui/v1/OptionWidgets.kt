package org.polyfrost.oneconfig.api.ui.v1

import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.internal.extensionsBridge

@ExperimentalOneConfigUiApi
object OptionWidgets {
    @JvmStatic
    fun refresh(property: Property<*>) {
        extensionsBridge()?.refreshOption(property)
    }
}
