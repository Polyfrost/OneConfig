@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.UUID
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.playerHeadBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public object PlayerHeads {
    @JvmStatic
    public fun face(id: UUID): ImageBitmap? = runCatching { playerHeadBridge()?.face(id) }.getOrNull()

    @JvmStatic
    public fun defaultFace(id: UUID): ImageBitmap? = runCatching { playerHeadBridge()?.defaultFace(id) }.getOrNull()

    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun bitmap(playerId: UUID?): PolyBitmap? =
        playerId?.let { id -> runCatching { playerHeadBridge()?.bitmap(id) }.getOrNull() }
}

@ExperimentalOneConfigUiApi
@Composable
public fun PlayerHead(
    playerId: UUID?,
    size: Dp,
    modifier: Modifier = Modifier,
    name: String? = null,
    shape: Shape = OneConfigTheme.shapes.small,
    contentDescription: String? = name,
) {
    val described = modifier
        .size(size)
        .clip(shape)
        .then(
            if (contentDescription == null) Modifier
            else Modifier.semantics {
                this.contentDescription = contentDescription
                role = Role.Image
            },
        )
    val observed = remember(playerId) { playerId?.let { id -> runCatching { playerHeadBridge()?.observe(id) }.getOrNull() } }
    val face = observed?.value
    if (face != null) {
        Image(BitmapPainter(face, filterQuality = FilterQuality.None), null, described)
        return
    }
    val theme = OneConfigTheme.current
    val initial = name?.firstOrNull { it.isLetterOrDigit() }?.uppercase()
        ?: playerId?.toString()?.take(1)?.uppercase()
        ?: "?"
    val fontSize = with(LocalDensity.current) { (size * 0.4f).toSp() }
    Box(described.background(placeholderColor(playerId, name), shape), contentAlignment = Alignment.Center) {
        Text(
            initial,
            color = theme.onMedia,
            style = theme.typography.body.copy(fontSize = fontSize, fontWeight = FontWeight.Bold),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun PlayerHeadStack(ids: List<UUID>, size: Dp, modifier: Modifier = Modifier, max: Int = 4) {
    val theme = OneConfigTheme.current
    val shape = theme.shapes.small
    val shown = ids.take(max.coerceAtLeast(1))
    val extra = ids.size - shown.size
    val ring = theme.pageSurface
    Layout(
        content = {
            for (id in shown) PlayerHead(id, size, Modifier.border(1.5.dp, ring, shape), name = null, shape = shape)
            if (extra > 0) {
                Box(
                    Modifier
                        .size(size)
                        .clip(shape)
                        .background(theme.componentBackground, shape)
                        .border(1.5.dp, ring, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    val fontSize = with(LocalDensity.current) { (size * 0.36f).toSp() }
                    Text("+$extra", color = theme.textColorSecondary, style = theme.typography.small.copy(fontSize = fontSize), maxLines = 1)
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val side = size.roundToPx()
        val step = side - (size / 4).roundToPx()
        val placeables = measurables.map { it.measure(Constraints.fixed(side, side)) }
        val width = if (placeables.isEmpty()) 0 else step * (placeables.size - 1) + side
        layout(constraints.constrainWidth(width), constraints.constrainHeight(side)) {
            placeables.forEachIndexed { index, placeable -> placeable.place(step * index, 0) }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun PlayerHead(playerId: UUID, size: Dp, modifier: Modifier, shape: Shape?) {
    PlayerHead(playerId, size, modifier, null, shape ?: OneConfigTheme.shapes.small)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun PlayerHead(playerId: UUID, size: Dp, modifier: Modifier) {
    PlayerHead(playerId = playerId, size = size, modifier = modifier, name = null)
}

private fun placeholderColor(id: UUID?, name: String?): Color =
    Color.hsv(((id?.hashCode() ?: name?.hashCode()) ?: 0).mod(360).toFloat(), 0.45f, 0.55f)
