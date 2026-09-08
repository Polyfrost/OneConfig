package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.localizedValue
import org.polyfrost.oneconfig.internal.ui.hud.components.HudPreview
import java.util.concurrent.ConcurrentHashMap

private const val HUD_CARD_ID_PREFIX = "oneconfig.hud:"

private fun ownerIdOf(hud: Hud): String = hud.configId ?: BUILTIN_HUD_CONFIG_ID

private fun hudCardId(hud: Hud, ownerId: String) =
    "$HUD_CARD_ID_PREFIX$ownerId:${hud.id}:${hud::class.java.name}"

private fun isCompatHud(hud: Hud) = hud.category.id == Hud.Category.COMPAT.id

private val COMPAT_CARD_HUD_IDS = setOf("ukus_armor_hud")

private fun cardHuds(): List<Hud> =
    HudManager.providers().filterNot { isCompatHud(it) } +
        activeInstancesSnapshot().filter { isCompatHud(it) && it.id in COMPAT_CARD_HUD_IDS }

private fun activeInstancesSnapshot(): List<Hud> =
    runCatching { ArrayList(HudManager.activeInstances).filterNotNull() }.getOrElse { emptyList() }

internal fun isHudModCard(id: String) = id.startsWith(HUD_CARD_ID_PREFIX)

private fun withHudSuffix(title: Any): Any {
    val text = title.asRenderText().trimEnd()
    return if (text.endsWith("HUD", ignoreCase = true)) title else "$text HUD"
}

private fun findOwner(ownerId: String): ConfigData? =
    ConfigRegistry.findById(ownerId)
        ?: ConfigRegistry.findById("$ownerId.json")
        ?: ConfigRegistry.configs.firstOrNull { it.id.substringBefore('/').removeSuffix(".json") == ownerId }

private fun ownerVisible(ownerId: String, owner: ConfigData?): Boolean {
    if (ownerId == BUILTIN_HUD_CONFIG_ID) return true
    return ConfigRegistry.shouldShowModCardId(ownerId) &&
        (owner == null || ConfigRegistry.shouldShowModCardId(owner.id))
}

/** Cache so the search corpus can keep using the same mod cards */
private val cardCache = HashMap<String, HudModCardData>()

internal fun cardsSupersededByHudCards(hudCards: List<ConfigData>): Set<ConfigData> =
    hudCards.mapNotNullTo(HashSet()) { card ->
        (card as? HudModCardData)?.takeIf { isCompatHud(it.hud) }?.owner
    }

internal fun hudModCardConfigs(): List<ConfigData> = synchronized(cardCache) {
    @Suppress("UNUSED_VARIABLE")
    val revision = HudManager.revision

    val out = ArrayList<ConfigData>()
    val seen = HashSet<String>()
    for (hud in cardHuds()) {
        val ownerId = ownerIdOf(hud)
        val owner = findOwner(ownerId)
        if (!ownerVisible(ownerId, owner)) continue
        val id = hudCardId(hud, ownerId)
        if (!seen.add(id)) continue
        val cached = cardCache[id]?.takeIf { it.hud === hud && it.owner === owner }
        out.add(cached ?: HudModCardData(hud, ownerId, owner, id).also { cardCache[id] = it })
    }
    cardCache.keys.retainAll(seen)
    return out
}

internal class HudModCardData(
    val hud: Hud,
    ownerId: String,
    val owner: ConfigData?,
    override val id: String,
) : ConfigData {
    override val title: Any
        get() = withHudSuffix(localizedValue(hud.title) ?: hud.title)

    override val description: String? = hud.description

    override val icon: String? = if (hud is LegacyHudMarker && !isCompatHud(hud)) null else {
        HudManager.iconFor(ownerId)
            ?: owner?.icon
            ?: if (ownerId == BUILTIN_HUD_CONFIG_ID) BUILTIN_HUD_ICON else "hud"
    }

    override val preview: (@Composable (Modifier) -> Unit)? =
        if (hud is LegacyHudMarker) null else ({ modifier -> HudPreview(hud, modifier) })

    override val authors: String? = owner?.authors

    override val credits: String? = owner?.credits

    override val version: String? = owner?.version

    override val source: ConfigSource = owner?.source ?: ConfigSource.OC

    override val category: Config.Category = Config.Category.HUD

    override val onOpen: () -> Unit = {
        val existing =
            if (activeInstancesSnapshot().any { it === hud }) listOf(hud)
            else HudManager.getHudsOfType(hud::class.java)
        HudManager.pendingSelection = existing.firstOrNull()
        HudManager.pendingAdd = if (existing.isEmpty()) hud else null
        HudManager.openEditor()
    }
}
