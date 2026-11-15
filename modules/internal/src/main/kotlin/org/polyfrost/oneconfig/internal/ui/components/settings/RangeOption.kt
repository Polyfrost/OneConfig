package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.components.NumberField
import org.polyfrost.oneconfig.api.ui.v1.components.RangeSlider
import org.polyfrost.oneconfig.internal.ui.api.settings.RangeOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/**
 * A start and end pair shown either as a two-thumb slider with an input per end or as the inputs alone
 *
 * Whatever is shown the stored pair always has start at or below end
 */
@Composable
fun RangeOption(data: RangeOptionData) {
    val initial = data.read()
    var start by remember(data.prop) { mutableStateOf(initial?.first ?: data.min) }
    var end by remember(data.prop) { mutableStateOf(initial?.second ?: data.max) }

    fun update(newStart: Float, newEnd: Float) {
        start = newStart
        end = newEnd
        data.write(minOf(newStart, newEnd), maxOf(newStart, newEnd))
    }

    val step = spinnerStep(data.step, data.min, data.max)

    when (data.style) {
        RangeOptionData.Style.Slider -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RangeSlider(
                start = start,
                end = end,
                onValueChange = ::update,
                range = data.min..data.max,
                step = data.step,
                modifier = Modifier
                    // the spinners beside the track outgrow narrow columns like the HUD editor's
                    .weight(1f, fill = false)
                    .width(LocalOptionWidth.current),
            )
            RangeSpinners(start, end, ::update, data.min, data.max, step, width = 80.dp, format = data.format)
        }
        RangeOptionData.Style.Spinner -> Row(
            modifier = Modifier.width(LocalOptionWidth.current),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RangeSpinners(
                start, end, ::update, data.min, data.max, step,
                width = (LocalOptionWidth.current - RangeSeparatorWidth) / 2,
                format = data.format,
                separated = true,
            )
        }
    }
}

internal val RangeSeparatorWidth = 20.dp

/**
 * An input for each end of a range laid out in the caller's row
 *
 * Typing is free to pass the other end so a number is never clamped midway through being typed and the pair
 * is put back in order once the input loses focus while the arrows stop at the other end unless the pair is
 * already out of order
 *
 * [separated] puts a dash between the inputs for rows that have no track to read them against
 */
@Composable
internal fun RangeSpinners(
    start: Float,
    end: Float,
    onValueChange: (start: Float, end: Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp,
    format: ((Float) -> String)?,
    separated: Boolean = false,
) {
    val latest = remember { FloatArray(2) }
    latest[0] = start
    latest[1] = end
    val reorder = {
        val low = latest[1]
        val high = latest[0]
        if (high > low) {
            latest[0] = low
            latest[1] = high
            onValueChange(low, high)
        }
    }
    NumberField(
        value = start,
        onValueChange = {
            latest[0] = it
            onValueChange(it, latest[1])
        },
        range = min..max,
        step = step,
        roundToDecimals = false, fractionDigits = 2,
        format = format,
        width = width,
        stepRange = min..(if (start <= end) end else max),
        onEditEnd = reorder,
        fontSize = 13.sp,
    )
    if (separated) {
        val theme = LocalTheme.current
        Box(Modifier.width(RangeSeparatorWidth), contentAlignment = Alignment.Center) {
            Text("-", color = theme.textColorSecondary, fontSize = theme.typography.label.fontSize)
        }
    }
    NumberField(
        value = end,
        onValueChange = {
            latest[1] = it
            onValueChange(latest[0], it)
        },
        range = min..max,
        step = step,
        roundToDecimals = false, fractionDigits = 2,
        format = format,
        width = width,
        stepRange = (if (start <= end) start else min)..max,
        onEditEnd = reorder,
        fontSize = 13.sp,
    )
}
