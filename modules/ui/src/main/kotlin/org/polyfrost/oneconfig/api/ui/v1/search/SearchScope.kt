package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
class SearchScope internal constructor(val key: String) {
    override fun equals(other: Any?): Boolean = this === other || (other is SearchScope && other.key == key)

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = "SearchScope($key)"

    companion object {
        @JvmField
        val MODS: SearchScope = SearchScope("mods")

        @JvmField
        val OPTIONS: SearchScope = SearchScope("options")

        @JvmField
        val KEYBINDS: SearchScope = SearchScope("keybinds")

        @JvmField
        val HUDS: SearchScope = SearchScope("huds")

        @JvmStatic
        fun config(id: String): SearchScope = SearchScope("config:$id")
    }
}
