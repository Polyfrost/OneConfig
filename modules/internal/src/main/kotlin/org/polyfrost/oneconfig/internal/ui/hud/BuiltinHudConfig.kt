package org.polyfrost.oneconfig.internal.ui.hud

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.ui.v1.ModCardType
import org.polyfrost.oneconfig.api.ui.v1.ModCardTypeResolver
import org.polyfrost.oneconfig.api.ui.v1.ModCardTypes
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.search.HudDocumentSource
import org.polyfrost.oneconfig.internal.ui.search.HudModCardDocumentSource
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus

internal const val BUILTIN_HUD_CONFIG_ID = "oneconfig.builtin"
internal const val BUILTIN_HUD_ICON = "/assets/oneconfig/brand/oneconfig-icon.svg"
internal const val HUD_MOD_CARD_TYPE_ID = "oneconfig.huds"

private object BuiltinHudConfigData : ConfigData {
    override val id = BUILTIN_HUD_CONFIG_ID
    override val title = "OneConfig"
    override val icon = BUILTIN_HUD_ICON
    override val source = ConfigSource.OC
    override val category = Config.Category.OTHER
}

object BuiltinHudRegistrar {
    private fun adoptHuds() = HudManager.providers().forEach { hud ->
        if (hud.configId == null) hud.configId = BUILTIN_HUD_CONFIG_ID
    }

    private val hudTypeResolver = ModCardTypeResolver { id ->
        if (isHudModCard(id)) HUD_MOD_CARD_TYPE_ID else null
    }

    @JvmStatic
    fun register() {
        ConfigRegistry.register(BuiltinHudConfigData)
        adoptHuds()
        // also the ones of mods that register theirs later, which would not be listed under any mod
        HudManager.addRegistrationListener(::adoptHuds)
        SearchCorpus.invalidate(HudDocumentSource, HudModCardDocumentSource)
        ModCardTypes.register(ModCardType(HUD_MOD_CARD_TYPE_ID, "HUDs", "hud", priority = 100))
        ModCardTypes.registerResolver(hudTypeResolver)
    }
}
