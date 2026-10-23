package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
class SearchDocument internal constructor(val id: String, val scopes: Set<SearchScope>, val metadata: SearchMetadata) {
    override fun toString(): String = "SearchDocument(id=$id, scopes=$scopes)"
}
