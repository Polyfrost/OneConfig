package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberListOptionData
import kotlin.math.roundToInt

@Composable
fun NumberListOption(data: NumberListOptionData) {
    val elementType = remember(data.prop) { data.elementType }
    val state = rememberListEntries(data.prop, data.floats()) { values ->
        data.setElements(values.map { it.toNumberType(elementType) })
    }

    val spinnerStep = remember(data.step, data.min, data.max) {
        when {
            data.step > 0f -> data.step
            data.min == data.min.roundToInt().toFloat() && data.max == data.max.roundToInt().toFloat() -> 1f
            else -> 0.1f
        }
    }

    ListOptionContainer(
        state = state,
        reorderable = data.reorderable,
        maxEntries = data.maxEntries,
        addText = data.addText,
        onAdd = { state.add(data.min.coerceIn(data.min, data.max)) },
    ) { entry, _ ->
        NumberEntry(
            data = data,
            value = entry.value,
            step = spinnerStep,
            onValueChange = { state.update(entry.id, it.coerceIn(data.min, data.max)) },
        )
    }
}

@Composable
private fun RowScope.NumberEntry(
    data: NumberListOptionData,
    value: Float,
    step: Float,
    onValueChange: (Float) -> Unit,
) {
    if (data.style == NumberListOptionData.Style.Slider) {
        Box(modifier = Modifier.weight(1f)) {
            SliderControl(
                value = value,
                onValueChange = onValueChange,
                min = data.min,
                max = data.max,
                step = data.step,
                thumbSize = 14.dp,
                trackHeight = 4.dp,
            )
        }
        NumberSpinner(
            value = value,
            onValueChange = onValueChange,
            min = data.min,
            max = data.max,
            step = step,
            width = 66.dp,
        )
    } else {
        Box(modifier = Modifier.weight(1f)) {
            NumberSpinner(
                value = value,
                onValueChange = onValueChange,
                min = data.min,
                max = data.max,
                step = step,
            )
        }
    }
}
