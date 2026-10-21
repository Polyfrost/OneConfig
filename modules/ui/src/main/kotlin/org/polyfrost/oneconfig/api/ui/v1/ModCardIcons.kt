package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.ConcurrentHashMap
import org.apache.logging.log4j.LogManager

@ExperimentalOneConfigUiApi
fun interface ModCardIconProvider {
    fun iconFor(modId: String, defaultIcon: String?): String?
}

@ExperimentalOneConfigUiApi
object ModCardIcons {
    private val LOGGER = LogManager.getLogger("OneConfig/ModCardIcons")
    private val lock = Any()
    private val failed = ConcurrentHashMap.newKeySet<ModCardIconProvider>()

    private var entries by mutableStateOf(emptyList<ModCardIconProvider>())

    @JvmStatic
    fun register(entry: ModCardIconProvider) {
        synchronized(lock) {
            if (entry in entries) {
                LOGGER.debug("Ignoring ModCardIconProvider {} that is already registered", entry.javaClass.name)
                return
            }
            Snapshot.withMutableSnapshot { entries = entries + entry }
        }
    }

    @JvmStatic
    fun unregister(entry: ModCardIconProvider): Boolean = synchronized(lock) {
        if (entry !in entries) return false
        Snapshot.withMutableSnapshot { entries = entries - entry }
        failed.remove(entry)
        true
    }

    @JvmStatic
    fun resolve(modId: String, defaultIcon: String?): String? = entries.fold(defaultIcon) { icon, provider ->
        try {
            provider.iconFor(modId, icon)
        } catch (t: Throwable) {
            if (failed.add(provider)) {
                LOGGER.error("Mod card icon provider {} failed for {}, further failures are not logged", provider.javaClass.name, modId, t)
            }
            icon
        }
    }
}
