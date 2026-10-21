package org.polyfrost.oneconfig.api.ui.v1.page

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
class PageText internal constructor(val key: String?, val fallback: String) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is PageText && other.key == key && other.fallback == fallback)

    override fun hashCode(): Int = 31 * (key?.hashCode() ?: 0) + fallback.hashCode()

    override fun toString(): String = "PageText(key=$key, fallback=$fallback)"

    companion object {
        @JvmStatic
        fun translatable(key: String, fallback: String): PageText {
            require(key.isNotBlank()) { "Translation key must not be blank" }
            return PageText(key, fallback)
        }

        @JvmStatic
        fun literal(text: String): PageText = PageText(null, text)
    }
}
