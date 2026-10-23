package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.ModOrder
import org.polyfrost.oneconfig.internal.ui.components.settings.bumpResetEpoch

class OneConfigUIExtensionsBridgeImpl : OneConfigUIExtensionsBridge {
    override fun modOrder(): List<String> = ModOrder.current()

    override fun replaceModOrder(ids: List<String>) = ModOrder.replace(ids)

    override fun refreshOption(property: Property<*>) = bumpResetEpoch(property)
}
