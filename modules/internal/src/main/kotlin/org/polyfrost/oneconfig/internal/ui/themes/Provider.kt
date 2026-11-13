package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.CornerBasedShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round
import kotlinx.coroutines.delay
import org.polyfrost.compose.backend.PolyFontSource
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.notifications.v1.NotificationTheme
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle
import org.polyfrost.oneconfig.api.ui.v1.theme.contrastAgainst
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.DESIGN_HEIGHT_DP
import org.polyfrost.oneconfig.internal.ui.DESIGN_WIDTH_DP
import org.polyfrost.oneconfig.internal.ui.EDGE_MARGIN_FRACTION

private var _accent by mutableStateOf(Color(ThemeConfig.accentColor.argb))

private var _chroma by mutableStateOf(ThemeConfig.accentColor.chroma)

private var _accentEnd by mutableStateOf<Color?>(null)

private fun paletteAccent(): Color? = ThemeRegistry.activeTheme?.palette?.let(PaletteAccent::resolve)

val Accent: Color get() = paletteAccent() ?: _accent

val PlayerAccent: Color get() = _accent

val AccentEnd: Color get() = paletteAccent() ?: _accentEnd ?: _accent

val AccentGradient: Boolean get() = paletteAccent() == null && _accentEnd != null

val AccentBrush: Brush get() = accentHorizontalBrush()

private val SheenBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.04f)))

val AccentSheen: Brush? get() = if (ThemeRegistry.activeTheme?.liquidGlass == true) SheenBrush else null

fun accentHorizontalBrush(startX: Float = 0f, endX: Float = Float.POSITIVE_INFINITY): Brush =
    paletteAccent()?.let { SolidColor(it) } ?: _accentEnd?.let { Brush.horizontalGradient(listOf(_accent, it), startX, endX) } ?: SolidColor(_accent)

fun updateAccent() {
    val accentColor = ThemeConfig.accentColor
    _accent = Color(accentColor.argb)
    _chroma = accentColor.chroma
    _accentEnd = if (accentColor.gradientEnabled && !accentColor.chroma) Color(accentColor.gradientArgb) else null
}

val LocalTheme = compositionLocalOf<UITheme> {
    runCatching { ThemeRegistry.activeTheme }.getOrNull()
        ?: error("The OneConfig UI has not initialised yet, so there is no theme to use. Wait for OneConfig to finish loading before composing OneConfig UI.")
}

private const val TOAST_MIN_ALPHA = 0.92f

private fun Color.withMinAlpha(min: Float): Color = if (alpha >= min) this else copy(alpha = min)

private const val GRID_ANCHOR_SP = 14f

private const val EM_STEP_PX = 5f

private const val MIN_EM_PX = 10f

private const val UI_SCALE_SETTLE_MS = 400L

var uiScalePercent by mutableStateOf(100f)

var pixelGridMode by mutableStateOf(0)

private val UITheme.isMinecraft get() = isMinecraftStyle

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
    val snap = pixelGridMode == 1 || (pixelGridMode == 0 && theme.isMinecraft)
    val base = if (snap) pixelGridScale(1f, headroom) else headroom
    var percent by remember { mutableStateOf(uiScalePercent) }
    LaunchedEffect(uiScalePercent) {
        if (percent != uiScalePercent) {
            delay(UI_SCALE_SETTLE_MS)
            percent = uiScalePercent
        }
    }
    val user = base * percent.coerceIn(50f, 200f) / 100f
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
) = ThemeWith(null, pixelGrid, designWidth, designHeight, content)

@Composable
fun ThemeWith(
    theme: UITheme?,
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

    val target = theme ?: ThemeRegistry.activeTheme ?: error("No active theme provided")
    val accent = Accent
    val opaque = ReduceTransparency.enabled
    val shown = target.withAccentText(target.accentTextOn(accent)).let { if (opaque) it.withOpaqueSurfaces() else it }
    val animated = animateTheme(shown)

    LaunchedEffect(target, accent, opaque) { syncNotificationTheme(target) }
    if (animated.liquidGlass) GlassTintColor.Drive()

    CompositionLocalProvider(
        LocalTheme provides animated,
        LocalScrollbarStyle provides remember(animated.scrollBarShape, animated.textColorSecondary) { scrollbarStyle(animated) },
        LocalDensity provides if (pixelGrid) pixelGridDensity(designWidth, designHeight, target) else LocalDensity.current,
        content = content
    )
}

internal fun syncNotificationTheme(source: UITheme) {
    val accent = Accent
    val theme = if (ReduceTransparency.enabled) source.withOpaqueSurfaces() else source
    val pixel = theme.style == ThemeStyle.Pixel
    val unitDensity = Density(1f)

    fun poly(color: Color): PolyColor = PolyColor(color.toArgb())

    fun onFill(fill: Color): Color =
        if (Color.White.contrastAgainst(fill) >= Color.Black.contrastAgainst(fill)) Color.White else Color.Black

    fun radius(shape: Shape, width: Float, height: Float): Float =
        (shape as? CornerBasedShape)?.topStart?.toPx(Size(width, height), unitDensity) ?: 0f

    fun fontKey(style: TextStyle): String? {
        val wanted = (style.fontWeight ?: FontWeight.Normal).weight
        val font = (style.fontFamily as? FontListFontFamily)?.fonts.orEmpty()
            .filterIsInstance<PolyFontSource>()
            .filter { it.style == FontStyle.Normal }
            .minByOrNull { abs(it.weight.weight - wanted) } ?: return null
        val key = if (pixel) "${font.key}#pixel" else font.key
        if (FontManager.fontData(key) != null) return key
        val loaded = FontManager.fontData(font.key)?.let { runCatching { FontManager.register(key, it, pixel) }.isSuccess }
            ?: when {
                font.key.startsWith("res:") -> FontManager.loadFromResource(font.key.removePrefix("res:"), key, pixelAligned = pixel)
                font.key.startsWith("file:") -> runCatching { FontManager.register(key, File(font.key.removePrefix("file:")).readBytes(), pixel) }.isSuccess
                else -> false
            }
        return key.takeIf { loaded }
    }

    NotificationTheme.set(
        background = poly(theme.popupBackground.withMinAlpha(TOAST_MIN_ALPHA)),
        border = poly(theme.borderColor),
        textPrimary = poly(theme.textColor),
        textSecondary = poly(theme.textColorSecondary),
        accent = poly(accent),
        subtleButton = poly(theme.componentBackground.withMinAlpha(TOAST_MIN_ALPHA)),
        actionPrimaryText = poly(theme.accentTextOn(accent)),
        actionSubtleText = poly(theme.textColor),
        fontTitle = fontKey(theme.typography.heading),
        fontBody = fontKey(theme.typography.small),
        radiusCard = radius(theme.shapes.large, 340f, 72f),
        radiusButton = radius(theme.shapes.medium, 80f, 26f),
        radiusProgress = radius(theme.shapes.pill, 312f, 6f),
        success = poly(theme.success),
        error = poly(theme.error),
        warning = poly(theme.warning),
        info = poly(theme.info),
        onSuccess = poly(theme.onSuccess),
        onError = poly(theme.onError),
        onWarning = poly(onFill(theme.warning)),
        onInfo = poly(onFill(theme.info)),
    )
    NotificationTheme.useIconFolder(if (pixel) "minecraft" else null)
}
