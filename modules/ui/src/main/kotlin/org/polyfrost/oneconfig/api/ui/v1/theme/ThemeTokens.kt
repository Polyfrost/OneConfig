package org.polyfrost.oneconfig.api.ui.v1.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public enum class ThemeStyle {
    Flat,

    Glass,

    Pixel,
}

@ExperimentalOneConfigUiApi
public class OneConfigShapes(
    public val small: Shape,
    public val medium: Shape,
    public val large: Shape,
    public val field: Shape,
    public val pill: Shape,
    public val circle: Shape,
)

@ExperimentalOneConfigUiApi
public open class OneConfigTypography(
    public val title: TextStyle,
    public val heading: TextStyle,
    public val body: TextStyle,
    public val label: TextStyle,
    public val small: TextStyle,
    public val caption: TextStyle,
    public val eyebrow: TextStyle,
    public val mono: TextStyle,
) {
    public constructor(fontFamily: FontFamily) : this(
        title = TextStyle(fontFamily = fontFamily, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        heading = TextStyle(fontFamily = fontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        body = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal),
        label = TextStyle(fontFamily = fontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium),
        small = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal),
        caption = TextStyle(fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Normal),
        eyebrow = TextStyle(fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp),
        mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Normal),
    )
}

@ExperimentalOneConfigUiApi
public class OneConfigSpacing(
    public val xxs: Dp = 2.dp,
    public val xs: Dp = 4.dp,
    public val s: Dp = 6.dp,
    public val m: Dp = 8.dp,
    public val l: Dp = 12.dp,
    public val xl: Dp = 16.dp,
    public val xxl: Dp = 24.dp,
    public val pageGutter: Dp = 25.dp,
) {
    public companion object {
        @JvmField
        public val Default: OneConfigSpacing = OneConfigSpacing()
    }
}

private const val CONTRAST_STEPS = 20

@ExperimentalOneConfigUiApi
public fun Color.contrastAgainst(other: Color): Float {
    val front = if (alpha < 1f) compositeOver(other) else this
    val a = front.luminance() + 0.05f
    val b = other.luminance() + 0.05f
    return if (a > b) a / b else b / a
}

@ExperimentalOneConfigUiApi
public fun Color.legibleOn(surface: Color, minContrast: Float = 4.5f): Color {
    if (contrastAgainst(surface) >= minContrast) return this
    val target = if (surface.luminance() > 0.5f) Color.Black else Color.White
    for (step in 1..CONTRAST_STEPS) {
        val candidate = lerp(this, target.copy(alpha = alpha), step / CONTRAST_STEPS.toFloat())
        if (candidate.contrastAgainst(surface) >= minContrast) return candidate
    }
    return target.copy(alpha = alpha)
}
