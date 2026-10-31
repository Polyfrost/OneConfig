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
    /** Id of the mod whose jar this one is nested in, or null for a top-level mod; [copy] does not carry it over */
    var parentId: String? = null
        private set

    constructor(
        id: String,
        name: String,
        version: String,
        file: Path?,
        modIconPath: String?,
        authors: String?,
        credits: String?,
        description: String?,
        parentId: String?,
    ) : this(id, name, version, file, modIconPath, authors, credits, description) {
        this.parentId = parentId
    }

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

        @Volatile
        private var seen: Map<String, ModInfo> = emptyMap()

        @get:JvmStatic
        val loadedMods: Set<ModInfo>
            get() = cached ?: Platform.compatibility().mods.also { if (it.isNotEmpty()) cached = it }

        @get:JvmStatic
        val seenMods: Set<ModInfo>
            get() = (seen + loadedMods.associateBy { it.id }).values.toSet()

        @JvmStatic
        fun refreshLoadedMods() {
            val live = Platform.compatibility().mods
            if (live.isEmpty() || live == cached) return
            seen = seen + loadedMods.associateBy { it.id }
            cached = live
        }
    }
}

interface ModInfoProvider {
    fun listMods(): Set<ModInfo>
}
