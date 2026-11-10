package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public interface OneConfigUIExtensionsBridge {
    public fun modOrder(): List<String>

    public fun replaceModOrder(ids: List<String>)

    public fun modSortModeId(): String

    public fun refreshOption(property: Property<*>)
}

@ExperimentalOneConfigUiApi
private val loadedExtensionsBridge: OneConfigUIExtensionsBridge? by lazy {
    loadBridge(OneConfigUIExtensionsBridge::class.java, "the mod order and option widgets cannot be changed")
}

@ExperimentalOneConfigUiApi
internal fun extensionsBridge(): OneConfigUIExtensionsBridge? = loadedExtensionsBridge
