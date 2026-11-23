@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.polyfrost.compose.backend.asComposeImageBitmap
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.playerHeadBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.WeakHashMap

@ExperimentalOneConfigUiApi
public object PlayerHeads {
    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.face(id)?.asComposeImageBitmap()", FACES, IMAGE))
    @JvmStatic
    public fun face(id: UUID): ImageBitmap? = head(id)?.image()

    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.defaultLayers(id)?.flattened?.asComposeImageBitmap()", FACES, IMAGE))
    @JvmStatic
    public fun defaultFace(id: UUID): ImageBitmap? = defaultHead(id)?.image()

    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.defaultLayers(id)?.flattened", FACES))
    @JvmStatic
    public fun defaultBitmap(id: UUID): PolyBitmap? = defaultHead(id)

    @Deprecated(MOVED, ReplaceWith("playerId?.let { McPlayerFaces.face(it) }", FACES))
    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun bitmap(playerId: UUID?): PolyBitmap? = playerId?.let { head(it) }

    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.face(id)?.asComposeImageBitmap()", FACES, IMAGE))
    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun face(id: UUID, preferLoadedSkin: Boolean): ImageBitmap? = head(id)?.image()

    @Deprecated(MOVED, ReplaceWith("playerId?.let { McPlayerFaces.face(it) }", FACES))
    @ExperimentalOneConfigUiApi
    @JvmStatic
    public fun bitmap(playerId: UUID?, preferLoadedSkin: Boolean): PolyBitmap? = playerId?.let { head(it) }

    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.localPlayerId()", FACES))
    @JvmStatic
    public fun localPlayerId(): UUID? = localId()

    @JvmStatic
    public fun localPlayerName(): String? = runCatching { playerHeadBridge()?.localPlayerName() }.getOrNull()

    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.face(null)?.asComposeImageBitmap()", FACES, IMAGE))
    @JvmStatic
    public fun localFace(): ImageBitmap? = localId()?.let { head(it) }?.image()

    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.layers(id)?.texture", FACES))
    @JvmStatic
    public fun skinTexture(id: UUID): String? = runCatching { playerHeadBridge()?.skinTexture(id) }.getOrNull()

    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.layers(id)", FACES))
    @Suppress("DEPRECATION")
    @JvmStatic
    public fun skinLayers(id: UUID): PlayerSkinLayers? = runCatching { playerHeadBridge()?.skinLayers(id) }.getOrNull()

    @Deprecated(MOVED, ReplaceWith("McPlayerFaces.layers(texture)", FACES))
    @Suppress("DEPRECATION")
    @JvmStatic
    public fun skinLayers(texture: String): PlayerSkinLayersResult =
        runCatching { playerHeadBridge()?.skinLayers(texture) }.getOrNull() ?: PlayerSkinLayersResult.Unreadable
}

@Deprecated(MOVED, ReplaceWith("McSkinLayersResult", "org.polyfrost.compose.minecraft.mc.McSkinLayersResult"))
@Suppress("DEPRECATION")
@ExperimentalOneConfigUiApi
public sealed interface PlayerSkinLayersResult {
    public data object Pending : PlayerSkinLayersResult

    public data object Unreadable : PlayerSkinLayersResult

    public class Loaded(public val layers: PlayerSkinLayers) : PlayerSkinLayersResult
}

@Deprecated(MOVED, ReplaceWith("McSkinLayers", "org.polyfrost.compose.minecraft.mc.McSkinLayers"))
@ExperimentalOneConfigUiApi
public class PlayerSkinLayers @InternalOneConfigUiApi constructor(
    public val texture: String,
    public val face: PolyBitmap,
    public val hat: PolyBitmap,
) {
    public val size: Int get() = face.width
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
    preferLoadedSkin: Boolean = false,
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
    val revision = runCatching { playerHeadBridge()?.revision }.getOrNull() ?: 0
    val bitmap = remember(playerId, revision) { playerId?.let { head(it) } }
    if (playerId != null) {
        LaunchedEffect(playerId) {
            while (true) {
                delay(LOADED_SKIN_REFRESH_MILLIS)
                head(playerId)
            }
        }
    }
    if (bitmap != null) {
        val image = remember(bitmap) { bitmap.image() }
        Image(BitmapPainter(image, filterQuality = FilterQuality.None), null, described)
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
public fun PlayerHead(
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = OneConfigTheme.shapes.small,
    contentDescription: String? = null,
) {
    val id = remember { localId() }
    val name = remember { PlayerHeads.localPlayerName() }
    PlayerHead(id, size, modifier, name, shape, contentDescription, preferLoadedSkin = true)
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

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun PlayerHead(playerId: UUID, size: Dp, modifier: Modifier, shape: Shape?) {
    PlayerHead(playerId, size, modifier, null, shape ?: OneConfigTheme.shapes.small)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun PlayerHead(playerId: UUID, size: Dp, modifier: Modifier) {
    PlayerHead(playerId = playerId, size = size, modifier = modifier, name = null, shape = OneConfigTheme.shapes.small)
}

private const val LOADED_SKIN_REFRESH_MILLIS = 1000L

private const val MOVED = "Moved to McPlayerFaces in PolyCompose"
private const val FACES = "org.polyfrost.compose.minecraft.mc.McPlayerFaces"
private const val IMAGE = "org.polyfrost.compose.backend.asComposeImageBitmap"

private val images = WeakHashMap<PolyBitmap, WeakReference<ImageBitmap>>()

private fun PolyBitmap.image(): ImageBitmap = synchronized(images) {
    images[this]?.get() ?: asComposeImageBitmap().also { images[this] = WeakReference(it) }
}

private fun head(id: UUID?): PolyBitmap? = runCatching { playerHeadBridge()?.head(id) }.getOrNull()

private fun defaultHead(id: UUID): PolyBitmap? = runCatching { playerHeadBridge()?.defaultHead(id) }.getOrNull()

private fun localId(): UUID? = runCatching { playerHeadBridge()?.localPlayerId() }.getOrNull()

private fun placeholderColor(id: UUID?, name: String?): Color =
    Color.hsv(((id?.hashCode() ?: name?.hashCode()) ?: 0).mod(360).toFloat(), 0.45f, 0.55f)
