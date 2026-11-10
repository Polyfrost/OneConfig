package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public abstract class SearchProvider {
    public abstract val priority: Int

    public abstract fun isAvailable(): Boolean

    public abstract fun search(query: String, scopes: Set<SearchScope>): List<String>

    public open fun onCorpusUpdate(added: List<SearchDocument>, removed: Set<String>) {}
}
