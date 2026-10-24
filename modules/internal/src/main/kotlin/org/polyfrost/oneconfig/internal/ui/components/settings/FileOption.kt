package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.internal.ui.api.settings.FileOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun FileOption(data: FileOptionData) {
    val theme = LocalTheme.current
    val scope = rememberCoroutineScope()

    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val value = remember(data.prop) { TextFieldState(data.strProp.shown() ?: "") }
    val isFocused = value.focused
    val text = value.text

    val borderColor = animateColorAsState(if (isHovered || isFocused) theme.textColorSecondary else theme.borderColor)

    fun setValue(newText: String) {
        value.replaceText(newText)
        value.moveCaret(newText.length)
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

    Row(
        modifier = Modifier
            .width(LocalOptionWidth.current)
            .background(theme.modCardBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, borderColor, theme.sideBarNavigationEntryShape)
            .hoverable(interactionSource)
            .focusOnPress(value)
            .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PathTextField(
            state = value,
            placeholder = data.placeholder,
            fontSize = 14f,
            modifier = Modifier.weight(1f),
            onValueChange = { data.strProp.set(it) },
        )
        if (text.isNotEmpty()) {
            IconButton(
                "close",
                modifier = Modifier.size(16.dp).pointerHoverIcon(PointerIcon.Hand),
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
}

private fun Modifier.focusOnPress(state: TextFieldState) = onPointerEvent {
    it.type == PointerEventType.Press && it.button == 0 && run { state.requestFocus(); true }
}

@Composable
internal fun PathTextField(
    state: TextFieldState,
    placeholder: String?,
    fontSize: Float,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    val theme = LocalTheme.current
    val clipboard = LocalUiClipboard.current
    val font = PolyFont(theme.typography.family.resolve(FontWeight.Normal), fontSize)
    val dim = state.preedit.isEmpty()
    Box(modifier) {
        if (dim) PathText(state, font)
        BasicTextField(
            state, font,
            color = if (!dim) theme.textColor else PolyColor.TRANSPARENT,
            caretColor = theme.textColor,
            selectionColor = Accent.withAlpha(0.4f),
            readClipboard = clipboard.read,
            writeClipboard = clipboard.write,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
            placeholder = placeholder ?: "",
            placeholderColor = theme.textColorSecondary,
            onKey = { ev ->
                val before = state.text
                val used = state.onKey(ev, clipboard.read, clipboard.write)
                if (used && state.text != before) onValueChange(state.text)
                used
            },
        )
    }
}

@Composable
private fun PathText(state: TextFieldState, font: PolyFont) {
    val theme = LocalTheme.current
    val m = font.metrics
    Canvas(Modifier.fillMaxWidth().height((m.descent - m.ascent).dp).align(Alignment.CenterStart), live = true) { x, y, w, h ->
        val raw = state.text
        if (raw.isEmpty()) return@Canvas
        val base = y - m.ascent
        val sep = raw.lastIndexOfAny(charArrayOf('/', '\\')) + 1
        save()
        clipRect(x, y, w, h)
        val tx = x - state.scrollX
        if (sep > 0) text(raw.substring(0, sep), tx, base, theme.textColorSecondary, font)
        text(raw.substring(sep), tx + font.measureTextWidth(raw.substring(0, sep)), base, theme.textColor, font)
        restore()
    }
}
