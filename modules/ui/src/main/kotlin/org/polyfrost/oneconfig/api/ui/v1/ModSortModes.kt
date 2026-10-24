package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.internal.extensionsBridge

@ExperimentalOneConfigUiApi
class ModSortMode @JvmOverloads constructor(
    val id: String,
    val icon: String,
    val title: String,
    val confirmation: String? = null,
    val onSelect: () -> Unit,
)

@ExperimentalOneConfigUiApi
object ModSortModes {
    private val LOGGER = LogManager.getLogger("OneConfig/ModSortModes")
    private val lock = Any()

    private var entries by mutableStateOf(emptyList<ModSortMode>())

    @JvmStatic
    fun register(mode: ModSortMode) {
        synchronized(lock) {
            if (entries.any { it.id == mode.id }) {
                LOGGER.debug("Ignoring ModSortMode {} that is already registered", mode.id)
                return
            }
            Snapshot.withMutableSnapshot { entries = entries + mode }
        }
    }

    @JvmStatic
    fun unregister(mode: ModSortMode): Boolean = synchronized(lock) {
        if (mode !in entries) return false
        Snapshot.withMutableSnapshot { entries = entries - mode }
        true
    }

    @JvmStatic
    fun all(): List<ModSortMode> = entries

    @JvmStatic
    fun currentId(): String? = extensionsBridge()?.modSortModeId()
}
