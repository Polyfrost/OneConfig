package org.polyfrost.oneconfig.internal.ui.search

import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.ConfigData


sealed interface SearchResult {
    val displayName: Any
    val icon: String?
}

data class ModResult(val config: ConfigData) : SearchResult {
    override val displayName get() = config.title
    override val icon get() = config.icon
}

data class OptionResult(
    val modId: String,
    val modTitle: Any,
    val optionTitle: Any,
    val category: String?,
    override val icon: String?,
    val prop: Property<*>?,
) : SearchResult {
    override val displayName get() = optionTitle
}
