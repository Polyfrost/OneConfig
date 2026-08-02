package org.polyfrost.oneconfig.internal.ui.search

import net.kyori.adventure.text.ComponentLike
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData
import org.polyfrost.oneconfig.internal.ui.components.asRenderText

internal object DefaultSearchProvider : SearchProvider {
    override val priority: Int = 0 // Low priority

    override fun isAvailable(): Boolean = true

    override fun performSearch(
        query: String,
        configs: List<ConfigData>,
        searchMods: Boolean
    ): Map<String, List<SearchResult>> {
        if (query.isBlank()) return emptyMap()
        val q = query.trim().lowercase()
        val results = LinkedHashMap<String, MutableList<SearchResult>>()

        val matchingMods = configs.filter { searchMatches(it.title.asRenderText(), q) }
        if (matchingMods.isNotEmpty()) {
            results["Mods"] = matchingMods.map { ModResult(it) }.toMutableList()
        }

        for (configData in configs) {
            val tree = (configData as? TreeConfigData)?.tree ?: continue
            val matchingOptions = mutableListOf<SearchResult>()
            tree.map.values.forEach { node ->
                val descriptionMatches = node.description?.asRenderText()?.let { searchMatches(it, q) } == true
                val searchTags = node.metadata?.get("searchTags")?.let {
                    if (it is Iterable<*>) it.mapNotNull {
                        if (it !is String && it !is ComponentLike) return@mapNotNull null
                        it.asRenderText()
                    } else if (it is String) listOf(it) else listOf()
                }?.any { searchMatches(it, q) } == true
                when (node) {
                    is Property<*> -> {
                        val title = node.title ?: return@forEach
                        if (searchMatches(title.asRenderText(), q) || descriptionMatches || searchTags) {
                            val cat = node.getMetadata<String>("category")
                            matchingOptions += OptionResult(
                                configData.id,
                                configData.title,
                                title,
                                cat,
                                configData.icon,
                                node
                            )
                        }
                    }

                    is Tree -> {
                        val subTitle = node.title
                        if (subTitle != null && searchMatches(subTitle.asRenderText(), q)) {
                            val cat = node.getMetadata<String>("category")
                            matchingOptions += OptionResult(
                                configData.id,
                                configData.title,
                                subTitle,
                                cat,
                                configData.icon,
                                null
                            )
                        }
                        node.map.values.filterIsInstance<Property<*>>().forEach { prop ->
                            val pt = prop.title ?: return@forEach
                            if (searchMatches(pt.asRenderText(), q) || descriptionMatches || searchTags) {
                                val cat = prop.getMetadata<String>("category")
                                matchingOptions += OptionResult(
                                    configData.id,
                                    configData.title,
                                    pt,
                                    cat,
                                    configData.icon,
                                    prop
                                )
                            }
                        }
                    }
                }
            }
            if (matchingOptions.isNotEmpty()) {
                results[configData.title.asRenderText()] = matchingOptions
            }
        }

        return results
    }
}

/**
 * Computes the Levenshtein (edit) distance between two strings, capped early once it exceeds [max].
 */
private fun levenshtein(a: String, b: String, max: Int): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    if (kotlin.math.abs(a.length - b.length) > max) return max + 1
    var prev = IntArray(b.length + 1) { it }
    var curr = IntArray(b.length + 1)
    for (i in 1..a.length) {
        curr[0] = i
        var rowMin = curr[0]
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            if (curr[j] < rowMin) rowMin = curr[j]
        }
        if (rowMin > max) return max + 1
        val tmp = prev; prev = curr; curr = tmp
    }
    return prev[b.length]
}

/**
 * Returns true if [text] matches [q] either as a substring or, when "Search Distance" > 0, by a fuzzy
 * (Levenshtein) match against the whole string or any of its words. [q] is expected to be lowercase.
 */
internal fun searchMatches(text: String, q: String): Boolean {
    val t = text.lowercase()
    if (t.contains(q)) return true
    val dist = OneConfigConfig.searchDistance
    if (dist <= 0) return false
    if (levenshtein(t, q, dist) <= dist) return true
    return t.split(' ', '.', '_', '-', '/').any { it.isNotEmpty() && levenshtein(it, q, dist) <= dist }
}