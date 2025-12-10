@file:Suppress("UnstableApiUsage", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.polyfrost.oneconfig.api.ui.v1.internal

import dev.deftu.omnicore.api.client.render.GlCapabilities
import dev.deftu.omnicore.api.client.render.OmniTextRenderer
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStack
import dev.deftu.omnicore.api.color.OmniColor
import dev.deftu.omnicore.api.math.OmniMatrix3f
import dev.deftu.omnicore.internal.client.render.shader.ShaderInternals
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL14.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.UIManager
import org.polyfrost.oneconfig.api.ui.v1.api.NanoSvgApi
import org.polyfrost.oneconfig.api.ui.v1.api.RendererExt
import org.polyfrost.oneconfig.api.ui.v1.api.StbApi
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.Vec4
import org.polyfrost.polyui.utils.toDirectByteBuffer
import org.polyfrost.polyui.utils.toDirectByteBufferNT
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.*

private val LOGGER = LogManager.getLogger("PolyUI/GLRenderer")

private const val MAX_UI_DEPTH = 16
private const val FONT_MAX_BITMAP_W = 1536
private const val FONT_MAX_BITMAP_H = 512
private const val ATLAS_SIZE = 2048
private const val ATLAS_SVG_UPSCALE_FACTOR = 2f
private const val STRIDE = 4 + 4 + 1 + 1 + 4 + 1 // bounds, radii, color0, color1, UV, thick
private const val MAX_BATCH = 1024

private val EMPTY_ROW = floatArrayOf(0f, 0f, 0f, 0f)
private val NO_UV = floatArrayOf(-1f, -1f, 1f, 1f)
private val IDENTITY = floatArrayOf(
    1f, 0f, 0f,
    0f, 1f, 0f,
    0f, 0f, 1f
)

private fun FloatArray.isIdentity(): Boolean {
    return this[0] == 1f && this[1] == 0f && this[2] == 0f &&
            this[3] == 0f && this[4] == 1f && this[5] == 0f &&
            this[6] == 0f && this[7] == 0f && this[8] == 1f
}

@kotlin.internal.InlineOnly
private inline fun FloatArray.set(other: FloatArray) {
    System.arraycopy(other, 0, this, 0, 9)
}

@kotlin.internal.InlineOnly
private inline fun FloatArray.setThenClear(other: FloatArray) {
    System.arraycopy(other, 0, this, 0, 9)
    System.arraycopy(IDENTITY, 0, other, 0, 9)
}

class GLRendererImpl(private val nsvg: NanoSvgApi, private val stb: StbApi) : Renderer, RendererExt {

    private val buffer = BufferUtils.createFloatBuffer(MAX_BATCH * STRIDE)
    private val scissorStack = IntArray(MAX_UI_DEPTH * 4)
    private val transformStack = Array(MAX_UI_DEPTH) { FloatArray(9) }
    private val transformBuffer = BufferUtils.createFloatBuffer(9)
    private val fonts = HashMap<Int, FontAtlas>()
    private val init get() = program != 0

    // lateinit
    private var mipmapMode = 0

    // GL objects
    private var instancedVbo = 0
    private var atlas = 0
    private var program = 0
    private var vao = 0 // GL3+ only
    private var quadVbo = 0


    private var uWindow = 0
    private var uTransform = 0
    private var aLocal = 0
    private var iRect = 0
    private var iRadii = 0
    private var iColor0 = 0
    private var iColor1 = 0
    private var iUVRect = 0
    private var iThickness = 0


    // Current batch state
    private val viewport = IntArray(4)
    private var count = 0
    private var transformDepth = 0
    private var scissorDepth = 0
    private var transform = IDENTITY.copyOf()
    private var pixelRatio = 1f
    private var alphaCap = 255
    private var popFlushNeeded = false

    // atlas data
    private var slotX = 0
    private var slotY = 0
    private var atlasRowHeight = 0

    private val FRAG = """
        #version $$$ // replaced by compileShader
        #if __VERSION__ >= 130
            #define VARYING in
            #define TEXTURE texture
            out vec4 fragColor;
        #else
            #define VARYING varying
            #define TEXTURE texture2D
            #define fragColor gl_FragColor
        #endif

        uniform sampler2D uTex;

        VARYING vec2 vUV;
        VARYING vec2 vUV2;      // used for gradients
        VARYING vec2 vPos;      // pixel coords in rect space
        VARYING vec4 vRect;     // rect x, y, w, h
        VARYING vec4 vRadii;    // per-corner radii
        VARYING vec4 vColor0;    // RGBA
        VARYING vec4 vColor1;    // RGBA (for gradients)
        VARYING float vThickness; // -1 for text, -2 for linear gradient, -3 for radial, -4 for box,  >0 for hollow rect

        // Signed distance function for rounded box
        float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
            // px = 1.0 if p.x > 0, else 0.0
            float px = step(0.0, p.x);
            float py = step(0.0, p.y);

            // Select radius per quadrant
            float rLeft  = mix(r.x, r.w, py); // top-left / bottom-left
            float rRight = mix(r.y, r.z, py); // top-right / bottom-right
            float radius = mix(rLeft, rRight, px); // left vs right

            vec2 q = abs(p) - (b - radius);
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
        }

        float hollowRoundedBoxSDF(vec2 p, vec2 b, vec4 r, float thickness) {
            float outer = roundedBoxSDF(p, b + 0.2, r + 0.2);
            float inner = roundedBoxSDF(p, b - thickness, max(r - thickness, 0.0)); 
            return max(outer, -inner);
        }

        float roundBoxSDF(vec2 p, vec2 halfSize, float radius) {
            vec2 q = abs(p) - (halfSize - radius);
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
        }

        void main() {
            vec2 halfSize = 0.5 * vRect.zw;
            vec2 center = vRect.xy + halfSize;
            vec2 p = vPos - center;

            float d = (vThickness > 0.0) ? hollowRoundedBoxSDF(p, halfSize, vRadii, vThickness) : roundedBoxSDF(p, halfSize, vRadii);

            vec4 col = vColor0;
            if (vUV.x >= 0.0 && vThickness >= -1.0) {     // textured if UV.x >= 0
                vec4 texColor = TEXTURE(uTex, vUV);
                // text check: use red channel as alpha
                col = (vThickness == -1.0) ? vec4(col.rgb, col.a * texColor.r) : col * texColor;
            }
            else if (vThickness == -2.0) { // linear gradient, vUV as start and vUV2 as end
                vec2 dir = vUV2 - vUV;
                float len = length(dir);
                float t = clamp(dot((p + halfSize) - vUV, dir / len) / len, 0.0, 1.0);
                col = mix(vColor0, vColor1, t);
            }
            else if (vThickness == -3.0) { // radial gradient, vUV as center and vUV2.x as radius
                float dist = length(p + halfSize - vUV);
                float t = clamp((dist - vUV2.x) / (vUV2.y - vUV2.x), 0.0, 1.0);
                col = mix(vColor0, vColor1, t);
            }
            else if (vThickness == -4.0) { // box gradient, vUV.x as radius and vUV.y as feather
                float dist = roundBoxSDF(p, halfSize, vUV.x);
                float t = clamp((dist + vUV.y * 0.5) / vUV.y, 0.0, 1.0);
                col = mix(vColor0, vColor1, t);
            }
            else if (vThickness == -5.0) { // drop shadow, vUV.x as spread and vUV.y as blur
                float dShadow = roundBoxSDF(p, halfSize + vUV.x, vRadii.x);
                col = vec4(vColor0.rgb, vColor0.a * (1.0 - smoothstep(-vUV.y, vUV.y, dShadow)));
            }

            // Proper antialiasing based on distance field
            float f = fwidth(d);
            float alpha = col.a * (1.0 - smoothstep(-f, f, d));

            fragColor = vec4(col.rgb * alpha, alpha);
        }
    """.trimIndent()

    private val VERT = """
        #version $$$ // replaced by compileShader
        #extension GL_EXT_gpu_shader4 : enable
        #if __VERSION__ >= 130
            #define ATTRIBUTE in
            #define VARYING out
            #define U_INT uint
        #else
            #define ATTRIBUTE attribute
            #define VARYING varying
            #define U_INT unsigned int
        #endif

        ATTRIBUTE vec2 aLocal;
        ATTRIBUTE vec4 iRect;
        ATTRIBUTE vec4 iRadii;
        ATTRIBUTE U_INT iColor0;
        ATTRIBUTE U_INT iColor1;
        ATTRIBUTE vec4 iUVRect;
        ATTRIBUTE float iThickness;

        uniform mat3 uTransform = mat3(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        );
        uniform vec2 uWindow;

        VARYING vec2 vPos;
        VARYING vec4 vRect;
        VARYING vec4 vRadii;
        VARYING vec4 vColor0;
        VARYING vec4 vColor1;
        VARYING vec2 vUV;
        VARYING vec2 vUV2;
        VARYING float vThickness;
        
        vec4 unpackColor(U_INT c) {
            float a = float((c >> 24) & 0xFFu) / 255.0;
            float r = float((c >> 16) & 0xFFu) / 255.0;
            float g = float((c >>  8) & 0xFFu) / 255.0;
            float b = float((c      ) & 0xFFu) / 255.0;

            return vec4(r, g, b, a);
        }

        void main() {
            // Position inside rect
            vec2 pos = iRect.xy + aLocal * iRect.zw;
            vec2 uv  = (iThickness > -2.0) ? iUVRect.xy + aLocal * iUVRect.zw : iUVRect.xy; // for gradients, just pass through the first two param to frag

            vec3 transformed = uTransform * vec3(pos, 1.0);

            vec2 ndc = (transformed.xy / uWindow) * 2.0 - 1.0;
            ndc.y = -ndc.y;

            gl_Position = vec4(ndc, 0.0, 1.0);

            vPos    = pos;
            vRect   = iRect;
            vRadii  = iRadii;
            vColor0 = unpackColor(iColor0);
            vColor1 = unpackColor(iColor1);
            vUV     = uv;
            // pass through for gradients.
            vUV2    = iUVRect.zw;
            vThickness = iThickness;
        }
    """.trimIndent()

    private fun compileShader(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        if (shader == 0) throw RuntimeException("Failed to create shader")

        glShaderSource(shader, source.replaceFirst("$$$", if (GlCapabilities.isGl3Available) "150" else "120"))
        glCompileShader(shader)

        val status = glGetShaderi(shader, GL_COMPILE_STATUS)
        if (status == GL_FALSE) {
            val log = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            throw RuntimeException("Shader compile failed!\n$log")
        }
        return shader
    }

    private fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
        val program = glCreateProgram()
        if (program == 0) throw RuntimeException("Failed to create program")

        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)

        val status = glGetProgrami(program, GL_LINK_STATUS)
        if (status == GL_FALSE) {
            throw RuntimeException("Program link failed:\n" + glGetProgramInfoLog(program))
        }

        glValidateProgram(program)
        val valid = glGetProgrami(program, GL_VALIDATE_STATUS)
        if (valid == GL_FALSE) {
            throw RuntimeException("Program validation failed:\n" + glGetProgramInfoLog(program))
        }

        glDetachShader(program, vertexShader)
        glDetachShader(program, fragmentShader)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)

        return program
    }


    @Suppress("SameParameterValue")
    private fun glUniformMatrix3fv(location: Int, transpose: Boolean, array: FloatArray) {
        val buf = transformBuffer
        buf.clear()
        buf.put(array).flip()
        ShaderInternals.uniformMatrix3(location, transpose, buf)
    }

    @kotlin.internal.InlineOnly
    private inline fun Int.capAlpha(): Int {
        val a = (this ushr 24) and 0xFF
        val capped = if (a > alphaCap) alphaCap else a
        return (capped shl 24) or (this and 0x00FFFFFF)
    }


    override fun init() {
        if (init) return
        // check if instancing extension is available
        require(GlCapabilities.isGl21Available) { "At least OpenGL 2.1 is required" }
        if (!GlCapabilities.isGl33Available) { // asm: skip check, both are core past 3.3
            if (GlCapabilities.isGl3Available) {
                // gl3 uses getStringi
                var foundDrawInstanced = GlCapabilities.isGl31Available
                var foundInstancedArrays = false
                for (i in 0..<glGetInteger(org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS)) {
                    val ext = org.lwjgl.opengl.GL30.glGetStringi(GL_EXTENSIONS, i) ?: continue
                    when (ext) {
                        "GL_ARB_draw_instanced" -> foundDrawInstanced = true
                        "GL_ARB_instanced_arrays" -> foundInstancedArrays = true
                    }
                    if (foundDrawInstanced && foundInstancedArrays) break
                }
                require(foundDrawInstanced) { "GL_ARB_draw_instanced is not supported and is required" }
                require(foundInstancedArrays) { "GL_ARB_instanced_arrays is not supported and is required" }
            } else {
                // gl2 check
                val extensions = glGetString(GL_EXTENSIONS) ?: ""
                require("GL_EXT_gpu_shader4" in extensions) { "GL_EXT_gpu_shader4 is not supported and is required" }
                require("GL_ARB_instanced_arrays" in extensions) { "GL_ARB_instanced_arrays is not supported and is required" }
                require("GL_ARB_draw_instanced" in extensions) { "GL_ARB_draw_instanced is not supported and is required" }
//                if ("GL_EXT_framebuffer_object" in extensions) {
//                    LOGGER.info("Using mipmaps as extension GL_EXT_framebuffer_object is available")
//                    mipmapMode = 2
//                }
            }
        }

        if (GlCapabilities.isGl3Available) {
//            LOGGER.info("Using mipmaps and VAOs as OpenGL 3+ is available.")
//            mipmapMode = 1
            // ...ok I guess this is needed
            vao = org.lwjgl.opengl.GL30.glGenVertexArrays()
            org.lwjgl.opengl.GL30.glBindVertexArray(vao)
        }

        program = linkProgram(compileShader(GL_VERTEX_SHADER, VERT), compileShader(GL_FRAGMENT_SHADER, FRAG))

        val prevBuf = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val quadData = BufferUtils.createFloatBuffer(8).put(
            floatArrayOf(
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
            )
        ).flip() as FloatBuffer
        quadVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
        glBufferData(GL_ARRAY_BUFFER, quadData, GL_STATIC_DRAW)
        instancedVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)
        glBufferData(GL_ARRAY_BUFFER, MAX_BATCH * STRIDE * 4L, GL_STREAM_DRAW)

        uWindow = glGetUniformLocation(program, "uWindow")
        uTransform = glGetUniformLocation(program, "uTransform")
        aLocal = glGetAttribLocation(program, "aLocal")
        iRect = glGetAttribLocation(program, "iRect")
        iRadii = glGetAttribLocation(program, "iRadii")
        iColor0 = glGetAttribLocation(program, "iColor0")
        iColor1 = glGetAttribLocation(program, "iColor1")
        iUVRect = glGetAttribLocation(program, "iUVRect")
        iThickness = glGetAttribLocation(program, "iThickness")

        if (GlCapabilities.isGl3Available) {
            var offset = 0L
            offset = enableAttrib(iRect, 4, offset)
            offset = enableAttrib(iRadii, 4, offset)
            offset = enableAttrib(iColor0, 1, offset, GL_UNSIGNED_INT)
            offset = enableAttrib(iColor1, 1, offset, GL_UNSIGNED_INT)
            offset = enableAttrib(iUVRect, 4, offset)
            enableAttrib(iThickness, 1, offset)

            glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
            glEnableVertexAttribArray(aLocal)
            glVertexAttribPointer(aLocal, 2, GL_FLOAT, false, 0, 0L)
            org.lwjgl.opengl.GL30C.glBindVertexArray(0)
        }

        val prevTex = glGetInteger(GL_TEXTURE_BINDING_2D)
        atlas = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, atlas)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            ATLAS_SIZE,
            ATLAS_SIZE,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            null as ByteBuffer?
        )
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, if (mipmapMode == 0) GL_LINEAR else GL_LINEAR_MIPMAP_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glBindTexture(GL_TEXTURE_2D, prevTex)
        glBindBuffer(GL_ARRAY_BUFFER, prevBuf)
    }

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float) {
        scissorDepth = 0
        alphaCap = 255
        count = 0
        buffer.clear()
        transform.set(IDENTITY)
        popFlushNeeded = false
        transformDepth = 0
        val prevProg = glGetInteger(GL_CURRENT_PROGRAM)
        glUseProgram(program)
        glUniform2f(uWindow, width, height)
        glUniformMatrix3fv(uTransform, false, transform)
        glUseProgram(prevProg)
        glDisable(GL_SCISSOR_TEST)
        Platform.gl().glViewport(viewport)
        this.pixelRatio = pixelRatio
    }

    override fun endFrame() {
        flush()
        glDisable(GL_SCISSOR_TEST)
    }

    private fun flush() {
        if (count == 0) return
        buffer.flip()
        val prevActive = glGetInteger(GL_ACTIVE_TEXTURE)
        val prevTex = glGetInteger(GL_TEXTURE_BINDING_2D)
        val prevProg = glGetInteger(GL_CURRENT_PROGRAM)
        val prevBuf = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val prevBlend = glGetBoolean(GL_BLEND)
        val prevBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB)
        val prevBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB)
        val prevBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA)
        val prevBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA)
        val prevDepth = glGetBoolean(GL_DEPTH_TEST)
        val prevCull = glGetBoolean(GL_CULL_FACE)
        glEnable(GL_BLEND)
        glDisable(GL_CULL_FACE)
        glDisable(GL_DEPTH_TEST)
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)
        // 'orphan' the buffer - give a hint to the driver that we don't need the old data so we don't stall waiting for it
        glBufferData(GL_ARRAY_BUFFER, MAX_BATCH * STRIDE * 4L, GL_STREAM_DRAW)
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer)

        val prevVao: Int
        if (GlCapabilities.isGl3Available) {
            prevVao = glGetInteger(org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING)
            org.lwjgl.opengl.GL30.glBindVertexArray(vao)
        } else prevVao = 0
        glUseProgram(program)

        if (popFlushNeeded) {
            glUniformMatrix3fv(uTransform, false, transform)
            popFlushNeeded = false
        }

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, atlas)

        // asm: on VAO the state is stored, so we only need to set it up once
        if (!GlCapabilities.isGl3Available) {
            // Quad attrib
            glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
            glEnableVertexAttribArray(aLocal)
            glVertexAttribPointer(aLocal, 2, GL_FLOAT, false, 0, 0L)

            // Instance attribs
            glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)

            var offset = 0L
            offset = enableAttrib(iRect, 4, offset)
            offset = enableAttrib(iRadii, 4, offset)
            offset = enableAttrib(iColor0, 1, offset, GL_UNSIGNED_INT)
            offset = enableAttrib(iColor1, 1, offset, GL_UNSIGNED_INT)
            offset = enableAttrib(iUVRect, 4, offset)
            enableAttrib(iThickness, 1, offset)
        }

        // Draw all instances
        if (GlCapabilities.isGl31Available) org.lwjgl.opengl.GL31.glDrawArraysInstanced(GL_TRIANGLE_FAN, 0, 4, count)
        else org.lwjgl.opengl.ARBDrawInstanced.glDrawArraysInstancedARB(GL_TRIANGLE_FAN, 0, 4, count)

        count = 0
        buffer.clear()
        if (!prevBlend) glDisable(GL_BLEND)
        glBlendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha)
        if (prevDepth) glEnable(GL_DEPTH_TEST)
        if (prevCull) glEnable(GL_CULL_FACE)
        glUseProgram(prevProg)
        glActiveTexture(prevActive)
        glBindTexture(GL_TEXTURE_2D, prevTex)
        glBindBuffer(GL_ARRAY_BUFFER, prevBuf)
        if (GlCapabilities.isGl3Available) org.lwjgl.opengl.GL30.glBindVertexArray(prevVao)
    }

    private fun enableAttrib(loc: Int, size: Int, offset: Long, type: Int = GL_FLOAT): Long {
        glEnableVertexAttribArray(loc)
        glVertexAttribPointer(loc, size, type, false, STRIDE * 4, offset)
        // I don't know why core disables the extension functions... but ok!
        if (GlCapabilities.isGl33Available) org.lwjgl.opengl.GL33.glVertexAttribDivisor(loc, 1)
        else org.lwjgl.opengl.ARBInstancedArrays.glVertexAttribDivisorARB(loc, 1)
        return offset + size * 4L
    }

    override fun rect(
        x: Float, y: Float, width: Float, height: Float,
        color: Color,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    ) {
        val buffer = buffer
        if (count >= MAX_BATCH) flush()
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(color.argb.capAlpha()))
        if (color is PolyColor.Gradient) {
            buffer.put(java.lang.Float.intBitsToFloat(color.color2.argb.capAlpha()))
            when (val type = color.type) {
                is PolyColor.Gradient.Type.LeftToRight -> {
                    buffer.put(0f).put(height / 2f).put(width).put(height / 2f)
                    buffer.put(-2f)
                }

                is PolyColor.Gradient.Type.TopToBottom -> {
                    buffer.put(width / 2f).put(0f).put(width / 2f).put(height)
                    buffer.put(-2f)
                }

                is PolyColor.Gradient.Type.BottomLeftToTopRight -> {
                    buffer.put(0f).put(height).put(width).put(0f)
                    buffer.put(-2f)
                }

                is PolyColor.Gradient.Type.TopLeftToBottomRight -> {
                    buffer.put(0f).put(0f).put(width).put(height)
                    buffer.put(-2f)
                }

                is PolyColor.Gradient.Type.Radial -> {
                    buffer.put(if (type.centerX == -1f) width / 2f else type.centerX)
                        .put(if (type.centerY == -1f) height / 2f else type.centerY).put(type.innerRadius)
                        .put(type.outerRadius)
                    buffer.put(-3f)
                }

                is PolyColor.Gradient.Type.Box -> {
                    buffer.put(type.radius).put(type.feather).put(0f).put(0f)
                    buffer.put(-4f)
                }
            }
        } else {
            buffer.put(0f) // color1 unused
            buffer.put(NO_UV) // -1f UVs to indicate no texture
            buffer.put(0f)
        }

        count += 1
    }

    override fun hollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    ) {
        if (count >= MAX_BATCH) flush()
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(color.argb.capAlpha()))
        buffer.put(0f) // color1 unused
        buffer.put(NO_UV) // -1f UVs to indicate no texture
        buffer.put(lineWidth)
        count += 1
    }

    override fun image(
        image: PolyImage, x: Float, y: Float, width: Float, height: Float,
        colorMask: Int, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float
    ) {
        if (count >= MAX_BATCH) flush()

        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(colorMask.capAlpha()))
        buffer.put(0f) // color1 unused
        buffer.put(image.uv.x).put(image.uv.y).put(image.uv.w).put(image.uv.h)
        buffer.put(0f) // thickness = 0 for filled rect
        count += 1
    }

    override fun text(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float) {
        if (font === UIManager.INSTANCE.mcFont) {
            val ctx = UIManager.INSTANCE.renderingContext
            // todo hi deftu https://github.com/Deftu/OmniCore/issues/57
            ctx.pose.push(OmniPoseStack.Entry(ctx.pose.current.positionMatrix, OmniMatrix3f.from(transform)))
            // asm: can be optimized by https://github.com/Deftu/OmniCore/issues/58
            OmniTextRenderer.render(ctx.pose, text, x, y, OmniColor.argb(color.argb), false)
            ctx.pose.pop()
            return
        }
        val fAtlas = getFontAtlas(font, fontSize)
        val s = transformScale()
        val fAtlasForRendering = if (s == 1f) fAtlas else getFontAtlas(font, fontSize * s)
        if (count >= MAX_BATCH) flush()

        var penX = x
        val scaleFactor = fontSize / fAtlas.renderedSize
        val penY = y + (fAtlas.ascent + fAtlas.descent) * scaleFactor
        val col = java.lang.Float.intBitsToFloat(color.argb.capAlpha())
        val buffer = buffer

        for (c in text) {
            if (count >= MAX_BATCH) {
                flush()
            }
            val glyph = fAtlas.get(c)
            buffer.put(penX + glyph.xOff * scaleFactor).put(penY + glyph.yOff * scaleFactor)
                .put(glyph.width * scaleFactor).put(glyph.height * scaleFactor)
            buffer.put(EMPTY_ROW) // zero radii
            buffer.put(col)
            buffer.put(0f) // color1 unused
            buffer.put(if (s == 1f) glyph else fAtlasForRendering.get(c), 0, 4)
            buffer.put(-1f) // thickness = -1 for text
            penX += glyph.xAdvance * scaleFactor
            count += 1
        }
    }

    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        return if (font === UIManager.INSTANCE.mcFont) {
            Vec2(OmniTextRenderer.width(text).toFloat(), OmniTextRenderer.lineHeight.toFloat())
        } else getFontAtlas(font, fontSize).measure(text, fontSize)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        if (y1 == y2) rect(x1, y1, x2 - x1, width, color, 0f, 0f, 0f, 0f)
        else rect(x1, y1, width, y2 - y1, color, 0f, 0f, 0f, 0f)
    }

    override fun dropShadow(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blur: Float,
        spread: Float,
        radius: Float
    ) {
        if (count >= MAX_BATCH) flush()
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(EMPTY_ROW) // zero radii
        buffer.put(java.lang.Float.intBitsToFloat(alphaCap shl 24)) // black, alpha to alphaCap
        buffer.put(0f) // color1 unused
        buffer.put(spread).put(blur).put(0f).put(0f)
        buffer.put(-5f) // thickness = -5 for drop shadow
        count += 1
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
        flush()
        val nx = (x * pixelRatio).roundToInt()
        val ny = (viewport[3] - (y + height) * pixelRatio).roundToInt()
        val nw = (width * pixelRatio).roundToInt()
        val nh = (height * pixelRatio).roundToInt()
        scissorStack[scissorDepth++] = nx
        scissorStack[scissorDepth++] = ny
        scissorStack[scissorDepth++] = nw
        scissorStack[scissorDepth++] = nh
        glEnable(GL_SCISSOR_TEST)
        glScissor(nx + viewport[0], ny + viewport[1], nw, nh)
    }

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) {
        if (scissorDepth < 4) {
            pushScissor(x, y, width, height)
            return
        }
        flush()
        val px = scissorStack[scissorDepth - 4]
        val py = scissorStack[scissorDepth - 3]
        val pw = scissorStack[scissorDepth - 2]
        val ph = scissorStack[scissorDepth - 1]
        val nx = (x * pixelRatio).roundToInt()
        val ny = (viewport[3] - (y + height) * pixelRatio).roundToInt()

        val ix = maxOf(nx, px)
        val iy = maxOf(ny, py)
        val iw = maxOf(0, minOf(nx + (width * pixelRatio).roundToInt(), px + pw) - ix)
        val ih = maxOf(0, minOf(ny + (height * pixelRatio).roundToInt(), py + ph) - iy)

        scissorStack[scissorDepth++] = ix
        scissorStack[scissorDepth++] = iy
        scissorStack[scissorDepth++] = iw
        scissorStack[scissorDepth++] = ih

        glEnable(GL_SCISSOR_TEST)
        glScissor(ix + viewport[0], iy + viewport[1], iw, ih)
    }

    override fun popScissor() {
        flush()
        if (scissorDepth <= 4) {
            scissorDepth = 0
            glDisable(GL_SCISSOR_TEST)
            return
        }
        scissorDepth -= 4
        val nx = scissorStack[scissorDepth - 4]
        val ny = scissorStack[scissorDepth - 3]
        val nw = scissorStack[scissorDepth - 2]
        val nh = scissorStack[scissorDepth - 1]
        glEnable(GL_SCISSOR_TEST)
        glScissor(nx + viewport[0], ny + viewport[1], nw, nh)
    }

    override fun globalAlpha(alpha: Float) {
        alphaCap = (alpha * 255f).toInt()
    }

    override fun resetGlobalAlpha() = globalAlpha(1f)

    override fun transformsWithPoint() = false

    override fun push() {
        if (transform.isIdentity()) return
        transformStack[transformDepth++].set(transform)
    }

    override fun pop() {
        if (transform.isIdentity()) return
        flush()
        if (transformDepth == 0) {
            transform.set(IDENTITY)
        } else {
            transform.setThenClear(transformStack[--transformDepth])
        }
        popFlushNeeded = true
    }

    private fun transformScale(): Float {
        val a = transform[0]
        val c = transform[1]
        val b = transform[3]
        val d = transform[4]
        // Fast-path: identity (no rotation, no scale, no shear)
        if (a == 1f && d == 1f && c == 0f && b == 0f) {
            return 1f
        }
        val sx = sqrt(a * a + b * b)
        val sy = sqrt(c * c + d * d)
        return (sx + sy) * 0.5f
    }

    override fun translate(x: Float, y: Float) {
        flush()
        transform[6] += transform[0] * x + transform[3] * y
        transform[7] += transform[1] * x + transform[4] * y
        popFlushNeeded = true
    }

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        flush()
        transform[0] *= sx; transform[1] *= sx
        transform[3] *= sy; transform[4] *= sy
        popFlushNeeded = true
    }

    override fun rotate(angleRadians: Double, px: Float, py: Float) {
        flush()
        val c = cos(angleRadians).toFloat()
        val s = sin(angleRadians).toFloat()
        val a00 = transform[0]
        val a01 = transform[1]
        val a10 = transform[3]
        val a11 = transform[4]
        transform[0] = a00 * c + a10 * s
        transform[1] = a01 * c + a11 * s
        transform[3] = a00 * -s + a10 * c
        transform[4] = a01 * -s + a11 * c
        popFlushNeeded = true
    }

    override fun skewX(angleRadians: Double, px: Float, py: Float) {
        flush()
        val t = tan(angleRadians).toFloat()
        val a00 = transform[0]
        val a01 = transform[1]
        val a10 = transform[3]
        val a11 = transform[4]
        transform[0] = a00 + a10 * t
        transform[1] = a01 + a11 * t
        popFlushNeeded = true

    }

    override fun skewY(angleRadians: Double, px: Float, py: Float) {
        flush()
        val t = tan(angleRadians).toFloat()
        val a00 = transform[0]
        val a01 = transform[1]
        val a10 = transform[3]
        val a11 = transform[4]
        transform[3] = a10 + a00 * t
        transform[4] = a11 + a01 * t
        popFlushNeeded = true
    }

    override fun initImage(image: PolyImage, size: Vec2) {
        if (image.initialized) return
        val w = IntArray(1)
        val h = IntArray(1)
        val d = initImage(image, w, h)

        if (slotX + w[0] >= ATLAS_SIZE) {
            slotX = 0
            slotY += atlasRowHeight
            atlasRowHeight = 0
            if (slotY + h[0] >= ATLAS_SIZE) {
                throw IllegalStateException("Texture atlas full!")
            }
        }

        // Store UV rect for this image
        image.uv = Vec4.of(
            slotX / ATLAS_SIZE.toFloat(),
            slotY / ATLAS_SIZE.toFloat(),
            w[0] / ATLAS_SIZE.toFloat(),
            h[0] / ATLAS_SIZE.toFloat()
        )

        val prevActive = glGetInteger(GL_ACTIVE_TEXTURE)
        val prevTex = glGetInteger(GL_TEXTURE_BINDING_2D)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, atlas)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0)
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0)
        glTexSubImage2D(GL_TEXTURE_2D, 0, slotX, slotY, w[0], h[0], GL_RGBA, GL_UNSIGNED_BYTE, d)
        when (mipmapMode) {
            1 -> org.lwjgl.opengl.GL30.glGenerateMipmap(GL_TEXTURE_2D)
            2 -> org.lwjgl.opengl.EXTFramebufferObject.glGenerateMipmapEXT(GL_TEXTURE_2D)
        }
        glBindTexture(GL_TEXTURE_2D, prevTex)
        glActiveTexture(prevActive)

        if (image.type == PolyImage.Type.Raster) stb.image_free(d)

        slotX += w[0] + 1
        if (h[0] + 1 > atlasRowHeight) atlasRowHeight = h[0] + 1
        image.reportInit()
    }

    private fun initImage(image: PolyImage, w: IntArray, h: IntArray): ByteBuffer {
        if (image.type == PolyImage.Type.Vector) {
            val svg = nsvg.parse(image.load().toDirectByteBufferNT())
                ?: throw IllegalStateException("Could not parse SVG image ${image.resourcePath}")
            if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(svg.width, svg.height))
            w[0] = (svg.width * ATLAS_SVG_UPSCALE_FACTOR).toInt()
            h[0] = (svg.height * ATLAS_SVG_UPSCALE_FACTOR).toInt()
            val dst = BufferUtils.createByteBuffer(w[0] * h[0] * 4)
            nsvg.rasterize(svg.address, 0f, 0f, ATLAS_SVG_UPSCALE_FACTOR, dst, w[0], h[0], w[0] * 4)
            nsvg.delete(svg.address)
            return dst
        } else {
            val data = image.load().toDirectByteBuffer()
            val d = stb.image_load_from_memory(data, w, h, IntArray(1), 4)
                ?: throw IllegalStateException("Failed to load image ${image.resourcePath}: ${stb.image_failure_reason()}")
            if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(w[0].toFloat(), h[0].toFloat()))
            return d
        }
    }

    private fun getFontAtlas(font: Font, fontSize: Float): FontAtlas {
        val renderSize = when (fontSize) {
            in 0f..36f -> 24f
            else -> 48f
        }
        return fonts.getOrPut(font.resourcePath.hashCode() + renderSize.toInt()) {
            val data = font.load {
                LOGGER.error("Failed to load font: $font", it)
                return@getOrPut fonts[PolyUI.defaultFonts.regular.resourcePath.hashCode() + 12f.toInt()]
                    ?: throw IllegalStateException("Default font couldn't be loaded")
            }.toDirectByteBuffer()
            FontAtlas(data, renderSize)
        }
    }

    private var i = 0
    override fun dumpAtlas() {
        val buf = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4)
        glBindTexture(GL_TEXTURE_2D, atlas)
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf)
        glBindTexture(GL_TEXTURE_2D, 0)
        stb.image_write_png(
            "debug_atlas$i.png",
            ATLAS_SIZE,
            ATLAS_SIZE,
            4,
            buf,
            ATLAS_SIZE * 4
        )
        i++
    }

    override fun cleanup() {
//        dumpAtlas()
        if (program != 0) glDeleteProgram(program)
        if (quadVbo != 0) glDeleteBuffers(quadVbo)
        if (instancedVbo != 0) glDeleteBuffers(instancedVbo)
        if (atlas != 0) glDeleteTextures(atlas)
        if (vao != 0) org.lwjgl.opengl.GL30C.glDeleteVertexArrays(vao)
        fonts.clear()
    }

    override fun delete(font: Font?) {}
    override fun delete(image: PolyImage?) {}

    override fun close() {
        cleanup()
    }

    private inner class FontAtlas(data: ByteBuffer, val renderedSize: Float) {
        private val glyphs: Array<FloatArray>
        val ascent: Float
        val descent: Float
        val lineGap: Float

        init {
            val stb = stb
            val fontInfo = stb.font_CreateFontInfo()
            if (!stb.font_InitFont(fontInfo, data)) {
                throw IllegalStateException("Failed to initialize font")
            }
            val scale = stb.font_ScaleForMappingEmToPixels(fontInfo, renderedSize)
            val asc = IntArray(1)
            val des = IntArray(1)
            val gap = IntArray(1)
            stb.font_GetFontVMetrics(fontInfo, asc, des, gap)
            stb.free(fontInfo)
            val pixelHeight = (asc[0] - des[0]) * scale
            ascent = asc[0] * scale
            descent = des[0] * scale
            lineGap = gap[0] * scale

            val range = stb.font_CreatePackRange()
            stb.font_RangeSetFontSize(range, renderedSize)
            stb.font_RangeSetFirstUnicodeCodepointInRange(range, 32)
            stb.font_RangeSetNumChars(range, 95)
            val packed = stb.font_CreatePackedCharArray(95)
            stb.font_RangeSetChardata(range, packed)

            val bitMap = BufferUtils.createByteBuffer(FONT_MAX_BITMAP_W * FONT_MAX_BITMAP_H)
            val pack = stb.font_CreatePackContext()
            if (!stb.font_PackBegin(pack, bitMap, FONT_MAX_BITMAP_W, FONT_MAX_BITMAP_H, 0, 1, 0L)) {
                throw IllegalStateException("Failed to initialize font packer")
            }

            if (!stb.font_PackFontRange(pack, data, 0, pixelHeight, 32, 95, packed)) {
                throw IllegalStateException("Failed to pack font range")
            }
            stb.font_PackEnd(pack)
            stb.free(pack)

            var minX = Short.MAX_VALUE
            var minY = Short.MAX_VALUE
            var maxX = Short.MIN_VALUE
            var maxY = Short.MIN_VALUE
            for (i in 0..<95) {
                val g = stb.font_GetPackedGlyph(packed, i)
                val x0 = stb.glyph_x0(g)
                val y0 = stb.glyph_y0(g)
                val x1 = stb.glyph_x1(g)
                val y1 = stb.glyph_y1(g)
                if (x0 < minX) minX = x0
                if (y0 < minY) minY = y0
                if (x1 > maxX) maxX = x1
                if (y1 > maxY) maxY = y1
            }
            val totalSizeX = maxX - minX
            val totalSizeY = maxY - minY

            if (slotX + totalSizeX > ATLAS_SIZE) {
                slotX = 0
                slotY += atlasRowHeight
                atlasRowHeight = 0
            }
            val sx = slotX
            val sy = slotY


            glyphs = Array(95) {
                val g = stb.font_GetPackedGlyph(packed, it)
                val x0 = stb.glyph_x0(g)
                val y0 = stb.glyph_y0(g)
                val x1 = stb.glyph_x1(g)
                val y1 = stb.glyph_y1(g)

                floatArrayOf(
                    (sx + x0) / ATLAS_SIZE.toFloat(),
                    (sy + y0) / ATLAS_SIZE.toFloat(),
                    (x1 - x0) / ATLAS_SIZE.toFloat(),
                    (y1 - y0) / ATLAS_SIZE.toFloat(),
                    stb.glyph_xoff(g),
                    stb.glyph_yoff(g),
                    (x1 - x0).toFloat(),
                    (y1 - y0).toFloat(),
                    stb.glyph_xadvance(g)
                )
            }
            stb.free(packed)
            stb.free(range)

            val prevActive = glGetInteger(GL_ACTIVE_TEXTURE)
            val prevTex = glGetInteger(GL_TEXTURE_BINDING_2D)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, atlas)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0)
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0)
            // can't write to the alpha channel in GL3 core! lol haha
            glTexSubImage2D(
                GL_TEXTURE_2D,
                0,
                sx,
                sy,
                FONT_MAX_BITMAP_W,
                FONT_MAX_BITMAP_H,
                GL_RED,
                GL_UNSIGNED_BYTE,
                bitMap
            )
            when (mipmapMode) {
                1 -> org.lwjgl.opengl.GL30.glGenerateMipmap(GL_TEXTURE_2D)
                2 -> org.lwjgl.opengl.EXTFramebufferObject.glGenerateMipmapEXT(GL_TEXTURE_2D)
            }
            glBindTexture(GL_TEXTURE_2D, prevTex)
            glActiveTexture(prevActive)

            slotX += totalSizeX + 1
            atlasRowHeight = maxOf(atlasRowHeight, totalSizeY + 1)
        }

        fun measure(text: String, fontSize: Float): Vec2 {
            var width = 0f
//            var height = 0f
            val scaleFactor = fontSize / this.renderedSize
            for (c in text) {
                val g = get(c)
                width += g.xAdvance * scaleFactor
//                height = maxOf(height, g.height + g.offsetY)
            }
            return Vec2.of(width, fontSize)
        }

        @Suppress("DEPRECATION")
        @kotlin.internal.InlineOnly
        inline fun get(char: Char) = if (char.toInt() in 32..32 + 95) glyphs[(char.toInt() - 32)] else glyphs['?'.toInt()]
    }

    @kotlin.internal.InlineOnly
    inline val FloatArray.u get() = this[0]

    @kotlin.internal.InlineOnly
    inline val FloatArray.v get() = this[1]

    @kotlin.internal.InlineOnly
    inline val FloatArray.uw get() = this[2]

    @kotlin.internal.InlineOnly
    inline val FloatArray.vh get() = this[3]

    @kotlin.internal.InlineOnly
    inline val FloatArray.xOff get() = this[4]

    @kotlin.internal.InlineOnly
    inline val FloatArray.yOff get() = this[5]

    @kotlin.internal.InlineOnly
    inline val FloatArray.width get() = this[6]

    @kotlin.internal.InlineOnly
    inline val FloatArray.height get() = this[7]

    @kotlin.internal.InlineOnly
    inline val FloatArray.xAdvance get() = this[8]
}
