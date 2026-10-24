package org.polyfrost.oneconfig.internal.ui.compose

//? if >= 1.21.11 {
//? if >= 26.3 {
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
//?} else {
/*import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
*///?}
//? if 26.2 {
/*import com.mojang.blaze3d.PrimitiveTopology
*///?} elif < 26.2
//import com.mojang.blaze3d.vertex.VertexFormat.Mode as PrimitiveTopology
//? if >= 26.1 && < 26.3
//import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.textures.GpuTextureView
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
//?} else {
/*import net.minecraft.client.gui.render.state.GuiElementRenderState
*///?}
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.locale.Language
import net.minecraft.network.chat.FormattedText
import org.joml.Matrix3x2f
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.font.TextRenderable
import net.minecraft.util.FormattedCharSequence
import org.joml.Matrix4f
import org.lwjgl.system.MemoryUtil
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyTessellator
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsRenderer
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.oneconfig.internal.ui.TextureRenderer
import org.polyfrost.oneconfig.internal.mixin.render.BufferBuilderAccessor
import org.polyfrost.oneconfig.internal.mixin.render.RenderPipelinesAccessor
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class BlazeRenderer(private val guiScale: Float, private val keepVertices: Boolean = false) : PolyTessellator(), TextureRenderer {
    private val ops = ArrayList<(GuiGraphicsExtractor) -> Unit>()

    val bitmaps = HashSet<ImageBitmap>()
    private val elementPose = Matrix3x2f().scale(1f / guiScale)
    private val screenW = Minecraft.getInstance().window.guiScaledWidth
    private val screenH = Minecraft.getInstance().window.guiScaledHeight

    override fun bitmapTexture(bitmap: ImageBitmap): Any {
        bitmaps.add(bitmap)
        return BitmapTextures.get(bitmap)
    }

    override fun submit(texture: Any, premultiplied: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
        val box = rectGui(bounds[0], bounds[1], bounds[2], bounds[3]) ?: return
        val setup = when (texture) {
            is AbstractTextureRef -> texture.setup()
            is GpuTextureView -> TextureSetup.singleTexture(texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            is PolyGlyphPage.Page -> GlyphAtlas.page(texture).setup()
            else -> GlyphAtlas.page(PolyGlyphPage.pages[0]).setup()
        }
        val pipeline = if (premultiplied) RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA else RenderPipelines.GUI_TEXTURED
        val mesh = Mesh(pipeline, setup, elementPose, verts, colors, box, keepVertices)
        //~ if >= 26.1 'submitGuiElement' -> 'addGuiElement'
        ops.add { g -> g.guiRenderState.addGuiElement(mesh) }
    }

    override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        flush()
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        val scissor = state.clip?.let { rectGui(it[0], it[1], it[2], it[3]) ?: return }
        val font = Minecraft.getInstance().font
        text.lines().forEachIndexed { index, line ->
            if (line.isEmpty()) return@forEachIndexed
            val m = Matrix3x2f().scale(1f / guiScale).translate(state.tx, state.ty).scale(state.sx, state.sy)
                .translate(x, y + index * font.lineHeight * scale).scale(scale)
            val seq = Language.getInstance().getVisualOrder(FormattedText.of(line))
            val draw: (GuiGraphicsExtractor) -> Unit = { g ->
                if (scissor != null) g.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom())
                g.pose().pushMatrix()
                g.pose().mul(m)
                //~ if >= 26.1 'drawString' -> 'text'
                g.text(font, seq, 0, 0, c, shadow)
                g.pose().popMatrix()
                if (scissor != null) g.disableScissor()
            }
            val baked = if (keepVertices && !line.contains("§k", ignoreCase = true)) bakeText(font, seq, m, c, shadow, scissor) else null
            if (baked != null) {
                ops.add { g ->
                    //~ if >= 26.1 'submitGuiElement' -> 'addGuiElement'
                    if (baked.any { it.textureSetup().texure0()!!.isClosed }) draw(g) else for (mesh in baked) g.guiRenderState.addGuiElement(mesh)
                }
                return@forEachIndexed
            }
            ops.add(draw)
        }
    }

    private fun bakeText(font: Font, seq: FormattedCharSequence, m: Matrix3x2f, color: Int, shadow: Boolean, scissor: ScreenRectangle?): List<Mesh>? {
        val prepared = font.prepareText(seq, 0f, 0f, color, shadow, false, 0)
        var bounds = prepared.bounds()?.transformMaxBounds(m) ?: return emptyList()
        if (scissor != null) bounds = scissor.intersection(bounds) ?: return emptyList()
        val pose = Matrix4f().mul(m)
        val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
        val meshes = ArrayList<Mesh>(1)
        var pipeline: RenderPipeline? = null
        var view: GpuTextureView? = null
        var cap = GlyphCapture()
        var unknown = false
        fun close() {
            if (cap.n == 0) return
            val setup = if (TEXT_LIGHTMAP) TextureSetup.singleTextureWithLightmap(view!!, sampler) else TextureSetup.singleTexture(view!!, sampler)
            meshes.add(Mesh(pipeline!!, setup, Matrix3x2f(), cap.verts.copyOf(cap.n * 4), cap.colors.copyOf(cap.n), bounds, true, scissor, TEXT_LIGHTMAP))
            cap = GlyphCapture()
        }
        prepared.visit(object : Font.GlyphVisitor {
            //? if >= 26.2 {
            override fun acceptRenderable(renderable: TextRenderable) = accept(renderable)
            //?} else {
            /*override fun acceptGlyph(glyph: TextRenderable.Styled) = accept(glyph)
            override fun acceptEffect(effect: TextRenderable) = accept(effect)
            *///?}

            fun accept(renderable: TextRenderable) {
                val p = renderable.guiPipeline()
                //~ if >= 26.2 'GUI_TEXT_INTENSITY' -> 'GUI_TEXT_GRAYSCALE'
                if (p !== RenderPipelines.GUI_TEXT && p !== RenderPipelines.GUI_TEXT_GRAYSCALE) { unknown = true; return }
                if (p !== pipeline || renderable.textureView() !== view) { close(); pipeline = p; view = renderable.textureView() }
                renderable.render(pose, cap, FULL_BRIGHT, true)
            }
        })
        if (unknown) return null
        close()
        return meshes
    }

    private class GlyphCapture : VertexConsumer {
        var verts = FloatArray(64)
        var colors = IntArray(16)
        var n = 0

        override fun addVertex(x: Float, y: Float, z: Float) = apply {
            if (n == colors.size) { colors = colors.copyOf(n * 2); verts = verts.copyOf(n * 8) }
            verts[n * 4] = x; verts[n * 4 + 1] = y; n++
        }
        override fun setUv(u: Float, v: Float) = apply { verts[n * 4 - 2] = u; verts[n * 4 - 1] = v }
        override fun setColor(color: Int) = apply { colors[n - 1] = color }
        override fun setColor(r: Int, g: Int, b: Int, a: Int) = setColor((a shl 24) or (r shl 16) or (g shl 8) or b)
        override fun setUv1(u: Int, v: Int) = this
        override fun setUv2(u: Int, v: Int) = this
        //? if >= 26.3
        override fun setUv3(u: Float, v: Float) = this
        override fun setNormal(x: Float, y: Float, z: Float) = this
        override fun setLineWidth(width: Float) = this
    }

    override fun texture(target: RenderTarget, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, alpha: Float) {
        texture(target.colorTextureView ?: return, x, y, w, h, u0, v0, u1, v1, alpha)
    }

    fun texture(view: GpuTextureView, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, alpha: Float) {
        val a = (alpha * state.alpha * 255f).roundToInt().coerceIn(0, 255)
        if (a == 0) return
        begin(view, true)
        val x0 = x * state.sx + state.tx
        val y0 = y * state.sy + state.ty
        quadPx(x0, y0, x0 + w * state.sx, y0 + h * state.sy, u0, v0, u1, v1, (a shl 24) or (a shl 16) or (a shl 8) or a)
    }

    override fun live(draw: (PolyRenderer) -> Unit) {
        flush()
        val s = state.copy()
        ops.add { g ->
            val r = BlazeRenderer(guiScale)
            r.state = s.copy()
            draw(r)
            for (op in r.finish()) op(g)
            GlyphAtlas.uploadDirty()
        }
    }

    fun finish(): List<(GuiGraphicsExtractor) -> Unit> {
        flush()
        return ops
    }

    private fun rectGui(x0: Float, y0: Float, x1: Float, y1: Float): ScreenRectangle? {
        val l = max(floor(x0 / guiScale).toInt(), 0)
        val t = max(floor(y0 / guiScale).toInt(), 0)
        val r = min(ceil(x1 / guiScale).toInt(), screenW)
        val b = min(ceil(y1 / guiScale).toInt(), screenH)
        return if (r <= l || b <= t) null else ScreenRectangle(l, t, r - l, b - t)
    }

    internal class Mesh(
        private val pipeline: RenderPipeline,
        private val textureSetup: TextureSetup,
        private val pose: Matrix3x2f,
        private val verts: FloatArray,
        private val colors: IntArray,
        private val bounds: ScreenRectangle,
        private val keep: Boolean,
        private val scissor: ScreenRectangle? = null,
        private val lightmap: Boolean = false,
    ) : GuiElementRenderState {
        @JvmField var gpu: Any? = null
        @JvmField var lastDrawn = 0
        val vertexCount get() = colors.size

        override fun buildVertices(consumer: VertexConsumer) {
            if (gpu != null) {
                //? if < 26.2 {
                /*                repeat(4) { vertex(consumer, 0f, 0f, 0f, 0f, 0) }
                *///?}
                return
            }
            val b = consumer as? BufferBuilderAccessor
            val before = b?.`oneconfig$getVertices`() ?: 0
            for (i in colors.indices) {
                val o = i * 4
                vertex(consumer, verts[o], verts[o + 1], verts[o + 2], verts[o + 3], colors[i])
            }
            if (keep && b != null && b.`oneconfig$getVertices`() - before == colors.size && b.`oneconfig$getElementsToFill`() == 0) {
                val size = b.`oneconfig$getVertexSize`()
                val len = colors.size * size
                val ptr = b.`oneconfig$getVertexPointer`() + size - len
                KeptMeshes.capture(this, ptr, len)
            }
        }

        private fun vertex(consumer: VertexConsumer, x: Float, y: Float, u: Float, v: Float, color: Int) {
            val c = consumer.addVertexWith2DPose(pose, x, y).setUv(u, v).setColor(color)
            if (lightmap) c.setLight(FULL_BRIGHT)
        }

        override fun pipeline() = if (gpu != null) KeptMeshes.pipeline(pipeline) else pipeline
        override fun textureSetup() = textureSetup
        override fun scissorArea(): ScreenRectangle? = scissor
        override fun bounds() = bounds
    }
}

private const val FULL_BRIGHT = 0xF000F0

//? if >= 26.2 {
private const val TEXT_LIGHTMAP = false
//?} else
//private const val TEXT_LIGHTMAP = true

internal object KeptMeshes {
    private const val RELEASE_FRAMES = 10
    private var frame = 0
    private val alive = ArrayList<BlazeRenderer.Mesh>()
    private val byDraw = java.util.IdentityHashMap<Any, ArrayList<BlazeRenderer.Mesh>>()

    private val snippet = RenderPipelinesAccessor.`oneconfig$getGuiTexturedSnippet`()
    private val straight = RenderPipeline.builder(snippet).withLocation("pipeline/oneconfig_gui_kept").build()
    private val premultiplied = RenderPipeline.builder(snippet).withLocation("pipeline/oneconfig_gui_kept_premultiplied")
        //? if >= 26.1 {
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)).build()
        //?} else
        //.withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA).build()

    private val textSnippet = RenderPipelinesAccessor.`oneconfig$getGuiTextSnippet`()
    //? if >= 26.3 {
    private val text = RenderPipeline.builder(textSnippet).withLocation("pipeline/oneconfig_gui_text_kept").build()
    private val textGrayscale = RenderPipeline.builder(textSnippet).withLocation("pipeline/oneconfig_gui_text_grayscale_kept").withShaderDefine("IS_GRAYSCALE").build()
    //?} elif >= 26.2 {
    /*    private val text = RenderPipeline.builder(textSnippet).withLocation("pipeline/oneconfig_gui_text_kept")
        .withVertexShader("core/text").withFragmentShader("core/text").build()
    private val textGrayscale = RenderPipeline.builder(textSnippet).withLocation("pipeline/oneconfig_gui_text_grayscale_kept")
        .withVertexShader("core/text").withFragmentShader("core/text").withShaderDefine("IS_GRAYSCALE").build()
    *///?} else {
    /*    private val fogSnippet = RenderPipelinesAccessor.`oneconfig$getFogSnippet`()
    private val text = RenderPipeline.builder(textSnippet, fogSnippet).withLocation("pipeline/oneconfig_gui_text_kept")
        .withVertexShader("core/rendertype_text").withFragmentShader("core/rendertype_text").withSampler("Sampler0").withSampler("Sampler2").build()
    private val textGrayscale = RenderPipeline.builder(textSnippet, fogSnippet).withLocation("pipeline/oneconfig_gui_text_intensity_kept")
        .withVertexShader("core/rendertype_text_intensity").withFragmentShader("core/rendertype_text_intensity").withSampler("Sampler0").withSampler("Sampler2").build()
    *///?}

    fun pipeline(vanilla: RenderPipeline) = when {
        vanilla === RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA -> premultiplied
        vanilla === RenderPipelines.GUI_TEXT -> text
        //~ if >= 26.2 'GUI_TEXT_INTENSITY' -> 'GUI_TEXT_GRAYSCALE'
        vanilla === RenderPipelines.GUI_TEXT_GRAYSCALE -> textGrayscale
        else -> straight
    }

    fun capture(mesh: BlazeRenderer.Mesh, ptr: Long, len: Int) {
        mesh.gpu = RenderSystem.getDevice().createBuffer({ "OneConfig kept GUI mesh" }, com.mojang.renderpearl.api.buffers.GpuBuffer.USAGE_VERTEX, MemoryUtil.memByteBuffer(ptr, len))
        mesh.lastDrawn = frame
        alive.add(mesh)
    }

    private fun kept(element: GuiElementRenderState): BlazeRenderer.Mesh? {
        if (element !is BlazeRenderer.Mesh || element.gpu == null || element.lastDrawn == frame) return null
        element.lastDrawn = frame
        RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(element.vertexCount / 4 * 6)
        return element
    }

    //? if >= 26.2 {
    @JvmStatic
    fun added(draw: Any, element: GuiElementRenderState) {
        val mesh = kept(element) ?: return
        byDraw.getOrPut(draw) { ArrayList(2) }.add(mesh)
    }
    //?} else {
    /*    private val byIndex = HashMap<Int, ArrayList<BlazeRenderer.Mesh>>()

    @JvmStatic
    fun added(index: Int, element: GuiElementRenderState) {
        val mesh = kept(element) ?: return
        byIndex.getOrPut(index) { ArrayList(2) }.add(mesh)
    }

    @JvmStatic
    fun recorded(draws: List<*>) {
        for ((i, meshes) in byIndex) byDraw[draws[i]!!] = meshes
        byIndex.clear()
    }
    *///?}

    @JvmStatic
    fun execute(draw: Any, pass: com.mojang.renderpearl.api.commands.RenderPass): Boolean {
        val meshes = byDraw[draw] ?: return false
        val first = meshes[0]
        val scissor = first.scissorArea()
        if (scissor == null) pass.disableScissor() else {
            val window = Minecraft.getInstance().window
            val s = window.guiScale.toInt()
            val right = min(scissor.right() * s, window.width)
            val bottom = min(scissor.bottom() * s, window.height)
            pass.enableScissor(scissor.left() * s, window.height - bottom, max(0, right - scissor.left() * s), max(0, bottom - scissor.top() * s))
        }
        val setup = first.textureSetup()
        //? if >= 26.3 {
        pass.setPipeline(RenderSystem.getCompiledPipeline(first.pipeline()))
        if (setup.texure0() != null) pass.setUniform("Sampler0", setup.texure0(), setup.sampler0())
        //?} else {
        /*pass.setPipeline(first.pipeline())
        if (setup.texure0() != null) pass.bindTexture("Sampler0", setup.texure0(), setup.sampler0())
        if (setup.texure2() != null) pass.bindTexture("Sampler2", setup.texure2(), setup.sampler2())
        *///?}
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        pass.setIndexBuffer(indices.getBuffer(meshes.maxOf { it.vertexCount } / 4 * 6), indices.type())
        for (m in meshes) {
            val gpu = m.gpu as com.mojang.renderpearl.api.buffers.GpuBuffer
            //? if >= 26.2 {
            pass.setVertexBuffer(0, gpu.slice())
            pass.drawIndexed(m.vertexCount / 4 * 6, 1, 0, 0, 0)
            //?} else {
            /*pass.setVertexBuffer(0, gpu)
            pass.drawIndexed(0, 0, m.vertexCount / 4 * 6, 1)
            *///?}
        }
        return true
    }

    @JvmStatic
    fun endFrame() {
        byDraw.clear()
        frame++
        alive.removeIf { m ->
            (frame - m.lastDrawn > RELEASE_FRAMES).also { if (it) { (m.gpu as com.mojang.renderpearl.api.buffers.GpuBuffer).close(); m.gpu = null } }
        }
    }
}

object BlazeHud {
    private var dirty = true
    private val recording = Recording()

    @JvmStatic
    fun markDirty() {
        dirty = true
    }

    @JvmStatic
    fun render(gfx: GuiGraphicsExtractor) {
        recording.render(gfx, dirty) { HudManager.render(it, HudManager.guiScreenWidth, HudManager.guiScreenHeight) }
        dirty = false
    }
}

object BlazeToasts {
    private val recording = Recording()
    private var drewLastFrame = false

    @JvmStatic
    fun render(gfx: GuiGraphicsExtractor) {
        val show = NotificationsManager.count > 0
        val first = show && !drewLastFrame
        drewLastFrame = show
        if (!show) return
        val sw = Platform.screen().guiWidth()
        val sh = Platform.screen().guiHeight()
        if (sw <= 0 || sh <= 0) return
        val changed = NotificationsRenderer.prepare(sw.toFloat(), sh.toFloat())
        gfx.nextStratum()
        recording.render(gfx, changed || first, NotificationsRenderer::draw)
    }
}

internal class Recording {
    private var ops: List<(GuiGraphicsExtractor) -> Unit> = emptyList()
    private var bitmaps: Set<ImageBitmap> = emptySet()
    private var scale = 0f

    fun render(gfx: GuiGraphicsExtractor, dirty: Boolean, draw: (RenderContext) -> Unit) {
        val guiScale = Minecraft.getInstance().window.guiScale.toFloat()
        if (dirty || guiScale != scale) {
            scale = guiScale
            val renderer = BlazeRenderer(guiScale, keepVertices = true)
            draw(RenderContext(renderer))
            ops = renderer.finish()
            bitmaps = renderer.bitmaps
            GlyphAtlas.uploadDirty()
            BitmapTextures.endFrame()
        } else {
            BitmapTextures.touch(bitmaps)
        }
        for (op in ops) op(gfx)
    }
}



class AbstractTextureRef(val texture: DynamicTexture) {
    fun setup(): TextureSetup = TextureSetup.singleTexture(texture.textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
}

internal object GlyphAtlas {
    private const val SIZE = PolyGlyphPage.SIZE

    private class Entry(val image: NativeImage, val ref: AbstractTextureRef, var uploaded: Int)

    private val entries = java.util.IdentityHashMap<PolyGlyphPage.Page, Entry>()

    private fun entry(page: PolyGlyphPage.Page) = entries.getOrPut(page) {
        val image = NativeImage(SIZE, SIZE, true)
        Entry(image, AbstractTextureRef(DynamicTexture({ "oneconfig_glyphs_${entries.size}" }, image)), 0)
    }

    fun page(page: PolyGlyphPage.Page): AbstractTextureRef = entry(page).ref

    fun uploadDirty() {
        for (page in PolyGlyphPage.pages) {
            val e = entry(page)
            if (e.uploaded == page.version) continue
            e.uploaded = page.version
            MemoryUtil.memIntBuffer(e.image.pointer, SIZE * SIZE).put(page.pixels)
            e.ref.texture.upload()
        }
    }
}


internal object BitmapTextures {
    private class Entry(val ref: AbstractTextureRef, var lastUsed: Long)

    private val cache = java.util.IdentityHashMap<ImageBitmap, Entry>()
    private var frame = 0L

    fun get(bitmap: ImageBitmap): AbstractTextureRef {
        cache[bitmap]?.let { it.lastUsed = frame; return it.ref }
        val w = bitmap.width
        val px = bitmap.pixels
        val img = NativeImage(w, bitmap.height, false)
        for (y in 0 until bitmap.height) for (x in 0 until w) {
            val o = (y * w + x) * 4
            val r = px[o].toInt() and 0xFF
            val g = px[o + 1].toInt() and 0xFF
            val b = px[o + 2].toInt() and 0xFF
            val a = px[o + 3].toInt() and 0xFF
            img.setPixel(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
        }
        val ref = AbstractTextureRef(DynamicTexture({ "oneconfig_hud_image" }, img))
        cache[bitmap] = Entry(ref, frame)
        return ref
    }

    val decoder = ImageBitmap.Decoder { bytes ->
        runCatching {
            NativeImage.read(bytes).use { img ->
                val w = img.width
                val px = ByteArray(w * img.height * 4)
                for (y in 0 until img.height) for (x in 0 until w) {
                    val c = img.getPixel(x, y)
                    val o = (y * w + x) * 4
                    px[o] = (c shr 16).toByte(); px[o + 1] = (c shr 8).toByte(); px[o + 2] = c.toByte(); px[o + 3] = (c ushr 24).toByte()
                }
                ImageBitmap(w, img.height, px)
            }
        }.getOrNull()
    }

    fun touch(bitmaps: Set<ImageBitmap>) {
        for (b in bitmaps) cache[b]?.lastUsed = frame
    }

    fun endFrame() {
        frame++
        val it = cache.values.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (frame - e.lastUsed > 600) {
                e.ref.texture.close()
                it.remove()
            }
        }
    }
}
//?}
