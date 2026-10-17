package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.settings.RangeListOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.RangeOptionData

/**
 * A user-editable list of ranges where each row is a [RangeOption]
 *
 * Rows keep whatever order their ends were typed in until the input loses focus but every write stores
 * each pair with start at or below end
 */
@Composable
fun RangeListOption(data: RangeListOptionData) {
    val state = rememberListEntries(data.prop, { data.ranges() }) { ranges ->
        data.setRanges(ranges.map { (start, end) -> minOf(start, end) to maxOf(start, end) })
    }

    val spinnerStep = remember(data.step, data.min, data.max) { spinnerStep(data.step, data.min, data.max) }

    ListOptionContainer(
        state = state,
        reorderable = data.reorderable,
        maxEntries = data.maxEntries,
        addText = data.addText,
        onAdd = { state.add(data.min to data.max) },
    ) { entry, _ ->
        val (start, end) = entry.value
        RangeEntry(
            data = data,
            start = start,
            end = end,
            step = spinnerStep,
            onValueChange = { newStart, newEnd -> state.update(entry.id, newStart to newEnd) },
        )
    }
}

@Composable
private fun RowScope.RangeEntry(
    data: RangeListOptionData,
    start: Float,
    end: Float,
    step: Float,
    onValueChange: (start: Float, end: Float) -> Unit,
) {
    if (data.style == RangeOptionData.Style.Slider) {
        RangeSliderTrack(
            start = start,
            end = end,
            onValueChange = onValueChange,
            min = data.min,
            max = data.max,
            step = data.step,
            modifier = Modifier.weight(1f),
            thumbWidth = 11.dp,
            thumbHeight = 14.dp,
            trackHeight = 4.dp,
        )
        RangeSpinners(start, end, onValueChange, data.min, data.max, step, width = 56.dp, format = data.format)
    } else {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val spinnerWidth = (maxWidth - RangeSeparatorWidth) / 2
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RangeSpinners(
                    start, end, onValueChange, data.min, data.max, step,
                    width = spinnerWidth,
                    format = data.format,
                    separated = true,
                )
            }
        }
    }
}
