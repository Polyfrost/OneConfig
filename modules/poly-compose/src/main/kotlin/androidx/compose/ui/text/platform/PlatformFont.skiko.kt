@file:JvmName("PlatformFont_skikoKt")

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.polyfrost.compose.backend.PolyFontSource


fun Font(identity: String, data: ByteArray, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal): Font =
    PolyFontSource("bytes:$identity", { data }, weight, style)

fun Font(identity: String, getData: () -> ByteArray, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal): Font =
    PolyFontSource("bytes:$identity", getData, weight, style)
