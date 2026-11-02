package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.toggle.v1.ModToggle
import org.polyfrost.oneconfig.api.ui.v1.internal.modIdKeys
import org.polyfrost.oneconfig.api.ui.v1.internal.normaliseModId
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registry of the master switches shown on mod cards
 *
 * A config with a boolean field marked `@MasterToggle`, or a root level option called `enabled`, gets a
 * switch on its card without registering anything here. Mods without such an option either
 * [register] their own [ModToggle] or ask OneConfig to keep the state for them with [manage] and
 * poll it through [isEnabled]
 */
public object ModToggles {
    private val LOGGER = LogManager.getLogger("OneConfig/ModToggles")
    private val lock = Any()
    private val revisionLock = Any()

    @Volatile
    private var toggles: Map<String, ModToggle> = emptyMap()

    @Volatile
    private var managed: Set<String> = emptySet()

    @Volatile
    private var disabled: Set<String> = emptySet()

    @Volatile
    private var store: Store? = null

    private val handles = HashMap<String, ManagedToggle>()

    private val listeners = CopyOnWriteArrayList<Listener>()

    @JvmStatic
    public var revision: Int by mutableIntStateOf(0)
        private set

    public interface Store {
        public fun load(): Set<String>

        public fun save(disabled: Set<String>)
    }

    public fun interface Listener {
        public fun onToggle(modId: String, enabled: Boolean)
    }

    /**
     * Shows a switch backed by [toggle] on the card of [modId]
     *
     * Takes precedence over an `enabled` option detected in the mod's config
     */
    @JvmStatic
    public fun register(modId: String, toggle: ModToggle) {
        val key = normaliseModId(modId)
        if (key.isEmpty()) {
            LOGGER.warn("Ignoring mod toggle with a blank mod id")
            return
        }
        synchronized(lock) { toggles = toggles + (key to toggle) }
        bump()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Binary compatibility for mods compiled against 1.4.0", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    public fun register(modId: String, toggle: org.polyfrost.oneconfig.api.ui.v1.ModToggle): Unit =
        register(modId, toggle as ModToggle)

    @ExperimentalOneConfigUiApi
    @JvmStatic
    @JvmOverloads
    public fun registerReadOnly(modId: String, isEnabled: () -> Boolean, lockedReason: () -> String? = { null }) {
        register(modId, LambdaReadOnlyModToggle(isEnabled, lockedReason))
    }

    /**
     * Shows a switch on the cards of [modIds] whose state is stored by OneConfig
     *
     * The mod, or a mixin into it, is expected to check [isEnabled] wherever it does its work
     */
    @JvmStatic
    public fun manage(vararg modIds: String) {
        val keys = modIds.map(::normaliseModId).filter { it.isNotEmpty() }
        if (keys.isEmpty()) return
        synchronized(lock) { managed = managed + keys }
        bump()
    }

    @JvmStatic
    public fun toggleFor(modId: String): ModToggle? {
        val keys = modIdKeys(modId).toList()
        keys.firstNotNullOfOrNull(toggles::get)?.let { return it }
        return keys.firstOrNull(managed::contains)?.let(::handleForKey)
    }

    @JvmStatic
    public fun handle(modId: String): ModToggle = handleForKey(normaliseModId(modId))

    private fun handleForKey(key: String): ModToggle =
        synchronized(lock) { handles.getOrPut(key) { ManagedToggle(key, key !in disabled) } }

    /**
     * Whether the [manage]d mod [modId] is switched on
     *
     * Cheap enough to call every frame. [modId] has to be the lowercase id passed to [manage].
     * Mods OneConfig does not manage are always reported as enabled
     */
    @JvmStatic
    public fun isEnabled(modId: String): Boolean {
        val off = disabled
        return off.isEmpty() || normaliseModId(modId) !in off
    }

    @JvmStatic
    public fun setEnabled(modId: String, enabled: Boolean) {
        setKey(modIdKeys(modId).firstOrNull(managed::contains) ?: normaliseModId(modId), enabled)
    }

    private fun setKey(key: String, enabled: Boolean) {
        if (key.isEmpty()) return
        synchronized(lock) {
            val next = if (enabled) disabled - key else disabled + key
            if (next == disabled) return
            disabled = next
            handles[key]?.enabled = enabled
            try {
                store?.save(next)
            } catch (t: Throwable) {
                LOGGER.error("Failed to persist the disabled mods", t)
            }
        }
        bump()
        notifyListeners(key, enabled)
    }

    @ApiStatus.Internal
    @JvmStatic
    public fun replaceDisabled(transform: (Set<String>) -> Set<String>) {
        val changed = synchronized(lock) {
            val previous = disabled
            val next = transform(previous).mapTo(HashSet(), ::normaliseModId).apply { remove("") }
            if (next == previous) return
            disabled = next
            val changed = (previous - next).associateWith { true } + (next - previous).associateWith { false }
            changed.forEach { (key, enabled) -> handles[key]?.enabled = enabled }
            changed
        }
        bump()
        changed.forEach(::notifyListeners)
    }

    private fun notifyListeners(key: String, enabled: Boolean) {
        listeners.forEach {
            try {
                it.onToggle(key, enabled)
            } catch (t: Throwable) {
                LOGGER.error("Mod toggle listener {} failed", it, t)
            }
        }
    }

    @JvmStatic
    public fun addListener(listener: Listener) {
        listeners.addIfAbsent(listener)
    }

    @JvmStatic
    public fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    @JvmStatic
    public fun setStore(store: Store) {
        val loaded = try {
            store.load().mapTo(HashSet(), ::normaliseModId)
        } catch (t: Throwable) {
            LOGGER.error("Failed to load the disabled mods", t)
            emptySet()
        }
        synchronized(lock) {
            this.store = store
            disabled = disabled + loaded
            loaded.forEach { handles[it]?.enabled = false }
        }
        bump()
    }

    private class ManagedToggle(private val key: String, @JvmField @Volatile var enabled: Boolean) : ModToggle {
        override fun isEnabled() = enabled

        override fun setEnabled(enabled: Boolean) = setKey(key, enabled)
    }

    private fun bump() {
        synchronized(revisionLock) { Snapshot.global { revision++ } }
    }
}
