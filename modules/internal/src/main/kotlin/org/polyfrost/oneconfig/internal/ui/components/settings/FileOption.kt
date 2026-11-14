package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.internal.ui.api.settings.FileOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun FileOption(data: FileOptionData) {
    FileOption(data, true)
}

@Composable
fun FileOption(data: FileOptionData, enabled: Boolean) {
    val theme = LocalTheme.current
    val scope = rememberCoroutineScope()

    var text by remember(data.prop) { mutableStateOf(data.strProp.shown() ?: "") }

    fun update(newText: String) {
        text = newText
        data.strProp.set(newText)
    }

    // clicks while the dialog is open are ignored rather than queueing up another one
    var dialogOpen by remember { mutableStateOf(false) }

    fun openDialog() {
        if (dialogOpen || !enabled) return
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
                update(selected.toString())
            } finally {
                dialogOpen = false
            }
        }
    }

    TextField(
        text,
        ::update,
        Modifier.width(LocalOptionWidth.current).heightIn(min = OptionControlHeight),
        placeholder = data.placeholder,
        enabled = enabled,
        fontSize = 14.sp,
        visualTransformation = rememberPathTransformation(),
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (text.isNotEmpty()) {
                    IconButton(
                        "close",
                        modifier = Modifier.size(16.dp),
                        foreground = theme.textColorSecondary,
                        hoveredForeground = theme.textColor,
                        enabled = enabled,
                    ) { update("") }
                }
                IconButton(
                    "folder",
                    modifier = Modifier.size(18.dp),
                    foreground = theme.textColorSecondary,
                    hoveredForeground = theme.textColor,
                    enabled = enabled,
                    onClick = ::openDialog,
                )
            }
        },
    )
}

@Suppress("DEPRECATION")
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
