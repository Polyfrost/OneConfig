@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import kotlin.math.cos
import kotlin.math.sin

private const val WHITE_MIN_CONTRAST = 3f
private const val GLASS_EDGE_ANGLE_DEG = 20.0
private const val GLASS_EDGE_STRONG_ALPHA = 0.5f
private const val GLASS_EDGE_WEAK_ALPHA = 0.15f

private object GlassEdgeBrush : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(GLASS_EDGE_ANGLE_DEG)
        val ux = cos(radians).toFloat()
        val uy = sin(radians).toFloat()
        val length = size.width * ux + size.height * uy
        return LinearGradientShader(
            from = Offset.Zero,
            to = Offset(ux * length, uy * length),
            colors = listOf(
                Color.White.copy(alpha = GLASS_EDGE_STRONG_ALPHA),
                Color.White.copy(alpha = GLASS_EDGE_WEAK_ALPHA),
                Color.White.copy(alpha = GLASS_EDGE_STRONG_ALPHA),
            ),
            colorStops = listOf(0f, 0.5f, 1f),
        )
    }
}

@ExperimentalOneConfigUiApi
public object ButtonDefaults {
    public val style: ControlStyle = ControlStyle.Secondary

    public val size: ControlSize = ControlSize.Regular

    public val contentSpacing: Dp = 8.dp

    public const val FILLED_HOVER_ALPHA: Float = 0.75f

    public const val DANGER_REST_ALPHA: Float = 0.12f

    public const val DANGER_HOVER_ALPHA: Float = 0.2f

    public const val DANGER_BORDER_ALPHA: Float = 0.4f

    public const val DANGER_BORDER_HOVER_ALPHA: Float = 0.6f

    public const val TINT_REST_ALPHA: Float = 0.16f

    public const val TINT_HOVER_ALPHA: Float = 0.28f

    public const val TINT_BORDER_ALPHA: Float = 0.55f

    public const val GLASS_FILL_ALPHA: Float = 0.5f

    public const val GLASS_HOVER_FILL_ALPHA: Float = 0.65f

    public val glassBorderWidth: Dp = 1.5.dp

    public val glassBorderBrush: Brush = GlassEdgeBrush

    public val Lift: ControlIndication = ControlIndication(hoverAlpha = 0.1f, pressedAlpha = 0.18f, tone = OverlayTone.Lighten, layer = OverlayLayer.Behind)

    public fun indication(style: ControlStyle): ControlIndication = buttonIndication(style, glass = false) ?: ControlDefaults.Bare

    public val shape: Shape
        @Composable get() = OneConfigTheme.current.buttonShape

    @Composable
    public fun shape(style: ControlStyle): Shape = if (style == ControlStyle.Footer) RectangleShape else OneConfigTheme.current.buttonShape

    public fun horizontalPadding(size: ControlSize): Dp = ControlDefaults.metrics(size).horizontalPadding

    public fun fontSize(size: ControlSize): TextUnit = when (size) {
        ControlSize.Compact, ControlSize.Regular -> 13.sp
        ControlSize.Large -> 14.sp
        ControlSize.ExtraLarge -> 16.sp
    }

    public fun iconSize(size: ControlSize): Dp = ControlDefaults.metrics(size).iconSize

    @Composable
    public fun contentColor(style: ControlStyle): Color {
        val theme = OneConfigTheme.current
        return when (style) {
            ControlStyle.Primary -> theme.accentTextColor
            ControlStyle.DangerFilled -> if (1.05f / (theme.error.luminance() + 0.05f) >= WHITE_MIN_CONTRAST) Color.White else theme.onError
            ControlStyle.Danger -> theme.error
            ControlStyle.Secondary, ControlStyle.Ghost, ControlStyle.Tinted, ControlStyle.Glass, ControlStyle.Footer -> theme.textColor
        }
    }
}
