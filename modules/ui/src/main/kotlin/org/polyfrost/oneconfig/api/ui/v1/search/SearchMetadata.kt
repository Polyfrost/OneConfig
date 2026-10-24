package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
class SearchMetadata internal constructor(
    val title: String?,
    val id: String?,
    val description: String?,
    val section: String?,
    val category: String?,
    val subcategory: String?,
    val tags: List<String>,
    val modTitle: String?,
    val modDescription: String?,
    val path: String?,
) {
    val distinctSubcategory: String?
        get() = subcategory?.takeUnless { it == category }

    override fun toString(): String = "SearchMetadata(title=$title, id=$id, path=$path)"
}
