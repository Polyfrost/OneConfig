package org.polyfrost.oneconfig.api.ui.v1.internal

import java.util.concurrent.ConcurrentHashMap
import org.polyfrost.oneconfig.api.ui.v1.search.SearchDocument as PublicSearchDocument
import org.polyfrost.oneconfig.api.ui.v1.search.SearchProvider as PublicSearchProvider
import org.polyfrost.oneconfig.api.ui.v1.search.SearchScope as PublicSearchScope
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchDocument
import org.polyfrost.oneconfig.internal.ui.search.SearchProvider
import org.polyfrost.oneconfig.internal.ui.search.SearchProviderRegistry
import org.polyfrost.oneconfig.internal.ui.search.SearchScope

class OneConfigUISearchBridgeImpl : OneConfigUISearchBridge {
    private val adapters = ConcurrentHashMap<PublicSearchProvider, SearchProvider>()

    override fun register(provider: PublicSearchProvider) {
        val adapter = adapters.computeIfAbsent(provider, ::PublicSearchProviderAdapter)
        SearchProviderRegistry.registerSearchProvider(adapter)
    }

    override fun unregister(provider: PublicSearchProvider): Boolean {
        val adapter = adapters.remove(provider) ?: return false
        SearchProviderRegistry.unregisterSearchProvider(adapter)
        return true
    }

    override fun documentIds(): Set<String> = SearchCorpus.corpus.keys

    override fun document(id: String): PublicSearchDocument? = SearchCorpus.corpus[id]?.toPublic()
}

private class PublicSearchProviderAdapter(private val provider: PublicSearchProvider) : SearchProvider {
    override val priority: Int get() = provider.priority

    override fun isAvailable(): Boolean = provider.isAvailable()

    override fun search(query: String, scopes: Set<SearchScope>): List<SearchDocument<*>> {
        val corpus = SearchCorpus.corpus
        return provider.search(query, scopes.mapTo(LinkedHashSet()) { it.toPublic() }).mapNotNull { corpus[it] }
    }

    override fun <T> searchGrouped(
        query: String,
        scopes: Set<SearchScope>,
        grouper: (SearchDocument<*>) -> T,
    ): Map<T, List<SearchDocument<*>>> = search(query, scopes).groupBy(grouper)

    override suspend fun onCorpusUpdate(added: List<SearchDocument<*>>, removed: Set<String>) {
        provider.onCorpusUpdate(added.map { it.toPublic() }, removed)
    }
}

private fun SearchScope.toPublic(): PublicSearchScope = when (this) {
    SearchScope.Mods -> PublicSearchScope.MODS
    SearchScope.Options -> PublicSearchScope.OPTIONS
    SearchScope.Keybinds -> PublicSearchScope.KEYBINDS
    SearchScope.Huds -> PublicSearchScope.HUDS
    is SearchScope.Config -> PublicSearchScope.config(id)
}

private fun SearchDocument<*>.toPublic(): PublicSearchDocument {
    val meta = metadata
    return SearchInterop.document(
        id,
        scopes.mapTo(LinkedHashSet()) { it.toPublic() },
        SearchInterop.metadata(
            meta.title,
            meta.id,
            meta.description,
            meta.section,
            meta.category,
            meta.subcategory,
            meta.tags,
            meta.modTitle,
            meta.modDescription,
            meta.path,
        ),
    )
}
