package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public class SearchMetadata internal constructor(
    public val title: String?,
    public val id: String?,
    public val description: String?,
    public val section: String?,
    public val category: String?,
    public val subcategory: String?,
    public val tags: List<String>,
    public val modTitle: String?,
    public val modDescription: String?,
    public val path: String?,
) {
    public val distinctSubcategory: String?
        get() = subcategory?.takeUnless { it == category }

    override fun toString(): String = "SearchMetadata(title=$title, id=$id, path=$path)"
}
