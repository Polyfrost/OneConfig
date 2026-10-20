package org.polyfrost.oneconfig.internal.ui.search

import java.util.concurrent.CopyOnWriteArrayList

/** Object storing all search providers */
object SearchProviderRegistry {
    private val providers: CopyOnWriteArrayList<SearchProvider> = CopyOnWriteArrayList()

    /** Register a new search provider */
    @Deprecated("Use SearchProviders.register. Removed in OneConfig 1.6.0")
    fun registerSearchProvider(provider: SearchProvider) {
        if (provider in providers) {
            return
        }
        providers.add(provider)
        providers.sortByDescending { it.priority }
        SearchCorpus.seed(provider)
    }

    internal fun unregisterSearchProvider(provider: SearchProvider) {
        providers.remove(provider)
    }

    /** The highest priority provider that is currently available */
    internal fun get(): SearchProvider =
        providers.firstOrNull { runCatching { it.isAvailable() }.getOrNull() ?: false } ?: DefaultSearchProvider

    internal fun all(): List<SearchProvider> = providers.toList()

    init {
        registerSearchProvider(DefaultSearchProvider)
    }
}