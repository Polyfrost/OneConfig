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
     * @param scopes The scopes to search in
     * @return A list of search results
     */
    fun search(
        query: String,
        scopes: Set<SearchScope>
    ): List<SearchDocument<*>>

    fun <T> searchGrouped(
        query: String,
        scopes: Set<SearchScope>,
        grouper: (SearchDocument<*>) -> T
    ): Map<T, List<SearchDocument<*>>>

    suspend fun onCorpusUpdate(added: List<SearchDocument<*>>, removed: Set<String>)
}

