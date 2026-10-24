package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.slf4j.LoggerFactory

object ModSortStore {
    const val CUSTOM = "custom"
    const val ALPHABETICAL = "alphabetical"

    private val LOGGER = LoggerFactory.getLogger("OneConfig/ModSortStore")
    private const val FILE_NAME = "mod-sort"

    private var selected by mutableStateOf(CUSTOM)
    private var loaded = false

    val selectedId: String
        get() {
            ensureLoaded()
            return selected
        }

    val alphabetical: Boolean get() = selectedId == ALPHABETICAL

    private fun file(): Path = ConfigManager.internal().folder.resolve(FILE_NAME)

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            val path = file()
            if (Files.exists(path)) {
                Files.readAllLines(path, StandardCharsets.UTF_8)
                    .map(String::trim)
                    .firstOrNull(String::isNotEmpty)
                    ?.let { selected = it }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load mod sort mode", e)
        }
    }

    @Synchronized
    fun select(id: String) {
        ensureLoaded()
        if (selected == id) return
        selected = id
        try {
            val path = file()
            Files.createDirectories(path.parent)
            Files.write(
                path,
                id.toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to persist mod sort mode", e)
        }
    }
}
