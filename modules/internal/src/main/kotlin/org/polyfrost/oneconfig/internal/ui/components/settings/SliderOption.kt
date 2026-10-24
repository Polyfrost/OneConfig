package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.settings.SliderOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown

@Composable
fun SliderOption(data: SliderOptionData) {
    var value by remember(data.prop) { mutableFloatStateOf(data.numProp.shown()?.toFloat() ?: data.min) }

    fun update(newValue: Float) {
        value = newValue
        data.numProp.set(newValue.toNumberType(data.prop.type))
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
            modifier = Modifier.width(LocalOptionWidth.current),
        )
        NumberSpinner(
            value = value,
            onValueChange = ::update,
            min = data.min,
            max = data.max,
            step = if (data.step > 0f) data.step else 1f,
            format = data.format,
        )
    }
}
