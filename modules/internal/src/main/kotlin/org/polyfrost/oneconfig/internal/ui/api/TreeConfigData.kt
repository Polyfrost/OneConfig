package org.polyfrost.oneconfig.internal.ui.api

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.ui.components.asRenderText

class TreeConfigData(
    val tree: Tree,
    override val source: ConfigSource,
    private val explicitOnOpen: (() -> Unit)? = null,
) : ConfigData {
    override val id: String get() = tree.id ?: ""
    override val title: Any get() = tree.getMetadata<Any>("mod_card_title") ?: tree.title ?: id

    /**
     * Explicit open handler, or the tree's "on_click" metadata (e.g. Mod Menu compat entries
     * that open the target mod's own screen instead of an OC config).
     */
    override val onOpen: (() -> Unit)?
        get() {
            explicitOnOpen?.let { return it }

            return when (val handler = tree.getMetadata<Any>("on_click")) {
                is Runnable -> handler::run
                is Function0<*> -> ({ handler.invoke() })
                else -> null
            }
        }

    /**
     * Returns the full resource path for the icon (from [Config.iconPath] stored as "icon_path" metadata),
     * or an OC icon name (from "icon_name" metadata), or null when the mod has no icon.
     */
    override val icon: String?
        get() = tree.getMetadata<String>("mod_card_icon_path")
            ?: tree.getMetadata<String>("icon_path")
            ?: tree.getMetadata<String>("icon_name")

    override val authors: String?
        get() = tree.getMetadata<String>("mod_card_authors")?.nonBlankOrNull()
            ?: modInfo?.authors?.nonBlankOrNull()

    override val credits: String?
        get() = tree.getMetadata<String>("mod_card_credits")?.nonBlankOrNull()
            ?: modInfo?.credits?.nonBlankOrNull()

    override val version: String?
        get() = tree.getMetadata<String>("mod_card_version")?.nonBlankOrNull()
            ?: modInfo?.version?.nonBlankOrNull()

    override val category: Config.Category
        get() {
            val explicit = tree.getMetadata<Config.Category>("category")
            if (explicit != null && explicit != Config.Category.OTHER) return explicit
            return ThirdPartyModCategories.categoryFor(id, modInfo) ?: explicit ?: Config.Category.OTHER
        }

    private val modInfo: ModInfo?
        get() {
            val mods = ModInfo.loadedMods
            if (mods.isEmpty()) return null
            val ids = listOfNotNull(
                id.nonBlankOrNull(),
                id.substringBeforeLast('.').nonBlankOrNull()?.takeIf { it != id },
            )
            for (modId in ids) {
                mods.firstOrNull { it.id == modId }?.let { return it }
            }
            val name = title.asRenderText().nonBlankOrNull() ?: return null
            return mods.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

    private fun String.nonBlankOrNull(): String? = trim().takeIf { it.isNotEmpty() }
}
