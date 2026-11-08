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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
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
    var local by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val shown = if (local.text == value) {
        local
    } else {
        TextFieldValue(value, TextRange(local.selection.start.coerceAtMost(value.length), local.selection.end.coerceAtMost(value.length)))
    }
    FieldCore(
        shown,
        { next ->
            local = next
            if (next.text != value) onValueChange(next.text)
        },
        modifier, placeholder, leadingIcon, maxLength, invalid, enabled, singleLine, framed, fontSize, label, masked,
        focusRequester, interactionSource, trailing, onSubmit,
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
    FieldCore(
        value, onValueChange, modifier, placeholder, leadingIcon, maxLength, invalid, enabled, singleLine, framed, fontSize, label, masked,
        focusRequester, interactionSource, trailing, onSubmit,
    )
}

@Composable
private fun FieldCore(
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
    val focused by source.collectIsFocusedAsState()
    val previous = remember { arrayOf(value) }
    LaunchedEffect(value) {
        val old = previous[0]
        previous[0] = value
        if (focused) speakChange(old, value, masked)
    }
    @Suppress("DEPRECATION")
    BasicTextField(
        value = value,
        onValueChange = { next -> onValueChange(next.limitedFrom(value, maxLength)) },
        modifier = fieldModifier(modifier, source, invalid, enabled, singleLine, framed, spokenLabel(label ?: placeholder, value.text, masked), focusRequester, onSubmit),
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

private class Edit(val start: Int, val removed: String, val inserted: String)

private fun editBetween(old: TextFieldValue, new: String): Edit {
    val text = old.text
    val anchorStart = old.selection.min.coerceIn(0, text.length)
    val anchorEnd = old.selection.max.coerceIn(anchorStart, text.length)
    var prefix = 0
    while (prefix < anchorStart && prefix < new.length && text[prefix] == new[prefix]) prefix++
    var suffix = 0
    val suffixLimit = text.length - maxOf(anchorEnd, prefix)
    while (suffix < suffixLimit && suffix < new.length - prefix && text[text.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    if (prefix < anchorStart || suffix < text.length - anchorEnd) {
        prefix = 0
        while (prefix < text.length && prefix < new.length && text[prefix] == new[prefix]) prefix++
        suffix = 0
        while (suffix < text.length - prefix && suffix < new.length - prefix && text[text.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    }
    return Edit(prefix, text.substring(prefix, text.length - suffix), new.substring(prefix, new.length - suffix))
}

private fun String.takeWhole(count: Int): String {
    if (count >= length) return this
    if (count <= 0) return ""
    val end = if (Character.isHighSurrogate(this[count - 1])) count - 1 else count
    return substring(0, end)
}

private fun TextFieldValue.limitedFrom(old: TextFieldValue, maxLength: Int): TextFieldValue {
    if (text == old.text || text.length <= maxLength || text.length <= old.text.length) return this
    val edit = editBetween(old, text)
    val head = text.substring(0, edit.start)
    val tail = text.substring(edit.start + edit.inserted.length)
    val kept = edit.inserted.takeWhole(maxLength - head.length - tail.length)
    return TextFieldValue(head + kept + tail, TextRange(head.length + kept.length))
}

private fun spokenText(text: String, masked: Boolean): String = when {
    masked -> if (text.length == 1) "hidden character" else "${text.length} hidden characters"
    text == " " -> "space"
    text == "\n" -> "new line"
    text == "\t" -> "tab"
    text.isBlank() -> "${text.length} spaces"
    else -> text
}

private fun speakChange(old: TextFieldValue, new: TextFieldValue, masked: Boolean) {
    if (!A11y.narratorActive()) return
    val spoken = if (old.text == new.text) {
        if (old.selection == new.selection) return
        val text = new.text
        val selection = new.selection
        when {
            !selection.collapsed -> "selected " + spokenText(text.substring(selection.min, selection.max), masked)
            text.isEmpty() -> "blank"
            selection.start == text.length && old.selection.start != text.length -> "end"
            selection.start == 0 && old.selection.start != 0 -> "start"
            else -> {
                val from = minOf(old.selection.start, selection.start).coerceIn(0, text.length)
                val to = maxOf(old.selection.start, selection.start).coerceIn(0, text.length)
                if (from == to) return
                spokenText(text.substring(from, to), masked)
            }
        }
    } else {
        val edit = editBetween(old, new.text)
        when {
            edit.inserted.isNotEmpty() -> spokenText(edit.inserted, masked)
            edit.removed.isNotEmpty() -> "deleted " + spokenText(edit.removed, masked)
            else -> return
        }
    }
    A11y.say(spoken, true)
}

private fun spokenLabel(label: String?, value: String, masked: Boolean): String {
    val content = when {
        value.isEmpty() -> "blank"
        masked -> "${value.length} hidden characters"
        else -> value
    }
    return if (label.isNullOrBlank()) content else "$label, $content"
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
