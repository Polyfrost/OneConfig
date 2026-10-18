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

object CommunityTranslations {
    private const val DISTRIBUTION_HASH = ""
    private const val DISTRIBUTION_URL = "https://distributions.crowdin.net/"
    private const val SOURCE_LANGUAGE = "en_us"
    private val MANIFEST_TTL = TimeUnit.HOURS.toMillis(6)
    private val RETRY_DELAY = TimeUnit.MINUTES.toMillis(10)
    private val LOGGER = LogManager.getLogger("OneConfig/Translations")
    private val SAFE_NAME = Regex("[a-z0-9_.-]+")
    private val LANGUAGE_NAME = Regex("[a-z0-9_]+")

    private val cacheDir: Path = Paths.get("oneconfig", "translations")
    private val manifestFile: Path = cacheDir.resolve("manifest.json")
    private val refreshLock = Any()

    @Volatile
    private var lastFailure = 0L

    private class Manifest(val timestamp: Long, val files: Map<String, Map<String, String>>)

    @JvmStatic
    fun apply(languages: List<String>, namespaces: Collection<String>, sink: BiConsumer<String, String>) {
        if (DISTRIBUTION_HASH.isEmpty()) return
        runCatching {
            val wanted = languages.map(::normalize).filter { it != SOURCE_LANGUAGE }.distinct()
            if (wanted.isEmpty()) return
            val present = namespaces.toSet()
            val manifest = readManifest()
            if (manifest != null) {
                for (language in wanted) {
                    for (namespace in manifest.files[language]?.keys.orEmpty()) {
                        if (namespace in present) readTranslations(cachedFile(manifest, language, namespace))?.forEach(sink)
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
            manifest.files[language].orEmpty().keys.all { it !in namespaces || Files.isRegularFile(cachedFile(manifest, language, it)) }
        }
    }

    private fun refresh(languages: List<String>, namespaces: Set<String>): Boolean {
        val previous = readManifest()
        if (isUpToDate(previous, languages, namespaces)) return false
        var manifest = previous
        var manifestText: String? = null
        if (manifest == null || manifestAge() >= MANIFEST_TTL) {
            manifestText = NetworkUtils.getString("$DISTRIBUTION_URL$DISTRIBUTION_HASH/manifest.json")
            manifest = manifestText?.let(::parseManifest)
            if (manifest == null) {
                lastFailure = System.currentTimeMillis()
                return false
            }
        }
        var changed = false
        var failed = false
        for (language in languages) {
            for ((namespace, path) in manifest.files[language].orEmpty()) {
                if (namespace !in namespaces) continue
                val target = cachedFile(manifest, language, namespace)
                if (Files.isRegularFile(target)) continue
                val text = NetworkUtils.getString("$DISTRIBUTION_URL$DISTRIBUTION_HASH$path?timestamp=${manifest.timestamp}")
                if (text != null && JsonUtils.parseOrNull(text) is JsonObject && write(target, text)) {
                    changed = true
                } else {
                    failed = true
                }
            }
        }
        val released = previous != null && previous.timestamp != manifest.timestamp
        if (failed) {
            lastFailure = System.currentTimeMillis()
            if (released) return false
        }
        if (manifestText != null && !write(manifestFile, manifestText)) {
            lastFailure = System.currentTimeMillis()
            return false
        }
        if (!failed) pruneReleases(manifest.timestamp)
        return changed || released
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

    private fun readTranslations(file: Path): Map<String, String>? {
        if (!Files.isRegularFile(file)) return null
        return runCatching {
            val json = JsonUtils.parseOrNull(String(Files.readAllBytes(file), StandardCharsets.UTF_8)) as? JsonObject ?: return null
            json.entrySet().mapNotNull { (key, value) ->
                value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.let { key to it.asString }
            }.toMap()
        }.onFailure { LOGGER.warn("Failed to read translations from {}", file, it) }.getOrNull()
    }

    private fun cachedFile(manifest: Manifest, language: String, namespace: String): Path =
        cacheDir.resolve(manifest.timestamp.toString()).resolve(language).resolve("$namespace.json")

    private fun pruneReleases(current: Long) {
        runCatching {
            Files.list(cacheDir).use { entries ->
                entries.filter { Files.isDirectory(it) && it.fileName.toString() != current.toString() }.forEach { release ->
                    Files.walk(release).use { walk -> walk.sorted(reverseOrder()).forEach(Files::deleteIfExists) }
                }
            }
        }.onFailure { LOGGER.warn("Failed to prune old translation releases", it) }
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

    private fun normalize(language: String): String = language.lowercase(Locale.ROOT).replace('-', '_')
}
