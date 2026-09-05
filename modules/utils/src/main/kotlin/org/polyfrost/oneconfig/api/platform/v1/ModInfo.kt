package org.polyfrost.oneconfig.api.platform.v1

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption


data class ModInfo @JvmOverloads constructor(
    val id: String,
    val name: String,
    val version: String,
    val file: Path?,
    val modIconPath: String?,
    val authors: String? = null,
    val credits: String? = null,
    val description: String? = null,
){
    /**
     * Resolves [modIconPath] against this mod's own jar root ([file])
     *
     * Copies the icon out to a stable temp file and returns its absolute path
     *
     * [modIconPath] is relative to the owning jar
     *
     * Loading it as a classpath resource would resolve against the whole merged mod classpath
     *
     * That collides with any other mod using the same filename such as icon.png
     *
     * Extracting from [file] keeps each mod's icon distinct
     *
     * @return absolute path to the extracted icon file or null if unavailable
     */
    fun extractIconFile(): String? {
        val iconPath = modIconPath ?: return null
        val root = file ?: return null
        return runCatching {
            val source = root.resolve(iconPath)
            if (!Files.exists(source)) return@runCatching null
            val ext = iconPath.substringAfterLast('.', "png")
            val dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "oneconfig-modicons"))
            val dest = dir.resolve("$id.$ext")
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING)
            dest.toAbsolutePath().toString()
        }.getOrNull()
    }

    companion object {
        @Volatile
        private var cached: Set<ModInfo>? = null

        @get:JvmStatic
        val loadedMods: Set<ModInfo>
            get() = cached ?: Platform.compatibility().mods.also { if (it.isNotEmpty()) cached = it }
    }
}

interface ModInfoProvider {
    fun listMods(): Set<ModInfo>
}
