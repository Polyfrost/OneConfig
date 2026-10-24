package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.round
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.SolidColor
import org.polyfrost.oneconfig.api.notifications.v1.NotificationTheme
import org.polyfrost.oneconfig.internal.ThemeConfig

private var _accent by mutableStateOf(Color(ThemeConfig.accentColor.argb))

private var _accentEnd by mutableStateOf<Color?>(null)

val Accent: Color get() = _accent

val AccentEnd: Color get() = _accentEnd ?: _accent

val AccentGradient: Boolean get() = _accentEnd != null

val AccentBrush: Brush get() = _accentEnd?.let { Brush.horizontalGradient(listOf(_accent, it)) } ?: SolidColor(_accent)

fun updateAccent() {
    val accentColor = ThemeConfig.accentColor
    _accent = Color(accentColor.argb)
    _accentEnd = if (accentColor.gradientEnabled && !accentColor.chroma) Color(accentColor.gradientArgb) else null
}

val LocalTheme = compositionLocalOf<UITheme> { error("A UI theme is required but was not provided") }

private const val EM_STEP_PX = 5f

private const val MIN_EM_PX = 10f

internal fun snapScaleToPixelGrid(anchorPx: Float, max: Float): Float {
    if (anchorPx <= 0f) return 1f
    val fits = floor(anchorPx * max / EM_STEP_PX) * EM_STEP_PX
    val nearest = round(anchorPx / EM_STEP_PX) * EM_STEP_PX
    val em = maxOf(nearest, fits)
    val scale = em.coerceAtLeast(MIN_EM_PX) / anchorPx
    return if (scale <= max) scale else max
}

@Composable
fun Theme(content: @Composable () -> Unit) {
    SideEffect { updateAccent() }

    LaunchedEffect(Unit) {
        while (true) {
            if (ThemeConfig.accentColor.chroma) withFrameNanos { updateAccent() } else delay(250)
        }
    }

    val target = ThemeRegistry.activeTheme ?: ThemeRegistry.registry.first()
    val animated = animateTheme(target)

    CompositionLocalProvider(LocalTheme provides animated, content = content)
}

internal fun syncNotificationTheme(theme: UITheme) {
    val minecraft = theme.previewImage.startsWith("minecraft")
    NotificationTheme.set(
        background = theme.popupBackground,
        border = theme.borderColor,
        textPrimary = theme.textColor,
        textSecondary = theme.textColorSecondary,
        accent = Accent,
        subtleButton = theme.componentBackground,
        actionPrimaryText = theme.accentTextColor,
        actionSubtleText = theme.textColor,
        fontTitle = if (minecraft) "minecraft-bold" else "poppins-medium",
        fontBody = if (minecraft) "minecraft" else null,
        radiusCard = if (minecraft) 0f else 12f,
        radiusButton = if (minecraft) 0f else 4f,
        radiusProgress = if (minecraft) 0f else 3f,
    )
    NotificationTheme.useIconFolder(if (minecraft) "minecraft" else null)
}
