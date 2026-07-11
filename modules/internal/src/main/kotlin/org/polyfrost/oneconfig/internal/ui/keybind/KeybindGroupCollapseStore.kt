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

    private const val MINECRAFT_PREFIX = "minecraft-keybinds."

    private val explicit = mutableStateMapOf<String, Boolean>()
    private var loaded = false

    private fun file(): Path = ConfigManager.internal().folder.resolve(FILE_NAME)

    private fun defaultCollapsed(modId: String): Boolean = modId.startsWith(MINECRAFT_PREFIX)

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            val path = file()
            if (Files.exists(path)) {
                Files.readAllLines(path, StandardCharsets.UTF_8).forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@forEach
                    val sep = trimmed.lastIndexOf('=')
                    val id = if (sep >= 0) trimmed.substring(0, sep) else trimmed
                    val collapsed = if (sep >= 0) trimmed.substring(sep + 1).toBooleanStrictOrNull() ?: true else true
                    if (id.isNotEmpty() && collapsed != defaultCollapsed(id)) explicit[id] = collapsed
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load collapsed keybind groups", e)
        }
    }

    fun isCollapsed(modId: String): Boolean {
        ensureLoaded()
        return explicit[modId] ?: defaultCollapsed(modId)
    }

    fun setCollapsed(modId: String, value: Boolean) {
        ensureLoaded()
        if (value == defaultCollapsed(modId)) explicit.remove(modId) else explicit[modId] = value
        persist()
    }

    private fun persist() {
        try {
            val path = file()
            Files.createDirectories(path.parent)
            val out = explicit.entries.joinToString("\n") { "${it.key}=${it.value}" }
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
