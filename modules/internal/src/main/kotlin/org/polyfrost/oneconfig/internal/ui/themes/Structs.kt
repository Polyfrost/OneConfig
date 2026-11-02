package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme

/** Sets a panel background color's alpha from an opacity percentage between 0 and 100 */
fun Color.withOpacityPercent(percent: Float): Color =
    copy(alpha = (percent / 100f).coerceIn(0f, 1f))

const val DEFAULT_PAGE_OPACITY = 88f

const val DEFAULT_SIDEBAR_OPACITY = 80f

fun Color.scaleOpacity(percent: Float, defaultPercent: Float): Color =
    copy(alpha = (alpha * percent / defaultPercent).coerceIn(0f, 1f))

fun UITheme.surfaceOpacity(color: Color, percent: Float, defaultPercent: Float): Color =
    if (liquidGlass) color.scaleOpacity(percent, defaultPercent) else color.withOpacityPercent(percent)

data class UITheme(
    val previewImage: String,
    override val name: String,

    override val pageBackground: Color,
    override val sidebarBackground: Color,
    override val chipBackground: Color,
    override val modCardBackground: Color,
    override val componentBackground: Color,
    override val popupBackground: Color,

    override val borderColor: Color,
    override val textColor: Color,
    override val textColorSecondary: Color,
    override val accentTextColor: Color,

    override val shadowColor: Color,
    override val controlThumbColor: Color,
    override val favoriteColor: Color,

    override val shadowEnabled: Boolean,

    override val backgroundShape: Shape,
    override val sideBarNavigationEntryShape: Shape,
    override val modCardShape: Shape,
    override val checkBoxShape: Shape,
    override val buttonShape: Shape,
    override val popupShape: Shape,
    override val circleShape: Shape,

    val branding: UIBranding,
    val typography: UITypography,
    val iconOverrides: Map<String, String> = emptyMap(),
) : OneConfigUITheme() {
    override val fontFamily: FontFamily get() = typography.family

    override val logoPath: String get() = brandedLogoPath(this)

    var controlTrackColor: Color = DefaultControlTrackColor
        private set

    fun withControlTrackColor(color: Color): UITheme =
        copy().also { it.controlTrackColor = color }

    // The shapes below are kept out of the constructor. PolyPlus calls the generated copy() with the
    // exact signature it was compiled against, so a new constructor parameter crashes every released
    // build of it on startup. They are looked up by theme id so they also survive that copy()
    val modIconShape: Shape get() = (extraShapes[previewImage] ?: DefaultExtraShapes).first

    val scrollBarShape: Shape get() = (extraShapes[previewImage] ?: DefaultExtraShapes).second

    fun withShapes(modIconShape: Shape, scrollBarShape: Shape): UITheme =
        also { extraShapes[previewImage] = modIconShape to scrollBarShape }

    val glowEnabled: Boolean get() = previewImage !in glowlessThemes

    fun withoutGlow(): UITheme =
        also { glowlessThemes += previewImage }

    val insetModFooter: Boolean get() = previewImage in insetFooterThemes

    fun withInsetModFooter(): UITheme =
        also { insetFooterThemes += previewImage }

    val translucentSelection: Boolean get() = previewImage in translucentSelectionThemes

    fun withTranslucentSelection(): UITheme =
        also { translucentSelectionThemes += previewImage }

    val liquidGlass: Boolean get() = previewImage in liquidGlassThemes

    fun withLiquidGlass(): UITheme =
        also { liquidGlassThemes += previewImage }

    val cursorEffects: Boolean get() = previewImage in cursorEffectThemes

    fun withCursorEffects(): UITheme =
        also { cursorEffectThemes += previewImage }

    companion object {
        val DefaultControlTrackColor = Color(0xFF74777F)

        private val DefaultExtraShapes: Pair<Shape, Shape> = RoundedCornerShape(4.dp) to RoundedCornerShape(8.dp)

        private val extraShapes = ConcurrentHashMap<String, Pair<Shape, Shape>>()

        private val glowlessThemes: MutableSet<String> = ConcurrentHashMap.newKeySet()

        private val insetFooterThemes: MutableSet<String> = ConcurrentHashMap.newKeySet()

        private val translucentSelectionThemes: MutableSet<String> = ConcurrentHashMap.newKeySet()

        private val liquidGlassThemes: MutableSet<String> = ConcurrentHashMap.newKeySet()

        private val cursorEffectThemes: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }
}

data class UIBranding(
    val logoPath: String
)

data class UITypography(
    val family: FontFamily,
)

private fun brandedLogoPath(theme: UITheme): String = OneConfigTheme.logoOverride ?: theme.branding.logoPath
