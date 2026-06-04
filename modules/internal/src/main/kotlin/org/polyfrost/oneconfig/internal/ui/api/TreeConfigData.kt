package org.polyfrost.oneconfig.internal.ui.api

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Tree

class TreeConfigData(
    val tree: Tree,
    override val source: ConfigSource,
    private val explicitOnOpen: (() -> Unit)? = null,
) : ConfigData {
    override val id: String get() = tree.id ?: ""
    override val title: Any get() = tree.title ?: id

    /**
     * Explicit open handler, or the tree's "on_click" metadata (e.g. Mod Menu compat entries
     * that open the target mod's own screen instead of an OC config).
     */
    override val onOpen: (() -> Unit)?
        get() = explicitOnOpen ?: tree.getMetadata<() -> Unit>("on_click")

    /**
     * Returns the full resource path for the icon (from [Config.iconPath] stored as "icon_path" metadata),
     * or an OC icon name (from "icon_name" metadata), or null when the mod has no icon.
     */
    override val icon: String?
        get() = tree.getMetadata<String>("icon_path")
            ?: tree.getMetadata<String>("icon_name")

    override val category: Config.Category
        get() = tree.getMetadata<Config.Category>("category") ?: Config.Category.OTHER
}
