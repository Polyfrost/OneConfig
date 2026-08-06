package org.polyfrost.oneconfig.internal.ui.search


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
     * Perform the search on all options/mods within a scope
     *
     * @param query The search query
     * @param scopes The scopes to search in
     * @return A list of search results
     */
    fun search(
        query: String,
        scopes: Set<SearchScope>
    ): List<SearchDocument<*>>

    /**
     * Perform the search on all options/mods within a scope, and then group by the grouper.
     *
     * @param query The search query
     * @param scopes The scopes to search in
     * @param grouper The grouper
     * @return A list of search results
     */
    fun <T> searchGrouped(
        query: String,
        scopes: Set<SearchScope>,
        grouper: (SearchDocument<*>) -> T
    ): Map<T, List<SearchDocument<*>>>

    /**
     * Function that is called every time the search corpus updates,
     * with the new/updated documents, and the removed document ids
     */
    suspend fun onCorpusUpdate(added: List<SearchDocument<*>>, removed: Set<String>) {}
}

