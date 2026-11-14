package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.components.Switch

@Composable
fun SwitchControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SwitchControl(checked, true, onCheckedChange)
}

@Composable
fun SwitchControl(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(checked, onCheckedChange, enabled = enabled)
}
