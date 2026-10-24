package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.runtime.Composable
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.components.settings.formatSpinnerValue
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun NumberSpinner(
    label: String,
    unit: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp = 128.dp,
) {
    NumberSpinner(
        value = value,
        onValueChange = onValueChange,
        min = min,
        max = max,
        step = step,
        width = width,
        format = { v -> "$label  ${formatSpinnerValue(v)}$unit" },
    )
}
@Suppress("UNUSED_PARAMETER")
@Composable
fun NumberSpinner(
    label: String,
    icon: String,
    unit: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp = 128.dp,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = LocalTheme.current.textColor, fontSize = 14.sp)
        NumberSpinner(
            value = value,
            onValueChange = onValueChange,
            min = min,
            max = max,
            step = step,
            width = width,
            format = { v -> "${formatSpinnerValue(v)} $unit" },
        )
    }
}


@Composable
fun NumberSpinnerWithIcon(
    icon: String,
    unit: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp = 62.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, color = LocalTheme.current.textColor, modifier = Modifier.size(12.dp))
        NumberSpinner(
            value = value,
            onValueChange = onValueChange,
            min = min,
            max = max,
            step = step,
            width = width,
            format = { v -> "${formatSpinnerValue(v)} $unit" },
        )
    }
}
