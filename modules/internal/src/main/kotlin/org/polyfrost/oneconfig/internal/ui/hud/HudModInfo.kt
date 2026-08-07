package org.polyfrost.oneconfig.internal.ui.hud

import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.asRenderText

/**
 * Get the config of a HUD's ùpd
 */
internal fun configForHud(configId: String): ConfigData? {
    val modId = configId.removeSuffix(".json").substringBefore('/')
    return ConfigRegistry.findById(configId)
        ?: ConfigRegistry.findById("$configId.json")
        ?: ConfigRegistry.configs.firstOrNull { it.id.removeSuffix(".json") == modId }
}

/** Get the name of the mod owning a hud */
internal fun modNameFor(configId: String): String? {
    val modId = configId.removeSuffix(".json").substringBefore('/')
    ModInfo.loadedMods.firstOrNull { it.id == modId }?.name?.let { return it }
    return configForHud(configId)?.title?.asRenderText()
}
