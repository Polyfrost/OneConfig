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
){
    /**
     * Resolves [modIconPath] against this mod's own jar root ([file]) and copies the icon out to a
     * stable temp file, returning its absolute path.
     *
     * [modIconPath] is relative to the owning jar, so loading it as a classpath resource resolves
     * against the whole merged mod classpath and collides with any other mod that uses the same
     * filename (e.g. "icon.png"). Extracting from [file] keeps each mod's icon distinct.
     *
     * @return absolute path to the extracted icon file, or null if unavailable.
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
        @get:JvmStatic
        val loadedMods: Set<ModInfo> get() = Platform.compatibility().mods
    }
}

interface ModInfoProvider {
    fun listMods(): Set<ModInfo>
}
