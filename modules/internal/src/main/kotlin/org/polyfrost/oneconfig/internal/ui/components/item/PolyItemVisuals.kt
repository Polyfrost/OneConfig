package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.runtime.Composable
import org.jetbrains.skia.Rect
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.PolyRow
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.PolyColor

private val PlaceholderColor = PolyColor(0x40FFFFFF)

@Composable
fun PolyItemIcon(
    id: String,
    size: Float = 16f,
    modifier: PolyModifier = PolyModifier,
    placeholderColor: PolyColor = PlaceholderColor,
) {
    val iconReady = rememberItemIconReady(id)
    PolyCanvas(modifier = modifier.size(size, size)) { x, y, w, h ->
        if (!iconReady || !ItemCatalog.drawIcon(id, canvas, Rect.makeLTRB(x, y, x + w, y + h))) {
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
