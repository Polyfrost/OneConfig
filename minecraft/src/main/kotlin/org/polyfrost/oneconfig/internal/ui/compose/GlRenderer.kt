package org.polyfrost.oneconfig.internal.ui.compose

import com.mojang.blaze3d.pipeline.RenderTarget
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.polyfrost.compose.render.CapturingRenderer
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.polyfrost.oneconfig.internal.ui.TextureRenderer
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.IdentityHashMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

//? if = 1.21.1 {
/*import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.renderer.GameRenderer
import org.joml.Matrix4f
*///?}

class GlRenderer private constructor(private val inLive: Boolean) : PolyTessellator(), TextureRenderer, CapturingRenderer {
    constructor() : this(false)

    private val batches = ArrayList<GlProgram.Batch>()

    private class Live(val at: Int, val state: State, val draw: (PolyRenderer) -> Unit, val key: (() -> Int)? = null) {
        private var last: Int? = null

        fun moved(at: Int) = Live(at, state, draw, key).also { it.last = last }

        fun changed() = key == null || key() != last

        fun commit() {
            last = key?.invoke()
        }
    }
    private val lives = ArrayList<Live>()

    private val screenW = Platform.screen().viewportWidth()
    private val screenH = Platform.screen().viewportHeight()
    private val guiW = Platform.screen().guiWidth()

    override fun bitmapTexture(bitmap: PolyBitmap): Any = GlTextures.bitmap(bitmap)

    private class Captured(val batches: List<GlProgram.Batch>, val lives: List<Live>, val boxes: FloatArray)

    override fun mark(): Any {
        flush()
        return intArrayOf(batches.size, lives.size, drawBoxMark)
    }

    override fun since(mark: Any): Any {
        flush()
        val m = mark as IntArray
        return Captured(ArrayList(batches.subList(m[0], batches.size)), lives.subList(m[1], lives.size).map { it.moved(it.at - m[0]) }, drawBoxesSince(m[2]))
    }

    override fun valid(captured: Any): Boolean = (captured as Captured).batches.all { GlTextures.alive(it.texture) }

    override fun append(captured: Any) {
        flush()
        val c = captured as Captured
        for (l in c.lives) lives.add(l.moved(l.at + batches.size))
        batches.addAll(c.batches)
        appendDrawBoxes(c.boxes)
    }

    override fun writeStateKey() {
        super.writeStateKey()
        keyAdd(screenW.toFloat()); keyAdd(screenH.toFloat()); keyAdd(guiW.toFloat())
    }

    fun matches(): Boolean =
        screenW == Platform.screen().viewportWidth() && screenH == Platform.screen().viewportHeight() && guiW == Platform.screen().guiWidth()

    fun reset() {
        batches.clear()
        lives.clear()
        resetTessellator()
    }

    override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
        batches.add(GlProgram.Batch(texture, premultiplied, nearest, verts, colors))
    }

    override val layered: Boolean get() = !GlHost.vanillaPipeline

    override fun layerMark(): Any = intArrayOf(batches.size, lives.size)

    override fun layer(mark: Any, alpha: Float) {
        val m = mark as IntArray
        val sub = batches.subList(m[0], batches.size)
        val subLives = lives.subList(m[1], lives.size)
        if (sub.isEmpty() && subLives.isEmpty()) return
        val shifted = subLives.map { it.moved(it.at - m[0]) }
        val live = if (shifted.isEmpty()) null else ({ shifted.forEach(Live::commit); segments(shifted) })
        val group = Group.of(ArrayList(sub), live, { shifted.any(Live::changed) }, alpha, screenW, screenH)
        sub.clear()
        subLives.clear()
        if (group != null) batches.add(group.batch)
    }

    override fun fade(mark: Any, alpha: Float): Boolean {
        val m = mark as IntArray
        for (i in m[0] until batches.size) batches[i] = batches[i].faded(alpha)
        for (i in m[1] until lives.size) {
            val l = lives[i]
            lives[i] = Live(l.at, l.state.copy().also { it.alpha *= alpha }, l.draw, l.key)
        }
        return true
    }

    override fun live(draw: (PolyRenderer) -> Unit) {
        if (inLive) return draw(this)
        flush()
        beginDraw()
        touchAll()
        lives.add(Live(batches.size, state.copy(), draw))
    }

    override fun mcTextLive(text: String, x: Float, y: Float, color: () -> Int, shadow: Boolean, scale: Float) {
        if (inLive || '§' in text) return super.mcTextLive(text, x, y, color, shadow, scale)
        flush()
        val start = batches.size
        mcText(text, x, y, -1, shadow, scale)
        flush()
        if (batches.size == start) return
        val tail = batches.subList(start, batches.size)
        val baked = ArrayList(tail)
        tail.clear()
        val base = baked.map { it.colors.copyOf() }
        lives.add(Live(batches.size, state.copy(), { r ->
            val alpha = (r as GlRenderer).state.alpha
            val c = mulAlpha(color(), alpha)
            if (c ushr 24 != 0) {
                val sc = mulAlpha((c and -0x1000000) or ((c and 0xFCFCFC) shr 2), alpha)
                for (k in baked.indices) {
                    val dst = baked[k].colors
                    val src = base[k]
                    for (i in dst.indices) dst[i] = if (src[i] and 0xFFFFFF == 0xFFFFFF) c else sc
                }
                r.batches.addAll(baked)
            }
        }, color))
    }

    override fun texture(target: RenderTarget, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, alpha: Float) {
        beginDraw()
        val a = (alpha * state.alpha * 255f).roundToInt().coerceIn(0, 255)
        if (a == 0) return
        begin(GlTextures.external(RenderTargetFbo.getColorTexId(target)), true)
        val x0 = x * state.sx + state.tx
        val y0 = y * state.sy + state.ty
        quadPx(x0, y0, x0 + w * state.sx, y0 + h * state.sy, u0, v0, u1, v1, (a shl 24) or (a shl 16) or (a shl 8) or a)
    }

    fun draw(fbo: Int, w: Int, h: Int, topDown: Boolean, clear: Boolean, kept: Boolean = false) {
        flush()
        drawBatches(fbo, w, h, topDown, clear, batches, segments(lives), kept, false)
    }

    //? if = 1.21.1 {
    /*fun drawVanilla(w: Int, h: Int, topDown: Boolean) {
        flush()
        val segments = segments(lives)
        if (batches.isNotEmpty() || segments.isNotEmpty()) VanillaProgram.draw(batches, w, h, topDown, segments)
        GlTextures.endFrame()
    }
    *///?}

    private fun flushPending() = flush()

    private fun replay(lives: List<Live>): List<GlProgram.Segment> = lives.map { l ->
        reset()
        state = l.state.copy()
        l.draw(this)
        flush()
        GlProgram.Segment(l.at, ArrayList(batches))
    }

    internal class Layer(private val coalesce: Boolean = false) {
        private var batches: List<GlProgram.Batch> = emptyList()
        private var lives: List<Live> = emptyList()
        private var raw: List<GlProgram.Batch> = emptyList()
        private var rawLives: List<Live> = emptyList()
        private var stable = 0

        fun take(renderer: GlRenderer) {
            renderer.flushPending()
            val next = renderer.batches
            val nextLives = renderer.lives
            if (coalesce && same(next, raw) && sameLives(nextLives, rawLives)) {
                idle()
                return
            }
            raw = ArrayList(next)
            rawLives = ArrayList(nextLives)
            batches = raw
            lives = rawLives
            stable = 0
        }

        private fun idle() {
            if (++stable == COALESCE_FRAMES) coalesce()
            if (batches !== raw && batches.any { !GlTextures.alive(it.texture) }) {
                batches = raw
                lives = rawLives
            }
        }

        private fun same(a: List<GlProgram.Batch>, b: List<GlProgram.Batch>) = a.size == b.size && a.indices.all { a[it] === b[it] }

        private fun sameLives(a: List<Live>, b: List<Live>) =
            a.size == b.size && a.indices.all { a[it].at == b[it].at && a[it].state === b[it].state && a[it].draw === b[it].draw }

        private fun coalesce() {
            val out = ArrayList<GlProgram.Batch>()
            val outLives = ArrayList<Live>()
            val run = ArrayList<GlProgram.Batch>()
            fun emit() {
                if (run.size == 1) out.add(run[0]) else if (run.isNotEmpty()) out.add(GlProgram.Batch.merge(run))
                run.clear()
            }
            var li = 0
            for (i in 0..raw.size) {
                if (li < rawLives.size && rawLives[li].at == i) emit()
                while (li < rawLives.size && rawLives[li].at == i) {
                    val l = rawLives[li++]
                    outLives.add(Live(out.size, l.state, l.draw))
                }
                if (i == raw.size) break
                val b = raw[i]
                if (run.isNotEmpty() && !run[0].sameKey(b)) emit()
                run.add(b)
            }
            emit()
            if (out.size == raw.size) return
            batches = out
            lives = outLives
        }

        companion object {
            fun draw(fbo: Int, w: Int, h: Int, topDown: Boolean, clear: Boolean, vararg layers: Layer) {
                val all = ArrayList<GlProgram.Batch>()
                val allLives = ArrayList<Live>()
                for (layer in layers) {
                    for (l in layer.lives) allLives.add(Live(l.at + all.size, l.state, l.draw))
                    all.addAll(layer.batches)
                }
                drawBatches(fbo, w, h, topDown, clear, all, segments(allLives), false, true)
            }
        }
    }

    internal class Group private constructor(
        private val batches: List<GlProgram.Batch>, private val live: (() -> List<GlProgram.Segment>)?, private val changed: () -> Boolean,
        alpha: Float, private val x: Int, private val y: Int, private val w: Int, private val h: Int,
    ) {
        private var target: IntArray? = null
        private var rendered = false
        private var lastUsed = 0L

        val tex: Int get() = target?.get(1) ?: 0

        val batch = GlProgram.Batch(
            this, true, true,
            floatArrayOf(x.toFloat(), y.toFloat(), 0f, 0f, x.toFloat(), (y + h).toFloat(), 0f, 1f, (x + w).toFloat(), (y + h).toFloat(), 1f, 1f, (x + w).toFloat(), y.toFloat(), 1f, 0f),
            IntArray(4) { (alpha * 255f).roundToInt().coerceIn(0, 255) * 0x01010101 },
        )

        private fun stale(): Boolean = (live != null && changed()) || batches.any { (it.texture as? Group)?.stale() == true }

        private fun render(composite: GlProgram.Batch, transient: ArrayList<Group>?, sink: ArrayList<Group>) {
            lastUsed = frame
            transient?.add(this)
            if (rendered && target != null && !stale()) return
            prepare(batches, transient, sink)
            val segs = live?.invoke() ?: emptyList()
            for (s in segs) prepare(s.batches, sink, sink)
            val t = target?.takeIf { it[2] >= w && it[3] >= h } ?: acquire(w, h).also { next ->
                target?.let(::recycle) ?: active.add(this)
                target = next
            }
            val u1 = w.toFloat() / t[2]
            val v1 = h.toFloat() / t[3]
            val v = composite.verts
            if (v[10] != u1 || v[11] != v1) {
                v[7] = v1; v[10] = u1; v[11] = v1; v[14] = u1
                GlProgram.invalidate(composite)
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, t[0])
            GL11.glViewport(0, 0, t[2], t[3])
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
            GL11.glColorMask(true, true, true, true)
            GL11.glClearColor(0f, 0f, 0f, 0f)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
            if (batches.isNotEmpty() || segs.isNotEmpty()) GlProgram.draw(batches, t[2], t[3], true, segs, false, x.toFloat(), y.toFloat())
            rendered = true
        }

        fun release() {
            active.remove(this)
            target?.let(::recycle)
            target = null
            rendered = false
        }

        companion object {
            private const val MAX_POOLED = 4
            private const val RELEASE_FRAMES = 8
            private val pool = ArrayList<IntArray>()
            private val active = ArrayList<Group>()
            private var frame = 0L

            fun of(batches: List<GlProgram.Batch>, live: (() -> List<GlProgram.Segment>)?, changed: () -> Boolean, alpha: Float, screenW: Int, screenH: Int): Group? {
                var l = 0; var t = 0; var r = screenW; var b = screenH
                if (live == null) {
                    var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
                    for (batch in batches) for (i in batch.colors.indices) {
                        val vx = batch.verts[i * 4]; val vy = batch.verts[i * 4 + 1]
                        x0 = min(x0, vx); y0 = min(y0, vy); x1 = max(x1, vx); y1 = max(y1, vy)
                    }
                    if (x0 > x1) return null
                    l = max(floor(x0).toInt(), 0); t = max(floor(y0).toInt(), 0)
                    r = min(ceil(x1).toInt(), screenW); b = min(ceil(y1).toInt(), screenH)
                }
                if (r <= l || b <= t) return null
                return Group(batches, live, changed, alpha, l, t, r - l, b - t)
            }

            fun prepare(batches: List<GlProgram.Batch>, transient: ArrayList<Group>?, sink: ArrayList<Group>) {
                for (b in batches) (b.texture as? Group)?.render(b, transient, sink)
            }

            fun endFrame() {
                frame++
                for (g in active.filter { frame - it.lastUsed > RELEASE_FRAMES }) g.release()
            }

            private fun recycle(t: IntArray) {
                if (pool.size < MAX_POOLED) pool.add(t) else delete(t)
            }

            private fun acquire(w: Int, h: Int): IntArray {
                val fit = pool.filter { it[2] >= w && it[3] >= h }.minByOrNull { it[2] * it[3] }
                if (fit != null) return fit.also { pool.remove(it) }
                val t = pool.removeLastOrNull() ?: intArrayOf(GL30.glGenFramebuffers(), GL11.glGenTextures(), 0, 0)
                t[2] = PolyTessellator.layerTargetSize(0, w); t[3] = PolyTessellator.layerTargetSize(0, h)
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, t[1])
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, t[2], t[3], 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, t[0])
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, t[1], 0)
                return t
            }

            private fun delete(t: IntArray) {
                GL30.glDeleteFramebuffers(t[0])
                GL11.glDeleteTextures(t[1])
            }
        }
    }

    private companion object {
        private const val COALESCE_FRAMES = 60
        private var liveRenderer: GlRenderer? = null

        fun segments(lives: List<Live>): List<GlProgram.Segment> {
            if (lives.isEmpty()) return emptyList()
            return (liveRenderer ?: GlRenderer(true).also { liveRenderer = it }).replay(lives)
        }

        fun drawBatches(fbo: Int, w: Int, h: Int, topDown: Boolean, clear: Boolean, batches: List<GlProgram.Batch>, segments: List<GlProgram.Segment>, kept: Boolean, arena: Boolean) {
            val transient = ArrayList<Group>()
            Group.prepare(batches, null, transient)
            for (s in segments) Group.prepare(s.batches, transient, transient)
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
            GL11.glViewport(0, 0, w, h)
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
            if (clear) {
                GL11.glColorMask(true, true, true, true)
                GL11.glClearColor(0f, 0f, 0f, 0f)
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
            }
            if (batches.isNotEmpty() || segments.isNotEmpty()) {
                if (arena) GlProgram.drawArena(batches, w, h, topDown, segments) else GlProgram.draw(batches, w, h, topDown, segments, kept)
            }
            for (g in transient) g.release()
            Group.endFrame()
            GlTextures.endFrame()
        }
    }
}

//? if = 1.21.1 {
/*internal object VanillaProgram {
    fun draw(batches: List<GlProgram.Batch>, w: Int, h: Int, topDown: Boolean, live: List<GlProgram.Segment>) {
        RenderSystem.backupProjectionMatrix()
        val bottom = if (topDown) 0f else h.toFloat()
        RenderSystem.setProjectionMatrix(Matrix4f().setOrtho(0f, w.toFloat(), bottom, h - bottom, -1f, 1f), VertexSorting.ORTHOGRAPHIC_Z)
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushMatrix()
        modelView.identity()
        RenderSystem.applyModelViewMatrix()
        RenderSystem.disableCull()
        RenderSystem.disableDepthTest()
        RenderSystem.disableScissor()
        RenderSystem.colorMask(true, true, true, true)
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader)
        try {
            var seg = 0
            for (i in 0..batches.size) {
                while (seg < live.size && live[seg].at == i) live[seg++].batches.forEach(::drawBatch)
                if (i == batches.size) break
                drawBatch(batches[i])
            }
        } finally {
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableBlend()
            RenderSystem.enableDepthTest()
            RenderSystem.enableCull()
            modelView.popMatrix()
            RenderSystem.applyModelViewMatrix()
            RenderSystem.restoreProjectionMatrix()
        }
    }

    private fun drawBatch(b: GlProgram.Batch) {
        if (b.colors.isEmpty()) return
        val src = if (b.premultiplied) GlStateManager.SourceFactor.ONE else GlStateManager.SourceFactor.SRC_ALPHA
        RenderSystem.blendFuncSeparate(src, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShaderTexture(0, GlTextures.id(b.texture))
        val buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
        for (i in b.colors.indices) {
            val o = i * 4
            buf.addVertex(b.verts[o], b.verts[o + 1], 0f).setUv(b.verts[o + 2], b.verts[o + 3]).setColor(b.colors[i])
        }
        BufferUploader.drawWithShader(buf.buildOrThrow())
    }
}
*///?}

internal object GlProgram {
    class Batch(val texture: Any, val premultiplied: Boolean, val nearest: Boolean, val verts: FloatArray, val colors: IntArray) {
        @JvmField var arenaAt = 0
        @JvmField var arenaGen = -1
        @JvmField var seen = -1L

        fun sameKey(o: Batch) = texture === o.texture && premultiplied == o.premultiplied && nearest == o.nearest

        fun faded(alpha: Float) = Batch(texture, premultiplied, nearest, verts, IntArray(colors.size) { i ->
            val c = colors[i]
            if (premultiplied) {
                var out = 0
                var shift = 0
                while (shift < 32) {
                    out = out or ((((c ushr shift) and 0xFF) * alpha).roundToInt().coerceIn(0, 255) shl shift)
                    shift += 8
                }
                out
            } else {
                (((c ushr 24) * alpha).roundToInt().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)
            }
        })

        companion object {
            fun merge(list: List<Batch>): Batch {
                var n = 0
                for (b in list) n += b.colors.size
                val verts = FloatArray(n * 4)
                val colors = IntArray(n)
                var o = 0
                for (b in list) {
                    System.arraycopy(b.verts, 0, verts, o * 4, b.colors.size * 4)
                    System.arraycopy(b.colors, 0, colors, o, b.colors.size)
                    o += b.colors.size
                }
                val first = list[0]
                return Batch(first.texture, first.premultiplied, first.nearest, verts, colors)
            }
        }
    }
    class Segment(val at: Int, val batches: List<Batch>)

    private val LOG = LoggerFactory.getLogger(GlProgram::class.java)
    private const val STRIDE = 20

    private var program = 0
    private var core = false
    private var vao = 0
    private class Vbo {
        var id = 0
        var uploaded: List<Batch>? = null
        var size = 0
    }
    private val keptVbo = Vbo()
    private val scratchVbo = Vbo()
    private val arenaVbo = Vbo()
    private var arenaUsed = 0
    private var arenaGen = 0
    private var arenaFrame = 0L
    private const val MIN_ARENA = 1 shl 14
    private var ibo = 0
    private var iboQuads = 0
    private var uScale = 0
    private var uOffset = 0
    private var uPremul = 0
    private var boundTex = -1
    private var boundPremul = -1
    private var vertexData: ByteBuffer = BufferUtils.createByteBuffer(1 shl 16)
    private var vertexInts: IntBuffer = vertexData.asIntBuffer()
    private var scratch = IntArray(vertexData.capacity() / 4)
    private val LITTLE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

    private fun init() {
        val glsl = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION) ?: "1.20"
        core = (glsl.substringBefore(' ').split('.').let { it[0].toInt() * 100 + it.getOrElse(1) { "0" }.take(2).padEnd(2, '0').toInt() }) >= 150
        val (vs, fs) = if (core) VS_150 to FS_150 else VS_120 to FS_120
        val p = GL20.glCreateProgram()
        GL20.glAttachShader(p, shader(GL20.GL_VERTEX_SHADER, vs))
        GL20.glAttachShader(p, shader(GL20.GL_FRAGMENT_SHADER, fs))
        GL20.glBindAttribLocation(p, 0, "aPos")
        GL20.glBindAttribLocation(p, 1, "aUv")
        GL20.glBindAttribLocation(p, 2, "aColor")
        GL20.glLinkProgram(p)
        check(GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) != 0) { "OneConfig GL program: " + GL20.glGetProgramInfoLog(p, 4096) }
        uScale = GL20.glGetUniformLocation(p, "uScale")
        uOffset = GL20.glGetUniformLocation(p, "uOffset")
        uPremul = GL20.glGetUniformLocation(p, "uPremul")
        GL20.glUseProgram(p)
        GL20.glUniform1i(GL20.glGetUniformLocation(p, "uTex"), 0)
        keptVbo.id = GL15.glGenBuffers()
        scratchVbo.id = GL15.glGenBuffers()
        arenaVbo.id = GL15.glGenBuffers()
        ibo = GL15.glGenBuffers()
        if (core) vao = GL30.glGenVertexArrays()
        program = p
        LOG.info("OneConfig GL renderer ready (GLSL {}, {})", glsl, if (core) "150" else "120")
    }

    private fun shader(type: Int, src: String): Int {
        val s = GL20.glCreateShader(type)
        GL20.glShaderSource(s, src)
        GL20.glCompileShader(s)
        check(GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) != 0) { "OneConfig GL shader: " + GL20.glGetShaderInfoLog(s, 4096) }
        return s
    }

    fun invalidate(b: Batch) {
        b.arenaGen = -1
        b.seen = -1L
        keptVbo.uploaded = null
    }

    fun draw(batches: List<Batch>, w: Int, h: Int, topDown: Boolean, live: List<Segment> = emptyList(), kept: Boolean = false, ox: Float = 0f, oy: Float = 0f) {
        if (program == 0) init()
        var staticVerts = 0
        for (b in batches) staticVerts += b.colors.size
        var liveVerts = 0
        for (s in live) for (b in s.batches) liveVerts += b.colors.size
        val vertices = staticVerts + liveVerts

        GL20.glUseProgram(program)
        if (core) GL30.glBindVertexArray(vao)
        val vbo = if (kept) keptVbo else scratchVbo
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo.id)
        if (batches !== vbo.uploaded || vertices > vbo.size) {
            vbo.size = vertices + liveVerts
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vbo.size.toLong() * STRIDE, GL15.GL_STREAM_DRAW)
            upload(0, batches)
            vbo.uploaded = batches
        }
        for (s in live) if (s.batches.isNotEmpty()) upload(staticVerts, s.batches).also { staticVerts += it }
        GL20.glEnableVertexAttribArray(0)
        GL20.glEnableVertexAttribArray(1)
        GL20.glEnableVertexAttribArray(2)
        attribPointers()
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo)
        ensureIndices(vertices / 4)
        setupState(w, h, topDown, ox, oy)

        var first = 0
        var liveFirst = (vertices - liveVerts) / 4
        var seg = 0
        for (i in 0..batches.size) {
            while (seg < live.size && live[seg].at == i) {
                for (b in live[seg++].batches) { drawBatch(b, liveFirst); liveFirst += b.colors.size / 4 }
            }
            if (i == batches.size) break
            drawBatch(batches[i], first)
            first += batches[i].colors.size / 4
        }
        GL20.glDisableVertexAttribArray(0)
        GL20.glDisableVertexAttribArray(1)
        GL20.glDisableVertexAttribArray(2)
        if (core) GL30.glBindVertexArray(0)
    }

    fun drawArena(batches: List<Batch>, w: Int, h: Int, topDown: Boolean, live: List<Segment>) {
        if (program == 0) init()
        val frame = ++arenaFrame
        val gen = arenaGen
        var inArena = 0
        var adding = 0
        for (b in batches) {
            if (b.arenaGen == gen) inArena += b.colors.size
            else if (b.seen == frame - 1) adding += b.colors.size
        }
        val rebuild = arenaUsed + adding > arenaVbo.size || arenaUsed - inArena > maxOf(inArena, MIN_ARENA)
        if (rebuild) arenaGen++
        val newGen = arenaGen
        val adds = ArrayList<Batch>()
        for (b in batches) {
            if (b.arenaGen == newGen) continue
            if (b.arenaGen == gen || b.seen == frame - 1) {
                b.arenaGen = newGen
                adds.add(b)
            }
        }
        var scratchVerts = 0
        for (b in batches) {
            if (b.arenaGen != newGen) scratchVerts += b.colors.size
            b.seen = frame
        }

        GL20.glUseProgram(program)
        if (core) GL30.glBindVertexArray(vao)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arenaVbo.id)
        if (rebuild) {
            var need = 0
            for (b in adds) need += b.colors.size
            arenaVbo.size = maxOf(Integer.highestOneBit(maxOf(need, 1)) shl 2, MIN_ARENA)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, arenaVbo.size.toLong() * STRIDE, GL15.GL_DYNAMIC_DRAW)
            arenaUsed = 0
        }
        if (adds.isNotEmpty()) {
            var at = arenaUsed
            for (b in adds) { b.arenaAt = at; at += b.colors.size }
            upload(arenaUsed, adds)
            arenaUsed = at
        }

        var liveVerts = 0
        for (seg in live) for (b in seg.batches) liveVerts += b.colors.size
        val scratchSize = scratchVerts + liveVerts
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, scratchVbo.id)
        if (scratchSize > 0) {
            scratchVbo.size = maxOf(scratchSize, scratchVbo.size)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, scratchVbo.size.toLong() * STRIDE, GL15.GL_STREAM_DRAW)
            scratchVbo.uploaded = null
            var at = if (scratchVerts > 0) upload(0, batches.filter { it.arenaGen != newGen }) else 0
            for (seg in live) if (seg.batches.isNotEmpty()) at += upload(at, seg.batches)
        }

        GL20.glEnableVertexAttribArray(0)
        GL20.glEnableVertexAttribArray(1)
        GL20.glEnableVertexAttribArray(2)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo)
        ensureIndices(maxOf(arenaVbo.size, scratchVbo.size) / 4)
        setupState(w, h, topDown)

        var bound = -1
        fun use(id: Int) {
            if (bound == id) return
            bound = id
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id)
            attribPointers()
        }
        var scratchFirst = 0
        var liveFirst = scratchVerts / 4
        var seg = 0
        for (i in 0..batches.size) {
            while (seg < live.size && live[seg].at == i) {
                use(scratchVbo.id)
                for (b in live[seg++].batches) { drawBatch(b, liveFirst); liveFirst += b.colors.size / 4 }
            }
            if (i == batches.size) break
            val b = batches[i]
            if (b.arenaGen == newGen) {
                use(arenaVbo.id)
                drawBatch(b, b.arenaAt / 4)
            } else {
                use(scratchVbo.id)
                drawBatch(b, scratchFirst)
                scratchFirst += b.colors.size / 4
            }
        }
        GL20.glDisableVertexAttribArray(0)
        GL20.glDisableVertexAttribArray(1)
        GL20.glDisableVertexAttribArray(2)
        if (core) GL30.glBindVertexArray(0)
    }

    private fun attribPointers() {
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, STRIDE, 0L)
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, STRIDE, 8L)
        GL20.glVertexAttribPointer(2, 4, GL11.GL_UNSIGNED_BYTE, true, STRIDE, 16L)
    }

    private fun setupState(w: Int, h: Int, topDown: Boolean, ox: Float = 0f, oy: Float = 0f) {
        GL20.glUniform2f(uScale, 2f / w, if (topDown) 2f / h else -2f / h)
        GL20.glUniform2f(uOffset, -1f - ox * 2f / w, if (topDown) -1f - oy * 2f / h else 1f + oy * 2f / h)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_STENCIL_TEST)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        if (!core) GL11.glDisable(GL11.GL_ALPHA_TEST)
        GL11.glColorMask(true, true, true, true)
        GL11.glEnable(GL11.GL_BLEND)
        GL14.glBlendEquation(GL14.GL_FUNC_ADD)
        GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        boundTex = -1
        boundPremul = -1
    }

    private fun drawBatch(b: Batch, first: Int) {
        val tex = GlTextures.id(b.texture)
        if (tex != boundTex) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex)
            boundTex = tex
        }
        val premul = if (b.premultiplied) 1 else 0
        if (premul != boundPremul) {
            GL20.glUniform1i(uPremul, premul)
            boundPremul = premul
        }
        if (b.nearest) filter(GL11.GL_NEAREST)
        GL11.glDrawElements(GL11.GL_TRIANGLES, b.colors.size / 4 * 6, GL11.GL_UNSIGNED_INT, first * 6L * 4L)
        if (b.nearest) filter(GL11.GL_LINEAR)
    }

    private fun filter(mode: Int) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode)
    }

    private fun upload(at: Int, batches: List<Batch>): Int {
        var vertices = 0
        for (b in batches) vertices += b.colors.size
        val ints = vertices * (STRIDE / 4)
        if (vertexData.capacity() < vertices * STRIDE) {
            vertexData = BufferUtils.createByteBuffer(Integer.highestOneBit(vertices * STRIDE) shl 1)
            vertexInts = vertexData.asIntBuffer()
            scratch = IntArray(vertexData.capacity() / 4)
        }
        val out = scratch
        var n = 0
        for (b in batches) {
            val v = b.verts
            val cs = b.colors
            for (i in cs.indices) {
                val o = i * 4
                out[n] = v[o].toRawBits()
                out[n + 1] = v[o + 1].toRawBits()
                out[n + 2] = v[o + 2].toRawBits()
                out[n + 3] = v[o + 3].toRawBits()
                val c = cs[i]
                out[n + 4] = if (LITTLE) (c and 0xFF00FF00.toInt()) or ((c shr 16) and 0xFF) or ((c and 0xFF) shl 16) else (c shl 8) or (c ushr 24)
                n += 5
            }
        }
        vertexInts.clear()
        vertexInts.put(out, 0, ints)
        val buf = vertexData
        buf.clear()
        buf.limit(vertices * STRIDE)
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, at.toLong() * STRIDE, buf)
        return vertices
    }

    private fun ensureIndices(quads: Int) {
        if (quads <= iboQuads) return
        val n = Integer.highestOneBit(quads.coerceAtLeast(256)) shl 1
        val idx = BufferUtils.createIntBuffer(n * 6)
        for (q in 0 until n) {
            val v = q * 4
            idx.put(v).put(v + 1).put(v + 2).put(v).put(v + 2).put(v + 3)
        }
        idx.flip()
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, idx, GL15.GL_STATIC_DRAW)
        iboQuads = n
    }

    private const val VS_150 = """#version 150
in vec2 aPos; in vec2 aUv; in vec4 aColor;
uniform vec2 uScale; uniform vec2 uOffset;
out vec2 vUv; out vec4 vColor;
void main() { vUv = aUv; vColor = aColor; gl_Position = vec4(aPos * uScale + uOffset, 0.0, 1.0); }
"""
    private const val FS_150 = """#version 150
in vec2 vUv; in vec4 vColor;
uniform sampler2D uTex; uniform int uPremul;
out vec4 fragColor;
void main() { vec4 c = texture(uTex, vUv) * vColor; fragColor = uPremul == 1 ? c : vec4(c.rgb * c.a, c.a); }
"""
    private const val VS_120 = """#version 120
attribute vec2 aPos; attribute vec2 aUv; attribute vec4 aColor;
uniform vec2 uScale; uniform vec2 uOffset;
varying vec2 vUv; varying vec4 vColor;
void main() { vUv = aUv; vColor = aColor; gl_Position = vec4(aPos * uScale + uOffset, 0.0, 1.0); }
"""
    private const val FS_120 = """#version 120
varying vec2 vUv; varying vec4 vColor;
uniform sampler2D uTex; uniform int uPremul;
void main() { vec4 c = texture2D(uTex, vUv) * vColor; gl_FragColor = uPremul == 1 ? c : vec4(c.rgb * c.a, c.a); }
"""
}

internal object GlTextures {
    class Tex(val bitmap: PolyBitmap?, var id: Int, var lastUsed: Long)

    private val bitmaps = IdentityHashMap<PolyBitmap, Tex>()
    private val external = HashMap<Int, Tex>()
    private val pages = IdentityHashMap<PolyGlyphPage.Page, IntArray>()
    private var pagePixels: IntBuffer? = null
    private var frame = 0L
    private var scanAt = 0L

    fun bitmap(bitmap: PolyBitmap): Tex = bitmaps.getOrPut(bitmap) {
        scanAt = min(scanAt, frame + RELEASE_FRAMES + 1)
        Tex(bitmap, 0, frame)
    }.also { it.lastUsed = frame }

    fun external(id: Int): Tex = external.getOrPut(id) { Tex(null, id, 0) }

    fun alive(key: Any): Boolean = key !is Tex || key.bitmap == null || bitmaps[key.bitmap] === key

    fun id(key: Any): Int = when (key) {
        is PolyGlyphPage.Page -> glyphPage(key)
        is GlRenderer.Group -> key.tex
        is Tex -> {
            key.lastUsed = frame
            if (key.id == 0 && key.bitmap != null) key.id = upload(key.bitmap)
            key.id
        }
        else -> 0
    }

    private fun glyphPage(page: PolyGlyphPage.Page): Int {
        val tex = pages.getOrPut(page) { intArrayOf(newTexture(), 0) }
        if (tex[1] != page.version) {
            tex[1] = page.version
            bind(tex[0])
            val px = (pagePixels ?: BufferUtils.createIntBuffer(page.pixels.size).also { pagePixels = it })
            px.clear()
            px.put(page.pixels).flip()
            unpackDefaults()
            texImage(PolyGlyphPage.SIZE, PolyGlyphPage.SIZE, px)
        }
        return tex[0]
    }

    private fun upload(bitmap: PolyBitmap): Int {
        val id = newTexture()
        val px = BufferUtils.createByteBuffer(bitmap.pixels.size).put(bitmap.pixels)
        px.flip()
        unpackDefaults()
        texImage(bitmap.width, bitmap.height, px.asIntBuffer())
        return id
    }

    private fun unpackDefaults() {
        pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4)
        pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0)
        pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0)
        pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0)
    }

    private fun newTexture(): Int {
        val id = genTexture()
        bind(id)
        texParameter(GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        texParameter(GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        texParameter(GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        texParameter(GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        return id
    }

    private fun genTexture(): Int {
        //? if = 1.21.1
        //if (GlHost.vanillaPipeline) return GlStateManager._genTexture()
        return GL11.glGenTextures()
    }

    private fun bind(id: Int) {
        //? if = 1.21.1
        //if (GlHost.vanillaPipeline) return GlStateManager._bindTexture(id)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
    }

    private fun texParameter(name: Int, value: Int) {
        //? if = 1.21.1
        //if (GlHost.vanillaPipeline) return GlStateManager._texParameter(GL11.GL_TEXTURE_2D, name, value)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, name, value)
    }

    private fun pixelStore(name: Int, value: Int) {
        //? if = 1.21.1
        //if (GlHost.vanillaPipeline) return GlStateManager._pixelStore(name, value)
        GL11.glPixelStorei(name, value)
    }

    private fun texImage(w: Int, h: Int, px: IntBuffer) {
        //? if = 1.21.1
        //if (GlHost.vanillaPipeline) return GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px)
    }

    private fun deleteTexture(id: Int) {
        //? if = 1.21.1
        //if (GlHost.vanillaPipeline) return GlStateManager._deleteTexture(id)
        GL11.glDeleteTextures(id)
    }

    fun endFrame() {
        frame++
        if (external.size > 64) external.clear()
        if (frame < scanAt) return
        var next = Long.MAX_VALUE
        val it = bitmaps.values.iterator()
        while (it.hasNext()) {
            val t = it.next()
            if (frame - t.lastUsed > RELEASE_FRAMES) {
                if (t.id != 0) deleteTexture(t.id)
                it.remove()
            } else next = min(next, t.lastUsed + RELEASE_FRAMES + 1)
        }
        scanAt = next
    }

    private const val RELEASE_FRAMES = 600L
}
