package org.polyfrost.oneconfig.internal.ui.hud

import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.asRenderText

internal fun configForHud(configId: String): ConfigData? {
    val modId = configId.removeSuffix(".json").substringBefore('/')
    return ConfigRegistry.findById(configId)
        ?: ConfigRegistry.findById("$configId.json")
        ?: ConfigRegistry.configList.firstOrNull { it.id.removeSuffix(".json") == modId }
}

internal fun modNameFor(configId: String): String? {
    val modId = configId.removeSuffix(".json").substringBefore('/')
    ModInfo.loadedMods.firstOrNull { it.id == modId }?.name?.let { return it }
    return configForHud(configId)?.title?.asRenderText()
}
