package org.polyfrost.oneconfig.api.ui.v1.search

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public class SearchScope internal constructor(public val key: String) {
    override fun equals(other: Any?): Boolean = this === other || (other is SearchScope && other.key == key)

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = "SearchScope($key)"

    public companion object {
        @JvmField
        public val MODS: SearchScope = SearchScope("mods")

        @JvmField
        public val OPTIONS: SearchScope = SearchScope("options")

        @JvmField
        public val KEYBINDS: SearchScope = SearchScope("keybinds")

        @JvmField
        public val HUDS: SearchScope = SearchScope("huds")

        @JvmStatic
        public fun config(id: String): SearchScope = SearchScope("config:$id")
    }
}
