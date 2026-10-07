package org.polyfrost.oneconfig.internal.ui.api

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicLong
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.ui.v1.ModToggle
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.slf4j.LoggerFactory

/** Root level option names treated as a mod's master switch, compared without case or an "is" prefix */
private val MASTER_SWITCH_NAMES = setOf("enabled", "enable", "modenabled")

private fun masterSwitchName(id: String): String =
    id.filter(Char::isLetterOrDigit).lowercase().let { if (it.startsWith("is")) it.substring(2) else it }

/**
 * Finds the boolean option that switches the whole mod on and off
 *
 * Only direct children of the tree are considered so the switch of a single feature or category
 * (which lives in a child tree) is never mistaken for the mod's own
 */
@Suppress("UNCHECKED_CAST")
fun Tree.findMasterSwitch(): Property<Boolean>? {
    val candidates = map.values.filterIsInstance<Property<*>>().filter {
        it.type == Boolean::class.javaPrimitiveType || it.type == java.lang.Boolean::class.java
    }
    for (name in MASTER_SWITCH_NAMES) {
        candidates.firstOrNull { masterSwitchName(it.id ?: "") == name }?.let { return it as Property<Boolean> }
    }
    return null
}

class PropertyModToggle(val property: Property<Boolean>) : ModToggle {
    override fun isEnabled(): Boolean = property.get() != false

    override fun setEnabled(enabled: Boolean) {
        property.set(enabled)
    }
}

object ModToggleStore : ModToggles.Store {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/ModToggles")
    private const val FILE_NAME = "disabled-mods"

    private var installed = false

    private val writeSeq = AtomicLong()

    private val writeLock = Any()

    private fun file(): Path = ConfigManager.internal().folder.resolve(FILE_NAME)

    @JvmStatic
    @Synchronized
    fun install() {
        if (installed) return
        installed = true
        ModToggles.setStore(this)
    }

    override fun load(): Set<String> {
        val path = file()
        if (!Files.exists(path)) return emptySet()
        return Files.readAllLines(path, StandardCharsets.UTF_8)
            .map(String::trim)
            .filterTo(HashSet(), String::isNotEmpty)
    }

    override fun save(disabled: Set<String>) {
        val bytes = disabled.sorted().joinToString("\n").toByteArray(StandardCharsets.UTF_8)
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
                    LOGGER.error("Failed to persist disabled mods", e)
                }
            }
        }
    }
}
