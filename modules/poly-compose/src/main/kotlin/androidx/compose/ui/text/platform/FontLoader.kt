@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontResourceLoaderWithResolver
import org.polyfrost.compose.backend.PolyText

@Suppress("DEPRECATION")
class FontLoader : Font.ResourceLoader, FontResourceLoaderWithResolver {
    override val fontFamilyResolver: FontFamily.Resolver = PolyText.createFontFamilyResolver()

    @Deprecated("Replaced by FontFamily.Resolver")
    override fun load(font: Font): Any = PolyText.typefaces.loadBlocking(font) ?: error("cannot load $font")
}
