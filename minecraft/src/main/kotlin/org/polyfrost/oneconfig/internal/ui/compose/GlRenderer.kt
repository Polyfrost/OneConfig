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
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.polyfrost.oneconfig.internal.ui.TextureRenderer
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.IdentityHashMap
import kotlin.math.roundToInt

class GlRenderer private constructor(private val inLive: Boolean) : PolyTessellator(), TextureRenderer {
    constructor() : this(false)

    private val batches = ArrayList<GlProgram.Batch>()

    private class Live(val at: Int, val state: State, val draw: (PolyRenderer) -> Unit)
    private val lives = ArrayList<Live>()

    override fun bitmapTexture(bitmap: PolyBitmap): Any = GlTextures.bitmap(bitmap)

    override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
        batches.add(GlProgram.Batch(texture, premultiplied, nearest, verts, colors))
    }

    override fun live(draw: (PolyRenderer) -> Unit) {
        if (inLive) return draw(this)
        flush()
        lives.add(Live(batches.size, state.copy(), draw))
    }

    override fun texture(target: RenderTarget, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, alpha: Float) {
        val a = (alpha * state.alpha * 255f).roundToInt().coerceIn(0, 255)
        if (a == 0) return
        begin(GlTextures.external(RenderTargetFbo.getColorTexId(target)), true)
        val x0 = x * state.sx + state.tx
        val y0 = y * state.sy + state.ty
        quadPx(x0, y0, x0 + w * state.sx, y0 + h * state.sy, u0, v0, u1, v1, (a shl 24) or (a shl 16) or (a shl 8) or a)
    }

    fun draw(fbo: Int, w: Int, h: Int, topDown: Boolean, clear: Boolean, kept: Boolean = false) {
        flush()
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
        GL11.glViewport(0, 0, w, h)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        if (clear) {
            GL11.glColorMask(true, true, true, true)
            GL11.glClearColor(0f, 0f, 0f, 0f)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
        }
        val segments = lives.map { l ->
            val r = GlRenderer(true)
            r.state = l.state.copy()
            l.draw(r)
            r.flush()
            GlProgram.Segment(l.at, r.batches)
        }
        if (batches.isNotEmpty() || segments.isNotEmpty()) GlProgram.draw(batches, w, h, topDown, segments, kept)
        GlTextures.endFrame()
    }
}

internal object GlProgram {
    class Batch(val texture: Any, val premultiplied: Boolean, val nearest: Boolean, val verts: FloatArray, val colors: IntArray)
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
    private var ibo = 0
    private var iboQuads = 0
    private var uScale = 0
    private var uOffset = 0
    private var uPremul = 0
    private var vertexData: ByteBuffer = BufferUtils.createByteBuffer(1 shl 16)

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

    fun draw(batches: List<Batch>, w: Int, h: Int, topDown: Boolean, live: List<Segment> = emptyList(), kept: Boolean = false) {
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
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, STRIDE, 0L)
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, STRIDE, 8L)
        GL20.glVertexAttribPointer(2, 4, GL11.GL_UNSIGNED_BYTE, true, STRIDE, 16L)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo)
        ensureIndices(vertices / 4)

        GL20.glUniform2f(uScale, 2f / w, if (topDown) 2f / h else -2f / h)
        GL20.glUniform2f(uOffset, -1f, if (topDown) -1f else 1f)
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

        fun drawBatch(b: Batch, first: Int) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, GlTextures.id(b.texture))
            GL20.glUniform1i(uPremul, if (b.premultiplied) 1 else 0)
            if (b.nearest) filter(GL11.GL_NEAREST)
            GL11.glDrawElements(GL11.GL_TRIANGLES, b.colors.size / 4 * 6, GL11.GL_UNSIGNED_INT, first * 6L * 4L)
            if (b.nearest) filter(GL11.GL_LINEAR)
        }

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

    private fun filter(mode: Int) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode)
    }

    private fun upload(at: Int, batches: List<Batch>): Int {
        var vertices = 0
        for (b in batches) vertices += b.colors.size
        if (vertexData.capacity() < vertices * STRIDE) vertexData = BufferUtils.createByteBuffer(Integer.highestOneBit(vertices * STRIDE) shl 1)
        val buf = vertexData
        buf.clear()
        buf.order(ByteOrder.nativeOrder())
        for (b in batches) for (i in b.colors.indices) {
            val o = i * 4
            buf.putFloat(b.verts[o]).putFloat(b.verts[o + 1]).putFloat(b.verts[o + 2]).putFloat(b.verts[o + 3])
            val c = b.colors[i]
            buf.put((c shr 16).toByte()).put((c shr 8).toByte()).put(c.toByte()).put((c ushr 24).toByte())
        }
        buf.flip()
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
    private var frame = 0L

    fun bitmap(bitmap: PolyBitmap): Tex = bitmaps.getOrPut(bitmap) { Tex(bitmap, 0, frame) }.also { it.lastUsed = frame }

    fun external(id: Int): Tex = external.getOrPut(id) { Tex(null, id, 0) }

    fun id(key: Any): Int = when (key) {
        is PolyGlyphPage.Page -> glyphPage(key)
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
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex[0])
            val px = BufferUtils.createIntBuffer(page.pixels.size).put(page.pixels)
            px.flip()
            unpackDefaults()
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, PolyGlyphPage.SIZE, PolyGlyphPage.SIZE, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px)
        }
        return tex[0]
    }

    private fun upload(bitmap: PolyBitmap): Int {
        val id = newTexture()
        val px = BufferUtils.createByteBuffer(bitmap.pixels.size).put(bitmap.pixels)
        px.flip()
        unpackDefaults()
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, bitmap.width, bitmap.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px)
        return id
    }

    private fun unpackDefaults() {
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4)
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0)
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0)
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0)
    }

    private fun newTexture(): Int {
        val id = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        return id
    }

    fun endFrame() {
        frame++
        val it = bitmaps.values.iterator()
        while (it.hasNext()) {
            val t = it.next()
            if (frame - t.lastUsed > 600) {
                if (t.id != 0) GL11.glDeleteTextures(t.id)
                it.remove()
            }
        }
        if (external.size > 64) external.clear()
    }
}
