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
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.commands.RenderPass
import java.util.Collections
import java.util.IdentityHashMap
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
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.network.chat.TextColor
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.internal.ui.OffscreenTarget
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState
//?} else
//import net.minecraft.client.gui.render.state.GuiRenderState
//? if < 26.2 {
/*import net.minecraft.client.renderer.fog.FogRenderer
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
*///?}
import net.minecraft.locale.Language
import net.minecraft.util.ARGB
import net.minecraft.network.chat.FormattedText
import org.joml.Matrix3x2f
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.font.TextRenderable
import net.minecraft.util.FormattedCharSequence
import net.minecraft.network.chat.Style
import net.minecraft.util.StringDecomposer
import org.joml.Matrix4f
import org.lwjgl.system.MemoryUtil
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyTessellator
import org.polyfrost.compose.render.CapturingRenderer
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.render.PolyBitmap
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

class BlazeRenderer(private val guiScale: Float, private val keepVertices: Boolean = false) : PolyTessellator(), TextureRenderer, CapturingRenderer {
    private val ops = ArrayList<(GuiGraphicsExtractor) -> Unit>()
    private val bitmapLog = ArrayList<PolyBitmap>()

    val bitmaps = HashSet<PolyBitmap>()
    private val elementPose = Matrix3x2f().scale(1f / guiScale)
    private val screenW = Minecraft.getInstance().window.guiScaledWidth
    private val screenH = Minecraft.getInstance().window.guiScaledHeight

    override fun bitmapTexture(bitmap: PolyBitmap): Any {
        bitmaps.add(bitmap)
        val ref = BitmapTextures.get(bitmap)
        bitmapLog.add(bitmap)
        return ref
    }

    private class Captured(val ops: List<(GuiGraphicsExtractor) -> Unit>, val bitmaps: List<PolyBitmap>, val refs: List<AbstractTextureRef>, val boxes: FloatArray)

    override fun mark(): Any {
        flush()
        return intArrayOf(ops.size, bitmapLog.size, drawBoxMark)
    }

    override fun since(mark: Any): Any {
        flush()
        val m = mark as IntArray
        val bmps = ArrayList(bitmapLog.subList(m[1], bitmapLog.size))
        return Captured(ArrayList(ops.subList(m[0], ops.size)), bmps, bmps.map(BitmapTextures::get), drawBoxesSince(m[2]))
    }

    override fun valid(captured: Any): Boolean {
        val c = captured as Captured
        return c.bitmaps.indices.all { BitmapTextures.get(c.bitmaps[it]) === c.refs[it] }
    }

    override fun append(captured: Any) {
        flush()
        val c = captured as Captured
        ops.addAll(c.ops)
        appendDrawBoxes(c.boxes)
        bitmaps.addAll(c.bitmaps)
        bitmapLog.addAll(c.bitmaps)
    }

    override fun writeStateKey() {
        super.writeStateKey()
        keyAdd(guiScale); keyAdd(screenW.toFloat()); keyAdd(screenH.toFloat())
    }

    fun matches(guiScale: Float): Boolean {
        val window = Minecraft.getInstance().window
        return guiScale == this.guiScale && screenW == window.guiScaledWidth && screenH == window.guiScaledHeight
    }

    fun reset() {
        ops.clear()
        bitmapLog.clear()
        bitmaps.clear()
        resetTessellator()
    }

    override val layered: Boolean get() = true

    override fun layerMark(): Any = ops.size

    override fun layer(mark: Any, alpha: Float) {
        val sub = ops.subList(mark as Int, ops.size)
        if (sub.isEmpty()) return
        val op = LayerOp.of(ArrayList(sub), alpha, guiScale, screenW, screenH)
        sub.clear()
        if (op != null) ops.add(op)
    }

    override fun fade(mark: Any, alpha: Float): Boolean {
        val from = mark as Int
        val faded = ArrayList<(GuiGraphicsExtractor) -> Unit>(ops.size - from)
        for (i in from until ops.size) faded += when (val op = ops[i]) {
            is MeshOp -> MeshOp(op.mesh.faded(alpha))
            is BakedTextOp -> op.faded(alpha)
            is LiveTextOp -> op.faded(alpha)
            is LayerOp -> op.faded(alpha)
            is LiveOp -> op.faded(alpha)
            else -> return false
        }
        for (i in faded.indices) ops[from + i] = faded[i]
        return true
    }

    private fun touchText(text: String, x: Float, y: Float, scale: Float, shadow: Boolean) {
        val font = Minecraft.getInstance().font
        val lines = text.lines()
        val w = (lines.maxOf { font.width(it) } + 1f) * scale
        val h = (lines.size * font.lineHeight + 1f) * scale
        repeat(if (shadow) 2 else 1) {
            beginDraw()
            touch(x * state.sx + state.tx, y * state.sy + state.ty, (x + w) * state.sx + state.tx, (y + h) * state.sy + state.ty)
        }
    }

    override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
        val box = rectGui(bounds[0], bounds[1], bounds[2], bounds[3]) ?: return
        val setup = when (texture) {
            is AbstractTextureRef -> texture.setup(nearest)
            is GpuTextureView -> TextureSetup.singleTexture(texture, sampler(nearest))
            is PolyGlyphPage.Page -> GlyphAtlas.page(texture).setup(nearest)
            else -> GlyphAtlas.page(PolyGlyphPage.pages[0]).setup()
        }
        val pipeline = if (premultiplied) RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA else RenderPipelines.GUI_TEXTURED
        ops.add(MeshOp(Mesh(pipeline, setup, elementPose, verts, colors, box, keepVertices)))
    }

    override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        flush()
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        val scissor = state.clip?.let { rectGui(it[0], it[1], it[2], it[3]) ?: return }
        touchText(text, x, y, scale, shadow)
        val font = Minecraft.getInstance().font
        text.lines().forEachIndexed { index, line ->
            if (line.isEmpty()) return@forEachIndexed
            val m = lineMatrix(x, y, index, font.lineHeight, scale)
            val seq = visualOrder(line)
            val baked = if (!keepVertices || line.contains("§k", ignoreCase = true)) null
                else if ('§' in line || Language.getInstance().isDefaultRightToLeft) bakeText(font, seq, m, c, shadow, scissor)
                else GlyphQuads.bake(font, line, m, c, shadow, scissor) ?: bakeText(font, seq, m, c, shadow, scissor)
            ops.add(BakedTextOp(baked, c) { g, color -> vanillaText(g, font, seq, m, color, shadow, scissor) })
        }
    }

    override fun mcTextLive(text: String, x: Float, y: Float, color: () -> Int, shadow: Boolean, scale: Float) {
        if (!keepVertices || '§' in text || Language.getInstance().isDefaultRightToLeft) return super.mcTextLive(text, x, y, color, shadow, scale)
        flush()
        val alpha = state.alpha
        val scissor = state.clip?.let { rectGui(it[0], it[1], it[2], it[3]) ?: return }
        touchText(text, x, y, scale, shadow)
        val font = Minecraft.getInstance().font
        text.lines().forEachIndexed { index, line ->
            if (line.isEmpty()) return@forEachIndexed
            val m = lineMatrix(x, y, index, font.lineHeight, scale)
            val seq = visualOrder(line)
            val meshes = GlyphQuads.bake(font, line, m, -1, shadow, scissor, keep = false)
            val op = LiveTextOp(meshes, color, alpha) { g, c -> vanillaText(g, font, seq, m, c, shadow, scissor) }
            ops.add(op)
        }
    }

    private fun lineMatrix(x: Float, y: Float, index: Int, lineHeight: Int, scale: Float): Matrix3x2f =
        Matrix3x2f().scale(1f / guiScale).translate(state.tx, state.ty).scale(state.sx, state.sy)
            .translate(x, y + index * lineHeight * scale).scale(scale)

    override fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float) {
        val a = mulAlpha(outlineColor, state.alpha) ushr 24
        val clip = state.clip
        val scissor = clip?.let { rectGui(it[0], it[1], it[2], it[3]) }
        if (a != 0 && (clip == null || scissor != null)) {
            flush()
            val s = state
            val ox = x * s.sx + s.tx
            val oy = y * s.sy + s.ty
            val px = floor(ox)
            val py = floor(oy)
            val outline = McOutlines.get(text, outlineColor and 0xFFFFFF, scale * s.sx, scale * s.sy, ox - px, oy - py)
            if (outline != null) {
                val x0 = px - outline.pad
                val y0 = py - outline.pad
                val bounds = rectGui(x0, y0, x0 + outline.w, y0 + outline.h)
                if (bounds != null) {
                    beginDraw()
                    touch(x0, y0, x0 + outline.w, y0 + outline.h)
                    val color4 = (a shl 24) or (a shl 16) or (a shl 8) or a
                    ops.add { g -> outline.draw(g, elementPose, x0, y0, color4, bounds, scissor) }
                }
            }
        }
        mcText(text, x, y, color, false, scale)
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
            val p = pipeline ?: return
            val v = view ?: return
            if (cap.n == 0) return
            val setup = if (TEXT_LIGHTMAP) TextureSetup.singleTextureWithLightmap(v, sampler) else TextureSetup.singleTexture(v, sampler)
            meshes.add(Mesh(p, setup, Matrix3x2f(), cap.verts.copyOf(cap.n * 4), cap.colors.copyOf(cap.n), bounds, true, scissor, TEXT_LIGHTMAP))
            cap = GlyphCapture()
        }
        visitRenderables(prepared) { renderable ->
            val p = renderable.guiPipeline()
            if (!isTextPipeline(p)) { unknown = true; return@visitRenderables }
            if (p !== pipeline || renderable.textureView() !== view) { close(); pipeline = p; view = renderable.textureView() }
            renderable.render(pose, cap, FULL_BRIGHT, true)
        }
        if (unknown) return null
        close()
        return meshes
    }

    internal class GlyphCapture : VertexConsumer {
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
        beginDraw()
        val a = (alpha * state.alpha * 255f).roundToInt().coerceIn(0, 255)
        if (a == 0) return
        begin(view, true)
        val x0 = x * state.sx + state.tx
        val y0 = y * state.sy + state.ty
        quadPx(x0, y0, x0 + w * state.sx, y0 + h * state.sy, u0, v0, u1, v1, (a shl 24) or (a shl 16) or (a shl 8) or a)
    }

    private var liveRenderer: BlazeRenderer? = null

    override fun live(draw: (PolyRenderer) -> Unit) {
        flush()
        beginDraw()
        touchAll()
        ops.add(LiveOp(state.copy(), draw))
    }

    private inner class LiveOp(private val s: State, private val draw: (PolyRenderer) -> Unit) : (GuiGraphicsExtractor) -> Unit {
        private var held: List<LayerOp> = emptyList()

        fun faded(alpha: Float) = LiveOp(s.copy().also { it.alpha *= alpha }, draw)

        override fun invoke(g: GuiGraphicsExtractor) {
            val r = liveRenderer ?: BlazeRenderer(guiScale).also { liveRenderer = it }
            r.state = s.copy()
            draw(r)
            val out = r.finish()
            for (layer in held) layer.release()
            held = out.filterIsInstance<LayerOp>()
            for (op in out) op(g)
            r.reset()
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

    internal class MeshOp(val mesh: Mesh) : (GuiGraphicsExtractor) -> Unit {
        override fun invoke(g: GuiGraphicsExtractor) {
            //~ if >= 26.1 'submitGuiElement' -> 'addGuiElement'
            g.guiRenderState.addGuiElement(mesh)
        }
    }

    internal class LiveTextOp(
        val meshes: List<Mesh>?, private val color: () -> Int, private val alpha: Float,
        private val base: List<IntArray>? = meshes?.map { it.colors.copyOf() },
        private val fallback: (GuiGraphicsExtractor, Int) -> Unit,
    ) : (GuiGraphicsExtractor) -> Unit {
        private var drawn: Int? = null

        fun shifted(dx: Int, dy: Int) = LiveTextOp(meshes?.map { it.shifted(dx, dy) }, color, alpha, base) { g, c -> shiftedDraw(g, dx, dy) { fallback(it, c) } }

        fun faded(a: Float) = LiveTextOp(meshes?.map { it.faded(1f) }, color, alpha * a, base, fallback)

        fun changed() = alphaMul(color(), alpha) != drawn

        override fun invoke(g: GuiGraphicsExtractor) {
            val c = alphaMul(color(), alpha)
            drawn = c
            if (c ushr 24 == 0) return
            if (meshes == null || meshes.any { it.closed }) return fallback(g, c)
            val sc = ARGB.scaleRGB(c, 0.25f)
            for (k in meshes.indices) {
                val dst = meshes[k].colors
                val src = base!![k]
                for (i in dst.indices) dst[i] = if (src[i] and 0xFFFFFF == 0xFFFFFF) c else sc
                //~ if >= 26.1 'submitGuiElement' -> 'addGuiElement'
                g.guiRenderState.addGuiElement(meshes[k])
            }
        }
    }

    internal class BakedTextOp(
        val meshes: List<Mesh>?, private val color: Int, private val draw: (GuiGraphicsExtractor, Int) -> Unit,
    ) : (GuiGraphicsExtractor) -> Unit {
        fun shifted(dx: Int, dy: Int) = BakedTextOp(meshes?.map { it.shifted(dx, dy) }, color) { g, c -> shiftedDraw(g, dx, dy) { draw(it, c) } }

        fun faded(alpha: Float) = BakedTextOp(meshes?.map { it.faded(alpha) }, alphaMul(color, alpha), draw)

        override fun invoke(g: GuiGraphicsExtractor) {
            if (meshes == null || meshes.any { it.closed }) return draw(g, color)
            //~ if >= 26.1 'submitGuiElement' -> 'addGuiElement'
            for (mesh in meshes) g.guiRenderState.addGuiElement(mesh)
        }
    }

    internal class Mesh(
        private val pipeline: RenderPipeline,
        private val textureSetup: TextureSetup,
        private val pose: Matrix3x2f,
        private val verts: FloatArray,
        internal val colors: IntArray,
        private val bounds: ScreenRectangle,
        private val keep: Boolean,
        private val scissor: ScreenRectangle? = null,
        private val lightmap: Boolean = false,
    ) : GuiElementRenderState {
        @JvmField var gpu: Any? = null
        @JvmField var lastDrawn = 0
        private var drawn = false
        val vertexCount get() = colors.size
        val closed: Boolean get() = textureSetup.texure0()?.isClosed == true


        fun shifted(dx: Int, dy: Int) = Mesh(
            pipeline, textureSetup, Matrix3x2f().translation(-dx.toFloat(), -dy.toFloat()).mul(pose), verts, colors,
            bounds.shifted(dx, dy), keep, scissor?.shifted(dx, dy), lightmap,
        )

        fun faded(alpha: Float): Mesh {
            val premul = pipeline === RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA
            val out = IntArray(colors.size) { fadeColor(colors[it], alpha, premul) }
            return Mesh(pipeline, textureSetup, pose, verts, out, bounds, keep, scissor, lightmap)
        }

        fun mergeable(o: Mesh): Boolean =
            keep && o.keep && pipeline === o.pipeline && textureSetup == o.textureSetup && pose == o.pose &&
                scissor == o.scissor && lightmap == o.lightmap

        fun overlaps(b: ScreenRectangle): Boolean =
            bounds.left() < b.right() && b.left() < bounds.right() && bounds.top() < b.bottom() && b.top() < bounds.bottom()

        companion object {
            fun merge(list: List<Mesh>): Mesh {
                val first = list[0]
                var n = 0
                for (m in list) n += m.colors.size
                val verts = FloatArray(n * 4)
                val colors = IntArray(n)
                var o = 0
                var l = Int.MAX_VALUE; var t = Int.MAX_VALUE; var r = Int.MIN_VALUE; var b = Int.MIN_VALUE
                for (m in list) {
                    System.arraycopy(m.verts, 0, verts, o * 4, m.colors.size * 4)
                    System.arraycopy(m.colors, 0, colors, o, m.colors.size)
                    o += m.colors.size
                    l = min(l, m.bounds.left()); t = min(t, m.bounds.top()); r = max(r, m.bounds.right()); b = max(b, m.bounds.bottom())
                }
                return Mesh(first.pipeline, first.textureSetup, first.pose, verts, colors, ScreenRectangle(l, t, r - l, b - t), true, first.scissor, first.lightmap)
            }
        }

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
            val again = drawn
            drawn = true
            if (keep && again && b != null && b.`oneconfig$getVertices`() - before == colors.size && b.`oneconfig$getElementsToFill`() == 0) {
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

    companion object {
        fun coalesce(ops: List<(GuiGraphicsExtractor) -> Unit>): List<(GuiGraphicsExtractor) -> Unit> {
            val out = ArrayList<(GuiGraphicsExtractor) -> Unit>()
            val buckets = ArrayList<ArrayList<Mesh>?>()
            val fixed = ArrayList<LiveTextOp?>()
            val areas = ArrayList<ScreenRectangle>()
            fun emit() {
                for (i in buckets.indices) out.add(fixed[i] ?: buckets[i]!!.let { MeshOp(if (it.size == 1) it[0] else Mesh.merge(it)) })
                buckets.clear()
                fixed.clear()
                areas.clear()
            }
            fun union(a: ScreenRectangle, b: ScreenRectangle): ScreenRectangle {
                val l = min(a.left(), b.left()); val t = min(a.top(), b.top())
                return ScreenRectangle(l, t, max(a.right(), b.right()) - l, max(a.bottom(), b.bottom()) - t)
            }
            fun place(m: Mesh) {
                for (i in buckets.indices.reversed()) {
                    val b = buckets[i]
                    if (b != null && b[0].mergeable(m)) {
                        b.add(m)
                        areas[i] = union(areas[i], m.bounds())
                        return
                    }
                    if (m.overlaps(areas[i]) && (b ?: fixed[i]!!.meshes!!).any { m.overlaps(it.bounds()) }) break
                }
                buckets.add(arrayListOf(m))
                fixed.add(null)
                areas.add(m.bounds())
            }
            for (op in ops) {
                if (op is LiveTextOp && !op.meshes.isNullOrEmpty() && op.meshes.none { it.closed }) {
                    buckets.add(null)
                    fixed.add(op)
                    areas.add(op.meshes.map { it.bounds() }.reduce(::union))
                    continue
                }
                val meshes = when (op) {
                    is MeshOp -> listOf(op.mesh)
                    is BakedTextOp -> op.meshes?.takeUnless { m -> m.any { it.closed } }
                    else -> null
                }
                if (meshes == null) {
                    emit()
                    out.add(op)
                } else {
                    for (m in meshes) place(m)
                }
            }
            emit()
            return out
        }
    }
}

private const val FULL_BRIGHT = 0xF000F0

private fun fadeColor(color: Int, alpha: Float, premultiplied: Boolean): Int {
    if (!premultiplied) return alphaMul(color, alpha)
    var out = 0
    var shift = 0
    while (shift < 32) {
        out = out or ((((color ushr shift) and 0xFF) * alpha).roundToInt().coerceIn(0, 255) shl shift)
        shift += 8
    }
    return out
}

private fun alphaMul(color: Int, alpha: Float): Int {
    if (alpha >= 1f) return color
    val a = ((color ushr 24) * alpha).roundToInt().coerceIn(0, 255)
    return (a shl 24) or (color and 0x00FFFFFF)
}

private fun vanillaText(g: GuiGraphicsExtractor, font: Font, seq: FormattedCharSequence, m: Matrix3x2f, c: Int, shadow: Boolean, scissor: ScreenRectangle?) {
    if (scissor != null) g.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom())
    g.pose().pushMatrix()
    g.pose().mul(m)
    //~ if >= 26.1 'drawString' -> 'text'
    g.text(font, seq, 0, 0, c, shadow)
    g.pose().popMatrix()
    if (scissor != null) g.disableScissor()
}

//~ if >= 26.2 'GUI_TEXT_INTENSITY' -> 'GUI_TEXT_GRAYSCALE'
private fun isTextPipeline(p: RenderPipeline) = p === RenderPipelines.GUI_TEXT || p === RenderPipelines.GUI_TEXT_GRAYSCALE

private inline fun visitRenderables(prepared: Font.PreparedText, crossinline accept: (TextRenderable) -> Unit) {
    prepared.visit(object : Font.GlyphVisitor {
        //? if >= 26.2 {
        override fun acceptRenderable(renderable: TextRenderable) = accept(renderable)
        //?} else {
        /*override fun acceptGlyph(glyph: TextRenderable.Styled) = accept(glyph)
        override fun acceptEffect(effect: TextRenderable) = accept(effect)
        *///?}
    })
}

private object GlyphQuads {
    private class Glyph(
        val advance: Float, val pipeline: RenderPipeline?, val view: GpuTextureView?,
        val left: Float, val right: Float, val up: Float, val down: Float,
        val u0: Float, val u1: Float, val v0: Float, val v1: Float,
    )

    private val UNSUPPORTED = Glyph(0f, null, null, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private val glyphs = arrayOfNulls<Glyph>(0x590)
    private var font: Font? = null

    init {
        EventManager.register(ResourceFinishedLoading::class.java, Runnable { glyphs.fill(null) })
    }

    private fun glyph(font: Font, c: Char): Glyph {
        glyphs[c.code]?.let { return it }
        return probe(font, c).also { glyphs[c.code] = it }
    }

    private fun probe(font: Font, c: Char): Glyph {
        val str = c.toString()
        val advance = font.splitter.stringWidth(str)
        val prepared = font.prepareText(FormattedCharSequence.forward(str, Style.EMPTY), 0f, 0f, -1, true, false, 0)
        var found: TextRenderable? = null
        var count = 0
        visitRenderables(prepared) { found = it; count++ }
        val r = found ?: return if (count == 0) Glyph(advance, null, null, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) else UNSUPPORTED
        if (count != 1 || !isTextPipeline(r.guiPipeline())) return UNSUPPORTED
        val cap = BlazeRenderer.GlyphCapture()
        r.render(Matrix4f(), cap, FULL_BRIGHT, true)
        if (cap.n != 8) return UNSUPPORTED
        val v = cap.verts
        val l = v[16]; val up = v[17]; val down = v[21]; val rt = v[24]
        val u0 = v[18]; val v0 = v[19]; val v1 = v[23]; val u1 = v[26]
        val main = floatArrayOf(l, up, u0, v0, l, down, u0, v1, rt, down, u1, v1, rt, up, u1, v0)
        val shadow = floatArrayOf(1f + l, 1f + up, u0, v0, 1f + l, 1f + down, u0, v1, 1f + rt, 1f + down, u1, v1, 1f + rt, 1f + up, u1, v0)
        for (i in 0 until 16) if (v[i] != shadow[i] || v[16 + i] != main[i]) return UNSUPPORTED
        return Glyph(advance, r.guiPipeline(), r.textureView(), l, rt, up, down, u0, u1, v0, v1)
    }

    fun bake(font: Font, line: String, m: Matrix3x2f, color: Int, shadow: Boolean, scissor: ScreenRectangle?, keep: Boolean = true): List<BlazeRenderer.Mesh>? {
        if (this.font !== font) { glyphs.fill(null); this.font = font }
        for (ch in line) {
            if (ch.code >= 0x590) return null
            val g = glyph(font, ch)
            if (g === UNSUPPORTED) return null
            if (g.view?.isClosed == true) { glyphs.fill(null); return null }
        }
        val shadowColor = ARGB.scaleRGB(color, 0.25f)
        val so = if (shadow) 1f else 0f
        var left = Float.MAX_VALUE; var top = Float.MAX_VALUE; var right = -Float.MAX_VALUE; var bottom = -Float.MAX_VALUE
        var pen = 0f
        for (ch in line) {
            val g = glyphs[ch.code]!!
            if (g.pipeline != null) {
                left = min(left, pen + g.left); top = min(top, 0f + g.up)
                right = max(right, pen + g.right + so); bottom = max(bottom, 0f + g.down + so)
            }
            pen += g.advance
        }
        if (left >= right || top >= bottom) return emptyList()
        val l = floor(left).toInt(); val t = floor(top).toInt()
        var bounds = ScreenRectangle(l, t, ceil(right).toInt() - l, ceil(bottom).toInt() - t).transformMaxBounds(m)
        if (scissor != null) bounds = scissor.intersection(bounds) ?: return emptyList()
        val pose = Matrix4f().mul(m)
        val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
        val meshes = ArrayList<BlazeRenderer.Mesh>(1)
        var pipeline: RenderPipeline? = null
        var view: GpuTextureView? = null
        var cap = BlazeRenderer.GlyphCapture()
        fun close() {
            val p = pipeline ?: return
            val v = view ?: return
            if (cap.n == 0) return
            val setup = if (TEXT_LIGHTMAP) TextureSetup.singleTextureWithLightmap(v, sampler) else TextureSetup.singleTexture(v, sampler)
            meshes.add(BlazeRenderer.Mesh(p, setup, Matrix3x2f(), cap.verts.copyOf(cap.n * 4), cap.colors.copyOf(cap.n), bounds, keep, scissor, TEXT_LIGHTMAP))
            cap = BlazeRenderer.GlyphCapture()
        }
        pen = 0f
        for (ch in line) {
            val g = glyphs[ch.code]!!
            val p = g.pipeline
            if (p != null) {
                if (p !== pipeline || g.view !== view) { close(); pipeline = p; view = g.view }
                if (shadow) quad(cap, pose, pen + 1f, 0f + 1f, g, shadowColor)
                quad(cap, pose, pen, 0f, g, color)
            }
            pen += g.advance
        }
        close()
        return meshes
    }

    private fun quad(cap: BlazeRenderer.GlyphCapture, pose: Matrix4f, x: Float, y: Float, g: Glyph, color: Int) {
        val x0 = x + g.left; val x1 = x + g.right; val y0 = y + g.up; val y1 = y + g.down
        cap.addVertex(pose, x0, y0, 0f).setColor(color).setUv(g.u0, g.v0)
        cap.addVertex(pose, x0, y1, 0f).setColor(color).setUv(g.u0, g.v1)
        cap.addVertex(pose, x1, y1, 0f).setColor(color).setUv(g.u1, g.v1)
        cap.addVertex(pose, x1, y0, 0f).setColor(color).setUv(g.u1, g.v0)
    }

}

private fun visualOrder(line: String): FormattedCharSequence {
    val lang = Language.getInstance()
    if (!lang.isDefaultRightToLeft && line.all { it < '\u0590' }) {
        return if ('§' in line) FormattedCharSequence { sink -> StringDecomposer.iterateFormatted(line, Style.EMPTY, sink) }
        else FormattedCharSequence.forward(line, Style.EMPTY)
    }
    return lang.getVisualOrder(FormattedText.of(line))
}

internal object McOutlines {
    private const val MAX_ENTRIES = 32
    private const val MAX_POOLED = 8
    private const val PAD_FONT_PX = 2f
    private val OFFSETS = floatArrayOf(-1f, -1f, 0f, -1f, 1f, -1f, -1f, 0f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f)

    internal data class Key(val text: String, val rgb: Int, val kx: Float, val ky: Float, val fx: Float, val fy: Float)

    private val entries = LinkedHashMap<Key, Outline>(16, 0.75f, true)
    private val pool = ArrayList<OffscreenTarget>()
    private var gui: Pair<GuiRenderState, GuiRenderer>? = null
    private var generation = 0

    init {
        // sizes depend on the font, so outlines from before a reload are dropped rather than redrawn
        EventManager.register(ResourceFinishedLoading::class.java, Runnable {
            generation++
            entries.values.forEach { it.release() }
            entries.clear()
        })
    }

    fun get(text: String, rgb: Int, kx: Float, ky: Float, fx: Float, fy: Float): Outline? {
        if (kx <= 0f || ky <= 0f) return null
        val key = Key(text, rgb, kx, ky, (fx * 8f).roundToInt() / 8f, (fy * 8f).roundToInt() / 8f)
        entries[key]?.let { return it }
        val font = Minecraft.getInstance().font
        val lines = text.lines()
        val width = lines.maxOf { font.width(visualOrder(it)) }
        val pad = ceil(PAD_FONT_PX * max(kx, ky))
        val w = ceil(pad * 2f + width * kx + 1f)
        val h = ceil(pad * 2f + lines.size * font.lineHeight * ky + 1f)
        val outline = Outline(key, lines, pad, w, h, generation)
        entries[key] = outline
        evict()
        return outline
    }

    private fun evict() {
        if (entries.size <= MAX_ENTRIES) return
        val now = System.nanoTime()
        val it = entries.values.iterator()
        while (entries.size > MAX_ENTRIES && it.hasNext()) {
            val outline = it.next()
            if (now - outline.lastUsed < 1_000_000_000L) break
            it.remove()
            outline.release()
        }
    }

    private fun uncolored(seq: FormattedCharSequence) = FormattedCharSequence { sink ->
        seq.accept { index, style, codePoint -> sink.accept(index, style.withColor(null as TextColor?), codePoint) }
    }

    class Outline internal constructor(internal val key: Key, private val lines: List<String>, val pad: Float, val w: Float, val h: Float, private val generation: Int) {
        private val obfuscated = key.text.contains("§k", ignoreCase = true)
        private var target: OffscreenTarget? = null
        private var dirty = true
        internal var lastUsed = System.nanoTime()

        fun draw(g: GuiGraphicsExtractor, pose: Matrix3x2f, x0: Float, y0: Float, color: Int, bounds: ScreenRectangle, scissor: ScreenRectangle?) {
            lastUsed = System.nanoTime()
            if (target == null && generation == McOutlines.generation) entries.putIfAbsent(key, this)
            if (dirty || obfuscated || target?.target == null) render()
            val rt = target?.target ?: return
            val view = rt.colorTextureView ?: return
            val u1 = w / rt.width
            val v1 = 1f - h / rt.height
            val x1 = x0 + w
            val y1 = y0 + h
            val verts = floatArrayOf(x0, y0, 0f, 1f, x0, y1, 0f, v1, x1, y1, u1, v1, x1, y0, u1, 1f)
            val setup = TextureSetup.singleTexture(view, sampler(true))
            val mesh = BlazeRenderer.Mesh(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, setup, pose, verts, IntArray(4) { color }, bounds, false, scissor)
            //~ if >= 26.1 'submitGuiElement' -> 'addGuiElement'
            g.guiRenderState.addGuiElement(mesh)
        }

        internal fun release() {
            val t = target ?: return
            target = null
            if (pool.size < MAX_POOLED) pool.add(t) else t.dispose()
        }

        private fun render() {
            val offscreen = target ?: (pool.removeLastOrNull() ?: OffscreenTarget()).also { target = it }
            if (!offscreen.resolve(w.toInt(), h.toInt())) return
            val rt = offscreen.target ?: return
            offscreen.clear()
            val mc = Minecraft.getInstance()
            val (state, renderer) = gui ?: GuiRenderState().let { s -> (s to createGuiRenderer(mc, s)).also { gui = it } }
            val graphics = GuiGraphicsExtractor(mc, state, rt.width, rt.height)
            val font = mc.font
            val color = 0xFF000000.toInt() or key.rgb
            val ox = pad + key.fx
            val oy = pad + key.fy
            lines.forEachIndexed { index, line ->
                if (line.isEmpty()) return@forEachIndexed
                val seq = uncolored(visualOrder(line))
                for (i in OFFSETS.indices step 2) {
                    val pose = graphics.pose()
                    pose.pushMatrix()
                    pose.translate(ox, oy).scale(key.kx, key.ky).translate(OFFSETS[i], OFFSETS[i + 1] + index * font.lineHeight)
                    //~ if >= 26.1 'drawString' -> 'text'
                    graphics.text(font, seq, 0, 0, color, false)
                    pose.popMatrix()
                }
            }
            val previousTarget = GuiTargetRedirect.target
            val previousSize = GuiTargetRedirect.itemRenderSizePx
            GuiTargetRedirect.target = rt
            GuiTargetRedirect.itemRenderSizePx = 1
            try {
                //? if >= 26.2 {
                renderer.render()
                //?} else {
                /*renderer.render((mc.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`().getBuffer(FogRenderer.FogMode.NONE))
                *///?}
            } finally {
                GuiTargetRedirect.itemRenderSizePx = previousSize
                GuiTargetRedirect.target = previousTarget
            }
            //? if >= 26.1 {
            renderer.endFrame()
            //?} else
            //renderer.incrementFrameNumber()
            dirty = false
        }
    }

    internal fun createGuiRenderer(mc: Minecraft, state: GuiRenderState): GuiRenderer {
        //? if >= 26.2 {
        return GuiRenderer(state, mc.gameRenderer.featureRenderDispatcher(), emptyList())
        //?} else {
        /*return GuiRenderer(state, mc.renderBuffers().bufferSource(), mc.gameRenderer.getSubmitNodeStorage(), mc.gameRenderer.getFeatureRenderDispatcher(), emptyList())
        *///?}
    }
}

//? if >= 26.2 {
private const val TEXT_LIGHTMAP = false
//?} else
//private const val TEXT_LIGHTMAP = true

internal object KeptMeshes {
    private const val RELEASE_FRAMES = 10
    private var frame = 0
    private var scanAt = 0
    private val alive = ArrayList<BlazeRenderer.Mesh>()
    private val byDraw = IdentityHashMap<Any, ArrayList<BlazeRenderer.Mesh>>()

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
        mesh.gpu = RenderSystem.getDevice().createBuffer({ "OneConfig kept GUI mesh" }, GpuBuffer.USAGE_VERTEX, MemoryUtil.memByteBuffer(ptr, len))
        mesh.lastDrawn = frame
        alive.add(mesh)
        scanAt = min(scanAt, frame + RELEASE_FRAMES + 1)
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
    fun execute(draw: Any, pass: RenderPass): Boolean {
        val meshes = byDraw[draw] ?: return false
        val first = meshes[0]
        val scissor = first.scissorArea()
        val transform = GuiTargetRedirect.scissorTransform
        if (scissor == null) pass.disableScissor() else if (transform != null) {
            val m = transform.map(scissor.left(), scissor.top(), scissor.right(), scissor.bottom())
            pass.enableScissor(m[0], m[1], m[2], m[3])
        } else {
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
            val gpu = m.gpu as GpuBuffer
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
        if (frame < scanAt) return
        var next = Int.MAX_VALUE
        alive.removeIf { m ->
            (frame - m.lastDrawn > RELEASE_FRAMES).also { if (it) { (m.gpu as GpuBuffer).close(); m.gpu = null } else next = min(next, m.lastDrawn + RELEASE_FRAMES + 1) }
        }
        scanAt = next
    }
}

object BlazeHud {
    private var dirty = true
    private var scale = 0f
    private val stable = Recording(coalesce = true)
    private val volatile = Recording()
    private var stableRenderer: BlazeRenderer? = null
    private var volatileRenderer: BlazeRenderer? = null

    @JvmStatic
    fun markDirty() {
        dirty = true
    }

    @JvmStatic
    fun render(gfx: GuiGraphicsExtractor) {
        val guiScale = Minecraft.getInstance().window.guiScale.toFloat()
        if (dirty || guiScale != scale) {
            scale = guiScale
            val a = reuse(stableRenderer, guiScale).also { stableRenderer = it }
            val b = reuse(volatileRenderer, guiScale).also { volatileRenderer = it }
            HudManager.render(RenderContext(a), HudManager.guiScreenWidth, HudManager.guiScreenHeight, RenderContext(b))
            stable.take(a, copy = true)
            volatile.take(b, copy = true)
            GlyphAtlas.uploadDirty()
            BitmapTextures.endFrame()
        } else {
            stable.idle()
            volatile.idle()
        }
        dirty = false
        stacking(gfx) {
            stable.replay(gfx)
            volatile.replay(gfx)
        }
    }

    private fun reuse(renderer: BlazeRenderer?, guiScale: Float): BlazeRenderer {
        if (renderer == null || !renderer.matches(guiScale)) return BlazeRenderer(guiScale, keepVertices = true)
        renderer.reset()
        return renderer
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

internal class Recording(private val coalesce: Boolean = false) {
    private var ops: List<(GuiGraphicsExtractor) -> Unit> = emptyList()
    private var raw: List<(GuiGraphicsExtractor) -> Unit> = emptyList()
    private var stable = 0
    private var bitmaps: Set<PolyBitmap> = emptySet()
    private var scale = 0f

    fun render(gfx: GuiGraphicsExtractor, dirty: Boolean, draw: (RenderContext) -> Unit) {
        val guiScale = Minecraft.getInstance().window.guiScale.toFloat()
        if (dirty || guiScale != scale) {
            scale = guiScale
            val renderer = BlazeRenderer(guiScale, keepVertices = true)
            draw(RenderContext(renderer))
            take(renderer)
            GlyphAtlas.uploadDirty()
            BitmapTextures.endFrame()
        } else {
            idle()
        }
        stacking(gfx) { replay(gfx) }
    }

    fun take(renderer: BlazeRenderer, copy: Boolean = false) {
        val next = renderer.finish()
        if (coalesce && next.size == raw.size && next.indices.all { next[it] === raw[it] }) {
            idle()
            return
        }
        val old = raw
        raw = if (copy) ArrayList(next) else next
        LayerOp.retire(old, raw)
        ops = raw
        stable = 0
        bitmaps = if (copy) HashSet(renderer.bitmaps) else renderer.bitmaps
        BitmapTextures.touch(bitmaps)
    }

    fun idle() {
        BitmapTextures.touch(bitmaps)
        if (coalesce && ++stable == COALESCE_FRAMES) ops = BlazeRenderer.coalesce(raw)
        if (ops !== raw && ops.any { it is BlazeRenderer.MeshOp && it.mesh.closed }) ops = raw
    }


    fun replay(gfx: GuiGraphicsExtractor) {
        for (op in ops) op(gfx)
    }
}

internal class LayerOp private constructor(
    private val ops: List<(GuiGraphicsExtractor) -> Unit>, private val alpha: Float, private val guiScale: Float, val bounds: ScreenRectangle,
) : (GuiGraphicsExtractor) -> Unit {
    private var target: OffscreenTarget? = null
    private var composite: BlazeRenderer.Mesh? = null
    private var lastUsed = 0L

    private fun moved(dx: Int, dy: Int) = LayerOp(ops, alpha, guiScale, bounds.shifted(dx, dy))

    fun faded(a: Float) = LayerOp(ops, alpha * a, guiScale, bounds)

    private fun stale(): Boolean = ops.any {
        when (it) {
            is BlazeRenderer.MeshOp, is BlazeRenderer.BakedTextOp -> false
            is BlazeRenderer.LiveTextOp -> it.changed()
            is LayerOp -> it.stale()
            else -> true
        }
    }

    override fun invoke(g: GuiGraphicsExtractor) {
        lastUsed = System.nanoTime()
        sweep(lastUsed)
        val c = composite?.takeUnless { it.closed || stale() } ?: render() ?: return
        //~ if >= 26.1 'submitGuiElement' -> 'addGuiElement'
        g.guiRenderState.addGuiElement(c)
    }

    fun release() {
        active.remove(this)
        target?.let { if (pool.size < MAX_POOLED) pool.add(it) else it.dispose() }
        target = null
        composite = null
        for (op in ops) if (op is LayerOp) op.release()
    }

    private fun render(): BlazeRenderer.Mesh? {
        val gs = guiScale.roundToInt()
        val w = bounds.width() * gs
        val h = bounds.height() * gs
        val offscreen = target ?: acquire(w, h).also { target = it; active.add(this) }
        val current = offscreen.target
        if (!offscreen.resolve(PolyTessellator.layerTargetSize(current?.width ?: 0, w), PolyTessellator.layerTargetSize(current?.height ?: 0, h))) return null
        val rt = offscreen.target ?: return null
        offscreen.clear()
        val mc = Minecraft.getInstance()
        val (state, renderer) = gui.getOrNull(depth) ?: GuiRenderState().let { s -> (s to McOutlines.createGuiRenderer(mc, s)).also { gui.add(it) } }
        depth++
        try {
            val graphics = GuiGraphicsExtractor(mc, state, bounds.width(), bounds.height())
            stacking(graphics) { for (op in ops) op(graphics) }
        } finally {
            depth--
        }
        val previousTarget = GuiTargetRedirect.target
        val previousScissor = GuiTargetRedirect.scissorTransform
        val previousSize = GuiTargetRedirect.itemRenderSizePx
        GuiTargetRedirect.target = rt
        GuiTargetRedirect.scissorTransform = GuiTargetRedirect.ScissorTransform { l, t, r, b -> intArrayOf(l * gs, rt.height - b * gs, (r - l) * gs, (b - t) * gs) }
        GuiTargetRedirect.itemRenderSizePx = 0
        try {
            //? if >= 26.2 {
            renderer.render()
            //?} else {
            /*renderer.render((mc.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`().getBuffer(FogRenderer.FogMode.NONE))
            *///?}
        } finally {
            GuiTargetRedirect.itemRenderSizePx = previousSize
            GuiTargetRedirect.scissorTransform = previousScissor
            GuiTargetRedirect.target = previousTarget
        }
        //? if >= 26.1 {
        renderer.endFrame()
        //?} else
        //renderer.incrementFrameNumber()
        val view = rt.colorTextureView ?: return null
        val a = (alpha * 255f).roundToInt().coerceIn(0, 255)
        val l = bounds.left().toFloat(); val t = bounds.top().toFloat(); val r = bounds.right().toFloat(); val b = bounds.bottom().toFloat()
        val u1 = w.toFloat() / rt.width
        val v1 = 1f - h.toFloat() / rt.height
        val verts = floatArrayOf(l, t, 0f, 1f, l, b, 0f, v1, r, b, u1, v1, r, t, u1, 1f)
        val setup = TextureSetup.singleTexture(view, sampler(true))
        return BlazeRenderer.Mesh(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, setup, Matrix3x2f(), verts, IntArray(4) { a * 0x01010101 }, bounds, false)
            .also { composite = it }
    }

    companion object {
        private const val MAX_POOLED = 4
        private val pool = ArrayList<OffscreenTarget>()
        private val active = ArrayList<LayerOp>()
        private val gui = ArrayList<Pair<GuiRenderState, GuiRenderer>>()
        private var depth = 0
        private var sweptAt = 0L

        fun of(ops: List<(GuiGraphicsExtractor) -> Unit>, alpha: Float, guiScale: Float, screenW: Int, screenH: Int): LayerOp? {
            var bounds: ScreenRectangle? = null
            fun add(b: ScreenRectangle) { bounds = bounds?.let { union(it, b) } ?: b }
            for (op in ops) when (op) {
                is BlazeRenderer.MeshOp -> add(op.mesh.bounds())
                is BlazeRenderer.BakedTextOp -> op.meshes?.forEach { add(it.bounds()) } ?: return LayerOp(ops, alpha, guiScale, ScreenRectangle(0, 0, screenW, screenH))
                is BlazeRenderer.LiveTextOp -> op.meshes?.forEach { add(it.bounds()) } ?: return LayerOp(ops, alpha, guiScale, ScreenRectangle(0, 0, screenW, screenH))
                is LayerOp -> add(op.bounds)
                else -> return LayerOp(ops, alpha, guiScale, ScreenRectangle(0, 0, screenW, screenH))
            }
            val b = bounds?.intersection(ScreenRectangle(0, 0, screenW, screenH)) ?: return null
            if (b.width() <= 0 || b.height() <= 0) return null
            val dx = b.left(); val dy = b.top()
            val shifted = ops.map { op ->
                when (op) {
                    is BlazeRenderer.MeshOp -> BlazeRenderer.MeshOp(op.mesh.shifted(dx, dy))
                    is BlazeRenderer.BakedTextOp -> op.shifted(dx, dy)
                    is BlazeRenderer.LiveTextOp -> op.shifted(dx, dy)
                    else -> (op as LayerOp).moved(dx, dy)
                }
            }
            return LayerOp(shifted, alpha, guiScale, b)
        }

        fun retire(old: List<(GuiGraphicsExtractor) -> Unit>, next: List<(GuiGraphicsExtractor) -> Unit>) {
            if (old.none { it is LayerOp }) return
            val kept = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
            for (op in next) if (op is LayerOp) kept.add(op)
            for (op in old) if (op is LayerOp && op !in kept) op.release()
        }

        private fun acquire(w: Int, h: Int): OffscreenTarget {
            val fit = pool.filter { it.target?.let { t -> t.width >= w && t.height >= h } == true }.minByOrNull { it.target!!.let { t -> t.width * t.height } }
            if (fit != null) return fit.also { pool.remove(it) }
            return pool.removeLastOrNull() ?: OffscreenTarget()
        }

        private fun sweep(now: Long) {
            if (now - sweptAt < 250_000_000L) return
            sweptAt = now
            active.filter { now - it.lastUsed > 1_000_000_000L }.forEach(LayerOp::release)
        }

        private fun union(a: ScreenRectangle, b: ScreenRectangle): ScreenRectangle {
            val l = min(a.left(), b.left()); val t = min(a.top(), b.top())
            return ScreenRectangle(l, t, max(a.right(), b.right()) - l, max(a.bottom(), b.bottom()) - t)
        }
    }
}

private fun ScreenRectangle.shifted(dx: Int, dy: Int) = ScreenRectangle(left() - dx, top() - dy, width(), height())

private fun shiftedDraw(g: GuiGraphicsExtractor, dx: Int, dy: Int, draw: (GuiGraphicsExtractor) -> Unit) {
    g.pose().pushMatrix()
    g.pose().translate(-dx.toFloat(), -dy.toFloat())
    try {
        draw(g)
    } finally {
        g.pose().popMatrix()
    }
}

private inline fun stacking(gfx: GuiGraphicsExtractor, draw: () -> Unit) {
    val stacking = gfx.guiRenderState as StackingGuiRenderState
    stacking.`oneconfig$setStacking`(true)
    try {
        draw()
    } finally {
        stacking.`oneconfig$setStacking`(false)
    }
}

private const val COALESCE_FRAMES = 60

interface StackingGuiRenderState {
    fun `oneconfig$setStacking`(stacking: Boolean)
}



class AbstractTextureRef(val texture: DynamicTexture) {
    fun setup(nearest: Boolean = false): TextureSetup = TextureSetup.singleTexture(texture.textureView, sampler(nearest))
}

private fun sampler(nearest: Boolean) = RenderSystem.getSamplerCache().getClampToEdge(if (nearest) FilterMode.NEAREST else FilterMode.LINEAR)

internal object GlyphAtlas {
    private const val SIZE = PolyGlyphPage.SIZE

    private class Entry(val image: NativeImage, val ref: AbstractTextureRef, var uploaded: Int)

    private val entries = IdentityHashMap<PolyGlyphPage.Page, Entry>()
    private var uploadedRevision = -1L

    private fun entry(page: PolyGlyphPage.Page) = entries.getOrPut(page) {
        uploadedRevision = -1L
        val image = NativeImage(SIZE, SIZE, true)
        Entry(image, AbstractTextureRef(DynamicTexture({ "oneconfig_glyphs_${entries.size}" }, image)), 0)
    }

    fun page(page: PolyGlyphPage.Page): AbstractTextureRef = entry(page).ref

    fun uploadDirty() {
        if (PolyGlyphPage.revision == uploadedRevision) return
        uploadedRevision = PolyGlyphPage.revision
        for (page in PolyGlyphPage.pages) {
            val e = entry(page)
            if (e.uploaded == page.version) continue
            // DynamicTexture already uploaded the blank image, so even a new page only needs its written rows
            val rows = page.dirtyRows(e.uploaded)
            e.uploaded = page.version
            if (rows.isEmpty()) continue
            val h = rows.last - rows.first + 1
            val at = e.image.pointer + rows.first * SIZE * 4L
            MemoryUtil.memIntBuffer(at, h * SIZE).put(page.pixels, rows.first * SIZE, h * SIZE)
            //~ if < 26.2 ', 0, 0, 0,' -> ', NativeImage.Format.RGBA, 0, 0, 0,'
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(e.ref.texture.texture, MemoryUtil.memByteBuffer(at, h * SIZE * 4), 0, 0, 0, rows.first, SIZE, h)
        }
    }
}


internal object BitmapTextures {
    private class Entry(val ref: AbstractTextureRef, var lastUsed: Long)

    private val cache = IdentityHashMap<PolyBitmap, Entry>()
    private var frame = 0L
    private var scanAt = 0L

    fun get(bitmap: PolyBitmap): AbstractTextureRef {
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
        scanAt = min(scanAt, frame + RELEASE_FRAMES + 1)
        return ref
    }

    val decoder = PolyBitmap.Decoder { bytes ->
        runCatching {
            NativeImage.read(bytes).use { img ->
                val w = img.width
                val px = ByteArray(w * img.height * 4)
                for (y in 0 until img.height) for (x in 0 until w) {
                    val c = img.getPixel(x, y)
                    val o = (y * w + x) * 4
                    px[o] = (c shr 16).toByte(); px[o + 1] = (c shr 8).toByte(); px[o + 2] = c.toByte(); px[o + 3] = (c ushr 24).toByte()
                }
                PolyBitmap(w, img.height, px)
            }
        }.getOrNull()
    }

    fun touch(bitmaps: Set<PolyBitmap>) {
        for (b in bitmaps) cache[b]?.lastUsed = frame
    }

    fun endFrame() {
        frame++
        if (frame < scanAt) return
        var next = Long.MAX_VALUE
        val it = cache.values.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (frame - e.lastUsed > RELEASE_FRAMES) {
                e.ref.texture.close()
                it.remove()
            } else next = min(next, e.lastUsed + RELEASE_FRAMES + 1)
        }
        scanAt = next
    }

    private const val RELEASE_FRAMES = 600L
}
//?}
