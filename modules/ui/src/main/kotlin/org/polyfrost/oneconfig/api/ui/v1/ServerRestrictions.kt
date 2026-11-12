package org.polyfrost.oneconfig.api.ui.v1

import java.util.concurrent.CopyOnWriteArrayList
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.toggle.v1.ModToggle

/**
 * Mods and HUDs switched off by the server the player is connected to
 *
 * A restriction lasts until it is replaced or [clear]ed, which happens on disconnect, and is never saved:
 * the player's own settings are kept and come back as they were once it is lifted. Ids are compared
 * without case and without a `.json` suffix. A mod id is the id of the mod, a HUD id is the [id][org.polyfrost.oneconfig.api.config.v1.Config.id]
 * of the HUD, which HUD mods check through [isHudDisabled]
 */
public object ServerRestrictions {
    private val LOGGER = LogManager.getLogger("OneConfig/ServerRestrictions")
    private val lock = Any()

    @Volatile
    private var mods: Set<String> = emptySet()

    @Volatile
    private var huds: Set<String> = emptySet()

    // each id also with its `.json` suffix, so a lookup by an id written either way needs no new string
    @Volatile
    private var modKeys: Set<String> = emptySet()

    @Volatile
    private var hudKeys: Set<String> = emptySet()

    private val listeners = CopyOnWriteArrayList<Listener>()

    public fun interface Listener {
        /** Called on the thread [set] was called from, with the mods whose restriction was added or lifted */
        public fun onChange(changedMods: Set<String>)
    }

    /** Replaces what the server restricts, meant to be called on the client thread */
    @JvmStatic
    public fun set(mods: Collection<String>, huds: Collection<String>) {
        val nextMods = mods.mapNotNullTo(HashSet()) { normalise(it).ifEmpty { null } }
        val nextHuds = huds.mapNotNullTo(HashSet()) { normalise(it).ifEmpty { null } }
        val changed = synchronized(lock) {
            val previous = this.mods
            if (previous == nextMods && this.huds == nextHuds) return
            this.mods = nextMods
            this.huds = nextHuds
            modKeys = keys(nextMods)
            hudKeys = keys(nextHuds)
            (previous - nextMods) + (nextMods - previous)
        }
        if (nextMods.isNotEmpty() || nextHuds.isNotEmpty()) {
            LOGGER.info("The server restricts mods {} and HUDs {}", nextMods, nextHuds)
        }
        ModToggles.serverRestrictionsChanged(changed)
        listeners.forEach {
            try {
                it.onChange(changed)
            } catch (t: Throwable) {
                LOGGER.error("Server restriction listener {} failed", it, t)
            }
        }
    }

    @JvmStatic
    public fun clear(): Unit = set(emptyList(), emptyList())

    /** Cheap enough to call every frame */
    @JvmStatic
    public fun isModDisabled(modId: String): Boolean = matches(modKeys, modId)

    /** Cheap enough to call every frame */
    @JvmStatic
    public fun isHudDisabled(hudId: String): Boolean = matches(hudKeys, hudId)

    @JvmStatic
    public fun disabledMods(): Set<String> = mods

    @JvmStatic
    public fun disabledHuds(): Set<String> = huds

    @JvmStatic
    public fun addListener(listener: Listener) {
        listeners.addIfAbsent(listener)
    }

    @JvmStatic
    public fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    /** [toggle] as the card of [modId] shows it, switched off and refusing changes while the server restricts the mod */
    @JvmStatic
    public fun guard(modId: String, toggle: ModToggle?): ModToggle? =
        if (toggle != null && isModDisabled(modId)) Locked(toggle, "mod") else toggle

    /** [toggle] as the card of the HUD [hudId] shows it, switched off and refusing changes while the server restricts it */
    @JvmStatic
    public fun guardHud(hudId: String, toggle: ModToggle?): ModToggle? =
        if (toggle != null && isHudDisabled(hudId)) Locked(toggle, "HUD") else toggle

    private class Locked(private val toggle: ModToggle, private val kind: String) : ModToggle {
        override fun isEnabled() = false

        override fun setEnabled(enabled: Boolean) {
            Notifications.info("Switched off by the server", "This server does not allow this $kind while you play on it.")
        }

        override fun needsSetup() = toggle.needsSetup()
    }

    private fun matches(keys: Set<String>, id: String): Boolean {
        if (keys.isEmpty()) return false
        if (id in keys) return true
        return id.any { it.isUpperCase() || it.isWhitespace() } && normalise(id) in keys
    }

    private fun keys(ids: Set<String>): Set<String> = ids.flatMapTo(HashSet()) { listOf(it, "$it.json") }

    private fun normalise(id: String) = id.trim().removeSuffix(".json").lowercase()
}
