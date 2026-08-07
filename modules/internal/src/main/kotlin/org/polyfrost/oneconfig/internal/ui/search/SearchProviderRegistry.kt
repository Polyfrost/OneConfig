package org.polyfrost.oneconfig.internal.ui.search

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Object storing all search providers
 */
object SearchProviderRegistry {
    private val providers: CopyOnWriteArrayList<SearchProvider> = CopyOnWriteArrayList()

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
    internal fun get(): SearchProvider =
        providers.firstOrNull { runCatching { it.isAvailable() }.getOrNull() ?: false } ?: DefaultSearchProvider

    internal fun all(): List<SearchProvider> = providers.toList()

    init {
        registerSearchProvider(DefaultSearchProvider)
    }
}