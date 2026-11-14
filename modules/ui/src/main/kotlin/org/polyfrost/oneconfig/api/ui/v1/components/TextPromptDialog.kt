@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
@Composable
public fun TextPromptDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = DialogDefaults.saveLabel,
    validate: (String) -> String? = { null },
) {
    val theme = OneConfigTheme.current
    var value by remember { mutableStateOf(initial) }
    var edited by remember { mutableStateOf(false) }
    val problem = validate(value)
    val latestConfirm by rememberUpdatedState(onConfirm)
    val submit = { if (validate(value) == null) latestConfirm(value) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { runCatching { focus.requestFocus() } }
    ModalDialog(
        onDismiss,
        modifier,
        title = { DialogTitle(title) },
        buttons = {
            TextButton(DialogDefaults.cancelLabel, onDismiss)
            TextButton(confirmLabel, submit, style = ControlStyle.Primary, enabled = problem == null)
        },
    ) {
        TextField(
            value = value,
            onValueChange = {
                value = it
                edited = true
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = null,
            leadingIcon = null,
            maxLength = Int.MAX_VALUE,
            invalid = edited && problem != null,
            enabled = true,
            singleLine = true,
            framed = true,
            fontSize = theme.typography.body.fontSize,
            label = title,
            masked = false,
            focusRequester = focus,
            interactionSource = null,
            trailing = null,
            onSubmit = submit,
        )
        if (edited && problem != null) {
            Text(problem, Modifier, theme.error, theme.typography.small)
        }
    }
}
