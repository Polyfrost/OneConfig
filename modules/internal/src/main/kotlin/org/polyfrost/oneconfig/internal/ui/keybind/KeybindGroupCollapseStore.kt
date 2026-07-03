package org.polyfrost.oneconfig.internal.ui.keybind

import androidx.compose.runtime.mutableStateMapOf
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object KeybindGroupCollapseStore {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/KeybindCollapse")
    private const val FILE_NAME = "keybind-collapsed"

    private val collapsed = mutableStateMapOf<String, Boolean>()
    private var loaded = false

    private fun file(): Path = ConfigManager.internal().folder.resolve(FILE_NAME)

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            val path = file()
            if (Files.exists(path)) {
                Files.readAllLines(path, StandardCharsets.UTF_8).forEach { line ->
                    val id = line.trim()
                    if (id.isNotEmpty()) collapsed[id] = true
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load collapsed keybind groups", e)
        }
    }

    fun isCollapsed(modId: String): Boolean {
        ensureLoaded()
        return collapsed[modId] == true
    }

    fun setCollapsed(modId: String, value: Boolean) {
        ensureLoaded()
        if (value) collapsed[modId] = true else collapsed.remove(modId)
        persist()
    }

    private fun persist() {
        try {
            val path = file()
            Files.createDirectories(path.parent)
            val out = collapsed.keys.joinToString("\n")
            Files.write(
                path,
                out.toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to persist collapsed keybind groups", e)
        }
    }
}
