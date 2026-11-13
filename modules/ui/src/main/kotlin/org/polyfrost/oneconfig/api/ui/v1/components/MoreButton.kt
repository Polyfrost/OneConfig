@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public object MoreButtonDefaults {
    public val contentDescription: String
        @Composable get() = kitString("oneconfig.ui.more_actions", "More actions")
}

@ExperimentalOneConfigUiApi
@Composable
public fun MoreButton(
    entries: List<MenuEntry>,
    modifier: Modifier = Modifier,
    contentDescription: String = MoreButtonDefaults.contentDescription,
    style: IconButtonStyle = IconButtonStyle.Ghost,
    size: ControlSize = ControlSize.Compact,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton("dots-vertical", contentDescription, { expanded = !expanded }, style = style, size = size, selected = expanded)
        DropdownMenu(expanded, { expanded = false }, entries, placement = Placement.BelowEnd)
    }
}
