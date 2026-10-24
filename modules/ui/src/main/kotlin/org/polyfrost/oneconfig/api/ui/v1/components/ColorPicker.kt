package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
class ColorPickerModel(initialColor: Color, hasAlpha: Boolean) {
    constructor(initialColor: Color) : this(initialColor, true)

    internal val handle: Any = componentsBridge().createColorPicker(initialColor, hasAlpha)
}

@ExperimentalOneConfigUiApi
@Composable
fun ColorPickerPopup(model: ColorPickerModel, onColorChanged: (Color) -> Unit, onClose: () -> Unit) {
    componentsBridge().ColorPickerPopup(model.handle, onColorChanged, onClose, true)
}

@ExperimentalOneConfigUiApi
@Composable
fun ColorPickerPopup(model: ColorPickerModel, onColorChanged: (Color) -> Unit, onClose: () -> Unit, chromaCapable: Boolean) {
    componentsBridge().ColorPickerPopup(model.handle, onColorChanged, onClose, chromaCapable)
}

@ExperimentalOneConfigUiApi
fun colorToHex(color: Color): String = componentsBridge().colorToHex(color)
