package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Property

@ExperimentalOneConfigUiApi
abstract class SettingRowDecorator {
    @Composable
    abstract fun Decorate(property: Property<*>, content: @Composable () -> Unit)
}

@ExperimentalOneConfigUiApi
object SettingRowDecorators {
    private val LOGGER = LogManager.getLogger("OneConfig/SettingRowDecorators")
    private val lock = Any()

    private var entries by mutableStateOf(emptyList<SettingRowDecorator>())

    @JvmStatic
    fun register(entry: SettingRowDecorator) {
        synchronized(lock) {
            if (entry in entries) {
                LOGGER.debug("Ignoring SettingRowDecorator {} that is already registered", entry.javaClass.name)
                return
            }
            Snapshot.withMutableSnapshot { entries = entries + entry }
        }
    }

    @JvmStatic
    fun unregister(entry: SettingRowDecorator): Boolean = synchronized(lock) {
        if (entry !in entries) return false
        Snapshot.withMutableSnapshot { entries = entries - entry }
        true
    }

    @JvmStatic
    fun all(): List<SettingRowDecorator> = entries
}
