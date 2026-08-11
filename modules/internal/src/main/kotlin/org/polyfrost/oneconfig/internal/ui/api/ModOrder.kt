package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * User-defined mod card order persisted one id per line
 *
 * Mods that were never dragged are unknown here and sort alphabetically behind the ones that were
 */
object ModOrder {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/ModOrder")
    private const val FILE_NAME = "mod-order"

    private val order = ArrayList<String>()
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
                    .filter { it.isNotEmpty() && it !in order }
                    .forEach(order::add)
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load mod order", e)
        }
    }

    fun indexOf(id: String): Int {
        ensureLoaded()
        val index = order.indexOf(id)
        return if (index >= 0) index else Int.MAX_VALUE
    }

    /**
     * Rewrites the order of [visible] while leaving every other mod where it was
     *
     * [all] is the full mod list in currently displayed order and seeds the stored order the first time an
     * unknown mod is dragged
     */
    fun reorder(visible: List<String>, all: List<String>) {
        ensureLoaded()
        all.forEach { if (it !in order) order.add(it) }
        val slots = order.indices.filter { order[it] in visible }
        if (slots.size != visible.size) {
            LOGGER.warn("Mod order slots ({}) did not match visible mods ({})", slots.size, visible.size)
            return
        }
        slots.forEachIndexed { i, slot -> order[slot] = visible[i] }
        revision++
        persist()
    }

    private fun persist() {
        try {
            val path = file()
            Files.createDirectories(path.parent)
            Files.write(
                path,
                order.joinToString("\n").toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to persist mod order", e)
        }
    }
}
