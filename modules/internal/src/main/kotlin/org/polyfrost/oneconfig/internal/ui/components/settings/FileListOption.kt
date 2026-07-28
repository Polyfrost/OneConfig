package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.internal.ui.api.settings.FileListOptionData
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun FileListOption(data: FileListOptionData) {
    val scope = rememberCoroutineScope()

    val state = rememberListEntries(data.prop, data.strings()) { values ->
        data.setElements(values.filter { it.isNotBlank() })
    }

    fun openDialog(id: Int, current: String) {
        scope.launch {
            val selected = withContext(Dispatchers.IO) {
                val tinyFd = TinyFdApi.getInstance()
                val start = current.takeIf { it.isNotBlank() }
                if (data.directory) {
                    tinyFd.openFolderSelector(data.dialogTitle, start)
                } else {
                    tinyFd.openFileSelector(data.dialogTitle, start, data.filterPatterns, data.filterName)
                }
            } ?: return@launch // cancelled: leave the existing entry untouched
            state.update(id, selected.toString())
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
        visualTransformation = rememberPathTransformation(),
        modifier = Modifier.weight(1f),
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
