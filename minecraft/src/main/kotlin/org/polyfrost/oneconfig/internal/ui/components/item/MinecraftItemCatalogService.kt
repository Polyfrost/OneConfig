package org.polyfrost.oneconfig.internal.ui.components.item

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
//? if >= 1.21.4 && < 1.21.8
//import com.mojang.blaze3d.ProjectionType
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
//? if >= 1.21.8
import net.minecraft.client.gui.render.GuiRenderer
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
//? } else if >= 1.21.8 {
/*import net.minecraft.client.gui.render.state.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
*///? }
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
//? if < 1.21.8
//import org.joml.Matrix4f

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
                ItemDescriptor(entry.id, Component.translatable(entry.item.descriptionId).string)
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
        // always queue the batch so the current Compose frame finishes before native GUI rendering starts
        Minecraft.getInstance().schedule(::renderPendingBatch)
    }

    private fun renderPendingBatch() {
        val guiWidth = Platform.screen().guiWidth()
        val guiHeight = Platform.screen().guiHeight()
        val windowWidth = Platform.screen().windowWidth()
        val windowHeight = Platform.screen().windowHeight()
        val viewportWidth = Platform.screen().viewportWidth()
        val viewportHeight = Platform.screen().viewportHeight()
        if (
            guiWidth <= 0 || guiHeight <= 0 ||
            windowWidth <= 0 || windowHeight <= 0 ||
            viewportWidth <= 0 || viewportHeight <= 0
        ) {
            completeBatch(currentBatch(), emptyMap())
            return
        }

        val scaleX = viewportWidth.toDouble() / guiWidth.toDouble()
        val scaleY = viewportHeight.toDouble() / guiHeight.toDouble()
        val pixelRatio = maxOf(
            viewportWidth.toDouble() / windowWidth.toDouble(),
            viewportHeight.toDouble() / windowHeight.toDouble(),
        )
        val iconPixelSize = (ICON_SIZE * pixelRatio).roundToInt().coerceIn(ICON_SIZE, MAX_ICON_PIXEL_SIZE)
        val columns = min(
            MAX_BATCH_COLUMNS,
            floor(MAX_TARGET_SIZE / (CELL_SIZE * scaleX)).toInt().coerceAtLeast(1),
        )
        val rows = min(
            MAX_BATCH_ROWS,
            floor(MAX_TARGET_SIZE / (CELL_SIZE * scaleY)).toInt().coerceAtLeast(1),
        )
        val batch = currentBatch(columns * rows)
        if (batch.ids.isEmpty()) {
            markRenderFinished()
            return
        }

        val usedColumns = min(columns, batch.ids.size)
        val usedRows = (batch.ids.size + usedColumns - 1) / usedColumns
        val renderGuiWidth = usedColumns * CELL_SIZE
        val renderGuiHeight = usedRows * CELL_SIZE
        val targetWidth = ceil(renderGuiWidth * scaleX).toInt().coerceIn(1, MAX_TARGET_SIZE)
        val targetHeight = ceil(renderGuiHeight * scaleY).toInt().coerceIn(1, MAX_TARGET_SIZE)
        val placements = batch.ids.mapIndexed { index, id ->
            Placement(id, index % usedColumns * CELL_SIZE, index / usedColumns * CELL_SIZE)
        }
        val target = try {
            createTarget(targetWidth, targetHeight)
        } catch (throwable: Throwable) {
            LOG.warn("Failed to create the item icon render target", throwable)
            completeBatch(batch, emptyMap())
            return
        }

        var renderResource: AutoCloseable? = null
        try {
            renderResource = renderItems(target, placements, renderGuiWidth, renderGuiHeight)
            readTarget(target, placements, renderGuiWidth, renderGuiHeight, iconPixelSize, batch, renderResource)
        } catch (throwable: Throwable) {
            LOG.warn("Failed to render item selector icons", throwable)
            closeRenderResource(renderResource)
            target.destroyBuffers()
            completeBatch(batch, emptyMap())
        }
    }

    private fun createTarget(width: Int, height: Int): TextureTarget {
        //? if >= 26.2 {
        return TextureTarget(null, width, height, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
        //? } else if >= 1.21.5 {
        /*return TextureTarget(null, width, height, true)
        *///? } else if >= 1.21.4 {
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
    ): AutoCloseable? {
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
        val guiRenderer = createItemGuiRenderer(client, state)
        val previousTarget = GuiTargetRedirect.target
        GuiTargetRedirect.target = target
        try {
            //? if >= 26.2 {
            guiRenderer.render()
            //? } else {
            /*val fog = (client.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`()
                .getBuffer(net.minecraft.client.renderer.fog.FogRenderer.FogMode.NONE)
            guiRenderer.render(fog)
            *///? }
        } catch (throwable: Throwable) {
            guiRenderer.close()
            throw throwable
        } finally {
            GuiTargetRedirect.target = previousTarget
        }
        return guiRenderer
        //? } else if >= 1.21.5 {
        /*val client = Minecraft.getInstance()
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        val previousTarget = GuiTargetRedirect.target
        withGuiProjection(guiWidth, guiHeight) {
            GuiTargetRedirect.target = target
            try {
                placements.forEach { placement ->
                    val item = entriesById[placement.id]?.item ?: return@forEach
                    graphics.renderFakeItem(ItemStack(item), placement.x + ITEM_PADDING, placement.y + ITEM_PADDING)
                }
                graphics.flush()
            } finally {
                GuiTargetRedirect.target = previousTarget
            }
        }
        return null
        *///? } else {
        /*val client = Minecraft.getInstance()
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        val previousTarget = GuiTargetRedirect.target
        withGuiProjection(guiWidth, guiHeight) {
            GuiTargetRedirect.target = target
            target.bindWrite(true)
            try {
                placements.forEach { placement ->
                    val item = entriesById[placement.id]?.item ?: return@forEach
                    graphics.renderFakeItem(ItemStack(item), placement.x + ITEM_PADDING, placement.y + ITEM_PADDING)
                }
                graphics.flush()
            } finally {
                GuiTargetRedirect.target = previousTarget
                (previousTarget ?: client.mainRenderTarget).bindWrite(true)
            }
        }
        return null
        *///? }
    }

    //? if >= 1.21.8 {
    private fun createItemGuiRenderer(client: Minecraft, state: GuiRenderState): GuiRenderer {
        // keep this icon batch separate from the active frame's render state and item atlas
        // because reusing the game's renderer would mix two independently-lived frames
        //? if >= 26.2 {
        return GuiRenderer(state, client.gameRenderer.featureRenderDispatcher(), emptyList())
        //? } else if >= 1.21.10 {
        /*return GuiRenderer(
            state,
            client.renderBuffers().bufferSource(),
            client.gameRenderer.getSubmitNodeStorage(),
            client.gameRenderer.getFeatureRenderDispatcher(),
            emptyList(),
        )
        *///? } else {
        /*return GuiRenderer(state, client.renderBuffers().bufferSource(), emptyList())
        *///? }
    }
    //? }

    //? if < 1.21.8 {
    /*private inline fun withGuiProjection(guiWidth: Int, guiHeight: Int, render: () -> Unit) {
        RenderSystem.backupProjectionMatrix()
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushMatrix()
        try {
            val projection = Matrix4f().setOrtho(
                0f,
                guiWidth.toFloat(),
                guiHeight.toFloat(),
                0f,
                1000f,
                21000f,
            )
            //? if >= 1.21.4 {
            RenderSystem.setProjectionMatrix(projection, ProjectionType.ORTHOGRAPHIC)
            //? } else {
            /*RenderSystem.setProjectionMatrix(projection, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z)
            *///? }
            modelView.translation(0f, 0f, -11000f)
            //? if < 1.21.4
            //RenderSystem.applyModelViewMatrix()
            render()
        } finally {
            modelView.popMatrix()
            //? if < 1.21.4
            //RenderSystem.applyModelViewMatrix()
            RenderSystem.restoreProjectionMatrix()
        }
    }
    *///? }

    private fun clearTarget(target: TextureTarget) {
        //? if >= 26.2 {
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        target.colorTexture?.let { encoder.clearColorTexture(it, org.joml.Vector4f(0f, 0f, 0f, 0f)) }
        target.depthTexture?.let { encoder.clearDepthTexture(it, 1.0) }
        //? } else if >= 1.21.5 {
        /*val encoder = RenderSystem.getDevice().createCommandEncoder()
        target.colorTexture?.let { encoder.clearColorTexture(it, 0) }
        target.depthTexture?.let { encoder.clearDepthTexture(it, 1.0) }
        *///? } else if >= 1.21.4 {
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
        iconSize: Int,
        batch: RenderBatch,
        renderResource: AutoCloseable?,
    ) {
        //? if >= 1.21.5 {
        val texture = target.colorTexture ?: error("Item icon render target has no color texture")
        val width = target.width
        val height = target.height
        //? if >= 26.2 {
        val pixelSize = texture.format.blockSize()
        //? } else
        //val pixelSize = texture.format.pixelSize()
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
        //val readEncoder = device.createCommandEncoder()
        val onCopied = Runnable {
            try {
                //? if >= 26.2 {
                buffer.map(true, false).use { view ->
                    completeReadback(
                        placements,
                        guiWidth,
                        guiHeight,
                        width,
                        height,
                        pixelSize,
                        iconSize,
                        view.data(),
                        batch,
                    )
                }
                //? } else if >= 1.21.8 {
                /*readEncoder.mapBuffer(buffer, true, false).use { view ->
                    completeReadback(
                        placements,
                        guiWidth,
                        guiHeight,
                        width,
                        height,
                        pixelSize,
                        iconSize,
                        view.data(),
                        batch,
                    )
                }
                *///? } else {
                /*readEncoder.readBuffer(buffer).use { view ->
                    completeReadback(
                        placements,
                        guiWidth,
                        guiHeight,
                        width,
                        height,
                        pixelSize,
                        iconSize,
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
                closeRenderResource(renderResource)
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
        val previousTexture = RenderSystem.getShaderTexture(0)
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
                iconSize,
            ) { x, y ->
                //? if >= 1.21.4 {
                image.getPixel(x, y)
                //? } else
                /*abgrToArgb(image.getPixelRGBA(x, y))*/
            }
            completeBatch(batch, icons)
        } finally {
            RenderSystem.bindTexture(previousTexture)
            image.close()
            closeRenderResource(renderResource)
            target.destroyBuffers()
        }
        *///? }
    }

    private fun closeRenderResource(resource: AutoCloseable?) {
        runCatching { resource?.close() }
            .onFailure { LOG.warn("Failed to release item selector render resources", it) }
    }

    private fun completeReadback(
        placements: List<Placement>,
        guiWidth: Int,
        guiHeight: Int,
        width: Int,
        height: Int,
        pixelSize: Int,
        iconSize: Int,
        data: ByteBuffer,
        batch: RenderBatch,
    ) {
        val nativeData = data.order(ByteOrder.nativeOrder())
        val icons = extractIcons(placements, guiWidth, guiHeight, width, height, iconSize) { x, y ->
            val sourceY = height - y - 1
            val raw = nativeData.getInt((x + sourceY * width) * pixelSize)
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
        iconSize: Int,
        pixel: (Int, Int) -> Int,
    ): Map<String, ItemIconData> {
        val scaleX = imageWidth.toDouble() / guiWidth.toDouble()
        val scaleY = imageHeight.toDouble() / guiHeight.toDouble()
        return placements.associate { placement ->
            val pixels = IntArray(iconSize * iconSize)
            for (y in 0 until iconSize) {
                val logicalY = placement.y + ITEM_PADDING + (y + 0.5) * ITEM_RENDER_SIZE / iconSize
                val sourceY = logicalY * scaleY - 0.5
                for (x in 0 until iconSize) {
                    val logicalX = placement.x + ITEM_PADDING + (x + 0.5) * ITEM_RENDER_SIZE / iconSize
                    val sourceX = logicalX * scaleX - 0.5
                    pixels[y * iconSize + x] = unpremultiply(
                        sampleBilinear(sourceX, sourceY, imageWidth, imageHeight, pixel),
                    )
                }
            }
            placement.id to ItemIconData(iconSize, iconSize, pixels)
        }
    }

    /** Smooths model edges while keeping transparent pixels free of dark color fringes */
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
        const val ITEM_RENDER_SIZE = CELL_SIZE - ITEM_PADDING * 2
        const val ICON_SIZE = 32
        const val MAX_ICON_PIXEL_SIZE = 64
        const val MAX_CACHED_ICONS = 512
        const val MAX_BATCH_COLUMNS = 8
        const val MAX_BATCH_ROWS = 8
        const val MAX_TARGET_SIZE = 512
    }
}
