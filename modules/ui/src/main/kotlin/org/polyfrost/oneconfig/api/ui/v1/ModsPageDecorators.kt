package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager

@ExperimentalOneConfigUiApi
abstract class ModsPageDecorator {
    @Composable
    abstract fun Decorate(content: @Composable () -> Unit)
}

@ExperimentalOneConfigUiApi
object ModsPageDecorators {
    private val LOGGER = LogManager.getLogger("OneConfig/ModsPageDecorators")
    private val lock = Any()

    private var entries by mutableStateOf(emptyList<ModsPageDecorator>())

    @JvmStatic
    fun register(entry: ModsPageDecorator) {
        synchronized(lock) {
            if (entry in entries) {
                LOGGER.debug("Ignoring ModsPageDecorator {} that is already registered", entry.javaClass.name)
                return
            }
            Snapshot.withMutableSnapshot { entries = entries + entry }
        }
    }

    @JvmStatic
    fun unregister(entry: ModsPageDecorator): Boolean = synchronized(lock) {
        if (entry !in entries) return false
        Snapshot.withMutableSnapshot { entries = entries - entry }
        true
    }

    @JvmStatic
    fun all(): List<ModsPageDecorator> = entries
}
