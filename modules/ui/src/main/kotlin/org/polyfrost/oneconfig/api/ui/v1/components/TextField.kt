@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11yField
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
@Composable
fun TextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    TextField(value, onValueChange, modifier, null, null, Int.MAX_VALUE, false)
}

@ExperimentalOneConfigUiApi
@Composable
fun TextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier, placeholder: String?) {
    TextField(value, onValueChange, modifier, placeholder, null, Int.MAX_VALUE, false)
}

@ExperimentalOneConfigUiApi
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String?,
    leadingIcon: String?,
    maxLength: Int,
    invalid: Boolean,
) {
    TextField(
        value, onValueChange, modifier, placeholder, leadingIcon, maxLength, invalid,
        true, true, true, 14.sp, null, false, null, null, null, null,
    )
}

@ExperimentalOneConfigUiApi
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String?,
    leadingIcon: String?,
    maxLength: Int,
    invalid: Boolean,
    enabled: Boolean,
    singleLine: Boolean,
    framed: Boolean,
    fontSize: TextUnit,
    label: String?,
    masked: Boolean,
    focusRequester: FocusRequester?,
    interactionSource: MutableInteractionSource?,
    trailing: (@Composable () -> Unit)?,
    onSubmit: (() -> Unit)?,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val style = rememberFieldStyle(fontSize)
    @Suppress("DEPRECATION")
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(if (it.length > maxLength) it.take(maxLength) else it) },
        modifier = fieldModifier(modifier, source, invalid, enabled, singleLine, framed, label ?: placeholder, focusRequester, onSubmit),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(OneConfigTheme.accent),
        visualTransformation = maskFor(masked),
        interactionSource = source,
        decorationBox = { inner ->
            FieldDecoration(value.isEmpty(), framed, placeholder, leadingIcon, singleLine, fontSize, source, trailing, inner)
        },
    )
}

@ExperimentalOneConfigUiApi
@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    placeholder: String?,
) {
    TextField(
        value, onValueChange, modifier, placeholder, null, Int.MAX_VALUE, false,
        true, true, true, 14.sp, null, false, null, null, null, null,
    )
}

@ExperimentalOneConfigUiApi
@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    placeholder: String?,
    leadingIcon: String?,
    maxLength: Int,
    invalid: Boolean,
    enabled: Boolean,
    singleLine: Boolean,
    framed: Boolean,
    fontSize: TextUnit,
    label: String?,
    masked: Boolean,
    focusRequester: FocusRequester?,
    interactionSource: MutableInteractionSource?,
    trailing: (@Composable () -> Unit)?,
    onSubmit: (() -> Unit)?,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val style = rememberFieldStyle(fontSize)
    @Suppress("DEPRECATION")
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.clamp(maxLength)) },
        modifier = fieldModifier(modifier, source, invalid, enabled, singleLine, framed, label ?: placeholder, focusRequester, onSubmit),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(OneConfigTheme.accent),
        visualTransformation = maskFor(masked),
        interactionSource = source,
        decorationBox = { inner ->
            FieldDecoration(value.text.isEmpty(), framed, placeholder, leadingIcon, singleLine, fontSize, source, trailing, inner)
        },
    )
}

@Suppress("DEPRECATION")
private fun maskFor(masked: Boolean): VisualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None

private fun TextFieldValue.clamp(maxLength: Int): TextFieldValue {
    if (text.length <= maxLength) return this
    return TextFieldValue(
        text.take(maxLength),
        TextRange(selection.start.coerceAtMost(maxLength), selection.end.coerceAtMost(maxLength)),
    )
}

@Composable
private fun rememberFieldStyle(fontSize: TextUnit): TextStyle {
    val theme = OneConfigTheme.current
    return remember(theme, fontSize) { TextStyle(color = theme.textColor, fontSize = fontSize, fontFamily = theme.fontFamily) }
}

@Composable
private fun fieldModifier(
    modifier: Modifier,
    source: MutableInteractionSource,
    invalid: Boolean,
    enabled: Boolean,
    singleLine: Boolean,
    framed: Boolean,
    label: String?,
    focusRequester: FocusRequester?,
    onSubmit: (() -> Unit)?,
): Modifier {
    val theme = OneConfigTheme.current
    val focused by source.collectIsFocusedAsState()
    val border by animateColorAsState(
        when {
            invalid -> DangerColor
            focused -> OneConfigTheme.accent
            else -> theme.borderColor
        },
    )
    val shape = theme.sideBarNavigationEntryShape
    return modifier
        .trackTextInputFocus()
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .a11yField(label)
        .then(
            if (onSubmit != null && singleLine) {
                Modifier.onPreviewKeyEvent { event ->
                    val submit = enabled && event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)
                    if (submit) onSubmit()
                    submit
                }
            } else {
                Modifier
            },
        )
        .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
        .then(
            if (framed) {
                Modifier.background(theme.modCardBackground, shape).border(1.dp, border, shape)
            } else {
                Modifier
            },
        )
}

@Composable
private fun FieldDecoration(
    empty: Boolean,
    framed: Boolean,
    placeholder: String?,
    leadingIcon: String?,
    singleLine: Boolean,
    fontSize: TextUnit,
    source: MutableInteractionSource,
    trailing: (@Composable () -> Unit)?,
    inner: @Composable () -> Unit,
) {
    val theme = OneConfigTheme.current
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    val iconColor by animateColorAsState(if (hovered || focused) theme.textColor else theme.textColorSecondary)
    Row(
        modifier = if (framed) Modifier.padding(horizontal = 12.dp, vertical = 7.dp) else Modifier,
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leadingIcon != null) Icon(leadingIcon, iconColor, Modifier.size(18.dp))
        FieldText(empty, placeholder, singleLine, fontSize, inner)
        trailing?.invoke()
    }
}

@Composable
private fun RowScope.FieldText(
    empty: Boolean,
    placeholder: String?,
    singleLine: Boolean,
    fontSize: TextUnit,
    inner: @Composable () -> Unit,
) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
        if (empty && placeholder != null) {
            Text(
                placeholder,
                Modifier,
                OneConfigTheme.current.textColorSecondary,
                fontSize,
                null,
                BaselineShift.None,
                FontWeight.Normal,
                TextAlign.Start,
                if (singleLine) 1 else Int.MAX_VALUE,
                TextOverflow.Ellipsis,
            )
        }
        inner()
    }
}
