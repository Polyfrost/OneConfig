package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.CompletableFuture
import org.apache.logging.log4j.LogManager

@ExperimentalOneConfigUiApi
public abstract class ChangelogSource {
    public abstract fun load(): CompletableFuture<String>

    public open fun installedVersion(): String? = null
}

@ExperimentalOneConfigUiApi
public object ChangelogSources {
    private val LOGGER = LogManager.getLogger("OneConfig/ChangelogSources")
    private val lock = Any()

    private var entries by mutableStateOf(emptyList<ChangelogSource>())

    @JvmStatic
    public fun register(source: ChangelogSource) {
        synchronized(lock) {
            if (source in entries) {
                LOGGER.debug("Ignoring ChangelogSource {} that is already registered", source.javaClass.name)
                return
            }
            Snapshot.withMutableSnapshot { entries = entries + source }
        }
    }

    @JvmStatic
    public fun unregister(source: ChangelogSource): Boolean = synchronized(lock) {
        if (source !in entries) return false
        Snapshot.withMutableSnapshot { entries = entries - source }
        true
    }

    @JvmStatic
    public fun active(): ChangelogSource? = entries.lastOrNull()
}
