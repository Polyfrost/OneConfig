package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
abstract class SearchProvider {
    abstract val priority: Int

    abstract fun isAvailable(): Boolean

    abstract fun search(query: String, scopes: Set<SearchScope>): List<String>

    open fun onCorpusUpdate(added: List<SearchDocument>, removed: Set<String>) {
    }
}
