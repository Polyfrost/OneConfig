package org.polyfrost.oneconfig.api.ui.v1.page

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public class PageText internal constructor(public val key: String?, public val fallback: String) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is PageText && other.key == key && other.fallback == fallback)

    override fun hashCode(): Int = 31 * (key?.hashCode() ?: 0) + fallback.hashCode()

    override fun toString(): String = "PageText(key=$key, fallback=$fallback)"

    public companion object {
        @JvmStatic
        public fun translatable(key: String, fallback: String): PageText {
            require(key.isNotBlank()) { "Translation key must not be blank" }
            return PageText(key, fallback)
        }

        @JvmStatic
        public fun literal(text: String): PageText = PageText(null, text)
    }
}
