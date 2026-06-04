package org.polyfrost.oneconfig.internal.ui

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.utils.v1.NetworkUtils
import java.nio.file.Files

object PlayerHeadLoader {
    fun loadLocalPlayerHeadPng(): ByteArray? =
        loadFromCachedSkin() ?: loadFromWeb()

    private fun loadFromCachedSkin(): ByteArray? {
        val pixels = PlayerHeadSkinAccess.loadSkinImage(Minecraft.getInstance()) ?: return null
        return encodeHeadPng(pixels)
    }

    private fun encodeHeadPng(skin: NativeImage): ByteArray? {
        val head = PlayerHeadBlit.extractHead(skin)
        return try {
            val path = Files.createTempFile("oneconfig-player-head", ".png")
            try {
                head.writeToFile(path)
                Files.readAllBytes(path)
            } finally {
                Files.deleteIfExists(path)
                head.close()
            }
        } catch (_: Exception) {
            head.close()
            null
        }
    }

    private fun loadFromWeb(): ByteArray? {
        val mc = Minecraft.getInstance()
        val uuid = mc.user.profileId.toString().replace("-", "")
        val url = "https://mc-heads.net/avatar/$uuid/32.png"
        return runCatching {
            NetworkUtils.setupConnection(url).use { it.readBytes() }
        }.getOrNull()
    }
}
