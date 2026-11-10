package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public class ConfigKeybind internal constructor(
    public val keybind: OneConfigKeybind,
    public val title: String,
    public val path: String,
    public val modId: String,
    public val modTitle: String,
) {
    override fun toString(): String = "ConfigKeybind(modId=$modId, path=$path)"
}
