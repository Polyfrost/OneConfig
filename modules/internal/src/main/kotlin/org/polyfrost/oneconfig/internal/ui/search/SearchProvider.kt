package org.polyfrost.oneconfig.internal.ui.search

import org.polyfrost.oneconfig.internal.ui.api.ConfigData


/**
 * A class responsible for searching configs
 */
interface SearchProvider {
    /**
     * The priority this provider has, a higher priority will be used when available
     */
    val priority: Int

    /**
     * Check if this search provider is currently available and ready to be used
     */
    fun isAvailable(): Boolean

    /**
     * Perform the search on the configs
     *
     * @param query The search query
     * @param configs The configs to search in
     * @param searchMods Whether to search include full mods as result, used by global search
     * @return A map of mod name (or if searching mods, "Mods") to search results
     */
    fun performSearch(
        query: String,
        configs: List<ConfigData>,
        searchMods: Boolean = false
    ): Map<String, List<SearchResult>>
}

