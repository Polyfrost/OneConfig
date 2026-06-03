package org.polyfrost.oneconfig.internal.ui.api

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Tree

class TreeConfigData(
    val tree: Tree,
    override val source: ConfigSource,
    override val onOpen: (() -> Unit)? = null,
) : ConfigData {
    override val id: String get() = tree.id ?: ""
    override val title: Any get() = tree.title ?: id

    /**
     * Returns the full resource path for the icon (from [Config.iconPath] stored as "icon_path" metadata),
     * or an OC icon name (from "icon_name" metadata), or the fallback "default_mod".
     */
    override val icon: String
        get() = tree.getMetadata<String>("icon_path")
            ?: tree.getMetadata<String>("icon_name")
            ?: "default_mod"

    override val category: Config.Category
        get() = tree.getMetadata<Config.Category>("category") ?: Config.Category.OTHER
}
