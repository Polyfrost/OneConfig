package org.polyfrost.oneconfig.internal.ui.hud

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.components.localizedValue

private const val HUD_CARD_ID_PREFIX = "oneconfig.hud:"

private fun ownerIdOf(hud: Hud): String = hud.configId ?: BUILTIN_HUD_CONFIG_ID

private fun hudCardId(hud: Hud, ownerId: String) =
    "$HUD_CARD_ID_PREFIX$ownerId:${hud.id}:${hud::class.java.name}"

private fun isCompatHud(hud: Hud) = hud.category.id == Hud.Category.COMPAT.id

private fun findOwner(ownerId: String): ConfigData? =
    ConfigRegistry.findById(ownerId)
        ?: ConfigRegistry.findById("$ownerId.json")
        ?: ConfigRegistry.configs.firstOrNull { it.id.substringBefore('/').removeSuffix(".json") == ownerId }

private fun ownerVisible(ownerId: String, owner: ConfigData?): Boolean {
    if (ownerId == BUILTIN_HUD_CONFIG_ID) return true
    return ConfigRegistry.shouldShowModCardId(ownerId) &&
        (owner == null || ConfigRegistry.shouldShowModCardId(owner.id))
}

internal fun hudModCardConfigs(): List<ConfigData> {
    @Suppress("UNUSED_VARIABLE")
    val revision = HudManager.revision

    val out = ArrayList<ConfigData>()
    val seen = HashSet<String>()
    for (hud in HudManager.providers()) {
        if (isCompatHud(hud)) continue
        val ownerId = ownerIdOf(hud)
        val owner = findOwner(ownerId)
        if (!ownerVisible(ownerId, owner)) continue
        val id = hudCardId(hud, ownerId)
        if (seen.add(id)) out.add(HudModCardData(hud, ownerId, owner, id))
    }
    return out
}

private class HudModCardData(
    private val hud: Hud,
    ownerId: String,
    owner: ConfigData?,
    override val id: String,
) : ConfigData {
    override val title: Any
        get() = localizedValue(hud.title) ?: hud.title

    override val icon: String? = HudManager.iconFor(ownerId)
        ?: owner?.icon
        ?: if (ownerId == BUILTIN_HUD_CONFIG_ID) BUILTIN_HUD_ICON else "hud"

    override val authors: String? = owner?.authors

    override val credits: String? = owner?.credits

    override val version: String? = owner?.version

    override val source: ConfigSource = owner?.source ?: ConfigSource.OC

    override val category: Config.Category = Config.Category.HUD

    override val onOpen: () -> Unit = {
        HudManager.pendingSelection = HudManager.getHudsOfType(hud::class.java).singleOrNull()
        HudManager.openEditor()
    }
}
