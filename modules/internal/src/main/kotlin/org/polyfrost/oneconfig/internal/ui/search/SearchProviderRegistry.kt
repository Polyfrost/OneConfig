package org.polyfrost.oneconfig.internal.ui.search

/**
 * Object storing all search providers
 */
object SearchProviderRegistry {
    private val providers: MutableList<SearchProvider> = mutableListOf()

    /**
     * Register a new search provider
     */
    fun registerSearchProvider(provider: SearchProvider) {
        if (provider in providers) {
            return
        }
        providers.add(provider)
        providers.sortByDescending { it.priority }
        SearchCorpus.seed(provider)
    }

    /**
     * Get the search provider with the highest priority that is currently available
     */
    internal fun get(): SearchProvider = providers.first { it.isAvailable() }

    internal fun all(): List<SearchProvider> = providers.toList()

    init {
        registerSearchProvider(DefaultSearchProvider)
    }
}