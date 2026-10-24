package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.foundation.TooltipArea
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxHeight
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.ui.v1.ModSortMode
import org.polyfrost.oneconfig.api.ui.v1.ModSortModes
import org.polyfrost.oneconfig.internal.ui.api.ModSortStore
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.poly.GUTTER
import org.polyfrost.oneconfig.internal.ui.poly.OpenTransition
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

    Box(modifier.padding(end = GUTTER.dp)) {
        Row(
            Modifier
                .height(36.dp)
                .background(theme.componentBackground, theme.buttonShape)
                .border(1.dp, theme.borderColor, theme.buttonShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            modes.forEach { mode ->
                SortSegment(mode, mode.id == selectedId) {
                    if (mode.confirmation != null) pending = mode else apply(mode)
                }
            }
        }
        pending?.let { mode ->
            Popup(
                alignment = Alignment.TopEnd,
                anchor = Alignment.BottomEnd,
                offset = DpOffset(0.dp, 6.dp),
                onDismissRequest = { pending = null },
                properties = PopupProperties(focusable = true),
            ) {
                OpenTransition {
                    Column(
                        Modifier
                            .width(280.dp)
                            .background(theme.popupBackground, theme.popupShape)
                            .border(1.dp, theme.borderColor, theme.popupShape)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(mode.confirmation.orEmpty(), color = theme.textColor, fontSize = 13.sp, softWrap = true)
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
}

@Composable
private fun SortSegment(mode: ModSortMode, selected: Boolean, onSwitch: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    TooltipArea(
        tooltip = {
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .background(theme.popupBackground, shape)
                    .border(1.dp, theme.borderColor, shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(mode.title, color = theme.textColor, fontSize = 12.sp)
            }
        },
        below = true,
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
            Icon(mode.icon, if (selected) theme.textColor else theme.textColorSecondary, Modifier.size(16.dp))
        }
    }
}
