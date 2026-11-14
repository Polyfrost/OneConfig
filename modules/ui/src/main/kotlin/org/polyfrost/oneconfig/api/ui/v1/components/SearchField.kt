@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val CLEAR_BUTTON_SIZE = 22.dp

@ExperimentalOneConfigUiApi
@Composable
public fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    onSubmit: (() -> Unit)? = null,
    clearable: Boolean = true,
    size: ControlSize = ControlSize.Large,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    var local by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    val shown = if (local.text == query) {
        local
    } else {
        TextFieldValue(query, TextRange(local.selection.start.coerceAtMost(query.length), local.selection.end.coerceAtMost(query.length)))
    }
    SearchFieldCore(
        shown,
        { next ->
            local = next
            if (next.text != query) onQueryChange(next.text)
        },
        { onQueryChange("") },
        modifier,
        placeholder,
        onSubmit,
        clearable,
        size,
        enabled,
        focusRequester,
        interactionSource,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    onSubmit: (() -> Unit)? = null,
    clearable: Boolean = true,
    size: ControlSize = ControlSize.Large,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    SearchFieldCore(
        value,
        onValueChange,
        { onValueChange(TextFieldValue()) },
        modifier,
        placeholder,
        onSubmit,
        clearable,
        size,
        enabled,
        focusRequester,
        interactionSource,
    )
}

@Composable
private fun SearchFieldCore(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier,
    placeholder: String?,
    onSubmit: (() -> Unit)?,
    clearable: Boolean,
    size: ControlSize,
    enabled: Boolean,
    focusRequester: FocusRequester?,
    interactionSource: MutableInteractionSource?,
) {
    val shownPlaceholder = placeholder ?: remember { searchString("oneconfig.header.search", "Search...") }
    val clearLabel = remember { searchString("oneconfig.ui.search.clear", "Clear search") }
    val trailing: (@Composable () -> Unit)? = if (clearable && value.text.isNotEmpty()) {
        {
            IconButton(
                "close",
                clearLabel,
                onClear,
                Modifier.size(CLEAR_BUTTON_SIZE),
                enabled = enabled,
                size = ControlSize.Compact,
                tooltip = null,
            )
        }
    } else {
        null
    }
    FieldCore(
        value,
        onValueChange,
        modifier,
        shownPlaceholder,
        enabled,
        false,
        true,
        size,
        shownPlaceholder,
        "search",
        null,
        trailing,
        false,
        Int.MAX_VALUE,
        true,
        TextUnit.Unspecified,
        focusRequester,
        onSubmit,
        interactionSource,
        fill = OneConfigTheme.current.chipBackground,
        glass = true,
    )
}

private fun searchString(key: String, fallback: String): String =
    runCatching { Platform.i18n().translateString(key) }.getOrNull()?.takeIf { it != key } ?: fallback
