package org.polyfrost.oneconfig.internal.ui

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.utils.v1.NetworkUtils
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

object PlayerHeadLoader {
    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/PlayerHead")
    private const val CLIENT_THREAD_TIMEOUT_MS = 5_000L

    private val cache = ConcurrentHashMap<UUID, ByteArray>()
    private val inFlight = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * The head already loaded for this account if any
     *
     * Safe to call from any thread
     */
    fun cachedLocalPlayerHeadPng(mc: Minecraft): ByteArray? = profileId(mc)?.let { cache[it] }

    /**
     * Must not be called on the client thread because awaiting the skin and the web fallback both block
     *
     * @return null when the head could not be loaded or when another thread is already loading it
     */
    fun loadLocalPlayerHeadPng(mc: Minecraft): ByteArray? {
        val id = profileId(mc) ?: return loadFromCachedSkin(mc)
        cache[id]?.let { return it }
        if (!inFlight.add(id)) return null
        return try {
            (loadFromCachedSkin(mc) ?: loadFromWeb(id))?.also { cache[id] = it }
        } finally {
            inFlight.remove(id)
        }
    }

    private fun profileId(mc: Minecraft): UUID? = runCatching { mc.user.profileId }.getOrNull()

    private fun loadFromCachedSkin(mc: Minecraft): ByteArray? {
        val head = onClientThread(mc, PlayerHeadSkinAccess.localPlayerHeadTask(mc))
            ?: PlayerHeadSkinAccess.profileHeadTask(mc)?.let { onClientThread(mc, it) }
            ?: return null
        return encodeHeadPng(head)
    }

    /**
     * Runs [task] on the client thread and waits for the head it produces
     *
     * `submit(Supplier)` runs the task inline when already on the client thread so the wait cannot
     * deadlock there
     */
    private fun onClientThread(mc: Minecraft, task: Supplier<NativeImage>): NativeImage? {
        // passed as a value not a lambda because submit(Runnable) and submit(Supplier) are both SAM candidates
        val future = mc.submit(task)
        return try {
            future.get(CLIENT_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            future.cancel(false)
            // the task may still run so free its image rather than leaking it off-heap
            future.whenComplete { image, _ -> image?.close() }
            LOGGER.debug("Failed to read the player head on the client thread", e)
            null
        }
    }

    private fun encodeHeadPng(head: NativeImage): ByteArray? = head.use {
        try {
            val path = Files.createTempFile("oneconfig-player-head", ".png")
            try {
                it.writeToFile(path)
                Files.readAllBytes(path)
            } finally {
                Files.deleteIfExists(path)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadFromWeb(profileId: UUID): ByteArray? {
        val uuid = profileId.toString().replace("-", "")
        val url = "https://mc-heads.net/avatar/$uuid/32.png"
        return runCatching {
            NetworkUtils.setupConnection(url).use { it.readBytes() }
        }.getOrNull()
    }
}
