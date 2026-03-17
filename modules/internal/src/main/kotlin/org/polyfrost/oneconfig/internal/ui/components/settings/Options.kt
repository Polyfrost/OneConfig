package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.internal.ui.api.settings.BooleanOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Option(prop: Property<*>) {
    val vis = remember(prop) { prop.getMetadata<Visualizer>("visualizer") }
    if (vis != null) {
        vis.visualize(prop)
        return
    }
    // Fallback: render value as text for properties with no visualizer set
    val value = prop.get()
    Text(
        when (value) {
            is Boolean -> if (value) "On" else "Off"
            null -> "—"
            else -> value.toString()
        },
        color = LocalTheme.current.textColorSecondary,
        fontSize = 14.sp,
    )
}

@Composable
fun BooleanOption(data: BooleanOptionData) {
    var checked by remember { mutableStateOf(data.boolProp.get() == true) }
    when (data.style) {
        BooleanOptionData.Style.Switch -> SwitchControl(checked) { checked = it; data.boolProp.set(it) }
        BooleanOptionData.Style.Checkbox -> CheckboxControl(checked) { checked = it; data.boolProp.set(it) }
    }
}
