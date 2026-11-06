package org.polyfrost.oneconfig.internal.ui.compose

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal object GlGlass {
    class Key

    private val LOG = LoggerFactory.getLogger(GlGlass::class.java)

    private var program = 0
    private var failed = false
    private var uScale = 0
    private var uOffset = 0
    private var uTexel = 0
    private var texture = 0
    private var fbo = 0
    private var texW = 0
    private var texH = 0
    private var target = -1
    private var targetW = 0
    private var targetH = 0

    fun begin(target: Int, w: Int, h: Int) {
        this.target = target
        targetW = w
        targetH = h
    }

    fun end() {
        target = -1
    }

    fun use(b: GlProgram.Batch, core: Boolean, scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float): Boolean {
        if (target < 0 || failed || b.colors.isEmpty() || targetW <= 0 || targetH <= 0) return false
        if (!copy(b, scaleX, scaleY, offsetX, offsetY)) return false
        if (program == 0 && !init(core)) {
            release()
            return false
        }
        GL20.glUseProgram(program)
        GL20.glUniform2f(uScale, scaleX, scaleY)
        GL20.glUniform2f(uOffset, offsetX, offsetY)
        GL20.glUniform2f(uTexel, 1f / texW, 1f / texH)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
        return true
    }

    fun release() {
        if (fbo != 0) GL30.glDeleteFramebuffers(fbo)
        if (texture != 0) GL11.glDeleteTextures(texture)
        fbo = 0
        texture = 0
        texW = 0
        texH = 0
    }

    private fun copy(glass: GlProgram.Batch, scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float): Boolean {
        val v = glass.verts
        val c = glass.colors
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (i in c.indices) {
            val o = i * 4
            val fx = (v[o] * scaleX + offsetX + 1f) * 0.5f * targetW
            val fy = (v[o + 1] * scaleY + offsetY + 1f) * 0.5f * targetH
            val reach = ((c[i] ushr 8) and 0xFF) * (c[i] and 0xFF) / 255f * 1.12f + 2.5f
            left = min(left, fx - reach)
            top = min(top, fy - reach)
            right = max(right, fx + reach)
            bottom = max(bottom, fy + reach)
        }
        val x0 = max(floor(left).toInt() - 1, 0)
        val y0 = max(floor(top).toInt() - 1, 0)
        val x1 = min(ceil(right).toInt() + 1, targetW)
        val y1 = min(ceil(bottom).toInt() + 1, targetH)
        if (x1 <= x0 || y1 <= y0) return false
        try {
            ensure(targetW, targetH)
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target)
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbo)
            GL30.glBlitFramebuffer(x0, y0, x1, y1, x0, y0, x1, y1, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST)
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target)
            return true
        } catch (e: Throwable) {
            LOG.warn("OneConfig backdrop glass is unavailable", e)
            failed = true
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
            release()
            return false
        }
    }

    private fun ensure(w: Int, h: Int) {
        if (texture != 0 && texW == w && texH == h) return
        val max = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE)
        check(max <= 0 || (w <= max && h <= max)) { "viewport ${w}x$h is past the max texture size ($max)" }
        if (texture == 0) texture = GL11.glGenTextures()
        if (fbo == 0) fbo = GL30.glGenFramebuffers()
        texW = 0
        texH = 0
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0)
        val status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
        check(status == GL30.GL_FRAMEBUFFER_COMPLETE) { "snapshot framebuffer is incomplete (0x${Integer.toHexString(status)})" }
        texW = w
        texH = h
    }

    private fun init(core: Boolean): Boolean {
        val p = GL20.glCreateProgram()
        val vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER)
        val fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)
        try {
            compile(vs, if (core) GlProgram.VS_150 else GlProgram.VS_120)
            compile(fs, if (core) FS_150 else FS_120)
            GL20.glAttachShader(p, vs)
            GL20.glAttachShader(p, fs)
            GL20.glBindAttribLocation(p, 0, "aPos")
            GL20.glBindAttribLocation(p, 1, "aUv")
            GL20.glBindAttribLocation(p, 2, "aColor")
            GL20.glLinkProgram(p)
            check(GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) != 0) { GL20.glGetProgramInfoLog(p, 4096) }
        } catch (e: Throwable) {
            LOG.warn("OneConfig backdrop glass is unavailable", e)
            GL20.glDeleteProgram(p)
            GL20.glDeleteShader(vs)
            GL20.glDeleteShader(fs)
            failed = true
            return false
        }
        GL20.glDeleteShader(vs)
        GL20.glDeleteShader(fs)
        uScale = GL20.glGetUniformLocation(p, "uScale")
        uOffset = GL20.glGetUniformLocation(p, "uOffset")
        uTexel = GL20.glGetUniformLocation(p, "uTexel")
        GL20.glUseProgram(p)
        GL20.glUniform1i(GL20.glGetUniformLocation(p, "uTex"), 0)
        program = p
        return true
    }

    private fun compile(shader: Int, src: String) {
        GL20.glShaderSource(shader, src)
        GL20.glCompileShader(shader)
        check(GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != 0) { GL20.glGetShaderInfoLog(shader, 4096) }
    }

    private const val GLASS_BODY = """
uniform sampler2D uTex; uniform vec2 uTexel;

float roundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

vec3 frosted(vec2 frag) {
    vec3 c = TEX(uTex, frag * uTexel).rgb * 0.2;
    c += TEX(uTex, (frag + vec2(2.5, 0.0)) * uTexel).rgb * 0.1;
    c += TEX(uTex, (frag - vec2(2.5, 0.0)) * uTexel).rgb * 0.1;
    c += TEX(uTex, (frag + vec2(0.0, 2.5)) * uTexel).rgb * 0.1;
    c += TEX(uTex, (frag - vec2(0.0, 2.5)) * uTexel).rgb * 0.1;
    c += TEX(uTex, (frag + vec2(1.8, 1.8)) * uTexel).rgb * 0.1;
    c += TEX(uTex, (frag - vec2(1.8, 1.8)) * uTexel).rgb * 0.1;
    c += TEX(uTex, (frag + vec2(1.8, -1.8)) * uTexel).rgb * 0.1;
    c += TEX(uTex, (frag - vec2(1.8, -1.8)) * uTexel).rgb * 0.1;
    return c;
}

vec4 glass() {
    vec2 st = vec2(dFdx(vUv.x), dFdy(vUv.y));
    vec2 size = 1.0 / max(abs(st), vec2(1e-6));
    vec2 halfSize = size * 0.5;
    vec2 p = (vUv - 0.5) * size;
    float radius = min(vColor.r * 255.0, min(halfSize.x, halfSize.y));
    float band = max(vColor.g * 255.0, 1.0);
    float strength = vColor.b;

    float d = roundedBox(p, halfSize, radius);
    float coverage = clamp(0.5 - d, 0.0, 1.0) * vColor.a;
    if (coverage <= 0.0) {
        return vec4(0.0);
    }

    vec2 e = vec2(0.5, 0.0);
    vec2 n = vec2(
        roundedBox(p + e.xy, halfSize, radius) - roundedBox(p - e.xy, halfSize, radius),
        roundedBox(p + e.yx, halfSize, radius) - roundedBox(p - e.yx, halfSize, radius)
    );
    n = n / max(length(n), 1e-4);

    float t = clamp(1.0 + d / band, 0.0, 1.0);
    float bend = 1.0 - sqrt(max(1.0 - t * t, 0.0));
    vec2 offset = n * band * strength * bend * sign(st);
    float spread = 0.12 * bend;

    vec2 frag = gl_FragCoord.xy;
    vec3 color = frosted(frag + offset);
    if (bend > 0.02) {
        color.r = frosted(frag + offset * (1.0 + spread)).r;
        color.b = frosted(frag + offset * (1.0 - spread)).b;
    }
    return vec4(color * coverage, coverage);
}
"""

    private const val FS_150 = """#version 150
#define TEX texture
in vec2 vUv; in vec4 vColor;
out vec4 fragColor;
$GLASS_BODY
void main() { fragColor = glass(); }
"""

    private const val FS_120 = """#version 120
#define TEX texture2D
varying vec2 vUv; varying vec4 vColor;
$GLASS_BODY
void main() { gl_FragColor = glass(); }
"""
}
