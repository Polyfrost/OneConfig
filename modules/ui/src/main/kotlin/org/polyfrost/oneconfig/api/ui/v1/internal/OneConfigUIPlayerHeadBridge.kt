package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import java.util.UUID
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerSkinLayers
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerSkinLayersResult

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
public interface OneConfigUIPlayerHeadBridge {
    public fun face(id: UUID): ImageBitmap?

    public fun observe(id: UUID): State<ImageBitmap?>

    public fun defaultFace(id: UUID): ImageBitmap?

    public fun defaultBitmap(id: UUID): PolyBitmap? = null

    public fun bitmap(id: UUID): PolyBitmap?

    public fun loadedFace(id: UUID): ImageBitmap?

    public fun loadedBitmap(id: UUID): PolyBitmap?

    public fun localPlayerId(): UUID?

    public fun localPlayerName(): String?

    public fun skinTexture(id: UUID): String?

    public fun skinLayers(id: UUID): PlayerSkinLayers?

    public fun skinLayers(texture: String): PlayerSkinLayersResult
}

@ExperimentalOneConfigUiApi
private val loadedHeadBridge: OneConfigUIPlayerHeadBridge? by lazy {
    loadBridge(OneConfigUIPlayerHeadBridge::class.java, "player heads fall back to a placeholder")
}

@ExperimentalOneConfigUiApi
internal fun playerHeadBridge(): OneConfigUIPlayerHeadBridge? = loadedHeadBridge
