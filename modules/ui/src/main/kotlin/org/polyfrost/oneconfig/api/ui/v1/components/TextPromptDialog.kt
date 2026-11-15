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
import androidx.compose.ui.unit.Dp
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
    subtitle: String? = null,
    placeholder: String? = null,
    maxLength: Int = Int.MAX_VALUE,
    width: Dp = DialogDefaults.width,
    dismissLabel: String = DialogDefaults.cancelLabel,
    buttonLayout: DialogButtonLayout = DialogButtonLayout.End,
    confirmIcon: String? = null,
    dismissIcon: String? = null,
) {
    val theme = OneConfigTheme.current
    var value by remember { mutableStateOf(initial.take(maxLength)) }
    var edited by remember { mutableStateOf(false) }
    val problem = validate(value)
    val latestConfirm by rememberUpdatedState(onConfirm)
    val submit = { if (validate(value) == null) latestConfirm(value) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { runCatching { focus.requestFocus() } }
    val fill = buttonLayout == DialogButtonLayout.Fill
    ModalDialog(
        onDismiss,
        modifier,
        width = width,
        title = { DialogTitle(title) },
        buttons = {
            val buttonModifier = if (fill) Modifier.weight(1f) else Modifier
            if (dismissIcon != null) {
                TextButton(dismissLabel, dismissIcon, onDismiss, modifier = buttonModifier)
            } else {
                TextButton(dismissLabel, onDismiss, modifier = buttonModifier)
            }
            if (confirmIcon != null) {
                TextButton(confirmLabel, confirmIcon, submit, modifier = buttonModifier, style = ControlStyle.Primary, enabled = problem == null)
            } else {
                TextButton(confirmLabel, submit, modifier = buttonModifier, style = ControlStyle.Primary, enabled = problem == null)
            }
        },
        buttonLayout = buttonLayout,
    ) {
        if (subtitle != null) DialogBody(subtitle)
        TextField(
            value = value,
            onValueChange = {
                value = it
                edited = true
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder,
            invalid = edited && problem != null,
            singleLine = true,
            label = title,
            maxLength = maxLength,
            fontSize = theme.typography.body.fontSize,
            focusRequester = focus,
            onSubmit = submit,
        )
        if (edited && problem != null) {
            Text(problem, Modifier, theme.error, theme.typography.small)
        }
    }
}
