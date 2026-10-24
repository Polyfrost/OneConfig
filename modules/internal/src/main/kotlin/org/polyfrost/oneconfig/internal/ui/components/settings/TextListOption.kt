package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.RowScope
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.oneconfig.internal.ui.api.settings.TextListOptionData
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun TextListOption(data: TextListOptionData) {
    val regex = remember(data.regex) { data.regex?.let { runCatching { Regex(it) }.getOrNull() } }
    fun isValid(value: String) = value.isEmpty() || regex == null || regex.matches(value)

    val state = rememberListEntries(data.prop, { data.strings() }) { values ->
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
    val clipboard = LocalUiClipboard.current
    val state = remember { TextFieldState(value) }
    LaunchedEffect(value) {
        if (state.text != value) {
            state.replaceText(value)
            state.moveCaret(value.length)
        }
    }
    SideEffect { ctx.onFieldFocus(state.focused) }
    val font = PolyFont(theme.typography.family.resolve(FontWeight.Normal), 13f)
    Box(modifier = Modifier.weight(1f)) {
        BasicTextField(
            state,
            font,
            color = theme.textColor,
            caretColor = theme.textColor,
            selectionColor = Accent.withAlpha(0.4f),
            readClipboard = clipboard.read,
            writeClipboard = clipboard.write,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
            placeholder = placeholder ?: "",
            placeholderColor = theme.textColorSecondary,
            onKey = textKeys(clipboard, state, onValueChange),
        )
    }
}
