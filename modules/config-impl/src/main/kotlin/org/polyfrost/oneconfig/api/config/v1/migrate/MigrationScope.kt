package org.polyfrost.oneconfig.api.config.v1.migrate

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.NullObject
import com.electronwill.nightconfig.json.JsonFormat
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.annotations.ApiStatus
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@ApiStatus.Experimental
public class MigrationScope internal constructor(
    public val modId: String,
    public val version: Int,
    public val profileFolder: Path,
) {
    public fun resolve(path: String): Path {
        val target = profileFolder.resolve(path).normalize()
        require(target.startsWith(profileFolder)) { "$path is outside the profile folder" }
        return target
    }

    public fun exists(path: String): Boolean = Files.isRegularFile(resolve(path))

    public fun listFiles(directory: String = "", glob: String = "*"): List<String> {
        val dir = resolve(directory)
        if (!Files.isDirectory(dir)) return emptyList()
        val out = ArrayList<String>()
        Files.newDirectoryStream(dir, glob).use { stream ->
            for (file in stream) {
                if (Files.isRegularFile(file)) out += profileFolder.relativize(file).joinToString("/")
            }
        }
        out.sort()
        return out
    }

    public fun readJson(path: String): MutableMap<String, Any?>? {
        val text = readText(path) ?: return null
        return try {
            MigrationJson.parse(text)
        } catch (t: Throwable) {
            LOGGER.warn("{} migration {}: {} is not a JSON object, treating it as missing", modId, version, path, t)
            null
        }
    }

    public fun writeJson(path: String, json: Map<String, Any?>) {
        writeText(path, MigrationJson.write(json))
    }

    public fun editJson(path: String, create: Boolean = false, edit: MutableMap<String, Any?>.() -> Unit): Boolean {
        val json = readJson(path) ?: if (create) LinkedHashMap<String, Any?>() else return false
        json.edit()
        writeJson(path, json)
        return true
    }

    public fun renameKey(path: String, from: String, to: String): Boolean {
        val json = readJson(path) ?: return false
        if (from !in json) return false
        json[to] = json.remove(from)
        writeJson(path, json)
        return true
    }

    public fun moveKey(fromPath: String, key: String, toPath: String, toKey: String = key): Boolean {
        if (resolve(fromPath) == resolve(toPath)) return renameKey(fromPath, key, toKey)
        val source = readJson(fromPath) ?: return false
        if (key !in source) return false
        val value = source.remove(key)
        val target = readJson(toPath) ?: LinkedHashMap<String, Any?>()
        target[toKey] = value
        writeJson(toPath, target)
        writeJson(fromPath, source)
        return true
    }

    public fun removeKeys(path: String, vararg keys: String): Boolean {
        val json = readJson(path) ?: return false
        var changed = false
        for (key in keys) {
            if (key !in json) continue
            json.remove(key)
            changed = true
        }
        if (changed) writeJson(path, json)
        return changed
    }

    public fun readText(path: String): String? {
        val file = resolve(path)
        if (!Files.isRegularFile(file)) return null
        return String(Files.readAllBytes(file), Charsets.UTF_8)
    }

    public fun writeText(path: String, text: String) {
        MigrationFiles.write(resolve(path), text)
    }

    public fun moveFile(from: String, to: String, replace: Boolean = false): Boolean {
        val source = resolve(from)
        val target = resolve(to)
        if (!Files.isRegularFile(source) || source == target) return false
        if (!replace && Files.exists(target)) return false
        target.parent?.let { Files.createDirectories(it) }
        if (replace) Files.move(source, target, StandardCopyOption.REPLACE_EXISTING) else Files.move(source, target)
        return true
    }

    public fun deleteFile(path: String): Boolean {
        val file = resolve(path)
        if (!Files.isRegularFile(file)) return false
        return Files.deleteIfExists(file)
    }

    private companion object {
        val LOGGER: Logger = LogManager.getLogger("OneConfig/Migrations")
    }
}

internal object MigrationFiles {
    fun write(file: Path, text: String) {
        file.parent?.let { Files.createDirectories(it) }
        val temp = file.resolveSibling(file.fileName.toString() + ".migrating")
        Files.write(temp, text.toByteArray(Charsets.UTF_8))
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

internal object MigrationJson {
    private val format = JsonFormat.fancyInstance()

    fun parse(text: String): MutableMap<String, Any?> = fromConfig(format.createParser().parse(text))

    fun write(json: Map<String, Any?>): String = format.createWriter().writeToString(toConfig(json))

    private fun fromConfig(config: Config): MutableMap<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for (entry in config.entrySet()) out[entry.key] = fromValue(entry.getRawValue<Any?>())
        return out
    }

    private fun fromValue(value: Any?): Any? = when (value) {
        null, NullObject.NULL_OBJECT -> null
        is Config -> fromConfig(value)
        is List<*> -> value.mapTo(ArrayList()) { fromValue(it) }
        else -> value
    }

    private fun toConfig(json: Map<*, *>): Config {
        val config = Config.inMemory()
        for ((key, value) in json) config.set<Any?>(listOf(key.toString()), toValue(value))
        return config
    }

    private fun toValue(value: Any?): Any = when (value) {
        null -> NullObject.NULL_OBJECT
        is Map<*, *> -> toConfig(value)
        is Iterable<*> -> value.map { toValue(it) }
        is Array<*> -> value.map { toValue(it) }
        is Number, is Boolean, is String -> value
        is Char -> value.toString()
        is Enum<*> -> value.name
        else -> throw IllegalArgumentException("Cannot write ${value.javaClass.name} as JSON")
    }
}
