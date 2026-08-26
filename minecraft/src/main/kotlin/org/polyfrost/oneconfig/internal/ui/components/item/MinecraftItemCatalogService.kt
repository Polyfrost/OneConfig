package org.polyfrost.oneconfig.internal.ui.components.item

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
//? if >= 1.21.4 && < 1.21.8
//import com.mojang.blaze3d.ProjectionType
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
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ContentChangeMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.SkiaOffscreenTarget
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect
import org.slf4j.LoggerFactory
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
//? if < 1.21.8
//import org.joml.Matrix4f

class MinecraftItemCatalogService : ItemCatalogService {
    private data class RegistryEntry(val item: Item, val id: String)
    private data class AtlasIcon(val source: Rect)
    private data class AtlasLayout(val columns: Int, val rows: Int, val renderScale: Int) {
        val capacity = columns * rows
    }
    private data class Placement(val id: String, val x: Int, val y: Int, val source: Rect)
    private data class RenderBatch(
        val placements: List<Placement>,
        val generation: Long,
    )
    private val entries: List<RegistryEntry> by lazy {
        BuiltInRegistries.ITEM.mapNotNull { item ->
            if (item === Items.AIR) return@mapNotNull null
            val id = BuiltInRegistries.ITEM.getKey(item).toString()
            RegistryEntry(item, id)
        }
    }
    private val entriesById: Map<String, RegistryEntry> by lazy { entries.associateBy(RegistryEntry::id) }
    private val atlas = SkiaOffscreenTarget()
    private val atlasPaint = Paint()
    private val requestLock = Any()
    private val icons = LinkedHashMap<String, AtlasIcon>()
    private val waiting = LinkedHashMap<String, MutableList<(Boolean) -> Unit>>()

    @Volatile
    private var catalogCache: List<ItemDescriptor>? = null
    private var atlasLayout: AtlasLayout? = null
    private var cacheGeneration = 0L
    private var nextSlot = 0
    private var atlasNeedsClear = true
    private var atlasReadyForSampling = false
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

    override fun requestIcon(id: String, onLoaded: (Boolean) -> Unit) {
        if (id !in entriesById) {
            deliverIcon(onLoaded, false)
            return
        }

        var available = false
        var atlasLost = false
        var shouldSchedule = false
        synchronized(requestLock) {
            if (icons.isNotEmpty() && !atlasAvailable()) {
                resetAtlasLocked()
                atlasLost = true
            }
            available = id in icons
            if (!available) {
                waiting.getOrPut(id, ::mutableListOf).add(onLoaded)
                if (!renderScheduled) {
                    renderScheduled = true
                    shouldSchedule = true
                }
            }
        }

        if (atlasLost) invalidateMountedIcons()
        if (available) deliverIcon(onLoaded, true)
        if (shouldSchedule) scheduleRender()
    }

    override fun drawIcon(id: String, canvas: Canvas, bounds: Rect, alpha: Float): Boolean {
        var atlasLost = false
        val resolved = synchronized(requestLock) {
            if (!atlasAvailable()) {
                if (icons.isNotEmpty()) {
                    resetAtlasLocked()
                    atlasLost = true
                }
                null
            } else {
                val cached = icons[id] ?: return@synchronized null
                cached to checkNotNull(atlas.surface)
            }
        }
        if (atlasLost) invalidateMountedIcons()
        if (resolved == null) {
            return false
        }
        val (icon, surface) = resolved

        return runCatching {
            val source = icon.source
            if (source.width <= 0f || source.height <= 0f || bounds.width <= 0f || bounds.height <= 0f) {
                return false
            }

            atlasPaint.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
            val saveCount = canvas.save()
            try {
                canvas.clipRect(bounds)
                canvas.translate(bounds.left, bounds.top)
                canvas.scale(bounds.width / source.width, bounds.height / source.height)
                canvas.translate(-source.left, -source.top)
                surface.draw(canvas, 0, 0, SamplingMode.LINEAR, atlasPaint)
            } finally {
                canvas.restoreToCount(saveCount)
            }
            true
        }.getOrElse {
            LOG.debug("Failed to draw an item icon from the atlas", it)
            false
        }
    }

    private fun atlasAvailable(): Boolean = atlas.target != null && atlas.surface != null

    private fun invalidateMountedIcons() {
        runCatching { queueOnRenderThread(ItemCatalog::invalidateIcons) }
            .onFailure { LOG.warn("Failed to invalidate item selector icons", it) }
    }

    private fun deliverIcon(callback: (Boolean) -> Unit, loaded: Boolean) {
        runCatching { callback(loaded) }
            .onFailure { LOG.warn("Failed to deliver a rendered item selector icon", it) }
    }

    private fun scheduleRender() {
        runCatching { queueOnRenderThread(::renderPendingBatch) }
            .onFailure {
                val callbacks = synchronized(requestLock) {
                    renderScheduled = false
                    val callbacks = waiting.values.flatten()
                    waiting.clear()
                    callbacks
                }
                callbacks.forEach { callback -> deliverIcon(callback, false) }
                LOG.warn("Failed to schedule item selector rendering", it)
            }
    }

    private fun queueOnRenderThread(block: () -> Unit) {
        val task = Runnable(block)
        //? if >= 1.21.4
        Minecraft.getInstance().schedule(task)
        //? if < 1.21.4
        //Minecraft.getInstance().tell(task)
    }

    private fun renderPendingBatch() {
        val layout = synchronized(requestLock) {
            atlasLayout ?: createAtlasLayout()?.also { atlasLayout = it }
        }
        if (layout == null) {
            failBatch(currentBatch(), "item icon atlas exceeds the maximum texture size")
            return
        }

        val previousTarget = atlas.target
        val resolved = resolveAtlasTarget(layout)
        val target = atlas.target as? TextureTarget
        val surface = atlas.surface
        if (!resolved || target == null || surface == null) {
            failBatch(currentBatch(), "item icon atlas is unavailable")
            return
        }

        val guiWidth = layout.columns * CELL_SIZE
        val guiHeight = layout.rows * CELL_SIZE
        val targetChanged = target !== previousTarget
        if (targetChanged) atlasReadyForSampling = false

        val batch: RenderBatch
        val shouldClear: Boolean
        synchronized(requestLock) {
            if (targetChanged && icons.isNotEmpty() || nextSlot > icons.size && waiting.isNotEmpty()) {
                rebuildAtlasLocked()
            }
            val availableSlots = layout.capacity - nextSlot
            val ids = waiting.keys.take(min(MAX_BATCH_SIZE, availableSlots.coerceAtLeast(0)))
            val generation = cacheGeneration
            val targetScaleX = target.width.toFloat() / guiWidth.toFloat()
            val targetScaleY = target.height.toFloat() / guiHeight.toFloat()
            val placements = ids.map { id ->
                val slot = nextSlot++
                val x = slot % layout.columns * CELL_SIZE
                val y = slot / layout.columns * CELL_SIZE
                Placement(
                    id,
                    x,
                    y,
                    Rect.makeLTRB(
                        (x + ITEM_PADDING) * targetScaleX,
                        (y + ITEM_PADDING) * targetScaleY,
                        (x + ITEM_PADDING + ITEM_RENDER_SIZE) * targetScaleX,
                        (y + ITEM_PADDING + ITEM_RENDER_SIZE) * targetScaleY,
                    ),
                )
            }
            batch = RenderBatch(placements, generation)
            shouldClear = atlasNeedsClear
            if (placements.isNotEmpty()) atlasNeedsClear = false
        }

        if (batch.placements.isEmpty()) {
            if (synchronized(requestLock) { waiting.isEmpty() }) {
                markRenderFinished()
            } else {
                failBatch(currentBatch(), "item icon atlas is full")
            }
            return
        }
        var renderResource: AutoCloseable? = null
        try {
            val backend = SkiaCtx.vulkanService
            surface.notifyContentWillChange(
                if (shouldClear) ContentChangeMode.DISCARD else ContentChangeMode.RETAIN,
            )
            if (atlasReadyForSampling) backend?.transitionOffscreenForRendering(target)
            if (shouldClear) clearTarget(target)
            renderResource = renderItems(target, batch.placements, guiWidth, guiHeight)
            backend?.midFrameFlush()
            closeRenderResource(renderResource)
            renderResource = null
            backend?.transitionOffscreenForSampling(target)
            backend?.midFrameFlush()
            atlasReadyForSampling = true
            completeBatch(batch)
        } catch (throwable: Throwable) {
            atlasReadyForSampling = false
            closeRenderResource(renderResource)
            if (shouldClear) synchronized(requestLock) {
                if (batch.generation == cacheGeneration) atlasNeedsClear = true
            }
            LOG.warn("Failed to render item selector icons", throwable)
            failBatch(batch)
        }
    }

    private fun renderItems(
        target: TextureTarget,
        placements: List<Placement>,
        guiWidth: Int,
        guiHeight: Int,
    ): AutoCloseable? {
        //? if >= 1.21.8 {
        val state = GuiRenderState()
        //? if >= 1.21.11 {
        val graphics = GuiGraphicsExtractor(Minecraft.getInstance(), state, guiWidth, guiHeight)
        //? } else
        //val graphics = GuiGraphicsExtractor(Minecraft.getInstance(), state)
        val pose = graphics.pose()
        pose.pushMatrix()
        val guiScale = Minecraft.getInstance().window.guiScale.toFloat().coerceAtLeast(1f)
        pose.scale(
            target.width.toFloat() / guiWidth.toFloat() / guiScale,
            target.height.toFloat() / guiHeight.toFloat() / guiScale,
        )
        try {
        placements.forEach { placement ->
            val item = entriesById[placement.id]?.item ?: return@forEach
            //? if >= 26.1 {
            graphics.fakeItem(ItemStack(item), placement.x + ITEM_PADDING, placement.y + ITEM_PADDING)
            //? } else
            //graphics.renderFakeItem(ItemStack(item), placement.x + ITEM_PADDING, placement.y + ITEM_PADDING)
        }
        } finally {
            pose.popMatrix()
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

    private fun closeRenderResource(resource: AutoCloseable?) {
        runCatching { resource?.close() }
            .onFailure { LOG.warn("Failed to release item selector render resources", it) }
    }

    private fun resolveAtlasTarget(layout: AtlasLayout): Boolean {
        val targetWidth = layout.columns * CELL_SIZE * layout.renderScale
        val targetHeight = layout.rows * CELL_SIZE * layout.renderScale
        return runCatching { atlas.resolveTarget(targetWidth, targetHeight) }.getOrElse {
            LOG.warn("Failed to create the item icon atlas", it)
            false
        }
    }

    private fun createAtlasLayout(): AtlasLayout? {
        val maxTextureSize = min(maxSupportedTextureSize(), MAX_ATLAS_TARGET_SIZE)
        val screen = Platform.screen()
        val guiWidth = screen.guiWidth()
        val guiHeight = screen.guiHeight()
        val desiredScale = if (guiWidth > 0 && guiHeight > 0) {
            ceil(
                maxOf(
                    screen.viewportWidth().toDouble() / guiWidth,
                    screen.viewportHeight().toDouble() / guiHeight,
                ),
            ).toInt().coerceIn(1, MAX_ATLAS_RENDER_SCALE)
        } else {
            1
        }
        val itemCount = entriesById.size.coerceAtLeast(1)

        for (renderScale in desiredScale downTo 1) {
            val cellsPerAxis = maxTextureSize / (CELL_SIZE * renderScale)
            if (cellsPerAxis <= 0 || cellsPerAxis.toLong() * cellsPerAxis < itemCount.toLong()) continue
            val columns = ceil(sqrt(itemCount.toDouble())).toInt().coerceAtMost(cellsPerAxis)
            val rows = (itemCount + columns - 1) / columns
            return AtlasLayout(columns, rows, renderScale)
        }
        return null
    }

    private fun rebuildAtlasLocked() {
        val residentIds = icons.keys.toList()
        val pending = waiting.mapValues { (_, callbacks) -> callbacks.toMutableList() }

        cacheGeneration++
        icons.clear()
        waiting.clear()
        residentIds.forEach { id -> waiting[id] = mutableListOf() }
        pending.forEach { (id, callbacks) ->
            waiting.getOrPut(id, ::mutableListOf).addAll(callbacks)
        }
        nextSlot = 0
        atlasNeedsClear = true
    }

    private fun maxSupportedTextureSize(): Int {
        //? if >= 26.2 {
        return RenderSystem.getDevice().getDeviceInfo().limits().maxTextureSize()
        //? } else if >= 1.21.5 {
        /*return RenderSystem.getDevice().getMaxTextureSize()
        *///? } else {
        /*return RenderSystem.maxSupportedTextureSize()
        *///? }
    }

    private fun completeBatch(batch: RenderBatch) {
        val callbacks = mutableListOf<(Boolean) -> Unit>()
        var scheduleNext = false
        synchronized(requestLock) {
            if (batch.generation == cacheGeneration) {
                batch.placements.forEach { placement ->
                    icons[placement.id] = AtlasIcon(placement.source)
                    waiting.remove(placement.id).orEmpty().forEach { callbacks += it }
                }
            }
            renderScheduled = false
            if (waiting.isNotEmpty()) {
                renderScheduled = true
                scheduleNext = true
            }
        }
        if (scheduleNext) scheduleRender()
        callbacks.forEach { callback -> deliverIcon(callback, true) }
    }

    private fun failBatch(batch: RenderBatch, reason: String? = null) {
        val callbacks = mutableListOf<(Boolean) -> Unit>()
        var scheduleNext = false
        synchronized(requestLock) {
            if (batch.generation == cacheGeneration) {
                batch.placements.forEach { placement ->
                    waiting.remove(placement.id).orEmpty().forEach { callbacks += it }
                }
            }
            renderScheduled = false
            if (waiting.isNotEmpty()) {
                renderScheduled = true
                scheduleNext = true
            }
        }
        if (reason != null) LOG.debug("Item selector rendering skipped: {}", reason)
        if (scheduleNext) scheduleRender()
        callbacks.forEach { callback -> deliverIcon(callback, false) }
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

    private fun currentBatch(): RenderBatch = synchronized(requestLock) {
        val batch = RenderBatch(
            waiting.keys.take(MAX_BATCH_SIZE).map { id ->
                Placement(id, 0, 0, Rect.makeLTRB(0f, 0f, 0f, 0f))
            },
            cacheGeneration,
        )
        batch
    }

    private fun clearCaches() {
        var shouldSchedule = false
        synchronized(requestLock) {
            catalogCache = null
            resetAtlasLocked()
            if (waiting.isNotEmpty() && !renderScheduled) {
                renderScheduled = true
                shouldSchedule = true
            }
        }
        ItemCatalog.invalidateIcons()
        if (shouldSchedule) scheduleRender()
    }

    private fun resetAtlasLocked() {
        cacheGeneration++
        icons.clear()
        atlasLayout = null
        nextSlot = 0
        atlasNeedsClear = true
    }

    private companion object {
        val LOG = LoggerFactory.getLogger("OneConfig/ItemList")
        const val CELL_SIZE = 20
        const val ITEM_PADDING = 2
        const val ITEM_RENDER_SIZE = CELL_SIZE - ITEM_PADDING * 2
        const val MAX_ATLAS_RENDER_SCALE = 4
        const val MAX_ATLAS_TARGET_SIZE = 4096
        const val MAX_BATCH_SIZE = 64
    }
}
