package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.PolyRow
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.ImageLoader
import org.polyfrost.compose.render.PolyColor

private val PlaceholderColor = PolyColor(0x40FFFFFF)
private val itemPaint = Paint()

fun itemImage(id: String): Image? {
    ImageLoader.get(itemCacheKey(id))?.let { return it }
    val image = ItemCatalog.icon(id)?.toSkiaImage() ?: return null
    ImageLoader.put(itemCacheKey(id), image)
    return image
}

fun evictItemImage(id: String) = ImageLoader.evictKey(itemCacheKey(id))

private fun itemCacheKey(id: String) = "item:$id"

@Composable
fun rememberItemImage(id: String): Image? {
    var image by remember(id) { mutableStateOf(itemImage(id)) }
    LaunchedEffect(id) {
        if (image == null) {
            ItemCatalog.loadIcon(id) { data ->
                val loaded = data?.toSkiaImage() ?: return@loadIcon
                ImageLoader.put(itemCacheKey(id), loaded)
                image = loaded
            }
        }
    }
    return image
}

@Composable
fun PolyItemIcon(
    id: String,
    size: Float = 16f,
    modifier: PolyModifier = PolyModifier,
    placeholderColor: PolyColor = PlaceholderColor,
) {
    val image = rememberItemImage(id)
    PolyCanvas(modifier = modifier.size(size, size)) { x, y, w, h ->
        if (image != null) {
            image(image, x, y, w, h, itemPaint)
        } else {
            rectStroke(x, y, w, h, placeholderColor, strokeWidth = 1f, radius = 1f)
        }
    }
}

@Composable
fun PolyItemRow(
    ids: List<String>,
    size: Float = 16f,
    gap: Float = 2f,
    modifier: PolyModifier = PolyModifier,
) {
    PolyRow(gap = gap, modifier = modifier) {
        ids.forEach { id ->
            PolyBox(modifier = PolyModifier.size(size, size)) {
                PolyItemIcon(id, size)
            }
        }
    }
}

private fun ItemIconData.toSkiaImage(): Image? = runCatching {
    require(width > 0 && height > 0 && argb.size >= width * height)
    val bytes = ByteArray(width * height * 4)
    for (i in 0 until width * height) {
        val pixel = argb[i]
        val offset = i * 4
        bytes[offset] = (pixel and 0xFF).toByte()
        bytes[offset + 1] = (pixel shr 8 and 0xFF).toByte()
        bytes[offset + 2] = (pixel shr 16 and 0xFF).toByte()
        bytes[offset + 3] = (pixel shr 24 and 0xFF).toByte()
    }
    Image.makeRaster(
        ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL),
        bytes,
        width * 4,
    )
}.getOrNull()
