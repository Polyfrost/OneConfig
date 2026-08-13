package org.polyfrost.oneconfig.internal.ui.search


/** A class responsible for searching configs */
interface SearchProvider {
    /** Higher priority providers are used when available */
    val priority: Int

    /** Whether this provider is currently available and ready to be used */
    fun isAvailable(): Boolean

    /**
     * Perform the search on all options and mods within a scope
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
     * Perform the search on all options and mods within a scope then group by the grouper
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

    /** Called every time the search corpus updates with the new documents and the removed document ids */
    suspend fun onCorpusUpdate(added: List<SearchDocument<*>>, removed: Set<String>) {}
}

