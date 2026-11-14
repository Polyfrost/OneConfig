package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public enum class ControlSize(
    public val height: Dp,
) {
    Compact(28.dp),

    Regular(32.dp),

    Large(38.dp),
}

@ExperimentalOneConfigUiApi
public enum class ControlStyle {
    Primary,

    Secondary,

    Ghost,

    Danger,

    DangerFilled,
}

@ExperimentalOneConfigUiApi
public object ControlDefaults {
    public val size: ControlSize = ControlSize.Regular

    public val disabledAlpha: Float
        @Composable get() = OneConfigTheme.current.disabledAlpha
}
