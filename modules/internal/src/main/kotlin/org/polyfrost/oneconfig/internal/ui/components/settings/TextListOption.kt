package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.api.settings.TextListOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun TextListOption(data: TextListOptionData) {
    val regex = remember(data.regex) { data.regex?.let { runCatching { Regex(it) }.getOrNull() } }
    fun isValid(value: String) = value.isEmpty() || regex == null || regex.matches(value)

    val state = rememberListEntries(data.prop, data.strings()) { values ->
        data.setElements(values.filter { it.isNotBlank() && isValid(it) })
    }

    ListOptionContainer(
        state = state,
        reorderable = data.reorderable,
        maxEntries = data.maxEntries,
        addText = data.addText,
        onAdd = { state.add("") },
        invalid = { !isValid(it) },
    ) { entry, ctx ->
        EntryTextField(
            value = entry.value,
            placeholder = data.placeholder,
            ctx = ctx,
            onValueChange = { state.update(entry.id, it) },
        )
    }
}

@Composable
internal fun RowScope.EntryTextField(
    value: String,
    placeholder: String?,
    ctx: ListRowContext,
    onValueChange: (String) -> Unit,
) {
    val theme = LocalTheme.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = theme.textColor,
            fontSize = 13.sp,
            fontFamily = theme.typography.family,
        ),
        interactionSource = ctx.fieldInteraction,
        cursorBrush = SolidColor(theme.textColor),
        modifier = Modifier.trackTextInputFocus().weight(1f),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder != null && !ctx.focused) {
                    Text(placeholder, color = theme.textColorSecondary, fontSize = 13.sp)
                }
                innerTextField()
            }
        },
    )
}
