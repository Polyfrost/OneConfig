package org.polyfrost.oneconfig.internal.ui.api

import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Heading
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

enum class ChangeKind(val heading: String) {
    ADDED("Added"),
    CHANGED("Changed"),
    DEPRECATED("Deprecated"),
    REMOVED("Removed"),
    FIXED("Fixed"),
    SECURITY("Security");

    val key: String get() = "oneconfig.changelog.kind.${name.lowercase()}"
}

class ChangelogEntry(val paragraph: Paragraph?, val text: String, val forDevelopers: Boolean, val children: List<ChangelogEntry>)

class ChangelogGroup(val kind: ChangeKind, val entries: List<ChangelogEntry>)

class ChangelogRelease(val version: String, val date: String?, val groups: List<ChangelogGroup>) {
    val searchText: String by lazy {
        buildString {
            append(version).append(' ').append(date.orEmpty())
            fun add(entries: List<ChangelogEntry>): Unit = entries.forEach { append(' ').append(it.text); add(it.children) }
            groups.forEach { add(it.entries) }
        }
    }
}

object ChangelogData {
    private val logger = LoggerFactory.getLogger("OneConfig/Changelog")
    private const val RESOURCE = "/assets/oneconfig/CHANGELOG.md"
    private const val DEVELOPER_PREFIX = "API: "
    private val releaseHeading = Regex("""^\[([^]]+)](?:\s*-\s*(\S+))?""")

    val releases: List<ChangelogRelease> by lazy {
        val text = runCatching {
            ChangelogData::class.java.getResourceAsStream(RESOURCE)?.use { String(it.readBytes(), StandardCharsets.UTF_8) }
        }.onFailure { logger.error("Failed to read the bundled changelog", it) }.getOrNull()
        if (text == null) emptyList() else parse(text)
    }

    fun parse(markdown: String): List<ChangelogRelease> {
        val releases = mutableListOf<ChangelogRelease>()
        var version: String? = null
        var date: String? = null
        var groups = mutableListOf<ChangelogGroup>()
        var kind: ChangeKind? = null

        fun finish() {
            val v = version ?: return
            if (!v.equals("Unreleased", ignoreCase = true) && groups.isNotEmpty()) releases += ChangelogRelease(v, date, groups)
        }

        var node = Parser.builder().build().parse(markdown).firstChild
        while (node != null) {
            when {
                node is Heading && node.level == 2 -> {
                    finish()
                    val match = releaseHeading.find(plainText(node).trim())
                    version = match?.groupValues?.get(1)
                    date = match?.groupValues?.get(2)?.takeIf { it.isNotEmpty() }
                    groups = mutableListOf()
                    kind = null
                }
                node is Heading && node.level == 3 -> {
                    val title = plainText(node).trim()
                    kind = ChangeKind.entries.firstOrNull { it.heading.equals(title, ignoreCase = true) }
                }
                node is BulletList && version != null -> {
                    val k = kind
                    if (k != null) {
                        val entries = entries(node)
                        val existing = groups.indexOfFirst { it.kind == k }
                        if (existing >= 0) groups[existing] = ChangelogGroup(k, groups[existing].entries + entries)
                        else groups += ChangelogGroup(k, entries)
                    }
                }
            }
            node = node.next
        }
        finish()
        return releases
    }

    private fun entries(list: BulletList): List<ChangelogEntry> = children(list).filterIsInstance<ListItem>().map { item ->
        val paragraph = item.firstChild as? Paragraph
        val first = paragraph?.firstChild as? Text
        val forDevelopers = first?.literal?.startsWith(DEVELOPER_PREFIX) == true
        if (forDevelopers) first.literal = first.literal.removePrefix(DEVELOPER_PREFIX)
        val nested = children(item).filterIsInstance<BulletList>().flatMap(::entries)
        ChangelogEntry(paragraph, paragraph?.let(::plainText).orEmpty(), forDevelopers, nested)
    }

    private fun children(node: Node): List<Node> = buildList {
        var c = node.firstChild
        while (c != null) { add(c); c = c.next }
    }

    private fun plainText(node: Node): String = buildString {
        fun walk(n: Node) {
            when (n) {
                is Text -> append(n.literal)
                is Code -> append(n.literal)
                else -> children(n).forEach(::walk)
            }
        }
        walk(node)
    }
}
