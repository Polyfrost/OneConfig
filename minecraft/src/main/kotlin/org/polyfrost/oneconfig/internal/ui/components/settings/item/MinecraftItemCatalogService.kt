package org.polyfrost.oneconfig.internal.ui.components.settings.item

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
//? if >= 1.21.8 {
import com.mojang.blaze3d.buffers.GpuBuffer
//? } else if >= 1.21.5 {
/*import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
*///? }
//? if < 1.21.5
/*import com.mojang.blaze3d.platform.NativeImage*/
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
//? } else if >= 1.21.8 {
/*import net.minecraft.client.gui.render.state.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
*///? }
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.api.platform.v1.Platform
//? if >= 1.21.5
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.CompletableFuture
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

class MinecraftItemCatalogService : ItemCatalogService {
    private data class RegistryEntry(val item: Item, val id: String)
    private data class Placement(val id: String, val x: Int, val y: Int)
    private data class RenderBatch(val ids: List<String>, val generation: Long)

    private val entries: List<RegistryEntry> by lazy {
        BuiltInRegistries.ITEM.mapNotNull { item ->
            if (item === Items.AIR) return@mapNotNull null
            val id = BuiltInRegistries.ITEM.getKey(item).toString()
            RegistryEntry(item, id)
        }
    }
    private val entriesById: Map<String, RegistryEntry> by lazy { entries.associateBy(RegistryEntry::id) }
    private val iconCache: MutableMap<String, ItemIconData> = Collections.synchronizedMap(
        object : LinkedHashMap<String, ItemIconData>(MAX_CACHED_ICONS, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ItemIconData>?): Boolean =
                size > MAX_CACHED_ICONS
        }
    )
    private val requestLock = Any()
    private val waiting = LinkedHashMap<String, MutableList<(ItemIconData?) -> Unit>>()
    @Volatile
    private var catalogCache: List<ItemDescriptor>? = null
    private var cacheGeneration = 0L
    private var renderScheduled = false

    init {
        EventManager.register(ResourceFinishedLoading::class.java, Runnable(::clearCaches))
    }

    override fun items(): List<ItemDescriptor> {
        catalogCache?.let { return it }
        return synchronized(requestLock) {
            catalogCache ?: entries.map { entry ->
                ItemDescriptor(entry.id, ItemStack(entry.item).hoverName.string)
            }.also { catalogCache = it }
        }
    }

    override fun icon(id: String): ItemIconData? = iconCache[id]

    override fun loadIcon(id: String, onLoaded: (ItemIconData?) -> Unit) {
        iconCache[id]?.let {
            onLoaded(it)
            return
        }
        if (id !in entriesById) {
            onLoaded(null)
            return
        }

        var cachedIcon: ItemIconData? = null
        var shouldSchedule = false
        synchronized(requestLock) {
            cachedIcon = iconCache[id]
            if (cachedIcon == null) {
                waiting.getOrPut(id, ::mutableListOf).add(onLoaded)
                if (!renderScheduled) {
                    renderScheduled = true
                    shouldSchedule = true
                }
            }
        }
        cachedIcon?.let(onLoaded)
        if (shouldSchedule) scheduleRender()
    }

    private fun scheduleRender() {
        // Defer until the current Compose frame has completed before touching Minecraft's GUI renderer.
        CompletableFuture.runAsync {
            Minecraft.getInstance().execute(::renderPendingBatch)
        }
    }

    private fun renderPendingBatch() {
        val guiWidth = Platform.screen().guiWidth()
        val guiHeight = Platform.screen().guiHeight()
        val viewportWidth = Platform.screen().viewportWidth()
        val viewportHeight = Platform.screen().viewportHeight()
        if (guiWidth <= 0 || guiHeight <= 0 || viewportWidth <= 0 || viewportHeight <= 0) {
            completeBatch(currentBatch(), emptyMap())
            return
        }

        val columns = (guiWidth / CELL_SIZE).coerceAtLeast(1)
        val rows = (guiHeight / CELL_SIZE).coerceAtLeast(1)
        val batch = currentBatch(columns * rows)
        if (batch.ids.isEmpty()) {
            markRenderFinished()
            return
        }

        val placements = batch.ids.mapIndexed { index, id ->
            Placement(id, index % columns * CELL_SIZE, index / columns * CELL_SIZE)
        }
        val target = try {
            createTarget(viewportWidth, viewportHeight)
        } catch (throwable: Throwable) {
            LOG.warn("Failed to create the item icon render target", throwable)
            completeBatch(batch, emptyMap())
            return
        }

        try {
            renderItems(target, placements, guiWidth, guiHeight)
            readTarget(target, placements, guiWidth, guiHeight, batch)
        } catch (throwable: Throwable) {
            LOG.warn("Failed to render item selector icons", throwable)
            target.destroyBuffers()
            completeBatch(batch, emptyMap())
        }
    }

    private fun createTarget(width: Int, height: Int): TextureTarget {
        //? if >= 26.2 {
        /*return TextureTarget(null, width, height, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
        *///? } else if >= 1.21.5 {
        return TextureTarget(null, width, height, true)
        //? } else if >= 1.21.4 {
        /*return TextureTarget(width, height, true).also { it.setClearColor(0f, 0f, 0f, 0f) }
        *///? } else {
        /*return TextureTarget(width, height, true, Minecraft.ON_OSX).also { it.setClearColor(0f, 0f, 0f, 0f) }
        *///? }
    }

    private fun renderItems(
        target: TextureTarget,
        placements: List<Placement>,
        guiWidth: Int,
        guiHeight: Int,
    ) {
        clearTarget(target)
        //? if >= 1.21.8 {
        val state = GuiRenderState()
        //? if >= 1.21.11 {
        val graphics = GuiGraphicsExtractor(Minecraft.getInstance(), state, guiWidth, guiHeight)
        //? } else
        /*val graphics = GuiGraphicsExtractor(Minecraft.getInstance(), state)*/
        placements.forEach { placement ->
            val item = entriesById[placement.id]?.item ?: return@forEach
            //? if >= 26.1 {
            graphics.fakeItem(ItemStack(item), placement.x + ITEM_PADDING, placement.y + ITEM_PADDING)
            //? } else
            /*graphics.renderFakeItem(ItemStack(item), placement.x + ITEM_PADDING, placement.y + ITEM_PADDING)*/
        }

        val client = Minecraft.getInstance()
        val guiRenderer = (client.gameRenderer as GameRendererAccessor).`oneconfig$getGuiRenderer`()
        val accessor = guiRenderer as GuiRendererAccessor
        val previousState = accessor.`oneconfig$getRenderState`()
        GuiTargetRedirect.target = target
        try {
            accessor.`oneconfig$setRenderState`(state)
            //? if >= 26.2 {
            /*guiRenderer.render()
            *///? } else {
            val fog = (client.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`()
                .getBuffer(net.minecraft.client.renderer.fog.FogRenderer.FogMode.NONE)
            guiRenderer.render(fog)
            //? }
        } finally {
            GuiTargetRedirect.target = null
            accessor.`oneconfig$setRenderState`(previousState)
        }
        //? } else if >= 1.21.5 {
        /*val client = Minecraft.getInstance()
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        GuiTargetRedirect.target = target
        try {
            placements.forEach { placement ->
                val item = entriesById[placement.id]?.item ?: return@forEach
                graphics.renderFakeItem(ItemStack(item), placement.x + ITEM_PADDING, placement.y + ITEM_PADDING)
            }
            graphics.flush()
        } finally {
            GuiTargetRedirect.target = null
        }
        *///? } else {
        /*val client = Minecraft.getInstance()
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        target.bindWrite(true)
        try {
            placements.forEach { placement ->
                val item = entriesById[placement.id]?.item ?: return@forEach
                graphics.renderFakeItem(ItemStack(item), placement.x + ITEM_PADDING, placement.y + ITEM_PADDING)
            }
            graphics.flush()
        } finally {
            client.mainRenderTarget.bindWrite(true)
        }
        *///? }
    }

    private fun clearTarget(target: TextureTarget) {
        //? if >= 26.2 {
        /*val color = target.colorTexture ?: return
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorTexture(color, org.joml.Vector4f(0f, 0f, 0f, 0f))
        *///? } else if >= 1.21.5 {
        val color = target.colorTexture ?: return
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(color, 0)
        //? } else if >= 1.21.4 {
        /*target.clear()
        *///? } else {
        /*target.clear(Minecraft.ON_OSX)
        *///? }
    }

    private fun readTarget(
        target: TextureTarget,
        placements: List<Placement>,
        guiWidth: Int,
        guiHeight: Int,
        batch: RenderBatch,
    ) {
        //? if >= 1.21.5 {
        val texture = target.colorTexture ?: error("Item icon render target has no color texture")
        val width = target.width
        val height = target.height
        //? if >= 26.2 {
        /*val pixelSize = texture.format.blockSize()
        *///? } else
        val pixelSize = texture.format.pixelSize()
        val device = RenderSystem.getDevice()
        //? if >= 1.21.11 {
        val buffer = device.createBuffer(
            { "OneConfig item icon readback" },
            GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
            width.toLong() * height.toLong() * pixelSize,
        )
        //? } else if >= 1.21.8 {
        /*val buffer = device.createBuffer(
            { "OneConfig item icon readback" },
            GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
            width * height * pixelSize,
        )
        *///? } else {
        /*val buffer = device.createBuffer(
            { "OneConfig item icon readback" },
            BufferType.PIXEL_PACK,
            BufferUsage.STATIC_READ,
            width * height * pixelSize,
        )
        *///? }
        //? if < 26.2
        val readEncoder = device.createCommandEncoder()
        val onCopied = Runnable {
            try {
                //? if >= 26.2 {
                /*buffer.map(true, false).use { view ->
                    completeReadback(
                        placements,
                        guiWidth,
                        guiHeight,
                        width,
                        height,
                        pixelSize,
                        view.data(),
                        batch,
                    )
                }
                *///? } else if >= 1.21.8 {
                readEncoder.mapBuffer(buffer, true, false).use { view ->
                    completeReadback(
                        placements,
                        guiWidth,
                        guiHeight,
                        width,
                        height,
                        pixelSize,
                        view.data(),
                        batch,
                    )
                }
                //? } else {
                /*readEncoder.readBuffer(buffer).use { view ->
                    completeReadback(
                        placements,
                        guiWidth,
                        guiHeight,
                        width,
                        height,
                        pixelSize,
                        view.data(),
                        batch,
                    )
                }
                *///? }
            } catch (throwable: Throwable) {
                LOG.warn("Failed to read item selector icons from the GPU", throwable)
                completeBatch(batch, emptyMap())
            } finally {
                buffer.close()
                target.destroyBuffers()
            }
        }
        try {
            //? if >= 1.21.11 {
            device.createCommandEncoder().copyTextureToBuffer(texture, buffer, 0L, onCopied, 0)
            //? } else
            /*device.createCommandEncoder().copyTextureToBuffer(texture, buffer, 0, onCopied, 0)*/
        } catch (throwable: Throwable) {
            buffer.close()
            throw throwable
        }
        //? } else {
        /*val image = NativeImage(target.width, target.height, false)
        try {
            RenderSystem.bindTexture(target.colorTextureId)
            image.downloadTexture(0, false)
            image.flipY()
            val icons = extractIcons(
                placements,
                guiWidth,
                guiHeight,
                image.width,
                image.height,
            ) { x, y ->
                //? if >= 1.21.4 {
                image.getPixel(x, y)
                //? } else
                /*abgrToArgb(image.getPixelRGBA(x, y))*/
            }
            completeBatch(batch, icons)
        } finally {
            image.close()
            target.destroyBuffers()
        }
        *///? }
    }

    private fun completeReadback(
        placements: List<Placement>,
        guiWidth: Int,
        guiHeight: Int,
        width: Int,
        height: Int,
        pixelSize: Int,
        data: ByteBuffer,
        batch: RenderBatch,
    ) {
        val icons = extractIcons(placements, guiWidth, guiHeight, width, height) { x, y ->
            val sourceY = height - y - 1
            val raw = data.getInt((x + sourceY * width) * pixelSize)
            abgrToArgb(raw)
        }
        completeBatch(batch, icons)
    }

    private fun extractIcons(
        placements: List<Placement>,
        guiWidth: Int,
        guiHeight: Int,
        imageWidth: Int,
        imageHeight: Int,
        pixel: (Int, Int) -> Int,
    ): Map<String, ItemIconData> {
        val scaleX = imageWidth.toDouble() / guiWidth.toDouble()
        val scaleY = imageHeight.toDouble() / guiHeight.toDouble()
        return placements.associate { placement ->
            val pixels = IntArray(ICON_SIZE * ICON_SIZE)
            for (y in 0 until ICON_SIZE) {
                val logicalY = placement.y + (y + 0.5) * CELL_SIZE / ICON_SIZE
                val sourceY = logicalY * scaleY - 0.5
                for (x in 0 until ICON_SIZE) {
                    val logicalX = placement.x + (x + 0.5) * CELL_SIZE / ICON_SIZE
                    val sourceX = logicalX * scaleX - 0.5
                    pixels[y * ICON_SIZE + x] = unpremultiply(
                        sampleBilinear(sourceX, sourceY, imageWidth, imageHeight, pixel),
                    )
                }
            }
            placement.id to ItemIconData(ICON_SIZE, ICON_SIZE, pixels)
        }
    }

    /** Smooths model edges while keeping transparent pixels free of dark color fringes. */
    private fun sampleBilinear(
        x: Double,
        y: Double,
        width: Int,
        height: Int,
        pixel: (Int, Int) -> Int,
    ): Int {
        val clampedX = x.coerceIn(0.0, (width - 1).toDouble())
        val clampedY = y.coerceIn(0.0, (height - 1).toDouble())
        val x0 = floor(clampedX).toInt()
        val y0 = floor(clampedY).toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        val xWeight = clampedX - x0
        val yWeight = clampedY - y0
        val c00 = pixel(x0, y0)
        val c10 = pixel(x1, y0)
        val c01 = pixel(x0, y1)
        val c11 = pixel(x1, y1)

        fun channel(shift: Int): Int {
            val top = ((c00 ushr shift) and 0xFF) * (1.0 - xWeight) +
                ((c10 ushr shift) and 0xFF) * xWeight
            val bottom = ((c01 ushr shift) and 0xFF) * (1.0 - xWeight) +
                ((c11 ushr shift) and 0xFF) * xWeight
            return (top * (1.0 - yWeight) + bottom * yWeight).roundToInt().coerceIn(0, 255)
        }

        return channel(24) shl 24 or
            (channel(16) shl 16) or
            (channel(8) shl 8) or
            channel(0)
    }

    private fun completeBatch(batch: RenderBatch, icons: Map<String, ItemIconData>) {
        val callbacks = mutableListOf<Pair<(ItemIconData?) -> Unit, ItemIconData?>>()
        var scheduleNext = false
        synchronized(requestLock) {
            if (batch.generation == cacheGeneration) {
                icons.forEach { (id, icon) -> iconCache[id] = icon }
                batch.ids.forEach { id ->
                    val icon = icons[id]
                    waiting.remove(id).orEmpty().forEach { callbacks += it to icon }
                }
            }
            renderScheduled = false
            if (waiting.isNotEmpty()) {
                renderScheduled = true
                scheduleNext = true
            }
        }
        if (scheduleNext) scheduleRender()
        callbacks.forEach { (callback, icon) ->
            runCatching { callback(icon) }
                .onFailure { LOG.warn("Failed to deliver a rendered item selector icon", it) }
        }
    }

    private fun markRenderFinished() {
        var scheduleNext = false
        synchronized(requestLock) {
            renderScheduled = false
            if (waiting.isNotEmpty()) {
                renderScheduled = true
                scheduleNext = true
            }
        }
        if (scheduleNext) scheduleRender()
    }

    private fun currentBatch(limit: Int = Int.MAX_VALUE): RenderBatch = synchronized(requestLock) {
        RenderBatch(waiting.keys.take(limit), cacheGeneration)
    }

    private fun clearCaches() {
        synchronized(requestLock) {
            cacheGeneration++
            catalogCache = null
            iconCache.clear()
        }
    }

    private fun abgrToArgb(color: Int): Int =
        (color and 0xFF00FF00.toInt()) or ((color and 0xFF) shl 16) or ((color ushr 16) and 0xFF)

    private fun unpremultiply(color: Int): Int {
        val alpha = color ushr 24
        if (alpha == 0 || alpha == 255) return color
        val red = min(255, ((color ushr 16) and 0xFF) * 255 / alpha)
        val green = min(255, ((color ushr 8) and 0xFF) * 255 / alpha)
        val blue = min(255, (color and 0xFF) * 255 / alpha)
        return alpha shl 24 or (red shl 16) or (green shl 8) or blue
    }

    private companion object {
        val LOG = LoggerFactory.getLogger("OneConfig/ItemList")
        const val CELL_SIZE = 20
        const val ITEM_PADDING = 2
        const val ICON_SIZE = 64
        const val MAX_CACHED_ICONS = 2048
    }
}
