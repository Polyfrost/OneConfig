@file:OptIn(ExperimentalOneConfigUiApi::class)
@file:Suppress("DEPRECATION")

package org.polyfrost.oneconfig.api.ui.v1.internal

import java.util.UUID
import java.util.WeakHashMap
import org.polyfrost.compose.minecraft.mc.McPlayerFaces
import org.polyfrost.compose.minecraft.mc.McSkinLayers
import org.polyfrost.compose.minecraft.mc.McSkinLayersResult
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerSkinLayers
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerSkinLayersResult
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

class OneConfigUIPlayerHeadBridgeImpl : OneConfigUIPlayerHeadBridge {
    private val wrapped = WeakHashMap<McSkinLayers, PlayerSkinLayers>()

    override val revision: Int
        get() = McPlayerFaces.revision

    override fun head(id: UUID?): PolyBitmap? = McPlayerFaces.face(id)

    override fun defaultHead(id: UUID): PolyBitmap? = McPlayerFaces.defaultLayers(id)?.flattened

    override fun localPlayerId(): UUID? = McPlayerFaces.localPlayerId()

    override fun localPlayerName(): String? = runCatching { Minecraft.getInstance().user.name }.getOrNull()

    override fun skinTexture(id: UUID): String? = runCatching { clientSkinLocation(id) }.getOrNull()?.toString()

    override fun skinLayers(id: UUID): PlayerSkinLayers? {
        if (skinTexture(id) == null) return null
        return McPlayerFaces.layers(id)?.let(::wrap)
    }

    override fun skinLayers(texture: String): PlayerSkinLayersResult = when (val result = McPlayerFaces.layers(texture)) {
        is McSkinLayersResult.Loaded -> PlayerSkinLayersResult.Loaded(wrap(result.layers))
        McSkinLayersResult.Pending -> PlayerSkinLayersResult.Pending
        McSkinLayersResult.Unreadable -> PlayerSkinLayersResult.Unreadable
    }

    private fun wrap(layers: McSkinLayers): PlayerSkinLayers =
        synchronized(wrapped) { wrapped.getOrPut(layers) { PlayerSkinLayers(layers.texture, layers.face, layers.hat) } }

    private fun clientSkinLocation(id: UUID): Identifier? {
        val mc = Minecraft.getInstance()
        //? if > 1.8.9 {
        val skin = mc.player?.takeIf { it.uuid == id }?.skin ?: mc.connection?.getPlayerInfo(id)?.skin ?: return null
        //? if >= 1.21.10 {
        return skin.body().texturePath()
        //?} else
        //return skin.texture()
        //?} else {
        /*mc.player?.takeIf { it.uuid == id }?.let { return it.skin.texturePath() }
        return mc.networkHandler?.getOnlinePlayer(id)?.skinTexture
        *///?}
    }
}
