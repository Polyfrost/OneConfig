package org.polyfrost.oneconfig.internal.ui.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.commonmark.node.Code
import org.commonmark.node.Heading
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.polyfrost.oneconfig.utils.v1.JsonUtils
import org.polyfrost.oneconfig.utils.v1.NetworkUtils
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class ModrinthVersionLink(val project: String, val version: String) {
    val pageUrl: String get() = "https://modrinth.com/project/${encode(project)}/version/${encode(version)}"
    val apiUrl: String get() = "https://api.modrinth.com/v2/project/${encode(project)}/version/${encode(version)}"
    val projectApiUrl: String get() = "https://api.modrinth.com/v2/project/${encode(project)}"

    private fun encode(part: String): String = URLEncoder.encode(part, "UTF-8").replace("+", "%20")
}

class ModrinthChangelog(val title: String, val document: Node)

object ModrinthChangelogs {
    private val logger = LoggerFactory.getLogger("OneConfig/Changelog")
    private const val USER_AGENT = "Polyfrost/OneConfig (polyfrost.org)"
    private const val TIMEOUT_MS = 10_000
    private val versionLink = Regex("""^https?://(?:www\.)?modrinth\.com/[^/?#]+/([^/?#]+)/version/([^/?#]+)/?(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    private val cache = ConcurrentHashMap<ModrinthVersionLink, ModrinthChangelog>()
    private val projectTitles = ConcurrentHashMap<String, String>()

    fun parseLink(url: String): ModrinthVersionLink? {
        val match = versionLink.matchEntire(url.trim()) ?: return null
        val version = decode(match.groupValues[2])
        if (version.equals("latest", ignoreCase = true)) return null
        return ModrinthVersionLink(decode(match.groupValues[1]), version)
    }

    fun cached(link: ModrinthVersionLink): ModrinthChangelog? = cache[link]

    suspend fun load(link: ModrinthVersionLink): ModrinthChangelog? {
        cache[link]?.let { return it }
        return try {
            withContext(Dispatchers.IO) {
                val text = NetworkUtils.getString(link.apiUrl, USER_AGENT, TIMEOUT_MS, false) ?: return@withContext null
                val root = JsonUtils.parseOrNull(text)?.takeIf { it.isJsonObject }?.asJsonObject ?: return@withContext null
                fun string(key: String): String? = root.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() }
                val version = string("version_number") ?: string("name") ?: link.version
                val title = projectTitle(link)?.let { "$it $version" } ?: version
                val changelog = root.get("changelog")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                val document = Parser.builder().build().parse(changelog)
                (document.firstChild as? Heading)?.takeIf { heading ->
                    val text = plainText(heading).trim().removePrefix("v")
                    text == version.removePrefix("v") || text == string("name")?.removePrefix("v")
                }?.unlink()
                ModrinthChangelog(title, document).also { cache[link] = it }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to load the Modrinth changelog for {}", link.apiUrl, e)
            null
        }
    }

    private fun projectTitle(link: ModrinthVersionLink): String? {
        projectTitles[link.project]?.let { return it }
        val text = NetworkUtils.getString(link.projectApiUrl, USER_AGENT, TIMEOUT_MS, false) ?: return null
        val title = JsonUtils.parseOrNull(text)?.takeIf { it.isJsonObject }?.asJsonObject?.get("title")
            ?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        projectTitles[link.project] = title
        return title
    }

    private fun plainText(node: Node): String = buildString {
        fun walk(n: Node) {
            if (n is Text) append(n.literal) else if (n is Code) append(n.literal)
            var c = n.firstChild
            while (c != null) { walk(c); c = c.next }
        }
        walk(node)
    }

    private fun decode(part: String): String = runCatching { URLDecoder.decode(part.replace("+", "%2B"), "UTF-8") }.getOrDefault(part)
}
