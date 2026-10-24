package org.polyfrost.oneconfig.internal.ui.api

object TranslationCredits {
    data class Pack(val modId: String, val name: String, val author: String)

    val packs = listOf(
        Pack("sodium", "Translations for Sodium", "robotkoer"),
    )

    @Volatile
    @JvmField
    var namespaces: Set<String> = emptySet()
}
