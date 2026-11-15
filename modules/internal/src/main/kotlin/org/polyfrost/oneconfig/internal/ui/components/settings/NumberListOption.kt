package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.NumberField
import org.polyfrost.oneconfig.api.ui.v1.components.Slider
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberListOptionData

@Composable
fun NumberListOption(data: NumberListOptionData) {
    val elementType = remember(data.prop) { data.elementType }
    val state = rememberListEntries(data.prop, { data.floats() }) { values ->
        data.setElements(values.map { it.toNumberType(elementType) })
    }

    val spinnerStep = remember(data.step, data.min, data.max) { spinnerStep(data.step, data.min, data.max) }

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
            Slider(
                value = value,
                onValueChange = onValueChange,
                range = data.min..data.max,
                step = data.step,
                size = ControlSize.Compact,
            )
        }
        NumberField(
            value = value,
            onValueChange = onValueChange,
            range = data.min..data.max,
            step = step,
            roundToDecimals = false, fractionDigits = 2,
            format = data.format,
            width = 66.dp,
            fontSize = 13.sp,
        )
    } else {
        Box(modifier = Modifier.weight(1f)) {
            NumberField(
                value = value,
                onValueChange = onValueChange,
                range = data.min..data.max,
                step = step,
                roundToDecimals = false, fractionDigits = 2,
                format = data.format,
                fontSize = 13.sp,
            )
        }
    }
}
