package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
//? if >= 26.2
import com.mojang.renderpearl.api.GpuFormat
//? if >= 1.21.8 && < 26.2
//import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
//? if < 1.21.8 {
/*import com.mojang.blaze3d.platform.Lighting
import net.minecraft.CrashReport
import net.minecraft.ReportedException
import net.minecraft.client.renderer.texture.OverlayTexture
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
*///?}
//? if = 1.21.5 {
/*import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.TextureFormat
*///?}
//? if >= 1.21.4 && < 1.21.8
//import com.mojang.blaze3d.ProjectionType
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.client.renderer.state.gui.GuiItemRenderState
import net.minecraft.util.Util
//?} elif >= 1.21.8 {
/*import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.gui.render.state.GuiItemRenderState
*///?} elif >= 1.21.4 {
/*import net.minecraft.client.renderer.item.ItemStackRenderState
*///?} else {
/*import net.minecraft.resources.ResourceLocation
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.renderer.block.model.BakedQuad
*///?}
//? if < 1.21.5
//import com.mojang.blaze3d.platform.GlStateManager
//? if >= 1.21.8 {
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.client.gui.navigation.ScreenRectangle
import org.joml.Matrix3x2f
//?} else {
/*import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
*///?}
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ContentChangeMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.api.event.v1.events.ResizeEvent
import org.polyfrost.oneconfig.api.event.v1.events.ServerJoinEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.SkiaOffscreenTarget
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect
import org.slf4j.LoggerFactory
import java.util.ArrayDeque
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MinecraftItemCatalogService : ItemCatalogService {
    internal interface ItemStackIconHandle : ItemIconHandle {
        /** Updates the stack rendered by this handle while preserving its atlas slot. */
        fun update(stack: ItemStack)
    }

    /** Reusable storage for comparing model identities without allocating a new buffer each frame. */
    private class IdentityBuffer {
        //? if >= 1.21.8 {
        val values = ArrayList<Any?>()
        fun clear() = values.clear()
        fun sameAs(other: IdentityBuffer): Boolean = values == other.values
        //?} else {
        /*private var kinds = ByteArray(16)
        private var refs = arrayOfNulls<Any>(16)
        private var ints = IntArray(16)
        private var size = 0

        fun clear() {
            for (index in 0 until size) refs[index] = null
            size = 0
        }

        fun ref(value: Any?) {
            ensureCapacity()
            kinds[size] = 0
            refs[size] = value
            size++
        }

        fun int(value: Int) {
            ensureCapacity()
            kinds[size] = 1
            ints[size] = value
            size++
        }

        fun sameAs(other: IdentityBuffer): Boolean {
            if (size != other.size) return false
            for (index in 0 until size) {
                if (kinds[index] != other.kinds[index]) return false
                if (kinds[index].toInt() == 0) {
                    if (refs[index] !== other.refs[index]) return false
                } else if (ints[index] != other.ints[index]) return false
            }
            return true
        }

        private fun ensureCapacity() {
            if (size < kinds.size) return
            val capacity = size * 2
            kinds = kinds.copyOf(capacity)
            refs = refs.copyOf(capacity)
            ints = ints.copyOf(capacity)
        }
        *///?}
    }

    //? if >= 1.21.8 {
    private class ReusableTrackingState : TrackingItemStackRenderState() {
        private var identity: Any = emptyList<Any?>()
        private var collector = ArrayList<Any?>()

        fun useIdentity(buffer: ArrayList<Any?>) {
            collector = buffer
            identity = buffer
        }

        fun freezeIdentity(identity: List<Any?>) {
            this.identity = identity
        }

        override fun clear() {
            super.clear()
            collector.clear()
        }

        override fun appendModelIdentityElement(element: Any) {
            collector.add(element)
        }

        override fun getModelIdentity(): Any = identity
    }
    //?}

    private data class RegistryEntry(val item: Item, val id: String)
    /** One handle owns its live stack, resolved state and atlas slot. */
    private inner class IconEntry(var stack: ItemStack, val forHud: Boolean) : ItemStackIconHandle {
        val revision = mutableIntStateOf(0)
        var requestedSizePx = 0
            private set
        private var closed = false
        var slot = 0
        var x = 0
        var y = 0
        var atlasSource: Rect = Rect.makeWH(0f, 0f)
        var atlas: AtlasState? = null
        var animated = false
        var special = false
        var specialRefreshAtNs = 0L
        var failed = false
        var identityInitialized = false
        var committedIdentity = IdentityBuffer()
        var scratchIdentity = IdentityBuffer()
        //? if >= 1.21.8 {
        var stableIdentity: List<Any?> = emptyList()
        val resolutionState = ReusableTrackingState()
        //?} elif >= 1.21.4 {
        /*val resolutionState = ItemStackRenderState()
        *///?} else {
        /*var resolvedGuiModel: BakedModel? = null
        var resolvedUsesBlockLight = true
        *///?}
        //? if < 1.21.5
        //val resolutionRandom = RandomSource.create(42L)

        override fun setRenderSizePx(size: Int) {
            synchronized(requestLock) {
                val requested = size.coerceAtLeast(1)
                if (closed || requestedSizePx == requested) return
                requestedSizePx = requested
                activeEntries.add(this)
                topologyDirty = true
            }
        }

        override fun update(stack: ItemStack) {
            synchronized(requestLock) {
                if (!closed) this.stack = stack
            }
        }

        override fun draw(canvas: Canvas, bounds: Rect, alpha: Float): Boolean {
            synchronized(requestLock) {
                if (closed) return false
                // Compose draw nodes re-record when this entry's pixels change.
                revision.intValue
            }
            return this@MinecraftItemCatalogService.draw(this, canvas, bounds, alpha)
        }

        override fun close() {
            synchronized(requestLock) {
                if (closed) return
                closed = true
                activeEntries.remove(this)
                topologyDirty = true
            }
        }
    }

    private data class AtlasLayout(val iconsPerSide: Int, val iconSizePx: Int) {
        val atlasSizePx: Int get() = iconsPerSide * iconSizePx
        val capacity: Int get() = iconsPerSide * iconsPerSide
    }
    /**
     * Atlas page grouped by icon size and HUD/screen ownership.
     *
     * If every icon cannot fit at [renderSizePx] within the maximum texture size, smaller cells are used.
     */
    private class AtlasState(val renderSizePx: Int, val forHud: Boolean) {
        val offscreenTarget = SkiaOffscreenTarget()
        val entries = ArrayList<IconEntry>()
        val changed = ArrayList<IconEntry>()
        var layout: AtlasLayout? = null
        var atlasImage: Image? = null
        var readyForSampling = false
        var rebuild = true
        var animatedTick = -1
        val freeSlots = ArrayDeque<Int>()
        var nextUnusedSlot = 0
        //? if >= 1.21.8
        var itemGuiResources: ItemGuiResources? = null
    }

    private val entries: List<RegistryEntry> by lazy {
        BuiltInRegistries.ITEM.mapNotNull { item ->
            if (item === Items.AIR) return@mapNotNull null
            val id = BuiltInRegistries.ITEM.getKey(item).toString()
            RegistryEntry(item, id)
        }
    }
    private val entriesById: Map<String, RegistryEntry> by lazy { entries.associateBy(RegistryEntry::id) }
    private val atlasPaint = Paint()
    private val requestLock = Any()
    private val atlases = HashMap<Pair<Int, Boolean>, AtlasState>()
    private val activeEntries = LinkedHashSet<IconEntry>()
    //? if = 1.21.5
    //private val clearTiles = HashMap<Int, GpuTexture>()
    private var topologyDirty = true
    private var iconInitializationStarted = false

    @Volatile
    private var catalogCache: List<ItemDescriptor>? = null
    @Volatile
    private var renderingFailed = false
    private val failedItems = HashSet<Item>()

    // Texture animations and glint only change visibly per tick, so animated icons rerender at that rate.
    private var clientTick = 0

    //? if >= 1.21.8
    private data class ItemGuiResources(val state: GuiRenderState, val renderer: GuiRenderer)

    init {
        EventManager.register(ResourceFinishedLoading::class.java, Runnable(::clearCaches))
        EventManager.register(ServerJoinEvent::class.java, Runnable {
            clearCaches()
            ItemCatalog.markIconsAvailable()
        })
        EventManager.register(ResizeEvent::class.java, Runnable(::recover))
        EventManager.register(TickEvent.End::class.java, Runnable { clientTick++ })
    }

    private fun clearCaches() {
        synchronized(requestLock) {
            catalogCache = null
            destroyAtlases(queueRendererDisposal = true)
        }
        recover()
    }

    /** Clears rendering failures so the atlas and failed items can retry after a reload or resize. */
    private fun recover() {
        renderingFailed = false
        synchronized(requestLock) {
            failedItems.clear()
            activeEntries.forEach { entry ->
                if (!entry.failed) return@forEach
                entry.failed = false
                entry.identityInitialized = false
            }
        }
        HudManager.invalidate()
    }

    private fun markFailed(entry: IconEntry, stage: String, throwable: Throwable) {
        entry.failed = true
        val item = entry.stack.item
        val firstFailure = synchronized(requestLock) { failedItems.add(item) }
        val id = BuiltInRegistries.ITEM.getKey(item)
        if (firstFailure) {
            LOG.warn("Item icon for {} failed while {}, it will stay blank until the next server join, resize, or resources reload", id, stage, throwable)
        }
    }

    override fun items(): List<ItemDescriptor> {
        catalogCache?.let { return it }
        return synchronized(requestLock) {
            catalogCache ?: entries.map { entry ->
                ItemDescriptor(entry.id, Component.translatable(entry.item.descriptionId).string)
            }.also { catalogCache = it }
        }
    }

    override fun requestIcons() {
        if (iconInitializationStarted) return
        iconInitializationStarted = true

        //? if >= 26.1 {
        // Item components are not bound before the first world load, so initialize them for title screen rendering.
        val componentsBound = { BuiltInRegistries.ITEM.all { BuiltInRegistries.ITEM.wrapAsHolder(it).areComponentsBound() } }
        java.util.concurrent.CompletableFuture.supplyAsync(
            {
                if (componentsBound()) null else {
                    //~ if < 26.3 'createWorldLookup' -> 'createLookup'
                    val lookup = net.minecraft.data.registries.VanillaRegistries.createWorldLookup()
                    BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
                }
            },
            Util.backgroundExecutor(),
        ).whenComplete { pending, failure ->
            Minecraft.getInstance().schedule {
                try {
                    if (failure != null) throw failure
                    if (!componentsBound()) pending?.forEach { it.apply() }
                    ItemCatalog.markIconsAvailable()
                } catch (throwable: Throwable) {
                    LOG.warn("Could not enable item icons before joining a world", throwable)
                }
            }
        }
        //?} else
        //ItemCatalog.markIconsAvailable()
    }

    override fun openIcon(id: String, forHud: Boolean): ItemIconHandle? {
        val entry = entriesById[id] ?: return null
        return openStack(ItemStack(entry.item), forHud)
    }

    internal fun openStack(stack: ItemStack, forHud: Boolean = false): ItemStackIconHandle =
        IconEntry(stack, forHud)

    override fun renderIcons(): Boolean = renderIcons(forHud = false)

    override fun renderHudIcons() {
        if (renderIcons(forHud = true)) HudManager.invalidate()
    }

    /** Renders the pages owned by one consumer and returns whether any of its pixels changed. */
    private fun renderIcons(forHud: Boolean): Boolean {
        if (!ItemCatalog.iconsAvailable || renderingFailed) return false
        var changed = false
        try {
            reconcileTopology()
            for (page in atlases.values) {
                if (page.forHud == forHud && renderAtlas(page)) changed = true
            }
        } catch (throwable: Throwable) {
            renderingFailed = true
            destroyAtlases()
            HudManager.invalidate()
            LOG.warn("Failed to render item selector icons", throwable)
            changed = true
        }
        return changed
    }

    private fun draw(entry: IconEntry, canvas: Canvas, bounds: Rect, alpha: Float): Boolean {
        val resolved = synchronized(requestLock) {
            val state = entry.atlas
            if (state == null || !atlasAvailable(state)) {
                if (state?.entries?.isNotEmpty() == true) {
                    resetAtlas(state, state.layout)
                    invalidateEntries(state.entries)
                }
                null
            } else {
                val image = state.atlasImage ?: return@synchronized null
                entry to image
            }
        }
        if (resolved == null) {
            return false
        }
        val (icon, image) = resolved
        return runCatching {
            val source = icon.atlasSource
            if (source.width <= 0f || source.height <= 0f || bounds.width <= 0f || bounds.height <= 0f) {
                return false
            }

            atlasPaint.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
            canvas.drawImageRect(image, source, bounds, SamplingMode.DEFAULT, atlasPaint, true)
            true
        }.getOrElse {
            LOG.debug("Failed to draw an item icon from the atlas", it)
            false
        }
    }

    private fun atlasAvailable(state: AtlasState): Boolean = state.offscreenTarget.target != null && state.offscreenTarget.surface != null

    private fun renderAtlas(atlasState: AtlasState): Boolean {
        val layout = atlasState.layout ?: return false
        if (atlasState.entries.isEmpty()) return false

        val offscreenTarget = atlasState.offscreenTarget
        val previousTarget = offscreenTarget.target
        if (previousTarget == null || offscreenTarget.surface == null) discardAtlasImage(atlasState)

        val resolved = offscreenTarget.resolveTarget(layout.atlasSizePx, layout.atlasSizePx)
        val target = offscreenTarget.target
        val surface = offscreenTarget.surface
        check(resolved && target != null && surface != null) {
            "Failed to create a ${layout.iconSizePx}px item icon atlas"
        }

        val guiSize = layout.iconsPerSide * CELL_SIZE
        val targetChanged = target !== previousTarget
        if (targetChanged) {
            discardAtlasImage(atlasState)
            atlasState.readyForSampling = false
        }

        val changed = atlasState.changed
        changed.clear()
        val rebuild = targetChanged || atlasState.rebuild
        val now = System.nanoTime()
        for (entry in atlasState.entries) {
            if (entry.failed) continue
            val resolvedChanged = try {
                resolveIconState(entry)
            } catch (throwable: Throwable) {
                markFailed(entry, "resolving its model", throwable)
                changed.add(entry)
                continue
            }
            val specialRefresh = entry.special && now - entry.specialRefreshAtNs >= 0
            if (resolvedChanged || (entry.animated && atlasState.animatedTick != clientTick) || rebuild || specialRefresh) {
                changed.add(entry)
                if (entry.special) entry.specialRefreshAtNs = now + SPECIAL_REFRESH_NS
            }
        }
        atlasState.animatedTick = clientTick

        if (changed.isEmpty()) return false

        val backend = SkiaCtx.vulkanService
        discardAtlasImage(atlasState)
        surface.notifyContentWillChange(if (rebuild) ContentChangeMode.DISCARD else ContentChangeMode.RETAIN)
        if (atlasState.readyForSampling) backend?.transitionOffscreenForRendering(target)
        if (rebuild) clearTarget(target) else clearCells(target, changed, layout)

        renderItemsIsolated(atlasState, target, layout, changed, guiSize)
        backend?.midFrameFlush()
        backend?.transitionOffscreenForSampling(target)
        backend?.midFrameFlush()
        atlasState.atlasImage = surface.makeImageSnapshot()
        atlasState.readyForSampling = true
        atlasState.rebuild = false
        // Rebuilds may move every atlas cell.
        invalidateEntries(if (rebuild) atlasState.entries else changed)
        return true
    }

    private fun invalidateEntries(entries: Collection<IconEntry>) {
        Snapshot.withMutableSnapshot {
            for (entry in entries) entry.revision.intValue++
            if (entries.any { !it.forHud }) HudManager.previewRevision.intValue++
        }
    }

    private fun resolveIconState(entry: IconEntry): Boolean {
        val stack = entry.stack
        val client = Minecraft.getInstance()
        val scratch = entry.scratchIdentity
        scratch.clear()
        //? if >= 1.21.8 {
        val state = entry.resolutionState
        state.useIdentity(scratch.values)
        client.itemModelResolver.updateForTopItem(
            state,
            stack,
            ItemDisplayContext.GUI,
            client.level,
            client.player,
            0,
        )
        val animated = state.isAnimated
        val special = false
        //?} else if >= 1.21.4 {
        /*val state = entry.resolutionState
        client.itemModelResolver.updateForTopItem(
            state,
            stack,
            ItemDisplayContext.GUI,
            //? if = 1.21.4
            //false,
            client.level,
            client.player,
            0,
        )
        var animated = false
        var special = false
        scratch.int(state.activeLayerCount)
        for (index in 0 until state.activeLayerCount) {
            val layer = state.layers[index]
            //? if >= 1.21.5 {
            val quads = layer.prepareQuadList()
            scratch.int(quads.size)
            var quadIndex = 0
            while (quadIndex < quads.size) {
                val quad = quads[quadIndex++]
                scratch.ref(quad)
                if (quad.sprite.contents().animatedTexture != null) animated = true
            }
            //?} else {
            /*val random = entry.resolutionRandom
            random.setSeed(42L)
            var quadCount = 0
            val directions = Direction.entries
            var directionIndex = 0
            while (directionIndex < directions.size) {
                val quads = layer.model?.getQuads(null, directions[directionIndex++], random).orEmpty()
                var quadIndex = 0
                while (quadIndex < quads.size) {
                    val quad = quads[quadIndex++]
                    scratch.ref(quad)
                    quadCount++
                    if (quad.sprite.contents().animatedTexture != null) animated = true
                }
                random.setSeed(42L)
            }
            val quads = layer.model?.getQuads(null, null, random).orEmpty()
            var quadIndex = 0
            while (quadIndex < quads.size) {
                val quad = quads[quadIndex++]
                scratch.ref(quad)
                quadCount++
                if (quad.sprite.contents().animatedTexture != null) animated = true
            }
            scratch.int(quadCount)
            *///?}
            if (layer.foilType != ItemStackRenderState.FoilType.NONE) animated = true
            if (layer.specialRenderer != null) special = true
            scratch.ref(layer.specialRenderer)
            scratch.int(layer.tintLayers.size)
            layer.tintLayers.forEach { tint -> scratch.int(tint) }
            scratch.ref(layer.renderType)
            //? if >= 1.21.5 {
            scratch.ref(layer.transform)
            scratch.int(if (layer.usesBlockLight) 1 else 0)
            //?} else {
            /*scratch.ref(layer.model?.transforms?.getTransform(ItemDisplayContext.GUI))
            scratch.int(if (layer.model?.usesBlockLight() == true) 1 else 0)
            *///?}
        }
        if (special) scratch.int(stack.components.hashCode())
        *///?} else {
        /*val renderer = client.itemRenderer
        val resolvedModel = renderer.getModel(stack, client.level, client.player, 0)
        // Vanilla replaces the held trident and spyglass models in GUIs
        val guiModel = when {
            stack.`is`(Items.TRIDENT) -> renderer.itemModelShaper.modelManager.getModel(
                ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident")),
            )
            stack.`is`(Items.SPYGLASS) -> renderer.itemModelShaper.modelManager.getModel(
                ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass")),
            )
            else -> resolvedModel
        }
        entry.resolvedGuiModel = guiModel
        entry.resolvedUsesBlockLight = resolvedModel.usesBlockLight()
        // These can change each frame without changing model identity.
        var animated = stack.hasFoil()
        val special = guiModel.isCustomRenderer
        if (special) scratch.int(stack.components.hashCode())
        scratch.ref(guiModel)
        val random = entry.resolutionRandom
        random.setSeed(42L)
        var quadCount = 0
        val directions = Direction.entries
        var directionIndex = 0
        while (directionIndex < directions.size) {
            val quads = guiModel.getQuads(null, directions[directionIndex++], random)
            var quadIndex = 0
            while (quadIndex < quads.size) {
                val quad = quads[quadIndex++]
                if (quad.sprite.contents().animatedTexture != null) animated = true
                scratch.ref(quad)
                scratch.int(if (quad.isTinted) 1 else 0)
                if (quad.isTinted) scratch.int(renderer.itemColors.getColor(stack, quad.tintIndex))
                quadCount++
            }
            random.setSeed(42L)
        }
        val quads = guiModel.getQuads(null, null, random)
        var quadIndex = 0
        while (quadIndex < quads.size) {
            val quad = quads[quadIndex++]
            if (quad.sprite.contents().animatedTexture != null) animated = true
            scratch.ref(quad)
            scratch.int(if (quad.isTinted) 1 else 0)
            if (quad.isTinted) scratch.int(renderer.itemColors.getColor(stack, quad.tintIndex))
            quadCount++
        }
        scratch.int(quadCount)
        scratch.ref(guiModel.transforms.getTransform(ItemDisplayContext.GUI))
        scratch.ref(ItemBlockRenderTypes.getRenderType(stack, true))
        scratch.int(if (resolvedModel.usesBlockLight()) 1 else 0)
        *///?}
        val changed = !entry.identityInitialized || !scratch.sameAs(entry.committedIdentity)
        if (changed) {
            val old = entry.committedIdentity
            entry.committedIdentity = scratch
            entry.scratchIdentity = old
            //? if >= 1.21.8
            entry.stableIdentity = entry.committedIdentity.values.toList()
        }
        // GuiRenderState uses the model identity as a hash key, so keep an immutable copy until the model changes.
        //? if >= 1.21.8
        entry.resolutionState.freezeIdentity(entry.stableIdentity)
        entry.identityInitialized = true
        val animationChanged = entry.animated != animated
        entry.animated = animated
        entry.special = special
        return changed || animationChanged
    }

    private fun renderItemsIsolated(
        atlasState: AtlasState,
        target: RenderTarget,
        layout: AtlasLayout,
        changed: List<IconEntry>,
        guiSize: Int
    ) {
        val renderable = if (changed.any { it.failed }) changed.filter { !it.failed } else changed
        if (renderable.isEmpty()) {
            //? if < 1.21.5
            //Minecraft.getInstance().mainRenderTarget.bindWrite(true)
            return
        }
        // Retry individually so one broken item does not prevent the rest of the batch from rendering.
        if (runCatching { renderItems(atlasState, target, renderable, guiSize, guiSize) }.isSuccess) return
        clearCells(target, renderable, layout)
        for (entry in renderable) {
            val single = listOf(entry)
            try {
                renderItems(atlasState, target, single, guiSize, guiSize)
            } catch (throwable: Throwable) {
                markFailed(entry, "rendering", throwable)
                clearCells(target, single, layout)
            }
        }
    }

    private fun renderItems(
        page: AtlasState,
        target: RenderTarget,
        entries: List<IconEntry>,
        guiWidth: Int,
        guiHeight: Int,
    ) {
        val mc = Minecraft.getInstance()
        val previousTarget = GuiTargetRedirect.target
        val previousScissor = GuiTargetRedirect.scissorTransform
        val renderSizePx = checkNotNull(page.layout).iconSizePx

        //? if >= 1.21.8 {
        try {
            val resources = page.itemGuiResources ?: GuiRenderState().let { state ->
                ItemGuiResources(state, createItemGuiRenderer(mc, state)).also { page.itemGuiResources = it }
            }
            //? if >= 1.21.11 {
            val graphics = GuiGraphicsExtractor(mc, resources.state, guiWidth, guiHeight)
            //?} else
            //val graphics = GuiGraphicsExtractor(mc, resources.state)
            val pose = graphics.pose()
            pose.pushMatrix()
            // Scale vanilla's 16×16 GUI slots to the atlas cell size.
            pose.scale(
                target.width.toFloat() / guiWidth.toFloat(),
                target.height.toFloat() / guiHeight.toFloat(),
            )
            try {
                entries.forEach { entry ->
                    val itemPose = Matrix3x2f(pose)
                    val scissor = ScreenRectangle(
                        entry.x / CELL_SIZE * renderSizePx, entry.y / CELL_SIZE * renderSizePx,
                        renderSizePx, renderSizePx,
                    )
                    // Submit the resolved state directly to avoid evaluating dynamic model properties twice.
                    //? if >= 26.1 {
                    resources.state.addItem(GuiItemRenderState(itemPose, entry.resolutionState, entry.x, entry.y, scissor))
                    //?} else {
                    /*resources.state.submitItem(
                        GuiItemRenderState(
                            entry.stack.item.name.toString(), itemPose, entry.resolutionState,
                            entry.x, entry.y, scissor,
                        ),
                    )
                    *///?}
                }
            } finally {
                pose.popMatrix()
            }

            val previousRenderSize = GuiTargetRedirect.itemRenderSizePx
            GuiTargetRedirect.target = target
            GuiTargetRedirect.scissorTransform = GuiTargetRedirect.ScissorTransform { left, top, right, bottom ->
                intArrayOf(left, target.height - bottom, right - left, bottom - top)
            }
            GuiTargetRedirect.itemRenderSizePx = renderSizePx
            try {
                //? if >= 26.2 {
                resources.renderer.render()
                //?} else {
                /*val fog = (mc.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`()
                    .getBuffer(net.minecraft.client.renderer.fog.FogRenderer.FogMode.NONE)
                resources.renderer.render(fog)
                *///?}
            } finally {
                GuiTargetRedirect.scissorTransform = previousScissor
                GuiTargetRedirect.itemRenderSizePx = previousRenderSize
                GuiTargetRedirect.target = previousTarget
            }
            //? if >= 26.1 {
            resources.renderer.endFrame()
            //?} else
            //resources.renderer.incrementFrameNumber()
        } catch (throwable: Throwable) {
            discardItemGuiResources(page)
            throw throwable
        }
        //?} else {
        /*val bufferSource = mc.renderBuffers().bufferSource()
        val graphics = GuiGraphics(mc, bufferSource)
        withGuiProjection(guiWidth, guiHeight) {
            GuiTargetRedirect.target = target
            GuiTargetRedirect.scissorTransform = atlasScissorTransform(target, renderSizePx, CELL_SIZE.toFloat())

            //? if < 1.21.5 {
            /*target.bindWrite(true)
            RenderSystem.disableScissor()
            *///?}

            try {
                entries.forEach { entry ->
                    graphics.enableScissor(entry.x, entry.y, entry.x + CELL_SIZE, entry.y + CELL_SIZE)
                    try {
                        val pose = graphics.pose()
                        pose.pushPose()
                        pose.translate(entry.x + 8f, entry.y + 8f, 150f)
                        try {
                            pose.scale(16f, -16f, 16f)
                            //? if >= 1.21.4 {
                            val flat = !entry.resolutionState.usesBlockLight()
                            if (flat) {
                                graphics.flush()
                                Lighting.setupForFlatItems()
                            }
                            entry.resolutionState.render(pose, bufferSource, 15728880, OverlayTexture.NO_OVERLAY)
                            //?} else {
                            /*val model = checkNotNull(entry.resolvedGuiModel)
                            val flat = !entry.resolvedUsesBlockLight
                            if (flat) Lighting.setupForFlatItems()
                            mc.itemRenderer.render(
                                entry.stack, ItemDisplayContext.GUI, false, pose, bufferSource,
                                15728880, OverlayTexture.NO_OVERLAY, model,
                            )
                            *///?}
                            graphics.flush()
                            if (flat) Lighting.setupFor3DItems()
                        } catch (throwable: Throwable) {
                            val report = CrashReport.forThrowable(throwable, "Rendering item")
                            val category = report.addCategory("Item being rendered")
                            category.setDetail("Item Type") { entry.stack.item.toString() }
                            category.setDetail("Item Components") { entry.stack.components.toString() }
                            category.setDetail("Item Foil") { entry.stack.hasFoil().toString() }
                            throw ReportedException(report)
                        } finally {
                            pose.popPose()
                        }
                    } finally { graphics.disableScissor() }
                }
                graphics.flush()
            } finally {
                //? if < 1.21.5 {
                /*Lighting.setupFor3DItems()
                RenderSystem.disableScissor()
                *///?}
                GuiTargetRedirect.scissorTransform = previousScissor
                GuiTargetRedirect.target = previousTarget
                //? if < 1.21.5
                //(previousTarget ?: mc.mainRenderTarget).bindWrite(true)
            }
        }
        *///?}
    }

    //? if < 1.21.8 {
    /** Scales the scissor rectangle to the item texture and flips its Y axis. */
    private fun atlasScissorTransform(target: RenderTarget, renderSizePx: Int, scissorSlotSize: Float) =
        GuiTargetRedirect.ScissorTransform { left, top, right, bottom ->
            val leftPx = (left / scissorSlotSize).roundToInt() * renderSizePx
            val topPx = (top / scissorSlotSize).roundToInt() * renderSizePx
            val rightPx = (right / scissorSlotSize).roundToInt() * renderSizePx
            val bottomPx = (bottom / scissorSlotSize).roundToInt() * renderSizePx
            intArrayOf(leftPx, target.height - bottomPx, rightPx - leftPx, bottomPx - topPx)
        }
    //?}

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

    private fun discardItemGuiResources(page: AtlasState, queued: Boolean = false) {
        val resources = page.itemGuiResources
        page.itemGuiResources = null
        if (resources != null) {
            val close: () -> Unit = {
                runCatching { resources.renderer.close() }
                    .onFailure { LOG.warn("Failed to release item icon render resources", it) }
            }
            if (queued) Minecraft.getInstance().schedule(Runnable(close)) else close()
        }
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

    private fun clearTarget(target: RenderTarget) {
        //? if >= 1.21.5 {
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        val colorTexture = target.colorTexture ?: return
        val depthTexture = target.depthTexture ?: return
        //? if >= 26.2 {
        val clearColor = org.joml.Vector4f(0f, 0f, 0f, 0f)
        val clearDepth = 0.0
        //?} else {
        /*val clearColor = 0
        val clearDepth = 1.0
        *///?}
        encoder.clearColorAndDepthTextures(colorTexture, clearColor, depthTexture, clearDepth)
        //?} else if >= 1.21.4 {
        /*target.clear()
        *///?} else {
        /*target.clear(Minecraft.ON_OSX)
        *///?}
    }

    private fun clearCells(target: RenderTarget, entries: List<IconEntry>, layout: AtlasLayout) {
        val guiSize = layout.iconsPerSide * CELL_SIZE

        //? if >= 1.21.8 {
        val colorTexture = target.colorTexture ?: return
        val depthTexture = target.depthTexture ?: return
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        //~ if < 26.2 '= 0.0' -> '= 1.0'
        val clearDepth = 0.0

        entries.forEach { entry ->
            val left = entry.x * target.width / guiSize
            val top = entry.y * target.height / guiSize
            val right = (entry.x + CELL_SIZE) * target.width / guiSize
            val bottom = (entry.y + CELL_SIZE) * target.height / guiSize

            encoder.clearColorAndDepthTextures(
                colorTexture, GuiRenderer.CLEAR_COLOR, depthTexture, clearDepth,
                left, target.height - bottom, right - left, bottom - top,
                //? if >= 26.3
                0,
            )
        }
        //?} elif = 1.21.5 {
        /*// 1.21.5 cannot clear a texture subregion directly, so copy a transparent tile into each changed cell.
        val colorTexture = target.colorTexture ?: return
        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        val clearTile = clearTiles.getOrPut(layout.iconSizePx) {
            device.createTexture(
                "OneConfig item icon clear tile",
                TextureFormat.RGBA8,
                layout.iconSizePx,
                layout.iconSizePx,
                1,
            ).also { encoder.clearColorTexture(it, 0) }
        }

        entries.forEach { entry ->
            val left = entry.x * target.width / guiSize
            val top = entry.y * target.height / guiSize
            val right = (entry.x + CELL_SIZE) * target.width / guiSize
            val bottom = (entry.y + CELL_SIZE) * target.height / guiSize

            encoder.copyTextureToTexture(
                clearTile, colorTexture, 0,
                left, target.height - bottom, 0, 0,
                right - left, bottom - top,
            )
        }
        target.depthTexture?.let { encoder.clearDepthTexture(it, 1.0) }
        *///?} else {
        /*target.bindWrite(false)
        GlStateManager._clearColor(0f, 0f, 0f, 0f)
        GlStateManager._clearDepth(1.0)
        GlStateManager._colorMask(true, true, true, true)
        GlStateManager._depthMask(true)
        GlStateManager._enableScissorTest()

        try {
            entries.forEach { entry ->
                val left = entry.x * target.width / guiSize
                val top = entry.y * target.height / guiSize
                val right = (entry.x + CELL_SIZE) * target.width / guiSize
                val bottom = (entry.y + CELL_SIZE) * target.height / guiSize

                val width = right - left
                val height = bottom - top
                // VulkanMod's glClear ignores the scissor box.
                if (SkiaCtx.vulkanService?.clearOffscreenRect(left, top, width, height) != true) {
                    GlStateManager._scissorBox(left, target.height - bottom, width, height)
                    //? if >= 1.21.4 {
                    GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
                    //?} else
                    //GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX)
                }
            }
        } finally {
            GlStateManager._disableScissorTest()
            Minecraft.getInstance().mainRenderTarget.bindWrite(true)
        }
        *///?}
    }

    /** Assigns active icon handles to atlas pages and stable slots. */
    private fun reconcileTopology() {
        synchronized(requestLock) {
            if (!topologyDirty) return
            if (activeEntries.isEmpty()) {
                destroyAtlases()
                topologyDirty = false
                return
            }

            val maxTextureSizePx = maxSupportedTextureSize()
            for (page in atlases.values) {
                for (index in page.entries.lastIndex downTo 0) {
                    val entry = page.entries[index]
                    if (entry !in activeEntries || entry.requestedSizePx != page.renderSizePx) {
                        releaseSlot(entry)
                    }
                }
            }
            activeEntries.forEach { entry ->
                if (entry.atlas != null) return@forEach
                entry.failed = entry.stack.item in failedItems
                val page = atlases.getOrPut(entry.requestedSizePx to entry.forHud) { AtlasState(entry.requestedSizePx, entry.forHud) }
                allocateSlot(page, entry, maxTextureSizePx)
            }

            val iterator = atlases.values.iterator()
            while (iterator.hasNext()) {
                val page = iterator.next()
                if (page.entries.isEmpty()) {
                    disposeAtlas(page)
                    //? if >= 1.21.8
                    discardItemGuiResources(page)
                    iterator.remove()
                }
            }

            //? if = 1.21.5 {
            /*val tileIterator = clearTiles.iterator()
            while (tileIterator.hasNext()) {
                val (size, texture) = tileIterator.next()
                if (atlases.values.none { it.layout?.iconSizePx == size }) {
                    texture.close()
                    tileIterator.remove()
                }
            }
            *///?}
            topologyDirty = false
        }

    }

    private fun allocateSlot(page: AtlasState, entry: IconEntry, maxTextureSizePx: Int) {
        takeFreeSlot(page)?.let { slot ->
            occupy(page, entry, slot)
            return
        }
        // Grow capacity by 50%, reducing cell resolution if necessary to keep the atlases within the texture limit.
        val side = ceil(sqrt(((page.layout?.capacity ?: 0) + 1) * 1.5)).toInt()
        val cellSizePx = page.renderSizePx.coerceAtMost(maxTextureSizePx / side).coerceAtLeast(1)
        val grown = AtlasLayout(side, cellSizePx)
        // Recreate GuiRenderer resources when cell resolution changes to avoid reusing storage sized for previous cells.
        //? if >= 1.21.8
        if (page.layout?.iconSizePx != cellSizePx) discardItemGuiResources(page)
        resetAtlas(page, grown)
        page.entries.forEach { place(it, it.slot, grown) }
        occupy(page, entry, checkNotNull(takeFreeSlot(page)))
    }

    private fun takeFreeSlot(page: AtlasState): Int? {
        page.freeSlots.pollFirst()?.let { return it }
        if (page.nextUnusedSlot >= (page.layout?.capacity ?: 0)) return null
        return page.nextUnusedSlot++
    }

    private fun occupy(page: AtlasState, entry: IconEntry, slot: Int) {
        check(entry.atlas == null) { "Item atlas entry is already placed" }
        entry.atlas = page
        place(entry, slot, checkNotNull(page.layout))
        page.entries.add(entry)
        entry.identityInitialized = false
    }

    private fun releaseSlot(entry: IconEntry) {
        val page = entry.atlas ?: return
        val slot = entry.slot
        page.entries.remove(entry)
        entry.atlas = null
        page.freeSlots.addLast(slot)
    }

    private fun maxSupportedTextureSize(): Int {
        //? if >= 26.2 {
        return RenderSystem.getDevice().deviceInfo.limits().maxTextureSizeForFormat(GpuFormat.RGBA8_UNORM)
        //?} else if >= 1.21.5 {
        /*return RenderSystem.getDevice().getMaxTextureSize()
        *///?} else
        //return RenderSystem.maxSupportedTextureSize()
    }

    private fun resetAtlas(state: AtlasState, layout: AtlasLayout?) {
        synchronized(requestLock) {
            discardAtlasImage(state)
            state.layout = layout
            state.rebuild = true
        }
    }

    private fun destroyAtlases(queueRendererDisposal: Boolean = false) {
        synchronized(requestLock) {
            invalidateEntries(activeEntries)
            atlases.values.forEach { page ->
                disposeAtlas(page)
                //? if >= 1.21.8
                discardItemGuiResources(page, queueRendererDisposal)
            }
            //? if = 1.21.5 {
            /*clearTiles.values.forEach(GpuTexture::close)
            clearTiles.clear()
            *///?}
            atlases.clear()
            topologyDirty = true
        }
    }

    private fun disposeAtlas(state: AtlasState) {
        discardAtlasImage(state)
        state.offscreenTarget.dispose()
        state.entries.forEach { it.atlas = null }
        state.entries.clear()
        state.freeSlots.clear()
        state.nextUnusedSlot = 0
    }

    private fun discardAtlasImage(state: AtlasState) {
        state.atlasImage?.close()
        state.atlasImage = null
    }

    private fun place(entry: IconEntry, slot: Int, layout: AtlasLayout) {
        val x = slot % layout.iconsPerSide * CELL_SIZE
        val y = slot / layout.iconsPerSide * CELL_SIZE
        val scale = layout.iconSizePx.toFloat() / CELL_SIZE
        entry.slot = slot
        entry.x = x
        entry.y = y
        entry.atlasSource = Rect.makeLTRB(
            x * scale,
            y * scale,
            (x + CELL_SIZE) * scale,
            (y + CELL_SIZE) * scale,
        )
    }

    private companion object {
        val LOG = LoggerFactory.getLogger("OneConfig/ItemList")
        const val CELL_SIZE = 16
        const val SPECIAL_REFRESH_NS = 1_000_000_000L
    }
}
