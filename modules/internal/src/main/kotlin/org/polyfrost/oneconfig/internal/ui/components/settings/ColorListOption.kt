package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.internal.ui.api.settings.ColorListOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val DefaultEntryColor = Color.White

@Composable
fun ColorListOption(data: ColorListOptionData) {
    val state = rememberListEntries(data.prop, data.argb()) { values -> data.setArgb(values) }

    var openEntry by remember(data.prop) { mutableStateOf(-1) }

    ListOptionContainer(
        state = state,
        reorderable = data.reorderable,
        maxEntries = data.maxEntries,
        addText = data.addText,
        onAdd = { state.add(DefaultEntryColor.toArgb()) },
    ) { entry, _ ->
        ColorSwatch(
            argb = entry.value,
            hasAlpha = data.alpha,
            expanded = openEntry == entry.id,
            onExpandedChange = { openEntry = if (it) entry.id else -1 },
            onColorChange = { state.update(entry.id, it.toArgb()) },
        )
    }
}

@Composable
private fun RowScope.ColorSwatch(
    argb: Int,
    hasAlpha: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onColorChange: (Color) -> Unit,
) {
    val theme = LocalTheme.current
    val shape = theme.sideBarNavigationEntryShape
    val interactionSource = rememberInteractionSource()

    val color = remember(argb) { Color(argb).let { if (hasAlpha) it else it.copy(alpha = 1f) } }
    val model = remember(hasAlpha) { ColorPickerModel(color, hasAlpha) }

    val textColor by animateColorAsState(if (color.luminance() > 0.6f) Color.Black else Color.White)

    Box(modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 5.dp)
                .clip(shape)
                .background(color, shape)
                .border(1.dp, theme.borderColor, shape)
                .pointerHoverIcon(PointerIcon.Hand)
                .onClick(interactionSource) { onExpandedChange(!expanded) }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon("paintbrush", color = textColor, modifier = Modifier.size(12.dp))
            Text("#${colorToHex(color)}", color = textColor, fontSize = 12.sp)
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, 38),
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(focusable = true),
            ) {
                ColorPickerPopup(
                    model = model,
                    onColorChanged = onColorChange,
                    onClose = { onExpandedChange(false) },
                    chromaCapable = false,
                )
            }
        }
    }
}
