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
import org.polyfrost.oneconfig.api.ui.v1.components.Chip
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.NumberField
import org.polyfrost.oneconfig.api.ui.v1.components.Slider
import org.polyfrost.oneconfig.internal.ui.api.settings.InheritableSliderOptionData
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/**
 * A slider that can also sit in an inherited state showing the value it falls back to in a muted colour
 *
 * Touching the track or spinner takes ownership of the value and the trailing chip hands it back
 */
@Composable
fun InheritableSliderOption(data: InheritableSliderOptionData) {
    // bumped on every write so the derived state below re-reads the property
    var revision by remember(data.prop) { mutableStateOf(0) }
    val inherited = remember(data.prop, revision) { data.isInherited }
    var value by remember(data.prop, revision) { mutableStateOf(data.shownValue) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Slider(
            value = value,
            onValueChange = { value = it; data.set(it); revision++ },
            modifier = Modifier
                // the spinners beside the track outgrow narrow columns like the HUD editor's
                .weight(1f, fill = false)
                .width(LocalOptionWidth.current),
            range = data.min..data.max,
            step = data.step,
            muted = inherited,
            trackColor = LocalTheme.current.chipBackground,
            fillColor = Accent,
        )
        NumberField(
            value = value,
            onValueChange = { value = it; data.set(it); revision++ },
            range = data.min..data.max,
            step = if (data.step > 0f) data.step else 1f,
            roundToDecimals = false, fractionDigits = 2,
            format = data.format,
            width = 80.dp,
            fontSize = 13.sp,
        )
        Chip(
            label = data.inheritLabel,
            selected = inherited,
            onClick = {
                if (!inherited) {
                    data.inherit()
                    revision++
                }
            },
            size = ControlSize.Compact,
        )
    }
}
