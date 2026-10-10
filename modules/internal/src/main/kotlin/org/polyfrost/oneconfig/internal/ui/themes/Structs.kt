package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

/** Sets a panel background color's alpha from an opacity percentage between 0 and 100 */
fun Color.withOpacityPercent(percent: Float): Color =
    copy(alpha = (percent / 100f).coerceIn(0f, 1f))

data class UITheme(
    val previewImage: String,
    val name: String,

    val pageBackground: Color,
    val sidebarBackground: Color,
    val chipBackground: Color,
    val modCardBackground: Color,
    val componentBackground: Color,
    val popupBackground: Color,

    val borderColor: Color,
    val textColor: Color,
    val textColorSecondary: Color,
    val accentTextColor: Color,

    val shadowColor: Color,
    val controlThumbColor: Color,
    val favoriteColor: Color,

    val shadowEnabled: Boolean,

    val backgroundShape: Shape,
    val sideBarNavigationEntryShape: Shape,
    val modCardShape: Shape,
    val checkBoxShape: Shape,
    val buttonShape: Shape,
    val popupShape: Shape,
    val circleShape: Shape,

    val branding: UIBranding,
    val typography: UITypography,
    val iconOverrides: Map<String, String> = emptyMap(),
) {
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

    companion object {
        val DefaultControlTrackColor = Color(0xFF74777F)

        private val DefaultExtraShapes: Pair<Shape, Shape> = RoundedCornerShape(4.dp) to RoundedCornerShape(8.dp)

        private val extraShapes = ConcurrentHashMap<String, Pair<Shape, Shape>>()
    }
}

data class UIBranding(
    val logoPath: String
)

data class UITypography(
    val family: FontFamily,
)
