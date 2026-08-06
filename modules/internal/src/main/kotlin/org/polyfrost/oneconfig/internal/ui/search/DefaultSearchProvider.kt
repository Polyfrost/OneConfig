package org.polyfrost.oneconfig.internal.ui.search

import org.polyfrost.oneconfig.internal.OneConfigConfig

internal object DefaultSearchProvider : SearchProvider {
    override val priority: Int = 0 // Low priority

    override fun isAvailable(): Boolean = true

    override fun search(
        query: String,
        scopes: Set<SearchScope>
    ): List<SearchDocument<*>> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val searchingKeybinds = SearchScope.Keybinds in scopes
        return SearchCorpus.corpus.values.filter {
            if (scopes.none { s -> s in it.scopes }) {
                return@filter false
            }

            val meta = it.metadata
            if (it.scopes.contains(SearchScope.Mods)) {
                return@filter meta.title.matches(q) || meta.tags.any { t -> t.matches(q) }
            }
            if (meta.title.matches(q) || meta.description.matches(q)) return@filter true
            if (meta.tags.any { t -> t.matches(q) }) return@filter true
            // Hud specific
            if (SearchScope.Huds in it.scopes && (
                        meta.category.matches(q) || meta.subcategory.matches(q) ||
                                meta.id.matches(q) || meta.modTitle.matches(q)
                        )
            ) return@filter true
            // Match old search for keybinds
            if (searchingKeybinds && (
                        meta.category.matches(q) || meta.subcategory.matches(q) ||
                                meta.id.matches(q) || meta.path.matches(q)
                        )
            ) return@filter true
            false
        }
    }

    override fun <T> searchGrouped(
        query: String,
        scopes: Set<SearchScope>,
        grouper: (SearchDocument<*>) -> T
    ): Map<T, List<SearchDocument<*>>> {
        return search(query, scopes).groupBy(grouper)
    }

    private fun String?.matches(query: String): Boolean = this != null && searchMatches(this, query)
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
 * Returns true if [text] matches [query] either as a substring or, when "Search Distance" > 0, by a fuzzy
 * (Levenshtein) match against the whole string or any of its words.
 */
internal fun searchMatches(text: String, query: String): Boolean {
    val q = query.lowercase()
    val t = text.lowercase()
    if (t.contains(q)) return true
    val dist = OneConfigConfig.searchDistance
    if (dist <= 0) return false
    if (levenshtein(t, q, dist) <= dist) return true
    return t.split(' ', '.', '_', '-', '/').any { it.isNotEmpty() && levenshtein(it, q, dist) <= dist }
}