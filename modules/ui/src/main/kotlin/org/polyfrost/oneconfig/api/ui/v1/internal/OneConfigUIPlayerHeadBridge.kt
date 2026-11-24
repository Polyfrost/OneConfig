package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerSkinLayers
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerSkinLayersResult
import java.util.UUID

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
@Suppress("DEPRECATION")
public interface OneConfigUIPlayerHeadBridge {
    public val revision: Int

    public fun head(id: UUID?): PolyBitmap?

    public fun defaultHead(id: UUID): PolyBitmap?

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
