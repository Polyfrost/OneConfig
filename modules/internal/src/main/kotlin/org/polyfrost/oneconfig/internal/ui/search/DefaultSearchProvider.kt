package org.polyfrost.oneconfig.internal.ui.search

import org.polyfrost.oneconfig.internal.OneConfigConfig

internal object DefaultSearchProvider : SearchProvider {
    override val priority: Int = 0 // Low priority

    override fun isAvailable(): Boolean = true

    override fun search(
        query: String,
        scopes: Set<SearchScope>
    ): List<SearchDocument<*>> {
        val corpus = SearchCorpus.corpus
        val q = query.trim().lowercase()
        return corpus.values.filter {
            if (it.scopes.intersect(scopes).isEmpty()) return@filter false
            if (it.scopes.contains(SearchScope.Mods)) {
                return@filter it.metadata.title != null && searchMatches(it.metadata.title, q)
            }
            if (listOfNotNull(it.metadata.title, it.metadata.description).any { p ->
                    searchMatches(p, q)
                } || it.metadata.tags.any { t -> searchMatches(t, q) }) return@filter true
            // Match old search for keybinds
            if (scopes.contains(SearchScope.Keybinds) && listOfNotNull(
                    it.metadata.category,
                    it.metadata.distinctSubcategory,
                    it.metadata.id, it.metadata.path
                ).any { k -> searchMatches(k, q) }
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

    override suspend fun onCorpusUpdate(
        added: List<SearchDocument<*>>,
        removed: Set<String>
    ) {
        // No-op, we just use the corpus directly
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