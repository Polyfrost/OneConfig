@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Tree

@ExperimentalOneConfigUiApi
public abstract class TreeHeaderDecorator {
    @Composable
    public abstract fun Decorate(tree: Tree, content: @Composable () -> Unit)
}

@ExperimentalOneConfigUiApi
public object TreeHeaderDecorators {
    private val LOGGER = LogManager.getLogger("OneConfig/TreeHeaderDecorators")
    private val lock = Any()

    private var entries by mutableStateOf(emptyList<TreeHeaderDecorator>())

    @JvmStatic
    public fun register(entry: TreeHeaderDecorator) {
        synchronized(lock) {
            if (entry in entries) {
                LOGGER.debug("Ignoring TreeHeaderDecorator {} that is already registered", entry.javaClass.name)
                return
            }
            Snapshot.withMutableSnapshot { entries = entries + entry }
        }
    }

    @JvmStatic
    public fun unregister(entry: TreeHeaderDecorator): Boolean = synchronized(lock) {
        if (entry !in entries) return false
        Snapshot.withMutableSnapshot { entries = entries - entry }
        true
    }

    @JvmStatic
    public fun all(): List<TreeHeaderDecorator> = entries
}
