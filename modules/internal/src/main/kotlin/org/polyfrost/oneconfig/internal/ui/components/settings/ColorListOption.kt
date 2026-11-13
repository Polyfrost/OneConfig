package org.polyfrost.oneconfig.internal.ui.components.settings

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ColorPickerPopup as KitColorPickerPopup
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.ui.api.settings.ColorListOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.readableOn
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper

private val DefaultEntryColor = Color.White

@Composable
fun ColorListOption(data: ColorListOptionData) {
    val state = rememberListEntries(data.prop, { data.argb() }) { values -> data.setArgb(values) }

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

    val textColor = Motion.animateColorAsState(readableOn(color))
    val latestOnColorChange by rememberUpdatedState(onColorChange)
    val hex = colorToHex(color)

    Box(
        modifier = Modifier
            .weight(1f)
            .contextMenu {
                swatchMenu(
                    model,
                    copy = { ClipboardHelper.setString("#$hex") },
                    paste = { pasted ->
                        val next = if (hasAlpha) pasted else pasted.copy(alpha = 1f)
                        model.set(next)
                        latestOnColorChange(next)
                    },
                    reset = {
                        model.set(DefaultEntryColor)
                        latestOnColorChange(DefaultEntryColor)
                    },
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 5.dp)
                .clip(shape)
                .background(color, shape)
                .border(1.dp, theme.borderColor, shape)
                .oneConfigClickable(
                    label = "${translated("oneconfig.colour", "Colour")} #$hex",
                    onClick = { onExpandedChange(!expanded) },
                    shape = shape,
                    indication = false,
                    interactionSource = interactionSource,
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon("paintbrush", tint = { textColor.value }, modifier = Modifier.size(12.dp))
            Text("#$hex", color = Color.Unspecified, fontSize = 12.sp, colorProducer = { textColor.value })
        }

        if (expanded) {
            KitColorPickerPopup(
                model,
                onColorChange,
                { onExpandedChange(false) },
                placement = Placement.BelowEnd,
            )
        }
    }
}
