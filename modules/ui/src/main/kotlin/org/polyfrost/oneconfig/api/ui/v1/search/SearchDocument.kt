package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public class SearchDocument internal constructor(public val id: String, public val scopes: Set<SearchScope>, public val metadata: SearchMetadata) {
    override fun toString(): String = "SearchDocument(id=$id, scopes=$scopes)"
}
