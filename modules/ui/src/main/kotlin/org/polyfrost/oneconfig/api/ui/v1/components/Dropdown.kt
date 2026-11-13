@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
    AnchoredMenu(expanded, onDismiss, placement, matchAnchorWidth) { anchorWidth ->
        MenuList(entries, onDismiss, anchorWidth.then(modifier).heightIn(max = maxHeight))
    }
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
    AnchoredMenu(expanded, onDismiss, placement, matchAnchorWidth) { anchorWidth ->
        MenuSurface(anchorWidth.then(modifier), popupMaxHeight(), content)
    }
}

@Composable
private fun AnchoredMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    placement: Placement,
    matchAnchorWidth: Boolean,
    content: @Composable (anchorWidth: Modifier) -> Unit,
) {
    if (!expanded) return
    var anchorWidth by remember { mutableIntStateOf(0) }
    if (matchAnchorWidth) {
        Layout(
            content = {},
            modifier = Modifier.onPlaced { anchorWidth = it.parentCoordinates?.size?.width ?: 0 },
        ) { _, _ -> layout(0, 0) {} }
    }
    EscapeCloses(onDismiss)
    val density = LocalDensity.current
    componentsBridge().Popup(rememberAnchoredPosition(placement), onDismiss, true) {
        val width = if (matchAnchorWidth && anchorWidth > 0) Modifier.widthIn(min = with(density) { anchorWidth.toDp() }) else Modifier
        content(width)
    }
}
