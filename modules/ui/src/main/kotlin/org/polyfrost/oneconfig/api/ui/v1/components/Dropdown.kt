@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
public fun DropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<MenuEntry>,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    matchAnchorWidth: Boolean = false,
    maxHeight: Dp = popupMaxHeight(),
) {
    TriggeredDropdownMenu(expanded, onDismiss, entries, null, modifier, placement, matchAnchorWidth, maxHeight)
}

@ExperimentalOneConfigUiApi
@Composable
public fun DropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    matchAnchorWidth: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    TriggeredDropdownMenu(expanded, onDismiss, null, modifier, placement, matchAnchorWidth, content)
}

@Composable
internal fun TriggeredDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<MenuEntry>,
    trigger: PopupTrigger?,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    matchAnchorWidth: Boolean = false,
    maxHeight: Dp = popupMaxHeight(),
) {
    AnchoredPopup(expanded, onDismiss, placement, matchAnchorWidth = matchAnchorWidth, trigger = trigger) { anchorWidth ->
        MenuList(entries, onDismiss, anchorWidth.then(modifier).heightIn(max = maxHeight))
    }
}

@Composable
internal fun TriggeredDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    trigger: PopupTrigger?,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    matchAnchorWidth: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnchoredPopup(expanded, onDismiss, placement, matchAnchorWidth = matchAnchorWidth, trigger = trigger) { anchorWidth ->
        MenuSurface(anchorWidth.then(modifier), popupMaxHeight(), content)
    }
}

internal class PopupTrigger {
    val focusRequester = FocusRequester()
    var focused = false

    fun restoreFocus() {
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
internal fun rememberPopupTrigger(): PopupTrigger = remember { PopupTrigger() }

internal fun Modifier.popupTrigger(trigger: PopupTrigger): Modifier =
    focusRequester(trigger.focusRequester).onFocusChanged { trigger.focused = it.hasFocus }

@Composable
internal fun AnchoredPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    placement: Placement,
    gap: Dp = 6.dp,
    matchAnchorWidth: Boolean = false,
    trigger: PopupTrigger? = null,
    content: @Composable (anchorWidth: Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    if (!expanded) return
    var anchorWidth by remember { mutableIntStateOf(0) }
    if (matchAnchorWidth) {
        Layout(
            content = {},
            modifier = Modifier.onPlaced { anchorWidth = it.parentCoordinates?.size?.width ?: 0 },
        ) { _, _ -> layout(0, 0) {} }
    }
    if (trigger != null) {
        val hadFocus = remember(trigger) { trigger.focused }
        DisposableEffect(trigger) {
            onDispose { if (hadFocus) scope.launch { trigger.restoreFocus() } }
        }
    }
    EscapeCloses(onDismiss)
    val density = LocalDensity.current
    componentsBridge().Popup(rememberAnchoredPosition(placement, gap), onDismiss, true) {
        val width = if (matchAnchorWidth && anchorWidth > 0) Modifier.widthIn(min = with(density) { anchorWidth.toDp() }) else Modifier
        content(width)
    }
}
