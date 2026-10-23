package org.polyfrost.oneconfig.internal.l10n

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.utils.v1.JsonUtils
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.polyfrost.oneconfig.utils.v1.NetworkUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Function

object CommunityTranslations {
    private const val DISTRIBUTION_HASH = ""
    private const val DISTRIBUTION_URL = "https://distributions.crowdin.net/"
    private const val SOURCE_LANGUAGE = "en_us"
    private val MANIFEST_TTL = TimeUnit.HOURS.toMillis(6)
    private val RETRY_DELAY = TimeUnit.MINUTES.toMillis(10)
    private val LOGGER = LogManager.getLogger("OneConfig/Translations")
    private val SAFE_NAME = Regex("[a-z0-9_-][a-z0-9_.-]*")
    private val LANGUAGE_NAME = Regex("[a-z0-9_]+")
    private val FORMAT_SPECIFIER = Regex("%(?:(\\d+)\\$)?[\\d.]*([A-Za-z%])")
    private val LANGUAGE_ALIASES = mapOf("nb_no" to "no_no")

    private val cacheDir: Path = Paths.get("oneconfig", "translations")
    private val manifestFile: Path = cacheDir.resolve("manifest.json")
    private val refreshLock = Any()

    @Volatile
    private var lastFailure = 0L

    private class Manifest(val timestamp: Long, val files: Map<String, Map<String, String>>)

    @JvmStatic
    fun apply(languages: List<String>, namespaces: Collection<String>, existing: Function<String, String?>, sink: BiConsumer<String, String>) {
        if (DISTRIBUTION_HASH.isEmpty()) return
        runCatching {
            val wanted = languages.map(::normalize).filter { it != SOURCE_LANGUAGE }.distinct()
            if (wanted.isEmpty()) return
            val present = namespaces.toSet()
            val manifest = readManifest()
            if (manifest != null) {
                for (language in wanted) {
                    for (namespace in manifest.files[language]?.keys.orEmpty()) {
                        if (namespace !in present) continue
                        readTranslations(language, namespace, manifest.timestamp)?.forEach { (key, value) ->
                            val current = existing.apply(key)
                            if (current == null || arity(current) == arity(value)) sink.accept(key, value)
                        }
                    }
                }
            }
            if (!isUpToDate(manifest, wanted, present) && System.currentTimeMillis() - lastFailure >= RETRY_DELAY) {
                Multithreading.submit {
                    if (synchronized(refreshLock) { refresh(wanted, present) }) reloadLanguages()
                }
            }
        }.onFailure { LOGGER.warn("Failed to apply community translations", it) }
    }

    private fun isUpToDate(manifest: Manifest?, languages: List<String>, namespaces: Set<String>): Boolean {
        if (manifest == null || manifestAge() >= MANIFEST_TTL) return false
        return languages.all { language ->
            manifest.files[language].orEmpty().keys.all { it !in namespaces || Files.isRegularFile(cachedFile(language, it, manifest.timestamp)) }
        }
    }

    private fun refresh(languages: List<String>, namespaces: Set<String>): Boolean {
        val previous = readManifest()
        if (isUpToDate(previous, languages, namespaces)) return false
        var manifest = previous
        if (manifest == null || manifestAge() >= MANIFEST_TTL) {
            val text = NetworkUtils.getString("$DISTRIBUTION_URL$DISTRIBUTION_HASH/manifest.json")
            val fetched = text?.let(::parseManifest)
            if (text == null || fetched == null || !write(manifestFile, text)) {
                lastFailure = System.currentTimeMillis()
                return false
            }
            manifest = fetched
        }
        var changed = previous != null && previous.timestamp != manifest.timestamp
        var failed = false
        for (language in languages) {
            for ((namespace, path) in manifest.files[language].orEmpty()) {
                if (namespace !in namespaces) continue
                val target = cachedFile(language, namespace, manifest.timestamp)
                if (Files.isRegularFile(target)) continue
                val text = NetworkUtils.getString("$DISTRIBUTION_URL$DISTRIBUTION_HASH$path?timestamp=${manifest.timestamp}")
                if (text != null && JsonUtils.parseOrNull(text) is JsonObject && write(target, text)) {
                    pruneOlder(target)
                    changed = true
                } else {
                    failed = true
                }
            }
        }
        if (failed) lastFailure = System.currentTimeMillis()
        return changed
    }

    private fun reloadLanguages() {
        val mc = Minecraft.getInstance()
        //? if > 1.8.9 {
        mc.execute { mc.languageManager.onResourceManagerReload(mc.resourceManager) }
        //?} else
        //mc.executeTask(Runnable { mc.languageManager.reload(mc.resourceManager) })
    }

    private fun parseManifest(text: String): Manifest? {
        val json = JsonUtils.parseOrNull(text) as? JsonObject ?: return null
        val timestamp = json.get("timestamp")?.takeIf { it.isJsonPrimitive }?.asLong ?: return null
        val content = json.get("content") as? JsonObject ?: return null
        val files = HashMap<String, MutableMap<String, String>>()
        for ((key, value) in content.entrySet()) {
            for (element in value as? JsonArray ?: continue) {
                val path = element.takeIf { it.isJsonPrimitive }?.asString ?: continue
                if (!path.startsWith("/") || !path.endsWith(".json")) continue
                val segments = path.split('/')
                val namespace = segments.last().removeSuffix(".json")
                if (!SAFE_NAME.matches(namespace)) continue
                val languages = listOfNotNull(segments.getOrNull(segments.size - 2), key).map(::normalize)
                for (language in languages.distinct()) {
                    if (LANGUAGE_NAME.matches(language)) files.getOrPut(language) { HashMap() }.putIfAbsent(namespace, path)
                }
            }
        }
        return Manifest(timestamp, files)
    }

    private fun readManifest(): Manifest? {
        if (!Files.isRegularFile(manifestFile)) return null
        return runCatching { parseManifest(String(Files.readAllBytes(manifestFile), StandardCharsets.UTF_8)) }.getOrNull()
    }

    private fun manifestAge(): Long =
        runCatching { System.currentTimeMillis() - Files.getLastModifiedTime(manifestFile).toMillis() }.getOrDefault(Long.MAX_VALUE)

    private fun readTranslations(language: String, namespace: String, timestamp: Long): Map<String, String>? {
        val file = cachedFile(language, namespace, timestamp).takeIf { Files.isRegularFile(it) }
            ?: newestCached(language, namespace)
            ?: return null
        return runCatching {
            val json = JsonUtils.parseOrNull(String(Files.readAllBytes(file), StandardCharsets.UTF_8)) as? JsonObject ?: return null
            json.entrySet().mapNotNull { (key, value) ->
                value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.let { key to it.asString }
            }.toMap()
        }.onFailure { LOGGER.warn("Failed to read translations from {}", file, it) }.getOrNull()
    }

    private fun arity(text: String): Int {
        var sequential = 0
        var highest = 0
        for (match in FORMAT_SPECIFIER.findAll(text)) {
            val conversion = match.groupValues[2]
            if (conversion == "%" || conversion == "n") continue
            val index = match.groupValues[1].toIntOrNull()
            if (index != null) highest = maxOf(highest, index) else sequential++
        }
        return maxOf(highest, sequential)
    }

    private fun cachedFile(language: String, namespace: String, timestamp: Long): Path =
        cacheDir.resolve(language).resolve(namespace).resolve("$timestamp.json")

    private fun releaseOf(file: Path): Long? = file.fileName.toString().removeSuffix(".json").toLongOrNull()

    private fun newestCached(language: String, namespace: String): Path? = runCatching {
        Files.list(cacheDir.resolve(language).resolve(namespace)).use { files ->
            files.filter { releaseOf(it) != null }.max(compareBy { releaseOf(it) }).orElse(null)
        }
    }.getOrNull()

    private fun pruneOlder(current: Path) {
        runCatching {
            Files.list(current.parent).use { files ->
                files.filter { it != current && releaseOf(it) != null }.forEach(Files::deleteIfExists)
            }
        }.onFailure { LOGGER.warn("Failed to prune old translations in {}", current.parent, it) }
    }

    private fun write(target: Path, text: String): Boolean {
        var temp: Path? = null
        return runCatching {
            Files.createDirectories(target.parent)
            val file = Files.createTempFile(target.parent, target.fileName.toString(), ".tmp").also { temp = it }
            Files.write(file, text.toByteArray(StandardCharsets.UTF_8))
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.onFailure {
            LOGGER.warn("Failed to write {}", target, it)
            temp?.let { file -> runCatching { Files.deleteIfExists(file) } }
        }.isSuccess
    }

    private fun normalize(language: String): String =
        language.lowercase(Locale.ROOT).replace('-', '_').let { LANGUAGE_ALIASES[it] ?: it }
}
