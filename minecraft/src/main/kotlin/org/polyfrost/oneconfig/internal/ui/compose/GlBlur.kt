package org.polyfrost.oneconfig.internal.ui.compose

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer

internal object GlBlur {
    private const val HALF = 0
    private const val A = 1
    private const val B = 2

    private var program = 0
    private var core = false
    private var alphaTest = false
    private var vao = 0
    private var vbo = 0
    private var uStep = 0
    private var uSize = 0
    private var uAlpha = 0
    private val textures = IntArray(3)
    private val fbos = IntArray(3)
    private val widths = IntArray(3)
    private val heights = IntArray(3)
    private var blurredAt = 0L
    private var blurredFrom = 0

    fun draw(dest: Int, w: Int, h: Int, alpha: Float, paused: Boolean) {
        if (program == 0) init()
        val hw = (w + 1) / 2
        val hh = (h + 1) / 2
        val qw = (hw + 1) / 2
        val qh = (hh + 1) / 2
        var fresh = ensure(HALF, hw, hh)
        fresh = ensure(A, qw, qh) || fresh
        fresh = ensure(B, qw, qh) || fresh
        val now = System.nanoTime()
        val reuse = paused && !fresh && blurredFrom == dest && now - blurredAt < REUSE_NS

        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        GL11.glColorMask(true, true, true, true)
        if (!reuse) {
            blit(dest, w, h, fbos[HALF], hw, hh)
            blit(fbos[HALF], hw, hh, fbos[A], qw, qh)
        }

        GL20.glUseProgram(program)
        if (core) GL30.glBindVertexArray(vao)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)
        GL20.glEnableVertexAttribArray(0)
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0L)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_STENCIL_TEST)
        if (alphaTest) GL11.glDisable(GL11.GL_ALPHA_TEST)
        GL13.glActiveTexture(GL13.GL_TEXTURE0)

        if (!reuse) {
            GL11.glDisable(GL11.GL_BLEND)
            pass(fbos[B], textures[A], qw, qh, 1f / qw, 0f, 1f)
            pass(fbos[A], textures[B], qw, qh, 0f, 1f / qh, 1f)
            blurredAt = now
            blurredFrom = dest
        }

        GL11.glEnable(GL11.GL_BLEND)
        GL14.glBlendEquation(GL14.GL_FUNC_ADD)
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE)
        pass(dest, textures[A], w, h, 0f, 0f, alpha)

        GL20.glDisableVertexAttribArray(0)
        if (core) GL30.glBindVertexArray(0)
    }

    fun release() {
        for (i in textures.indices) {
            if (fbos[i] != 0) GL30.glDeleteFramebuffers(fbos[i])
            if (textures[i] != 0) GL11.glDeleteTextures(textures[i])
            fbos[i] = 0
            textures[i] = 0
            widths[i] = 0
            heights[i] = 0
        }
        blurredAt = 0L
    }

    private fun blit(from: Int, fw: Int, fh: Int, to: Int, tw: Int, th: Int) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, from)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to)
        GL30.glBlitFramebuffer(0, 0, fw, fh, 0, 0, tw, th, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR)
    }

    private fun pass(fbo: Int, texture: Int, w: Int, h: Int, stepX: Float, stepY: Float, alpha: Float) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
        GL11.glViewport(0, 0, w, h)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
        GL20.glUniform2f(uStep, stepX, stepY)
        GL20.glUniform2f(uSize, w.toFloat(), h.toFloat())
        GL20.glUniform1f(uAlpha, alpha)
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3)
    }

    private fun ensure(i: Int, w: Int, h: Int): Boolean {
        if (textures[i] != 0 && widths[i] == w && heights[i] == h) return false
        if (textures[i] == 0) textures[i] = GL11.glGenTextures()
        if (fbos[i] == 0) fbos[i] = GL30.glGenFramebuffers()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i])
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbos[i])
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textures[i], 0)
        widths[i] = w
        heights[i] = h
        return true
    }

    private fun init() {
        val glsl = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION) ?: "1.20"
        core = glsl.substringBefore(' ').split('.').let { it[0].toInt() * 100 + it.getOrElse(1) { "0" }.take(2).padEnd(2, '0').toInt() } >= 150
        alphaTest = GL.getCapabilities().let { it.GL_ARB_compatibility || !it.OpenGL31 }
        val p = GL20.glCreateProgram()
        GL20.glAttachShader(p, shader(GL20.GL_VERTEX_SHADER, if (core) VS_150 else VS_120))
        GL20.glAttachShader(p, shader(GL20.GL_FRAGMENT_SHADER, if (core) FS_150 else FS_120))
        GL20.glBindAttribLocation(p, 0, "aPos")
        GL20.glLinkProgram(p)
        check(GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) != 0) { "OneConfig blur program: " + GL20.glGetProgramInfoLog(p, 4096) }
        uStep = GL20.glGetUniformLocation(p, "uStep")
        uSize = GL20.glGetUniformLocation(p, "uSize")
        uAlpha = GL20.glGetUniformLocation(p, "uAlpha")
        GL20.glUseProgram(p)
        GL20.glUniform1i(GL20.glGetUniformLocation(p, "uTex"), 0)
        if (core) vao = GL30.glGenVertexArrays()
        vbo = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(6).put(floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)).flip(), GL15.GL_STATIC_DRAW)
        program = p
    }

    private fun shader(type: Int, src: String): Int {
        val s = GL20.glCreateShader(type)
        GL20.glShaderSource(s, src)
        GL20.glCompileShader(s)
        check(GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) != 0) { "OneConfig blur shader: " + GL20.glGetShaderInfoLog(s, 4096) }
        return s
    }

    private const val REUSE_NS = 50_000_000L

    private const val BLUR_BODY = """
    vec2 uv = gl_FragCoord.xy / uSize;
    vec4 c;
    if (uStep == vec2(0.0)) {
        c = TEX(uTex, uv);
    } else {
        c = TEX(uTex, uv) * 0.19967563;
        c += (TEX(uTex, uv - uStep * 1.4073334) + TEX(uTex, uv + uStep * 1.4073334)) * 0.29732251;
        c += (TEX(uTex, uv - uStep * 3.2942150) + TEX(uTex, uv + uStep * 3.2942150)) * 0.09184834;
        c += (TEX(uTex, uv - uStep * 5.2018132) + TEX(uTex, uv + uStep * 5.2018132)) * 0.01099133;
    }"""

    private const val VS_150 = """#version 150
in vec2 aPos;
void main() { gl_Position = vec4(aPos, 0.0, 1.0); }
"""
    private const val FS_150 = """#version 150
#define TEX texture
uniform sampler2D uTex; uniform vec2 uStep; uniform vec2 uSize; uniform float uAlpha;
out vec4 fragColor;
void main() {$BLUR_BODY
    fragColor = vec4(c.rgb, uAlpha);
}
"""
    private const val VS_120 = """#version 120
attribute vec2 aPos;
void main() { gl_Position = vec4(aPos, 0.0, 1.0); }
"""
    private const val FS_120 = """#version 120
#define TEX texture2D
uniform sampler2D uTex; uniform vec2 uStep; uniform vec2 uSize; uniform float uAlpha;
void main() {$BLUR_BODY
    gl_FragColor = vec4(c.rgb, uAlpha);
}
"""
}
