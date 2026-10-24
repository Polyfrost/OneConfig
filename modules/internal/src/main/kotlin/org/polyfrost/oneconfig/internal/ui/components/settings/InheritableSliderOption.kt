package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.InheritableSliderOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/**
 * A slider that can also sit in an inherited state showing the value it falls back to in a muted colour
 *
 * Touching the track or spinner takes ownership of the value and the trailing chip hands it back
 */
@Composable
fun InheritableSliderOption(data: InheritableSliderOptionData) {
    val theme = LocalTheme.current

    // bumped on every write so the derived state below re-reads the property
    var revision by remember(data.prop) { mutableIntStateOf(0) }
    val inherited = remember(data.prop, revision) { data.isInherited }
    var value by remember(data.prop, revision) { mutableFloatStateOf(data.shownValue) }

    val filledColor by animateColorAsState(
        if (inherited) theme.textColorSecondary.copy(alpha = theme.textColorSecondary.alphaF * 0.4f) else Accent
    )
    val thumbColor by animateColorAsState(
        if (inherited) theme.controlThumbColor.copy(alpha = theme.controlThumbColor.alphaF * 0.5f) else theme.controlThumbColor
    )

    fun update(newValue: Float) {
        value = newValue
        data.set(newValue)
        revision++
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SliderControl(
            value = value,
            onValueChange = ::update,
            min = data.min,
            max = data.max,
            step = data.step,
            modifier = Modifier
                // the spinners beside the track outgrow narrow columns like the HUD editor's
                .weight(1f, fill = false)
                .width(LocalOptionWidth.current),
            trackColor = theme.chipBackground,
            fillColor = filledColor,
            thumbColor = thumbColor,
        )
        NumberSpinner(
            value = value,
            onValueChange = ::update,
            min = data.min,
            max = data.max,
            step = if (data.step > 0f) data.step else 1f,
            width = 80.dp,
            format = data.format,
        )
        InheritChip(label = data.inheritLabel, active = inherited) {
            if (!inherited) {
                data.inherit()
                revision++
            }
        }
    }
}

@Composable
private fun InheritChip(label: String, active: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val background = animateColorAsState(
        if (active) Accent.copy(alpha = Accent.alphaF * 0.2f) else if (isHovered) theme.componentBackground else theme.chipBackground
    )
    val border = animateColorAsState(if (active) Accent else theme.borderColor)
    val textColor = animateColorAsState(if (active) Accent else theme.textColorSecondary)

    Box(
        modifier = Modifier
            .background(background, theme.sideBarNavigationEntryShape)
            .border(1.dp, border, theme.sideBarNavigationEntryShape)
            .onClick(interactionSource, onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = textColor, fontSize = 12.sp)
    }
}
