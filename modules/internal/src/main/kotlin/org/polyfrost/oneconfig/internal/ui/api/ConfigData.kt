package org.polyfrost.oneconfig.internal.ui.api

import org.polyfrost.oneconfig.api.config.v1.Config

enum class ConfigSource {
    OC,
    Command,
    Compat;
}

interface ConfigData {
    val id: String
    val title: Any
    val icon: String
    val source: ConfigSource
    val category: Config.Category
    val onOpen: (() -> Unit)? get() = null
}
