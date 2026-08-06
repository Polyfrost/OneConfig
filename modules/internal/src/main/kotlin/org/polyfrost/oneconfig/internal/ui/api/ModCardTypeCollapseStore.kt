package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.ui.v1.ModCardTypes
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object ModCardTypeCollapseStore {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/ModCardTypes")
    private const val FILE_NAME = "mod-card-type-collapsed"

    private val overrides = mutableStateMapOf<String, Boolean>()
    private var loaded = false

    var revision by mutableIntStateOf(0)
        private set

    private fun file(): Path = ConfigManager.internal().folder.resolve(FILE_NAME)

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            val path = file()
            if (Files.exists(path)) {
                Files.readAllLines(path, StandardCharsets.UTF_8)
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .forEach { line ->
                        val id = line.substringBefore('=').trim()
                        if (id.isEmpty()) return@forEach
                        val value = if ('=' in line) line.substringAfter('=').trim().toBoolean() else true
                        overrides[id] = value
                    }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load collapsed mod card types", e)
        }
    }

    private fun defaultCollapsed(typeId: String) = ModCardTypes.byId(typeId)?.collapsedByDefault ?: false

    fun isCollapsed(typeId: String): Boolean {
        ensureLoaded()
        return overrides[typeId] ?: defaultCollapsed(typeId)
    }

    fun setCollapsed(typeId: String, value: Boolean) {
        ensureLoaded()
        val changed = if (value == defaultCollapsed(typeId)) {
            overrides.remove(typeId) != null
        } else {
            overrides.put(typeId, value) != value
        }
        if (!changed) return
        revision++
        persist()
    }

    fun toggle(typeId: String) = setCollapsed(typeId, !isCollapsed(typeId))

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
            LOGGER.error("Failed to persist collapsed mod card types", e)
        }
    }
}
