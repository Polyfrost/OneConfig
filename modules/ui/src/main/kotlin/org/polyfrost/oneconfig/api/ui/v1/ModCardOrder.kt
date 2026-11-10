package org.polyfrost.oneconfig.api.ui.v1

import org.polyfrost.oneconfig.api.ui.v1.internal.extensionsBridge

@ExperimentalOneConfigUiApi
public object ModCardOrder {
    @JvmStatic
    public fun current(): List<String> = extensionsBridge()?.modOrder() ?: emptyList()

    @JvmStatic
    public fun replace(ids: List<String>) {
        extensionsBridge()?.replaceModOrder(ids)
    }
}
