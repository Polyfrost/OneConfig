@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val WHITE_MIN_CONTRAST = 3f

@ExperimentalOneConfigUiApi
public object ButtonDefaults {
    public val style: ControlStyle = ControlStyle.Secondary

    public val size: ControlSize = ControlSize.Regular

    public val contentSpacing: Dp = 8.dp

    public const val FILLED_HOVER_ALPHA: Float = 0.75f

    public const val DANGER_REST_ALPHA: Float = 0.12f

    public const val DANGER_HOVER_ALPHA: Float = 0.2f

    public val shape: Shape
        @Composable get() = OneConfigTheme.current.buttonShape

    public fun horizontalPadding(size: ControlSize): Dp = if (size == ControlSize.Compact) 10.dp else 16.dp

    public fun fontSize(size: ControlSize): TextUnit = if (size == ControlSize.Large) 14.sp else 13.sp

    public fun iconSize(size: ControlSize): Dp = if (size == ControlSize.Compact) 14.dp else 16.dp

    @Composable
    public fun contentColor(style: ControlStyle): Color {
        val theme = OneConfigTheme.current
        return when (style) {
            ControlStyle.Primary -> theme.accentTextColor
            ControlStyle.DangerFilled -> if (1.05f / (theme.error.luminance() + 0.05f) >= WHITE_MIN_CONTRAST) Color.White else theme.onError
            ControlStyle.Danger -> theme.error
            ControlStyle.Secondary, ControlStyle.Ghost -> theme.textColor
        }
    }
}
