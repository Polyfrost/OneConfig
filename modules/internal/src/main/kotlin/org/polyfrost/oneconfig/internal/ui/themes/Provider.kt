package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.round
import kotlinx.coroutines.delay
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.notifications.v1.NotificationTheme
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.DESIGN_HEIGHT_DP
import org.polyfrost.oneconfig.internal.ui.DESIGN_WIDTH_DP
import org.polyfrost.oneconfig.internal.ui.EDGE_MARGIN_FRACTION

private var _accent by mutableStateOf(Color(ThemeConfig.accentColor.argb))

private var _chroma by mutableStateOf(ThemeConfig.accentColor.chroma)

private var _accentEnd by mutableStateOf<Color?>(null)

val Accent: Color get() = _accent

val AccentEnd: Color get() = _accentEnd ?: _accent

val AccentGradient: Boolean get() = _accentEnd != null

val AccentBrush: Brush get() = accentHorizontalBrush()

fun accentHorizontalBrush(startX: Float = 0f, endX: Float = Float.POSITIVE_INFINITY): Brush =
    _accentEnd?.let { Brush.horizontalGradient(listOf(_accent, it), startX, endX) } ?: SolidColor(_accent)

fun updateAccent() {
    val accentColor = ThemeConfig.accentColor
    _accent = Color(accentColor.argb)
    _chroma = accentColor.chroma
    _accentEnd = if (accentColor.gradientEnabled && !accentColor.chroma) Color(accentColor.gradientArgb) else null
}

val LocalTheme = compositionLocalOf<UITheme> { error("A UI theme is required but was not provided") }

private const val GRID_ANCHOR_SP = 14f

private const val EM_STEP_PX = 5f

private const val MIN_EM_PX = 10f

private const val UI_SCALE_SETTLE_MS = 400L

var uiScalePercent by mutableStateOf(100f)

var pixelGridMode by mutableStateOf(0)

private val UITheme.isMinecraft get() = previewImage.startsWith("minecraft")

private fun scrollbarStyle(theme: UITheme) = ScrollbarStyle(
    minimalHeight = 24.dp,
    thickness = 8.dp,
    shape = theme.scrollBarShape,
    hoverDurationMillis = 300,
    unhoverColor = theme.textColorSecondary.copy(alpha = 0.40f),
    hoverColor = theme.textColorSecondary.copy(alpha = 0.70f),
)

private val screenPlatform by lazy { runCatching { Platform.screen() }.getOrNull() }

private fun surfaceRatio(): Float = screenPlatform?.surfaceRatio()?.takeIf { it > 0f } ?: 1f

@Composable
private fun gridAnchorPx(scale: Float, anchorSp: Float = GRID_ANCHOR_SP): Float {
    val density = LocalDensity.current
    return anchorSp * density.fontScale * density.density * scale * surfaceRatio()
}

@Composable
fun pixelGridScale(scale: Float, max: Float, anchorSp: Float = GRID_ANCHOR_SP): Float {
    if (scale <= 0f) return scale
    return scale * snapScaleToPixelGrid(gridAnchorPx(scale, anchorSp), max / scale)
}

@Composable
private fun pixelGridDensity(designWidth: Dp, designHeight: Dp, theme: UITheme): Density {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val headroom = with(density) {
        minOf(
            (window.width  * EDGE_MARGIN_FRACTION) / designWidth.toPx(),
            (window.height * EDGE_MARGIN_FRACTION) / designHeight.toPx(),
        )
    }
    val base = pixelGridScale(1f, headroom)
    var percent by remember { mutableStateOf(uiScalePercent) }
    LaunchedEffect(uiScalePercent) {
        if (percent != uiScalePercent) {
            delay(UI_SCALE_SETTLE_MS)
            percent = uiScalePercent
        }
    }
    val user = base * percent.coerceIn(50f, 200f) / 100f
    val snap = pixelGridMode == 1 || (pixelGridMode == 0 && theme.isMinecraft)
    val scale = when {
        percent == 100f -> base
        snap -> user * snapScaleToNearestStep(gridAnchorPx(user))
        else -> user
    }
    return remember(density, scale) {
        if (scale == 1f) density else Density(density.density * scale, density.fontScale)
    }
}

internal fun snapScaleToPixelGrid(anchorPx: Float, max: Float): Float {
    if (anchorPx <= 0f) return 1f
    val fits = floor(anchorPx * max / EM_STEP_PX) * EM_STEP_PX
    val nearest = round(anchorPx / EM_STEP_PX) * EM_STEP_PX
    val em = maxOf(nearest, fits)
    val scale = em.coerceAtLeast(MIN_EM_PX) / anchorPx
    return if (scale <= max) scale else max
}

internal fun snapScaleToNearestStep(anchorPx: Float): Float {
    if (anchorPx <= 0f) return 1f
    return (round(anchorPx / EM_STEP_PX) * EM_STEP_PX).coerceAtLeast(MIN_EM_PX) / anchorPx
}

@Composable
fun Theme(content: @Composable () -> Unit) = Theme(pixelGrid = false, content = content)

@Composable
fun Theme(
    pixelGrid: Boolean,
    designWidth: Dp = DESIGN_WIDTH_DP.dp,
    designHeight: Dp = DESIGN_HEIGHT_DP.dp,
    content: @Composable () -> Unit,
) {
    SideEffect { updateAccent() }

    LaunchedEffect(_chroma) {
        while (_chroma) {
            withFrameNanos { }
            updateAccent()
        }
    }

    val target = ThemeRegistry.activeTheme ?: error("No active theme provided")
    val animated = animateTheme(target)

    SideEffect { syncNotificationTheme(animated) }

    CompositionLocalProvider(
        LocalTheme provides animated,
        LocalScrollbarStyle provides remember(animated.scrollBarShape) { scrollbarStyle(animated) },
        LocalDensity provides if (pixelGrid) pixelGridDensity(designWidth, designHeight, target) else LocalDensity.current,
        content = content
    )
}

internal fun syncNotificationTheme(theme: UITheme) {
    val minecraft = theme.isMinecraft
    NotificationTheme.set(
        background = PolyColor(theme.popupBackground.toArgb()),
        border = PolyColor(theme.borderColor.toArgb()),
        textPrimary = PolyColor(theme.textColor.toArgb()),
        textSecondary = PolyColor(theme.textColorSecondary.toArgb()),
        accent = PolyColor(Accent.toArgb()),
        subtleButton = PolyColor(theme.componentBackground.toArgb()),
        actionPrimaryText = PolyColor(theme.accentTextColor.toArgb()),
        actionSubtleText = PolyColor(theme.textColor.toArgb()),
        fontTitle = if (minecraft) "minecraft-bold" else "poppins-medium",
        fontBody = if (minecraft) "minecraft" else null,
        radiusCard = if (minecraft) 0f else 12f,
        radiusButton = if (minecraft) 0f else 4f,
        radiusProgress = if (minecraft) 0f else 3f,
    )
    NotificationTheme.useIconFolder(if (minecraft) "minecraft" else null)
}
