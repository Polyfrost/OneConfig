package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.CopyOnWriteArrayList
import org.apache.logging.log4j.LogManager

/**
 * The master switch shown on a mod's card
 */
interface ModToggle {
    fun isEnabled(): Boolean

    fun setEnabled(enabled: Boolean)

    /**
     * Whether the mod has to be set up before it can be switched on
     *
     * Switching such a mod on opens it from its card instead, the same as clicking the card does
     */
    fun needsSetup(): Boolean = false
}

/**
 * Registry of the master switches shown on mod cards
 *
 * A config with a root level boolean option called `enabled` gets a switch on its card without
 * registering anything here. Mods without such an option either [register] their own [ModToggle]
 * or ask OneConfig to keep the state for them with [manage] and poll it through [isEnabled]
 */
object ModToggles {
    private val LOGGER = LogManager.getLogger("OneConfig/ModToggles")
    private val lock = Any()

    @Volatile
    private var toggles: Map<String, ModToggle> = emptyMap()

    @Volatile
    private var managed: Set<String> = emptySet()

    @Volatile
    private var disabled: Set<String> = emptySet()

    @Volatile
    private var store: Store? = null

    private val listeners = CopyOnWriteArrayList<Listener>()

    @JvmStatic
    var revision by mutableIntStateOf(0)
        private set

    interface Store {
        fun load(): Set<String>

        fun save(disabled: Set<String>)
    }

    fun interface Listener {
        fun onToggle(modId: String, enabled: Boolean)
    }

    /**
     * Shows a switch backed by [toggle] on the card of [modId]
     *
     * Takes precedence over an `enabled` option detected in the mod's config
     */
    @JvmStatic
    fun register(modId: String, toggle: ModToggle) {
        val key = normalise(modId)
        if (key.isEmpty()) {
            LOGGER.warn("Ignoring mod toggle with a blank mod id")
            return
        }
        synchronized(lock) { toggles = toggles + (key to toggle) }
        bump()
    }

    /**
     * Shows a switch on the cards of [modIds] whose state is stored by OneConfig
     *
     * The mod, or a mixin into it, is expected to check [isEnabled] wherever it does its work
     */
    @JvmStatic
    fun manage(vararg modIds: String) {
        val keys = modIds.map(::normalise).filter { it.isNotEmpty() }
        if (keys.isEmpty()) return
        synchronized(lock) { managed = managed + keys }
        bump()
    }

    @JvmStatic
    fun toggleFor(modId: String): ModToggle? {
        val keys = keysFor(modId).toList()
        keys.firstNotNullOfOrNull(toggles::get)?.let { return it }
        return keys.firstOrNull(managed::contains)?.let(::ManagedToggle)
    }

    /**
     * Whether the [manage]d mod [modId] is switched on
     *
     * Cheap enough to call every frame. [modId] has to be the lowercase id passed to [manage].
     * Mods OneConfig does not manage are always reported as enabled
     */
    @JvmStatic
    fun isEnabled(modId: String): Boolean {
        val off = disabled
        return off.isEmpty() || modId !in off
    }

    @JvmStatic
    fun setEnabled(modId: String, enabled: Boolean) {
        val key = keysFor(modId).firstOrNull(managed::contains) ?: normalise(modId)
        if (key.isEmpty()) return
        val snapshot = synchronized(lock) {
            val next = if (enabled) disabled - key else disabled + key
            if (next == disabled) return
            disabled = next
            next
        }
        bump()
        try {
            store?.save(snapshot)
        } catch (t: Throwable) {
            LOGGER.error("Failed to persist the disabled mods", t)
        }
        listeners.forEach {
            try {
                it.onToggle(key, enabled)
            } catch (t: Throwable) {
                LOGGER.error("Mod toggle listener {} failed", it, t)
            }
        }
    }

    @JvmStatic
    fun addListener(listener: Listener) {
        listeners.addIfAbsent(listener)
    }

    @JvmStatic
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    @JvmStatic
    fun setStore(store: Store) {
        val loaded = try {
            store.load().mapTo(HashSet(), ::normalise)
        } catch (t: Throwable) {
            LOGGER.error("Failed to load the disabled mods", t)
            emptySet()
        }
        synchronized(lock) {
            this.store = store
            disabled = disabled + loaded
        }
        bump()
    }

    private class ManagedToggle(private val key: String) : ModToggle {
        override fun isEnabled() = ModToggles.isEnabled(key)

        override fun setEnabled(enabled: Boolean) = ModToggles.setEnabled(key, enabled)
    }

    private fun normalise(modId: String) = modId.trim().removeSuffix(".json").lowercase()

    private fun keysFor(modId: String) = sequenceOf(
        modId,
        modId.removeSuffix(".json"),
        modId.substringBefore('/'),
        modId.substringBefore('/').removeSuffix(".json"),
    ).map { it.trim().lowercase() }.distinct()

    private fun bump() {
        Snapshot.withMutableSnapshot { revision++ }
    }
}
