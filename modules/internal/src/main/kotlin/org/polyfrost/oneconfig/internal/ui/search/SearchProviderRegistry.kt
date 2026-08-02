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
        providers.add(provider)
        providers.sortByDescending { it.priority }
    }

    /**
     * Get the search provider with the highest priority that is currently available
     */
    fun get(): SearchProvider {
        return providers.first { it.isAvailable() }
    }

    init {
        registerSearchProvider(DefaultSearchProvider)
    }
}