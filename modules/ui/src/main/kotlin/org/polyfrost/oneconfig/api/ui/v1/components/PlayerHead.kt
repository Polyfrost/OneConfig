@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.playerHeadBridge

@ExperimentalOneConfigUiApi
object PlayerHeads {
    @JvmStatic
    fun face(id: UUID): ImageBitmap? = runCatching { playerHeadBridge()?.face(id) }.getOrNull()

    @JvmStatic
    fun defaultFace(id: UUID): ImageBitmap? = runCatching { playerHeadBridge()?.defaultFace(id) }.getOrNull()
}

@ExperimentalOneConfigUiApi
@Composable
fun PlayerHead(playerId: UUID, size: Dp, modifier: Modifier) {
    PlayerHead(playerId, size, modifier, null)
}

@ExperimentalOneConfigUiApi
@Composable
fun PlayerHead(playerId: UUID, size: Dp, modifier: Modifier, shape: Shape?) {
    val clip = shape ?: RoundedCornerShape(size / 4)
    val observed = remember(playerId) { runCatching { playerHeadBridge()?.observe(playerId) }.getOrNull() }
    val face = observed?.value
    if (face != null) {
        Image(BitmapPainter(face, filterQuality = FilterQuality.None), null, modifier.size(size).clip(clip))
        return
    }
    Box(modifier.size(size).clip(clip).background(placeholderColor(playerId), clip), contentAlignment = Alignment.Center) {
        Text(
            playerId.toString().take(1).uppercase(),
            Modifier,
            Color.White,
            (size.value * 0.4f).sp,
            null,
            BaselineShift.None,
            FontWeight.Bold,
            TextAlign.Center,
            1,
            TextOverflow.Clip,
        )
    }
}

private fun placeholderColor(id: UUID): Color = Color.hsv(id.hashCode().mod(360).toFloat(), 0.45f, 0.55f)
