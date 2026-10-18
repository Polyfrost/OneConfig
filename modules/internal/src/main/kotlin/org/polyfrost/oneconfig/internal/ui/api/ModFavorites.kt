package org.polyfrost.oneconfig.internal.ui.api

object ModFavorites : ModIdStore("favorite-mods") {
    fun isFavorite(id: String): Boolean = id in this
}
