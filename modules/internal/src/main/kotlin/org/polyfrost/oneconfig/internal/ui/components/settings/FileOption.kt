package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.api.settings.FileOptionData
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun FileOption(data: FileOptionData) {
    val theme = LocalTheme.current
    val scope = rememberCoroutineScope()

    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(if (isHovered || isFocused) theme.textColorSecondary else theme.borderColor)

    var value by remember(data.prop) {
        val initial = data.strProp.get() ?: ""
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    val text = value.text

    val pathTransformation = rememberPathTransformation()

    fun setValue(newText: String) {
        value = TextFieldValue(newText, selection = TextRange(newText.length))
        data.strProp.set(newText)
    }

    // clicks while the dialog is open are ignored rather than queueing up another one
    var dialogOpen by remember { mutableStateOf(false) }

    fun openDialog() {
        if (dialogOpen) return
        dialogOpen = true
        scope.launch {
            try {
                val current = text.takeIf { it.isNotBlank() }
                val selected = withContext(Dispatchers.IO) {
                    val tinyFd = TinyFdApi.getInstance()
                    if (data.directory) {
                        tinyFd.openFolderSelector(data.dialogTitle, current)
                    } else {
                        tinyFd.openFileSelector(data.dialogTitle, current, data.filterPatterns, data.filterName)
                    }
                } ?: return@launch // cancelled so leave the existing value untouched
                setValue(selected.toString())
            } finally {
                dialogOpen = false
            }
        }
    }

    BasicTextField(
        value = value,
        onValueChange = { value = it; data.strProp.set(it.text) },
        singleLine = true,
        textStyle = TextStyle(
            color = theme.textColor,
            fontSize = 14.sp,
            fontFamily = theme.typography.family,
        ),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(theme.textColor),
        visualTransformation = pathTransformation,
        modifier = Modifier.trackTextInputFocus()
            .width(LocalOptionWidth.current)
            .background(theme.modCardBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, borderColor, theme.sideBarNavigationEntryShape)
            .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EditableValue(
                    modifier = Modifier.weight(1f),
                    showPlaceholder = text.isEmpty() && data.placeholder != null && !isFocused,
                    placeholder = data.placeholder,
                    inner = innerTextField,
                )
                if (text.isNotEmpty()) {
                    IconButton(
                        "close",
                        modifier = Modifier.size(16.dp),
                        foreground = theme.textColorSecondary,
                        hoveredForeground = theme.textColor,
                    ) { setValue("") }
                }
                IconButton(
                    "folder",
                    modifier = Modifier.size(18.dp).pointerHoverIcon(PointerIcon.Hand),
                    foreground = theme.textColorSecondary,
                    hoveredForeground = theme.textColor,
                    onClick = ::openDialog,
                )
            }
        },
    )
}

@Composable
internal fun rememberPathTransformation(): VisualTransformation {
    val theme = LocalTheme.current
    return remember(theme.textColor, theme.textColorSecondary) {
        VisualTransformation { annotated ->
            val raw = annotated.text
            val sep = raw.lastIndexOfAny(charArrayOf('/', '\\'))
            val builder = AnnotatedString.Builder(raw)
            if (sep >= 0) {
                builder.addStyle(SpanStyle(color = theme.textColorSecondary), 0, sep + 1)
                builder.addStyle(SpanStyle(color = theme.textColor), sep + 1, raw.length)
            } else {
                builder.addStyle(SpanStyle(color = theme.textColor), 0, raw.length)
            }
            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }
}

@Composable
private fun EditableValue(
    modifier: Modifier,
    showPlaceholder: Boolean,
    placeholder: String?,
    inner: @Composable () -> Unit,
) {
    val theme = LocalTheme.current
    Box(modifier) {
        if (showPlaceholder && placeholder != null) {
            Text(placeholder, color = theme.textColorSecondary, fontSize = 14.sp)
        }
        inner()
    }
}
