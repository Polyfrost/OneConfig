package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.settings.SliderOptionData

@Composable
fun SliderOption(data: SliderOptionData) {
    var value by remember(data.prop) { mutableStateOf(data.numProp.get()?.toFloat() ?: data.min) }

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
        )
    }
}
