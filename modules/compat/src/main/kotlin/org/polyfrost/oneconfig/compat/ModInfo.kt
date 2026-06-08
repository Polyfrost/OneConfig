package org.polyfrost.oneconfig.compat

import java.nio.file.Path

data class ModInfo(
    val id: String,
    val name: String,
    val version: String,
    val file: Path?,
    val authors: String? = null,
    val credits: String? = null,
){
    companion object {
        @get:JvmStatic
        val loadedMods: Set<ModInfo> get() = Pl.listMods()
    }
}
