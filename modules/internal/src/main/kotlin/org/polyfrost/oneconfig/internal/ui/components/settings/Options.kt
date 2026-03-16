package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.BooleanOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.ButtonOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.ColorOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.DropdownOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.InfoOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.RadioButtonOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.SliderOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.TextOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.optionDataFrom
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Option(prop: Property<*>) {
    val data = remember(prop) { optionDataFrom(prop) }
    if (data == null) {
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
        return
    }
    when (data) {
        is BooleanOptionData -> BooleanOption(data)
        is SliderOptionData -> SliderOption(data)
        is NumberOptionData -> NumberOption(data)
        is TextOptionData -> TextOption(data)
        is DropdownOptionData -> DropdownOption(data)
        is RadioButtonOptionData -> RadioButtonOption(data)
        is ColorOptionData -> ColorOption(data)
        is KeybindOptionData -> KeybindOption(data)
        is ButtonOptionData -> ButtonOption(data)
        else -> {}
    }
}

@Composable
fun BooleanOption(data: BooleanOptionData) {
    var checked by remember { mutableStateOf(data.boolProp.get() == true) }
    when (data.style) {
        BooleanOptionData.Style.Switch -> SwitchControl(checked) { checked = it; data.boolProp.set(it) }
        BooleanOptionData.Style.Checkbox -> CheckboxControl(checked) { checked = it; data.boolProp.set(it) }
    }
}