package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.components.Checkbox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun CheckboxIndicator(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Checkbox(checked, { onToggle() }, modifier)
}

@Composable
fun CheckboxControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    CheckboxControl(checked, true, onCheckedChange)
}

@Composable
fun CheckboxControl(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val theme = LocalTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (checked) translated("oneconfig.control.on", "On") else translated("oneconfig.control.off", "Off"),
            modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha },
            color = theme.textColorSecondary,
            fontSize = 12.sp,
        )
        Checkbox(checked, onCheckedChange, enabled = enabled)
    }
}
