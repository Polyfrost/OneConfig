package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import org.polyfrost.oneconfig.api.ui.v1.components.ArmedConfirmButton
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.internal.ui.api.settings.ButtonOptionData
import org.polyfrost.oneconfig.internal.ui.components.translated

@Composable
fun ButtonOption(data: ButtonOptionData) {
    ButtonOption(data, true)
}

@Composable
fun ButtonOption(data: ButtonOptionData, enabled: Boolean) {
    val text = data.buttonText?.asStyledLabel() ?: AnnotatedString(translated("oneconfig.option.button.default", "Click"))
    val confirmText = data.confirmText?.asStyledLabel()
    val modifier = Modifier.widthIn(max = LocalOptionWidth.current)
    if (confirmText == null) {
        TextButton(text, { data.runnable?.run() }, modifier, style = ControlStyle.Primary, enabled = enabled)
    } else {
        ArmedConfirmButton(
            text,
            { data.runnable?.run() },
            modifier,
            armedText = confirmText,
            style = ControlStyle.Primary,
            enabled = enabled,
        )
    }
}
