package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.ui.v1.ModSortMode
import org.polyfrost.oneconfig.api.ui.v1.ModSortModes
import org.polyfrost.oneconfig.internal.ui.api.ModSortStore
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

private val customMode = ModSortMode(ModSortStore.CUSTOM, "dots-grid", "Custom order") {}
private val alphabeticalMode = ModSortMode(ModSortStore.ALPHABETICAL, "capitalized", "A to Z") {}

@Composable
fun ModSortControl(modifier: Modifier = Modifier) {
    val modes = listOf(customMode, alphabeticalMode) + ModSortModes.all()
    val selectedId = ModSortStore.selectedId.takeIf { id -> modes.any { it.id == id } } ?: ModSortStore.CUSTOM
    var pending by remember { mutableStateOf<ModSortMode?>(null) }
    val theme = LocalTheme.current

    fun apply(mode: ModSortMode) {
        ModSortStore.select(mode.id)
        mode.onSelect()
    }

    Box(modifier.padding(end = PageGutter)) {
        IconSegments(
            modes.map { IconSegment(it.icon, it.title) },
            selected = modes.indexOfFirst { it.id == selectedId },
        ) { index ->
            val mode = modes[index]
            if (mode.confirmation != null) pending = mode else apply(mode)
        }
        pending?.let { mode ->
            ShellPopup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(LocalDensity.current) { 42.dp.roundToPx() }),
                onDismissRequest = { pending = null },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    Modifier
                        .width(280.dp)
                        .background(theme.popupBackground, theme.popupShape)
                        .border(1.dp, theme.borderColor, theme.popupShape)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(mode.confirmation.orEmpty(), color = theme.textColor, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Cancel", selected = false) { pending = null }
                        Chip("Switch", selected = true) {
                            pending = null
                            apply(mode)
                        }
                    }
                }
            }
        }
    }
}

class IconSegment(val icon: String, val title: String)

/** A row of icon-only choices with the [selected] one highlighted, as the mods page's sort control */
@Composable
fun IconSegments(
    segments: List<IconSegment>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val theme = LocalTheme.current
    Row(
        modifier
            .height(36.dp)
            .background(theme.componentBackground, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            Segment(segment, index == selected) { onSelect(index) }
        }
    }
}

@Composable
private fun Segment(segment: IconSegment, selected: Boolean, onSwitch: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    Tooltip(
        text = { Text(segment.title, color = theme.textColor, fontSize = 12.sp) },
        modifier = Modifier,
        anchor = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .background(
                    if (selected) theme.textColor.copy(alpha = 0.08f) else Color.Transparent,
                    theme.buttonShape.concentric(3.dp),
                )
                .onClick(interactionSource, !selected, onSwitch)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(segment.icon, if (selected) theme.textColor else theme.textColorSecondary, Modifier.size(16.dp))
        }
    }
}
