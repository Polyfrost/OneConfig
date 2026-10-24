package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import org.polyfrost.compose.foundation.layout.RowScope
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.internal.ui.api.settings.FileListOptionData
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun FileListOption(data: FileListOptionData) {
    val scope = rememberCoroutineScope()

    val state = rememberListEntries(data.prop, { data.strings() }) { values ->
        data.setElements(values.filter { it.isNotBlank() })
    }

    // clicks while a dialog is open are ignored rather than queueing up another one
    var dialogOpen by remember { mutableStateOf(false) }

    fun openDialog(id: Int, current: String) {
        if (dialogOpen) return
        dialogOpen = true
        scope.launch {
            try {
                val selected = withContext(Dispatchers.IO) {
                    val tinyFd = TinyFdApi.getInstance()
                    val start = current.takeIf { it.isNotBlank() }
                    if (data.directory) {
                        tinyFd.openFolderSelector(data.dialogTitle, start)
                    } else {
                        tinyFd.openFileSelector(data.dialogTitle, start, data.filterPatterns, data.filterName)
                    }
                } ?: return@launch // cancelled so leave the existing entry untouched
                state.update(id, selected.toString())
            } finally {
                dialogOpen = false
            }
        }
    }

    ListOptionContainer(
        state = state,
        reorderable = data.reorderable,
        maxEntries = data.maxEntries,
        addText = data.addText,
        onAdd = { state.add("") },
    ) { entry, ctx ->
        PathField(
            value = entry.value,
            placeholder = data.placeholder,
            ctx = ctx,
            onValueChange = { state.update(entry.id, it) },
        )
        IconButton(
            "folder",
            modifier = Modifier.size(16.dp).pointerHoverIcon(PointerIcon.Hand),
            foreground = ctx.contentColor,
            hoveredForeground = LocalTheme.current.textColor,
            onClick = { openDialog(entry.id, entry.value) },
        )
    }
}

@Composable
private fun RowScope.PathField(
    value: String,
    placeholder: String?,
    ctx: ListRowContext,
    onValueChange: (String) -> Unit,
) {
    val field = remember { TextFieldState(value) }
    LaunchedEffect(value) {
        if (field.text != value) {
            field.replaceText(value)
            field.moveCaret(value.length)
        }
    }
    SideEffect { ctx.onFieldFocus(field.focused) }
    PathTextField(
        state = field,
        placeholder = placeholder,
        fontSize = 13f,
        modifier = Modifier.weight(1f),
        onValueChange = onValueChange,
    )
}
