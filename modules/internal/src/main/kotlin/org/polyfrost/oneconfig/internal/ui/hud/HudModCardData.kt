package org.polyfrost.oneconfig.internal.ui.hud

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.components.localizedValue

private const val HUD_CARD_ID_PREFIX = "oneconfig.hud:"

internal fun hudModCardConfigs(): List<ConfigData> =
    HudManager.providers().map(::HudModCardData)

private class HudModCardData(private val hud: Hud) : ConfigData {
    private val ownerId: String
        get() = hud.configId ?: BUILTIN_HUD_CONFIG_ID

    private val owner: ConfigData?
        get() {
            val id = ownerId
            val modId = id.removeSuffix(".json").substringBefore('/')
            return ConfigRegistry.findById(id)
                ?: ConfigRegistry.findById("$id.json")
                ?: ConfigRegistry.configs.firstOrNull {
                    it.id.removeSuffix(".json").substringBefore('/') == modId
                }
        }

    override val id: String
        get() = "$HUD_CARD_ID_PREFIX$ownerId:${hud.id}:${hud::class.java.name}"

    override val title: Any
        get() = localizedValue(hud.title) ?: hud.title

    override val icon: String?
        get() = HudManager.iconFor(ownerId)
            ?: owner?.icon
            ?: if (ownerId == BUILTIN_HUD_CONFIG_ID) BUILTIN_HUD_ICON else "hud"

    override val authors: String?
        get() = owner?.authors

    override val credits: String?
        get() = owner?.credits

    override val version: String?
        get() = owner?.version

    override val source: ConfigSource
        get() = owner?.source ?: ConfigSource.OC

    override val category: Config.Category = Config.Category.HUD

    override val onOpen: () -> Unit = HudManager::openEditor
}
