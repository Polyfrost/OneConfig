package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private val LOGGER = LoggerFactory.getLogger("OneConfig/UI")

/** Persists the groups whose collapsed state the user changed away from [defaultCollapsed] */
abstract class CollapseStore(private val fileName: String, private val description: String) {
    private val overrides = mutableStateMapOf<String, Boolean>()
    private var loaded = false

    var revision by mutableIntStateOf(0)
        private set

    protected abstract fun defaultCollapsed(id: String): Boolean

    private fun file(): Path = ConfigManager.internal().folder.resolve(fileName)

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            val path = file()
            if (Files.exists(path)) {
                Files.readAllLines(path, StandardCharsets.UTF_8).map(String::trim).forEach { line ->
                    val id = line.substringBeforeLast('=')
                    if (id.isNotEmpty()) overrides[id] = line.substringAfterLast('=', "true").toBooleanStrictOrNull() ?: true
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load collapsed {}", description, e)
        }
    }

    fun isCollapsed(id: String): Boolean {
        ensureLoaded()
        return overrides[id] ?: defaultCollapsed(id)
    }

    fun setCollapsed(id: String, value: Boolean) {
        ensureLoaded()
        val changed = if (value == defaultCollapsed(id)) {
            overrides.remove(id) != null
        } else {
            overrides.put(id, value) != value
        }
        if (!changed) return
        revision++
        persist()
    }

    fun toggle(id: String) = setCollapsed(id, !isCollapsed(id))

    private fun persist() {
        try {
            val path = file()
            Files.createDirectories(path.parent)
            Files.write(
                path,
                overrides.entries.joinToString("\n") { "${it.key}=${it.value}" }.toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to persist collapsed {}", description, e)
        }
    }
}
