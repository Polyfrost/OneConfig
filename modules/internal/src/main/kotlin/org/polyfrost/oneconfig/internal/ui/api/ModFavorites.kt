package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicLong

object ModFavorites {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/ModFavorites")
    private const val FILE_NAME = "favorite-mods"

    private val favorites = mutableStateSetOf<String>()
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
                    .forEach(favorites::add)
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load favorite mods", e)
        }
    }

    fun isFavorite(id: String): Boolean {
        ensureLoaded()
        return id in favorites
    }

    fun toggle(id: String) {
        ensureLoaded()
        if (!favorites.remove(id)) favorites.add(id)
        revision++
        persist()
    }

    private val writeSeq = AtomicLong()

    private val writeLock = Any()

    /**
     * Writes the file off the render thread
     *
     * The list is joined here, on the thread that owns it, so only finished bytes cross over and a
     * star click never puts a disk write in the middle of the frame it happened in.
     */
    private fun persist() {
        val bytes = favorites.joinToString("\n").toByteArray(StandardCharsets.UTF_8)
        val seq = writeSeq.incrementAndGet()
        Multithreading.submit {
            synchronized(writeLock) {
                if (seq != writeSeq.get()) return@submit
                try {
                    val path = file()
                    Files.createDirectories(path.parent)
                    Files.write(
                        path,
                        bytes,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                    )
                } catch (e: Exception) {
                    LOGGER.error("Failed to persist favorite mods", e)
                }
            }
        }
    }
}
