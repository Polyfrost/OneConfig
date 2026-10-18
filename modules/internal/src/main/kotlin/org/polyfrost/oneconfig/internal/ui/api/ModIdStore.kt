package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicLong
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.slf4j.LoggerFactory

abstract class ModIdStore(private val fileName: String) {
    private val logger = LoggerFactory.getLogger("OneConfig/${javaClass.simpleName}")

    private val ids = mutableStateSetOf<String>()
    private var loaded = false

    var revision by mutableIntStateOf(0)
        private set

    private fun file(): Path = ConfigManager.internal().folder.resolve(fileName)

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            val path = file()
            if (Files.exists(path)) {
                Files.readAllLines(path, StandardCharsets.UTF_8)
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .forEach(ids::add)
            }
        } catch (e: Exception) {
            logger.error("Failed to load {}", fileName, e)
        }
    }

    operator fun contains(id: String): Boolean {
        ensureLoaded()
        return id in ids
    }

    fun toggle(id: String) {
        ensureLoaded()
        if (!ids.remove(id)) ids.add(id)
        revision++
        persist()
    }

    private val writeSeq = AtomicLong()

    private val writeLock = Any()

    private fun persist() {
        val bytes = ids.joinToString("\n").toByteArray(StandardCharsets.UTF_8)
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
                    logger.error("Failed to persist {}", fileName, e)
                }
            }
        }
    }
}
