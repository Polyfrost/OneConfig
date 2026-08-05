package org.polyfrost.oneconfig.internal.ui.search

/**
 * Scope of a search/where a search document should appear
 */
sealed interface SearchScope {
    /** Mod cards */
    data object Mods : SearchScope

    /** Every option visible in global search */
    data object Options : SearchScope

    /** Keybinds in keybind screen */
    data object Keybinds : SearchScope

    /** Every option in a specific mod's config */
    data class Config(val id: String) : SearchScope
}

data class SearchMetadata(
    val title: String? = null,
    val id: String? = null,
    val description: String? = null,
    /** Accordion a config option is in */
    val section: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val tags: List<String> = emptyList(),
    /** Data about the mod/config owning this option */
    val modTitle: String? = null,
    val modDescription: String? = null,
    val path: String? = null,
) {
    /**
     * Returns subcategory only if it is not the same as category
     */
    val distinctSubcategory: String?
        get() = subcategory?.takeUnless { it == category }
}

/**
 * A searchable document
 *
 * [payload] is used by screens to map back to what they need to render.
 */
class SearchDocument<T>(
    val id: String,
    val scopes: Set<SearchScope>,
    val metadata: SearchMetadata,
    val payload: T,
) {
    /**
     * Whether this document indexes the same text as [other].
     */
    fun contentEquals(other: SearchDocument<*>): Boolean =
        id == other.id && scopes == other.scopes && metadata == other.metadata

    override fun toString(): String {
        return "SearchDocument(id=$id, scopes=$scopes, metadata=$metadata, payload=$payload)"
    }
}

/**
 * Produces part of the searchable corpus. Called async when the
 */
fun interface SearchDocumentSource {
    fun documents(): List<SearchDocument<*>>
}
