package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.search.SearchDocument
import org.polyfrost.oneconfig.api.ui.v1.search.SearchMetadata
import org.polyfrost.oneconfig.api.ui.v1.search.SearchProvider
import org.polyfrost.oneconfig.api.ui.v1.search.SearchScope

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
public interface OneConfigUISearchBridge {
    public fun register(provider: SearchProvider)

    public fun unregister(provider: SearchProvider): Boolean

    public fun documentIds(): Set<String>

    public fun document(id: String): SearchDocument?
}

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
public object SearchInterop {
    @JvmStatic
    public fun scope(key: String): SearchScope = SearchScope(key)

    @JvmStatic
    public fun metadata(
        title: String?,
        id: String?,
        description: String?,
        section: String?,
        category: String?,
        subcategory: String?,
        tags: List<String>,
        modTitle: String?,
        modDescription: String?,
        path: String?,
    ): SearchMetadata = SearchMetadata(title, id, description, section, category, subcategory, tags, modTitle, modDescription, path)

    @JvmStatic
    public fun document(id: String, scopes: Set<SearchScope>, metadata: SearchMetadata): SearchDocument = SearchDocument(id, scopes, metadata)
}

@ExperimentalOneConfigUiApi
private val loadedSearchBridge: OneConfigUISearchBridge? by lazy {
    loadBridge(OneConfigUISearchBridge::class.java, "OneConfig search providers cannot be registered")
}

@ExperimentalOneConfigUiApi
internal fun searchBridge(): OneConfigUISearchBridge? = loadedSearchBridge

@ExperimentalOneConfigUiApi
internal fun requireSearchBridge(): OneConfigUISearchBridge =
    loadedSearchBridge ?: error("No OneConfigUISearchBridge is available; OneConfig search providers cannot be registered")
