package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.internal.extensionsBridge

@ExperimentalOneConfigUiApi
public class ModSortMode @JvmOverloads constructor(
    public val id: String,
    public val icon: String,
    public val title: String,
    public val confirmation: String? = null,
    public val onSelect: () -> Unit,
)

@ExperimentalOneConfigUiApi
public object ModSortModes {
    private val LOGGER = LogManager.getLogger("OneConfig/ModSortModes")
    private val lock = Any()

    private var entries by mutableStateOf(emptyList<ModSortMode>())

    @JvmStatic
    public fun register(mode: ModSortMode) {
        synchronized(lock) {
            if (entries.any { it.id == mode.id }) {
                LOGGER.debug("Ignoring ModSortMode {} that is already registered", mode.id)
                return
            }
            Snapshot.withMutableSnapshot { entries = entries + mode }
        }
    }

    @JvmStatic
    public fun unregister(mode: ModSortMode): Boolean = synchronized(lock) {
        if (mode !in entries) return false
        Snapshot.withMutableSnapshot { entries = entries - mode }
        true
    }

    @JvmStatic
    public fun all(): List<ModSortMode> = entries

    @JvmStatic
    public fun currentId(): String? = extensionsBridge()?.modSortModeId()
}
