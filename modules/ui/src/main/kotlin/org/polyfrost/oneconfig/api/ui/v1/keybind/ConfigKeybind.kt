package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
class ConfigKeybind internal constructor(
    val keybind: OneConfigKeybind,
    val title: String,
    val path: String,
    val modId: String,
    val modTitle: String,
) {
    override fun toString(): String = "ConfigKeybind(modId=$modId, path=$path)"
}
