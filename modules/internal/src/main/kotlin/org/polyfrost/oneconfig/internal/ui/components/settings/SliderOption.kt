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
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.components.NumberField
import org.polyfrost.oneconfig.api.ui.v1.components.Slider
import org.polyfrost.oneconfig.internal.ui.api.settings.SliderOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown

@Composable
fun SliderOption(data: SliderOptionData) {
    SliderOption(data, true)
}

@Composable
fun SliderOption(data: SliderOptionData, enabled: Boolean) {
    var value by remember(data.prop) { mutableStateOf(data.numProp.shown()?.toFloat() ?: data.min) }

    fun update(newValue: Float) {
        value = newValue
        data.numProp.set(newValue.toNumberType(data.prop.type))
    }

    val shown = value.coerceAtLeast(data.min).coerceAtMost(data.max)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Slider(
            value = shown,
            onValueChange = ::update,
            modifier = Modifier.width(LocalOptionWidth.current),
            range = data.min..data.max,
            step = data.step,
            enabled = enabled,
        )
        NumberField(
            value = shown,
            onValueChange = ::update,
            range = data.min..data.max,
            step = if (data.step > 0f) data.step else 1f,
            roundToDecimals = false, fractionDigits = 2,
            format = data.format,
            enabled = enabled,
            fontSize = 13.sp,
        )
    }
}
