package org.polyfrost.oneconfig.internal.ui.hud

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource

internal const val BUILTIN_HUD_CONFIG_ID = "oneconfig.builtin"
internal const val BUILTIN_HUD_ICON = "/assets/oneconfig/brand/oneconfig-icon.svg"

private object BuiltinHudConfigData : ConfigData {
    override val id = BUILTIN_HUD_CONFIG_ID
    override val title = "OneConfig"
    override val icon = BUILTIN_HUD_ICON
    override val source = ConfigSource.OC
    override val category = Config.Category.OTHER
}

object BuiltinHudRegistrar {
    @JvmStatic
    fun register() {
        ConfigRegistry.register(BuiltinHudConfigData)
        HudManager.providers().forEach { hud ->
            if (hud.configId == null) hud.configId = BUILTIN_HUD_CONFIG_ID
        }
    }
}
