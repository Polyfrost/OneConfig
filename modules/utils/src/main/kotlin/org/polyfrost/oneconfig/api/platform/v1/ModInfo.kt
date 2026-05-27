package org.polyfrost.oneconfig.api.platform.v1

import java.nio.file.Path


data class ModInfo(
    val id: String,
    val name: String,
    val version: String,
    val file: Path?,
){
    companion object {
        @get:JvmStatic
        val loadedMods: Set<ModInfo> get() = Platform.compatibility().mods
    }
}

interface ModInfoProvider {
    fun listMods(): Set<ModInfo>
}