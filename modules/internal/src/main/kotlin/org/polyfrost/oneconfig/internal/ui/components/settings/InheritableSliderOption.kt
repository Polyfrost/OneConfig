package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.InheritableSliderOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

/**
 * A slider that can also sit in an "inherited" state, where it shows the value it falls back to in a muted
 * colour. Touching the track or spinner takes ownership of the value; the trailing chip hands it back.
 */
@Composable
fun InheritableSliderOption(data: InheritableSliderOptionData) {
    val theme = LocalTheme.current

    // Bumped on every write so the derived state below re-reads the property.
    var revision by remember(data.prop) { mutableStateOf(0) }
    val inherited = remember(data.prop, revision) { data.isInherited }
    var value by remember(data.prop, revision) { mutableStateOf(data.shownValue) }
    var trackWidthPx by remember { mutableStateOf(0f) }

    val thumbDp = 19.dp
    val span = (data.max - data.min).takeIf { it != 0f } ?: 1f
    val fraction by animateFloatAsState(((value - data.min) / span).coerceIn(0f, 1f), animationSpec = spring())

    val filledColor by animateColorAsState(if (inherited) theme.textColorSecondary.copy(alpha = 0.4f) else Accent)
    val thumbColor by animateColorAsState(
        if (inherited) theme.controlThumbColor.copy(alpha = 0.5f) else theme.controlThumbColor
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(LocalOptionWidth.current)
                .height(19.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(data.min, data.max, data.step) {
                    val thumbPx = thumbDp.toPx()
                    var lastTickValue = Float.NaN
                    var lastTickAt = 0L
                    fun maybeTick(newValue: Float) {
                        if (newValue == lastTickValue) return
                        lastTickValue = newValue
                        val now = System.currentTimeMillis()
                        if (now - lastTickAt >= 70L) {
                            lastTickAt = now
                            UiSounds.play(UiSoundEvent.SLIDER_TICK)
                        }
                    }
                    fun snap(raw: Float): Float {
                        val clamped = raw.coerceIn(data.min, data.max)
                        if (data.step <= 0f) return clamped
                        return (data.min + ((clamped - data.min) / data.step).roundToInt() * data.step)
                            .coerceIn(data.min, data.max)
                    }
                    fun xToValue(x: Float): Float {
                        val usable = (trackWidthPx - thumbPx).coerceAtLeast(1f)
                        return snap(data.min + ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f) * (data.max - data.min))
                    }
                    fun apply(newValue: Float) {
                        value = newValue
                        maybeTick(newValue)
                        data.set(newValue)
                        revision++
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        apply(xToValue(down.position.x))
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    apply(xToValue(change.position.x))
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Box(
                Modifier
                    .fillMaxWidth().height(5.dp)
                    .align(Alignment.CenterStart)
                    .clip(theme.checkBoxShape)
                    .background(theme.chipBackground)
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction).height(5.dp)
                    .align(Alignment.CenterStart)
                    .clip(theme.checkBoxShape)
                    .background(filledColor)
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((fraction * (trackWidthPx - thumbDp.toPx())).toInt(), 0) }
                    .size(thumbDp)
                    .background(thumbColor, theme.circleShape)
            )
        }
        NumberSpinner(
            value = value,
            onValueChange = { value = it; data.set(it); revision++ },
            min = data.min,
            max = data.max,
            step = if (data.step > 0f) data.step else 1f,
            width = 80.dp,
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
    val background by animateColorAsState(
        if (active) Accent.copy(alpha = 0.2f) else if (isHovered) theme.componentBackground else theme.chipBackground
    )
    val border by animateColorAsState(if (active) Accent else theme.borderColor)
    val textColor by animateColorAsState(if (active) Accent else theme.textColorSecondary)

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
