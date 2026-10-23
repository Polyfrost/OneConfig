package org.polyfrost.oneconfig.api.ui.v1

import org.polyfrost.oneconfig.api.ui.v1.internal.extensionsBridge

@ExperimentalOneConfigUiApi
object ModCardOrder {
    @JvmStatic
    fun current(): List<String> = extensionsBridge()?.modOrder() ?: emptyList()

    @JvmStatic
    fun replace(ids: List<String>) {
        extensionsBridge()?.replaceModOrder(ids)
    }
}
