package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.config.v1.Config

enum class ConfigSource {
    OC,
    Command,
    Compat;
}

interface ConfigData {
    val id: String
    val title: Any
    val icon: String?
    val authors: String? get() = null
    val credits: String? get() = null
    val version: String? get() = null
    val source: ConfigSource
    val category: Config.Category
    val onOpen: (() -> Unit)? get() = null

    val preview: (@Composable (Modifier) -> Unit)? get() = null
}
