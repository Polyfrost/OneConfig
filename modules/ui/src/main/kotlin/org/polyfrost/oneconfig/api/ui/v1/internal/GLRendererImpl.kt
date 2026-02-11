@file:Suppress("UnstableApiUsage", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.polyfrost.oneconfig.api.ui.v1.internal

import dev.deftu.omnicore.api.client.render.GlCapabilities
import dev.deftu.omnicore.api.client.render.OmniResolution
import dev.deftu.omnicore.api.client.render.OmniTextRenderer
import dev.deftu.omnicore.api.math.OmniMatrix4f
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
import org.polyfrost.polyui.utils.forEachCodepoint
import org.polyfrost.polyui.utils.roundTo
import org.polyfrost.polyui.utils.toDirectByteBuffer
import org.polyfrost.polyui.utils.toDirectByteBufferNT
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.*

private val LOGGER = LogManager.getLogger("PolyUI/GLRenderer")

private const val MAX_UI_DEPTH = 16
private const val ATLAS_SIZE = 2048
private const val ATLAS_SVG_UPSCALE_FACTOR = 2f
private const val STRIDE = 4 + 4 + 1 + 1 + 4 + 1 + 4 // bounds, radii, color0, color1, UV, thick, clip
private const val MAX_BATCH = 2048
private const val FONT_SCALE_MAX_FIDELITY = 0.25f
private const val FONT_RENDERED_SIZE = 64f

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
    private val scissorStack = FloatArray(MAX_UI_DEPTH * 4)
    private val transformStack = Array(MAX_UI_DEPTH) { FloatArray(9) }
    private val transformBuffer = BufferUtils.createFloatBuffer(9)
    private val fonts = HashMap<String, FontAtlas>()
    private val init get() = program != 0
    private lateinit var atlas: GLAtlasManager

    // GL objects
    private var instancedVbo = 0
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
    private var iClipRect = 0


    // Current batch state
    private var viewport = FloatArray(4)
    private var count = 0
    private var transformDepth = 0
    private var scissorDepth = 0
    private var transform = IDENTITY.copyOf()
    private var pixelRatio = 1f
    private var alphaCap = 255
    private var popFlushNeeded = false

    private val FRAG = """
        #version $$$ // replaced by compileShader
        #if __VERSION__ >= 130
            #define IN in
            #define TEXTURE texture
            out vec4 fragColor;
        #else
            #define IN varying
            #define TEXTURE texture2D
            #define fragColor gl_FragColor
        #endif

        uniform sampler2D uTex;

        IN vec4 vUV;         // UV sampler position
        IN vec4 vP_HalfSize; // rect x, y, 0.5x wh
        IN vec4 vClipRect;   // clipping rectangle
        IN vec4 vRadii;      // per-corner radii
        IN vec4 vColor0;     // RGBA
        IN vec4 vColor1;     // RGBA (for gradients)
        IN float vThickness; // -1 for text, -2 for linear gradient, -3 for radial, -4 for box,  >0 for hollow rect

        // Signed distance function for rounded box
        float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
            vec2 s = step(0.0, p);
            float radius =
                mix(
                    mix(r.x, r.y, s.x),
                    mix(r.w, r.z, s.x),
                    s.y
                );

            vec2 q = abs(p) - (b - radius);
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
        }

        float hollowRoundedBoxSDF(vec2 p, vec2 b, vec4 r, float thickness) {
            float d = roundedBoxSDF(p, b, r);
            return abs(d) - thickness;
        }

        float roundBoxSDF(vec2 p, vec2 halfSize, float radius) {
            vec2 q = abs(p) - (halfSize - radius);
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
        }
        
        float boxSDF(vec2 p, vec2 halfSize) {
            vec2 q = abs(p) - halfSize;
            return max(q.x, q.y);
        }
        
        float hollowBoxSDF(vec2 p, vec2 halfSize, float thickness) {
            float d = boxSDF(p, halfSize); 
            return abs(d) - thickness;
        }

        void main() {
//            float clip =
//                step(vClipRect.x, gl_FragCoord.x) *
//                step(vClipRect.y, gl_FragCoord.y) *
//                step(gl_FragCoord.x, vClipRect.z) *
//                step(gl_FragCoord.y, vClipRect.w);
//            if (clip == 0.0) discard;

            vec4 col = vColor0;
            float d;
            if (vThickness <= 0.0) {
                if (vThickness == -1.0) { // text
                    float sdf = TEXTURE(uTex, vUV.xy).r;
                    float w = fwidth(sdf);
                    // bias increases as glyph gets smaller
                    float bias = 0.5 - min(w * 0.6, 0.08);
                    col.a *= smoothstep(bias - w, bias + w, sdf);
                }
                else if (vThickness == -2.0) { // image
                    col = col * TEXTURE(uTex, vUV.xy);
                } 
                else if (vThickness == -3.0) { // linear gradient, vUV.xy as start and vUV.zw as end
                    vec2 dir = vUV.zw - vUV.xy;
                    float invLen2 = 1.0 / dot(dir, dir);
                    float t = clamp(dot((vP_HalfSize.xy + vP_HalfSize.zw) - vUV.xy, dir) * invLen2, 0.0, 1.0);
                    col = mix(vColor0, vColor1, t);
                }
                else if (vThickness == -4.0) { // radial gradient, vUV as center and vUV.w as radius
                    float dist = length(vP_HalfSize.xy + vP_HalfSize.zw - vUV.xy);
                    float t = clamp((dist - vUV.w) / (vUV.w - vUV.z), 0.0, 1.0);
                    col = mix(vColor0, vColor1, t);
                }
                else if (vThickness == -5.0) { // box gradient, vUV.x as radius and vUV.y as feather
                    float dist = roundBoxSDF(vP_HalfSize.xy, vP_HalfSize.zw, vUV.x);
                    float t = clamp((dist + vUV.y * 0.5) / vUV.y, 0.0, 1.0);
                    col = mix(vColor0, vColor1, t);
                }
                else if (vThickness == -6.0) { // drop shadow, vUV.x as spread and vUV.y as blur
                    float dShadow = roundBoxSDF(vP_HalfSize.xy, vP_HalfSize.zw + vUV.x, vRadii.x);
                    col.a *= (1.0 - smoothstep(-vUV.y, vUV.y, dShadow));
                }
                d = (vRadii.y == -1.0) ? boxSDF(vP_HalfSize.xy, vP_HalfSize.zw) : roundedBoxSDF(vP_HalfSize.xy, vP_HalfSize.zw, vRadii);
            } else {
                d = (vRadii.y == -1.0) ? hollowBoxSDF(vP_HalfSize.xy, vP_HalfSize.zw, vThickness) : hollowRoundedBoxSDF(vP_HalfSize.xy, vP_HalfSize.zw, vRadii, vThickness);
            }

            // Proper antialiasing based on distance field
            float alpha = col.a * clamp(0.5 - d, 0.0, 1.0);

            fragColor = vec4(col.rgb * alpha, alpha);
        }
    """.trimIndent()

    private val VERT = """
        #version $$$ // replaced by compileShader
        #extension GL_EXT_gpu_shader4 : enable
        #if __VERSION__ >= 130
            #define ATTRIBUTE in
            #define OUT out
            #define U_INT uint
        #else
            #define ATTRIBUTE attribute
            #define OUT varying
            #define U_INT unsigned int
        #endif

        ATTRIBUTE vec2 aLocal;
        ATTRIBUTE vec4 iRect;
        ATTRIBUTE vec4 iRadii;
        ATTRIBUTE U_INT iColor0;
        ATTRIBUTE U_INT iColor1;
        ATTRIBUTE vec4 iUVRect;
        ATTRIBUTE float iThickness;
        ATTRIBUTE vec4 iClipRect;

        uniform mat3 uTransform = mat3(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        );
        uniform vec2 uWindow;

        OUT vec4 vP_HalfSize;
        OUT vec4 vClipRect;
        OUT vec4 vRadii;
        OUT vec4 vColor0;
        OUT vec4 vColor1;
        OUT vec4 vUV;
        OUT float vThickness;
        
        vec4 unpackColor(U_INT c) {
            float a = float((c >> 24) & 0xFFu) / 255.0;
            float r = float((c >> 16) & 0xFFu) / 255.0;
            float g = float((c >>  8) & 0xFFu) / 255.0;
            float b = float((c      ) & 0xFFu) / 255.0;

            return vec4(r, g, b, a);
        }

        void main() {
            // Position inside rect
            vec2 pos = aLocal * iRect.zw;
            vec2 uv  = (iThickness > -3.0) ? iUVRect.xy + aLocal * iUVRect.zw : iUVRect.xy; // for gradients, just pass through the first two param to frag
            vec2 halfSize = iRect.zw * 0.5;

            vec3 transformed = uTransform * vec3(pos + iRect.xy, 1.0);

            vec2 ndc = (transformed.xy / uWindow) * 2.0 - 1.0;
            ndc.y = -ndc.y;

            gl_Position = vec4(ndc, 0.0, 1.0);

            vP_HalfSize = vec4(pos - halfSize, halfSize); 
            vClipRect   = iClipRect;
            vRadii      = iRadii;
            vColor0     = unpackColor(iColor0);
            vColor1     = unpackColor(iColor1);
            vUV         = vec4(uv, iUVRect.zw);
            vThickness  = iThickness;
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
        iClipRect = glGetAttribLocation(program, "iClipRect")

        if (vao != 0) {
            var offset = 0L
            offset = enableAttrib(iRect, 4, offset)
            offset = enableAttrib(iRadii, 4, offset)
            offset = enableAttribui(iColor0, 1, offset)
            offset = enableAttribui(iColor1, 1, offset)
            offset = enableAttrib(iUVRect, 4, offset)
            offset = enableAttrib(iThickness, 1, offset)
            enableAttrib(iClipRect, 4, offset)

            glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
            glEnableVertexAttribArray(aLocal)
            glVertexAttribPointer(aLocal, 2, GL_FLOAT, false, 0, 0L)
            org.lwjgl.opengl.GL30.glBindVertexArray(0)
        }

        val prevTex = glGetInteger(GL_TEXTURE_BINDING_2D)
        atlas = GLAtlasManager(ATLAS_SIZE, ATLAS_SIZE)
        glBindTexture(GL_TEXTURE_2D, prevTex)
        glBindBuffer(GL_ARRAY_BUFFER, prevBuf)
    }

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float, viewport: FloatArray?) {
        this.viewport = viewport ?: run {
            this.viewport[2] = width
            this.viewport[3] = height
            this.viewport
        }
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
        if (vao != 0) {
            prevVao = glGetInteger(org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING)
            org.lwjgl.opengl.GL30.glBindVertexArray(vao)
        } else prevVao = 0
        glUseProgram(program)

        if (popFlushNeeded) {
            glUniformMatrix3fv(uTransform, false, transform)
            popFlushNeeded = false
        }

        glActiveTexture(GL_TEXTURE0)
        atlas.bind()
        if (GlCapabilities.isGl33Available) org.lwjgl.opengl.GL33.glBindSampler(0, 0)

        // asm: on VAO the state is stored, so we only need to set it up once
        if (vao == 0) {
            // Quad attrib
            glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
            glEnableVertexAttribArray(aLocal)
            glVertexAttribPointer(aLocal, 2, GL_FLOAT, false, 0, 0L)

            // Instance attribs
            glBindBuffer(GL_ARRAY_BUFFER, instancedVbo)

            var offset = 0L
            offset = enableAttrib(iRect, 4, offset)
            offset = enableAttrib(iRadii, 4, offset)
            offset = enableAttribui(iColor0, 1, offset)
            offset = enableAttribui(iColor1, 1, offset)
            offset = enableAttrib(iUVRect, 4, offset)
            offset = enableAttrib(iThickness, 1, offset)
            enableAttrib(iClipRect, 4, offset)
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
        if (vao != 0) org.lwjgl.opengl.GL30.glBindVertexArray(prevVao)
    }

    private fun enableAttrib(loc: Int, size: Int, offset: Long): Long {
        glEnableVertexAttribArray(loc)
        glVertexAttribPointer(loc, size, GL_FLOAT, false, STRIDE * 4, offset)
        // I don't know why core disables the extension functions... but ok!
        if (GlCapabilities.isGl33Available) org.lwjgl.opengl.GL33.glVertexAttribDivisor(loc, 1)
        else org.lwjgl.opengl.ARBInstancedArrays.glVertexAttribDivisorARB(loc, 1)
        return offset + size * 4L
    }

    private fun enableAttribui(loc: Int, size: Int, offset: Long): Long {
        glEnableVertexAttribArray(loc)
        if (GlCapabilities.isGl3Available) org.lwjgl.opengl.GL30.glVertexAttribIPointer(loc, size, GL_UNSIGNED_INT, STRIDE * 4, offset)
        else org.lwjgl.opengl.EXTGPUShader4.glVertexAttribIPointerEXT(loc, size, GL_UNSIGNED_INT, STRIDE * 4, offset)
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
        val topRightRadius = if (topLeftRadius == 0f && topLeftRadius == topRightRadius && topLeftRadius == bottomLeftRadius && topLeftRadius == bottomRightRadius) -1f else topRightRadius
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(color.argb.capAlpha()))
        if (color is PolyColor.Gradient) {
            buffer.put(java.lang.Float.intBitsToFloat(color.color2.argb.capAlpha()))
            when (val type = color.type) {
                is PolyColor.Gradient.Type.LeftToRight -> {
                    buffer.put(0f).put(height / 2f).put(width).put(height / 2f)
                    buffer.put(-3f)
                }

                is PolyColor.Gradient.Type.TopToBottom -> {
                    buffer.put(width / 2f).put(0f).put(width / 2f).put(height)
                    buffer.put(-3f)
                }

                is PolyColor.Gradient.Type.BottomLeftToTopRight -> {
                    buffer.put(0f).put(height).put(width).put(0f)
                    buffer.put(-3f)
                }

                is PolyColor.Gradient.Type.TopLeftToBottomRight -> {
                    buffer.put(0f).put(0f).put(width).put(height)
                    buffer.put(-3f)
                }

                is PolyColor.Gradient.Type.Radial -> {
                    buffer.put(if (type.centerX == -1f) width / 2f else type.centerX)
                        .put(if (type.centerY == -1f) height / 2f else type.centerY).put(type.innerRadius)
                        .put(type.outerRadius)
                    buffer.put(-4f)
                }

                is PolyColor.Gradient.Type.Box -> {
                    buffer.put(type.radius).put(type.feather).put(0f).put(0f)
                    buffer.put(-5f)
                }
            }
        } else {
            buffer.put(0f) // color1 unused
            buffer.put(NO_UV) // -1f UVs to indicate no texture
            buffer.put(0f)  // standard filled rect
        }
        buffer.put(scissorStack, (scissorDepth - 4).coerceAtLeast(0), 4)
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
        val topRightRadius = if (topLeftRadius == 0f && topLeftRadius == topRightRadius && topLeftRadius == bottomLeftRadius && topLeftRadius == bottomRightRadius) -1f else topRightRadius
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(color.argb.capAlpha()))
        buffer.put(0f) // color1 unused
        buffer.put(NO_UV) // -1f UVs to indicate no texture
        buffer.put(lineWidth)
        buffer.put(scissorStack, (scissorDepth - 4).coerceAtLeast(0), 4)
        count += 1
    }

    override fun image(
        image: PolyImage, x: Float, y: Float, width: Float, height: Float,
        colorMask: Int, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float
    ) {
        if (count >= MAX_BATCH) flush()

        val topRightRadius = if (topLeftRadius == 0f && topLeftRadius == topRightRadius && topLeftRadius == bottomLeftRadius && topLeftRadius == bottomRightRadius) -1f else topRightRadius
        buffer.put(x).put(y).put(width).put(height)
        buffer.put(topLeftRadius).put(topRightRadius).put(bottomRightRadius).put(bottomLeftRadius)
        buffer.put(java.lang.Float.intBitsToFloat(colorMask.capAlpha()))
        buffer.put(0f) // color1 unused
        buffer.put(image.uv.x).put(image.uv.y).put(image.uv.w).put(image.uv.h)
        buffer.put(-2f) // thickness = -2 for textured rect
        buffer.put(scissorStack, (scissorDepth - 4).coerceAtLeast(0), 4)
        count += 1
    }

    private fun FloatArray.getScaledMat4(): OmniMatrix4f {
        // asm: scale to MC instance coordinates and mutate to a 4x4 matrix
        val sf = pixelRatio / OmniResolution.scaleFactor.toFloat()
        return OmniMatrix4f.from(floatArrayOf(
            this[0] * sf, this[1] * sf, 0f, 0f,
            this[3] * sf, this[4] * sf, 0f, 0f,
            0f, 0f, 1f, 0f,
            this[6] * sf, this[7] * sf, 0f, 1f
        ))
    }

    override fun text(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float) {
        if (font === UIManager.INSTANCE.mcFont) {
            // todo broken on 1.21.10(+)
//            val ctx = UIManager.INSTANCE.renderingContext
//            ctx.pose.push(OmniPoseStack.Entry(transform.getScaledMat4(), ctx.pose.current.normalMatrix))
//             asm: can be optimized by https://github.com/Deftu/OmniCore/issues/58
//            OmniTextRenderer.render(ctx.pose, text, x, y, OmniColor.argb(color.argb), false)
//            ctx.pose.pop()
            return
        }
        val s = transformScale()
        val fAtlas = getFontAtlas(font)
        if (count >= MAX_BATCH) flush()

        var penX = floor(x + 0.5f)
        val scaleFactor = (fontSize.roundTo(FONT_SCALE_MAX_FIDELITY) / FONT_RENDERED_SIZE) * s
        val penY = (y + (fAtlas.ascent + fAtlas.descent) * scaleFactor) + (fontSize / 6f)
        val col = java.lang.Float.intBitsToFloat(color.argb.capAlpha())
        val buffer = buffer

        text.forEachCodepoint {
            if (count >= MAX_BATCH) flush()
            val glyph = fAtlas.get(it)
            // opt: early exit when we are out of the scissored region
            if (scissorDepth > 3 && penX > scissorStack[scissorDepth - 2]) return
            buffer.put(penX + glyph.xOff * scaleFactor).put(penY + glyph.yOff * scaleFactor)
                .put(glyph.width * scaleFactor).put(glyph.height * scaleFactor)
            buffer.put(0f).put(-1f).put(0f).put(0f) // zero radii (-1 optimization)
            buffer.put(col)
            buffer.put(0f) // color1 unused
            buffer.put(glyph, 0, 4) // UVs
            buffer.put(-1f) // thickness = -1 for text
            buffer.put(scissorStack, (scissorDepth - 4).coerceAtLeast(0), 4)
            penX += glyph.xAdvance * scaleFactor
            count += 1
        }
    }

    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        return if (font === UIManager.INSTANCE.mcFont) {
            Vec2(OmniTextRenderer.width(text).toFloat(), OmniTextRenderer.lineHeight.toFloat())
        } else getFontAtlas(font).measure(text, fontSize)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        if (y1 == y2) rect(x1, y1, x2 - x1, width, color, 0f, -1f, 0f, 0f)
        else rect(x1, y1, width, y2 - y1, color, 0f, -1f, 0f, 0f)
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
        buffer.put(0f).put(-1f).put(0f).put(0f) // zero radii
        buffer.put(java.lang.Float.intBitsToFloat(alphaCap shl 24)) // black, alpha to alphaCap
        buffer.put(0f) // color1 unused
        buffer.put(spread).put(blur).put(0f).put(0f)
        buffer.put(-6f) // thickness = -6 for drop shadow
        buffer.put(scissorStack, (scissorDepth - 4).coerceAtLeast(0), 4)
        count += 1
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
        val ny = ((viewport[3] + viewport[1]) - (y + height) * pixelRatio)
        scissorStack[scissorDepth++] = x * pixelRatio
        scissorStack[scissorDepth++] = ny
        scissorStack[scissorDepth++] = (x + width) * pixelRatio
        scissorStack[scissorDepth++] = ny + height * pixelRatio
    }

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) {
        if (scissorDepth < 4) {
            pushScissor(x, y, width, height)
            return
        }
        val px = scissorStack[scissorDepth - 4]
        val py = scissorStack[scissorDepth - 3]
        val pl = scissorStack[scissorDepth - 2]
        val pr = scissorStack[scissorDepth - 1]
        val ny = ((viewport[3] + viewport[1]) - (y + height) * pixelRatio)

        val ix = maxOf(x * pixelRatio, px)
        val iy = maxOf(ny, py)
        val il = minOf((x + width) * pixelRatio, pl)
        val ir = minOf(ny + height * pixelRatio, pr)

        scissorStack[scissorDepth++] = ix
        scissorStack[scissorDepth++] = iy
        scissorStack[scissorDepth++] = il
        scissorStack[scissorDepth++] = ir
    }

    override fun popScissor() {
        if (scissorDepth <= 4) {
            scissorDepth = 0
            pushScissor(0f, 0f, 1_000_000f, 1_000_000f)
            return
        }
        scissorDepth -= 4
    }

    override fun globalAlpha(alpha: Float) {
        alphaCap = (alpha * 255f).toInt()
    }

    override fun resetGlobalAlpha() {
        alphaCap = 255
    }

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

        // Store UV rect for this image
        image.uv = atlas.insert(w[0], h[0], d)
        if (image.type == PolyImage.Type.Raster) stb.image_free(d)
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

    private fun getFontAtlas(font: Font): FontAtlas {
        return fonts.getOrPut(font.resourcePath) {
            val data = font.load {
                LOGGER.error("Failed to load font: $font", it)
                return@getOrPut fonts[PolyUI.defaultFonts.regular.resourcePath]
                    ?: throw IllegalStateException("Default font couldn't be loaded")
            }.toDirectByteBuffer()
            FontAtlas(data)
        }
    }

    private var i = 0
    override fun dumpAtlas() {
        val buf = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4)
        atlas.bind()
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
        atlas.cleanup()
        fonts.values.forEach(FontAtlas::cleanup)
        if (vao != 0) org.lwjgl.opengl.GL30C.glDeleteVertexArrays(vao)
        fonts.clear()
    }

    override fun delete(font: Font?) {}
    override fun delete(image: PolyImage?) {}

    override fun close() {
        cleanup()
    }

    private inner class FontAtlas(private val data: ByteBuffer) {
        private val glyphs = HashMap<Int, FloatArray>()
        val ascent: Float
        val descent: Float
        val lineGap: Float
        private val fontInfo = stb.font_CreateFontInfo()
        val scale: Float

        init {
            if (!stb.font_InitFont(fontInfo, data)) {
                throw IllegalStateException("Failed to initialize font")
            }
            scale = stb.font_ScaleForMappingEmToPixels(fontInfo, FONT_RENDERED_SIZE)
            val asc = IntArray(1)
            val des = IntArray(1)
            val gap = IntArray(1)
            stb.font_GetFontVMetrics(fontInfo, asc, des, gap)
            ascent = asc[0] * scale
            descent = des[0] * scale
            lineGap = gap[0] * scale
        }

        private fun makeGlyph(codepoint: Int): FloatArray {
            val w = IntArray(1)
            val h = IntArray(1)
            val xoff = IntArray(1)
            val yoff = IntArray(1)
            val xAdvance = IntArray(1)

            val idx = stb.font_FindGlyphIndex(fontInfo, codepoint)
//            if (idx == 0) {
//                val o = getFontAtlas(Font.of("polyui/fonts/NotoEmoji-Regular.ttf"), 12f).get(codepoint)
//                glyphs[codepoint] = o
//                return o
//            }

            stb.font_GetGlyphHMetrics(fontInfo, idx, xAdvance, null)
            var sdf = stb.font_GetGlyphSDF(fontInfo, scale, idx, 4, 128.toByte(), 64f, w, h, xoff, yoff)
            if (sdf == 0L) sdf = stb.font_GetGlyphBitmap(fontInfo, scale, scale, idx, w, h, xoff, yoff)
            if (sdf == 0L) sdf = Platform.gl().memAddress(BufferUtils.createByteBuffer(w[0] * h[0] * 4))

            val (u, v, uw, uh) = atlas.insert(w[0], h[0], sdf, GL_RED)
            return floatArrayOf(
                u, v, uw, uh,
                xoff[0].toFloat(),
                yoff[0].toFloat(),
                w[0].toFloat(),
                h[0].toFloat(),
                xAdvance[0].toFloat() * scale
            )
        }

        fun measure(text: String, fontSize: Float): Vec2 {
            var width = 0f
//            var height = 0f
            val scaleFactor = fontSize.roundTo(FONT_SCALE_MAX_FIDELITY) / FONT_RENDERED_SIZE
            text.forEachCodepoint {
                width += get(it).xAdvance * scaleFactor
            }
            return Vec2.of(width, fontSize)
        }

        @Suppress("DEPRECATION")
        @kotlin.internal.InlineOnly
        inline fun get(codepoint: Int) = glyphs.getOrPut(codepoint) { makeGlyph(codepoint) }

        fun cleanup() {
            stb.free(fontInfo)
            glyphs.clear()
        }
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
