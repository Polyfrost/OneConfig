package org.polyfrost.oneconfig.internal.ui.compose.opengl

//? if > 1.8.9 {
//? if >= 1.21.5 {
import com.mojang.blaze3d.opengl.GlStateManager
//? } else {
/*import com.mojang.blaze3d.platform.GlStateManager
*///? }
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL45.*
//?} else {
/*import net.minecraft.client.render.platform.GlStateManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33
*///?}

fun resyncTextureBindCache() {
    //? if > 1.8.9 {
    for (unit in 0..7) {
        GlStateManager._activeTexture(GL_TEXTURE0 + unit)
        GlStateManager._bindTexture(0)
    }
    GlStateManager._activeTexture(GL_TEXTURE0)
    //?} else {
    /*for (unit in 0..7) {
        GlStateManager.activeTexture(GL13.GL_TEXTURE0 + unit)
        GlStateManager.bindTexture(0)
    }
    GlStateManager.activeTexture(GL13.GL_TEXTURE0)
    *///?}
}

class StoredGLState(private val glVersion: Int) {
    private val props = StoredGLStateProps()

    //? if > 1.8.9 {
    fun capture(): StoredGLState {
        with(props) {
            glGetIntegerv(GL_ACTIVE_TEXTURE, lastActiveTexture)
            glActiveTexture(GL_TEXTURE0)
            glGetIntegerv(GL_CURRENT_PROGRAM, lastProgram)
            glGetIntegerv(GL_TEXTURE_BINDING_2D, lastTexture)
            if (glVersion >= 330 || GL.getCapabilities().GL_ARB_sampler_objects) {
                glGetIntegerv(GL_SAMPLER_BINDING, lastSampler)
            }
            glGetIntegerv(GL_ARRAY_BUFFER_BINDING, lastArrayBuffer)
            glGetIntegerv(GL_VERTEX_ARRAY_BINDING, lastVertexArrayObject)
            if (glVersion >= 200) {
                glGetIntegerv(GL_POLYGON_MODE, lastPolygonMode)
            }
            glGetIntegerv(GL_DEPTH_FUNC, lastDepthFunc)
            glGetIntegerv(GL_VIEWPORT, lastViewport)
            glGetIntegerv(GL_SCISSOR_BOX, lastScissorBox)
            glGetIntegerv(GL_BLEND_SRC_RGB, lastBlendSrcRgb)
            glGetIntegerv(GL_BLEND_DST_RGB, lastBlendDstRgb)
            glGetIntegerv(GL_BLEND_SRC_ALPHA, lastBlendSrcAlpha)
            glGetIntegerv(GL_BLEND_DST_ALPHA, lastBlendDstAlpha)
            glGetIntegerv(GL_BLEND_EQUATION_RGB, lastBlendEquationRgb)
            glGetIntegerv(GL_BLEND_EQUATION_ALPHA, lastBlendEquationAlpha)
            lastColorMask.clear()
            glGetBooleanv(GL_COLOR_WRITEMASK, lastColorMask)
            lastColorMask.rewind()

            glGetIntegerv(GL_STENCIL_FUNC, lastStencilFunc)
            glGetIntegerv(GL_STENCIL_REF, lastStencilRef)
            glGetIntegerv(GL_STENCIL_VALUE_MASK, lastStencilValueMask)
            glGetIntegerv(GL_STENCIL_WRITEMASK, lastStencilWriteMask)
            glGetIntegerv(GL_STENCIL_FAIL, lastStencilFail)
            glGetIntegerv(GL_STENCIL_PASS_DEPTH_FAIL, lastStencilPassDepthFail)
            glGetIntegerv(GL_STENCIL_PASS_DEPTH_PASS, lastStencilPassDepthPass)
            lastFramebufferSrgb = glIsEnabled(GL_FRAMEBUFFER_SRGB)

            lastEnableBlend = glIsEnabled(GL_BLEND)
            lastEnableCullFace = glIsEnabled(GL_CULL_FACE)
            lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST)
            lastEnableStencilTest = glIsEnabled(GL_STENCIL_TEST)
            lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST)
            if (glVersion >= 310) {
                lastEnablePrimitiveRestart = glIsEnabled(GL_PRIMITIVE_RESTART)
            }

            lastDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK)

            glGetIntegerv(GL_PIXEL_UNPACK_BUFFER_BINDING, lastPixelUnpackBufferBinding)
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0)

            glGetIntegerv(GL_PACK_SWAP_BYTES, lastPackSwapBytes)
            glGetIntegerv(GL_PACK_LSB_FIRST, lastPackLsbFirst)
            glGetIntegerv(GL_PACK_ROW_LENGTH, lastPackRowLength)
            glGetIntegerv(GL_PACK_SKIP_PIXELS, lastPackSkipPixels)
            glGetIntegerv(GL_PACK_SKIP_ROWS, lastPackSkipRows)
            glGetIntegerv(GL_PACK_ALIGNMENT, lastPackAlignment)

            glGetIntegerv(GL_UNPACK_SWAP_BYTES, lastUnpackSwapBytes)
            glGetIntegerv(GL_UNPACK_LSB_FIRST, lastUnpackLsbFirst)
            glGetIntegerv(GL_UNPACK_ALIGNMENT, lastUnpackAlignment)
            glGetIntegerv(GL_UNPACK_ROW_LENGTH, lastUnpackRowLength)
            glGetIntegerv(GL_UNPACK_SKIP_PIXELS, lastUnpackSkipPixels)
            glGetIntegerv(GL_UNPACK_SKIP_ROWS, lastUnpackSkipRows)

            if (glVersion >= 120) {
                glGetIntegerv(GL_PACK_IMAGE_HEIGHT, lastPackImageHeight)
                glGetIntegerv(GL_PACK_SKIP_IMAGES, lastPackSkipImages)
                glGetIntegerv(GL_UNPACK_IMAGE_HEIGHT, lastUnpackImageHeight)
                glGetIntegerv(GL_UNPACK_SKIP_IMAGES, lastUnpackSkipImages)
            }

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0)
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0)
        }
        return this
    }

    fun restore(): StoredGLState {
        with(props) {
            glUseProgram(lastProgram[0])
            glBindTexture(GL_TEXTURE_2D, lastTexture[0])
            if (glVersion >= 330 || GL.getCapabilities().GL_ARB_sampler_objects) {
                glBindSampler(0, lastSampler[0])
            }
            glActiveTexture(lastActiveTexture[0])

            resyncTextureBindCache()

            glBindVertexArray(lastVertexArrayObject[0])
            glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer[0])
            glBlendEquationSeparate(lastBlendEquationRgb[0], lastBlendEquationAlpha[0])
            glBlendFuncSeparate(
                lastBlendSrcRgb[0],
                lastBlendDstRgb[0],
                lastBlendSrcAlpha[0],
                lastBlendDstAlpha[0]
            )
            glColorMask(
                lastColorMask.get(0).toInt() != 0,
                lastColorMask.get(1).toInt() != 0,
                lastColorMask.get(2).toInt() != 0,
                lastColorMask.get(3).toInt() != 0
            )

            // Skia's resetGLAll changes real GL without updating GlStateManager's cache and raw
            // glEnable/glDisable leaves it stale so toggle through GlStateManager to resync both
            //? if >= 26.2 {
            forceToggle(lastEnableBlend, { GlStateManager._enableBlend(0) }, { GlStateManager._disableBlend(0) })
            //?} else {
            /*forceToggle(lastEnableBlend, GlStateManager::_enableBlend, GlStateManager::_disableBlend)
            *///?}
            forceToggle(lastEnableCullFace, GlStateManager::_enableCull, GlStateManager::_disableCull)
            forceToggle(
                lastEnableDepthTest,
                GlStateManager::_enableDepthTest,
                GlStateManager::_disableDepthTest,
            )
            if (lastEnableStencilTest) glEnable(GL_STENCIL_TEST)
            else glDisable(GL_STENCIL_TEST)
            glStencilFunc(lastStencilFunc[0], lastStencilRef[0], lastStencilValueMask[0])
            glStencilMask(lastStencilWriteMask[0])
            glStencilOp(lastStencilFail[0], lastStencilPassDepthFail[0], lastStencilPassDepthPass[0])
            if (lastFramebufferSrgb) glEnable(GL_FRAMEBUFFER_SRGB)
            else glDisable(GL_FRAMEBUFFER_SRGB)
            forceToggle(
                lastEnableScissorTest,
                GlStateManager::_enableScissorTest,
                GlStateManager::_disableScissorTest,
            )
            if (glVersion >= 310) {
                if (lastEnablePrimitiveRestart) glEnable(GL_PRIMITIVE_RESTART)
                else glDisable(GL_PRIMITIVE_RESTART)
            }
            if (glVersion >= 200) {
                glPolygonMode(GL_FRONT_AND_BACK, lastPolygonMode[0])
            }
            forceDepthFunc(lastDepthFunc[0])
            forceDepthMask(lastDepthMask)
            glViewport(lastViewport[0], lastViewport[1], lastViewport[2], lastViewport[3])
            glScissor(
                lastScissorBox[0],
                lastScissorBox[1],
                lastScissorBox[2],
                lastScissorBox[3]
            )

            glPixelStorei(GL_PACK_SWAP_BYTES, lastPackSwapBytes[0])
            glPixelStorei(GL_PACK_LSB_FIRST, lastPackLsbFirst[0])
            glPixelStorei(GL_PACK_ROW_LENGTH, lastPackRowLength[0])
            glPixelStorei(GL_PACK_SKIP_PIXELS, lastPackSkipPixels[0])
            glPixelStorei(GL_PACK_SKIP_ROWS, lastPackSkipRows[0])
            glPixelStorei(GL_PACK_ALIGNMENT, lastPackAlignment[0])

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, lastPixelUnpackBufferBinding[0])
            glPixelStorei(GL_UNPACK_SWAP_BYTES, lastUnpackSwapBytes[0])
            glPixelStorei(GL_UNPACK_LSB_FIRST, lastUnpackLsbFirst[0])
            glPixelStorei(GL_UNPACK_ALIGNMENT, lastUnpackAlignment[0])
            glPixelStorei(GL_UNPACK_ROW_LENGTH, lastUnpackRowLength[0])
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, lastUnpackSkipPixels[0])
            glPixelStorei(GL_UNPACK_SKIP_ROWS, lastUnpackSkipRows[0])

            if (glVersion >= 120) {
                glPixelStorei(GL_PACK_IMAGE_HEIGHT, lastPackImageHeight[0])
                glPixelStorei(GL_PACK_SKIP_IMAGES, lastPackSkipImages[0])
                glPixelStorei(GL_UNPACK_IMAGE_HEIGHT, lastUnpackImageHeight[0])
                glPixelStorei(GL_UNPACK_SKIP_IMAGES, lastUnpackSkipImages[0])
            }
        }
        return this
    }

    private fun forceToggle(enabled: Boolean, enable: () -> Unit, disable: () -> Unit) {
        if (enabled) {
            disable()
            enable()
        } else {
            enable()
            disable()
        }
    }

    private fun forceDepthFunc(func: Int) {
        val bogus = if (func == GL_LEQUAL) GL_GREATER else GL_LEQUAL
        GlStateManager._depthFunc(bogus)
        GlStateManager._depthFunc(func)
    }

    private fun forceDepthMask(mask: Boolean) {
        GlStateManager._depthMask(!mask)
        GlStateManager._depthMask(mask)
    }
    //?} else {
    /*private val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    private var alphaTestEnabled = false

    fun capture() {
        alphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST)
        GL11.glPushClientAttrib(GL11.GL_CLIENT_ALL_ATTRIB_BITS)
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

        with(props) {
            if (!isMacOS) {
                lastSampler[0] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING)
                lastVertexArrayObject[0] = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)
            }

            lastEnableBlend = GL11.glIsEnabled(GL11.GL_BLEND)

            lastProgram[0] = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
            lastActiveTexture[0] = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
            lastArrayBuffer[0] = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING)

            lastBlendSrcRgb[0] = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB)
            lastBlendDstRgb[0] = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB)
            lastBlendSrcAlpha[0] = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA)
            lastBlendDstAlpha[0] = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)
            lastBlendEquationRgb[0] = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB)
            lastBlendEquationAlpha[0] = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA)

            lastPackSwapBytes[0] = GL11.glGetInteger(GL11.GL_PACK_SWAP_BYTES)
            lastPackLsbFirst[0] = GL11.glGetInteger(GL11.GL_PACK_LSB_FIRST)
            lastPackRowLength[0] = GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH)
            lastPackImageHeight[0] = GL11.glGetInteger(GL12.GL_PACK_IMAGE_HEIGHT)
            lastPackSkipPixels[0] = GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS)
            lastPackSkipRows[0] = GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS)
            lastPackSkipImages[0] = GL11.glGetInteger(GL12.GL_PACK_SKIP_IMAGES)
            lastPackAlignment[0] = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT)

            lastUnpackSwapBytes[0] = GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES)
            lastUnpackLsbFirst[0] = GL11.glGetInteger(GL11.GL_UNPACK_LSB_FIRST)
            lastUnpackRowLength[0] = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH)
            lastUnpackImageHeight[0] = GL11.glGetInteger(GL12.GL_UNPACK_IMAGE_HEIGHT)
            lastUnpackSkipPixels[0] = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS)
            lastUnpackSkipRows[0] = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS)
            lastUnpackSkipImages[0] = GL11.glGetInteger(GL12.GL_UNPACK_SKIP_IMAGES)
            lastUnpackAlignment[0] = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT)
        }
    }

    fun restore() {
        GL11.glPopAttrib()
        GL11.glPopClientAttrib()
        if (alphaTestEnabled) GlStateManager.enableAlphaTest() else GlStateManager.disableAlphaTest()

        with(props) {
            if (!isMacOS) {
                for (unit in 0..7) {
                    GL33.glBindSampler(unit, 0)
                }
                GL33.glBindSampler(0, lastSampler[0])
                GL30.glBindVertexArray(lastVertexArrayObject[0])
            }

            if (lastEnableBlend) GlStateManager.enableBlend() else GlStateManager.disableBlend()

            GL20.glUseProgram(lastProgram[0])
            GL13.glActiveTexture(GL13.GL_TEXTURE0)
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lastArrayBuffer[0])

            GL20.glBlendEquationSeparate(lastBlendEquationRgb[0], lastBlendEquationAlpha[0])
            GlStateManager.blendFuncSeparate(
                lastBlendSrcRgb[0],
                lastBlendDstRgb[0],
                lastBlendSrcAlpha[0],
                lastBlendDstAlpha[0],
            )

            GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, lastPackSwapBytes[0])
            GL11.glPixelStorei(GL11.GL_PACK_LSB_FIRST, lastPackLsbFirst[0])
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, lastPackRowLength[0])
            GL11.glPixelStorei(GL12.GL_PACK_IMAGE_HEIGHT, lastPackImageHeight[0])
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, lastPackSkipPixels[0])
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, lastPackSkipRows[0])
            GL11.glPixelStorei(GL12.GL_PACK_SKIP_IMAGES, lastPackSkipImages[0])
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, lastPackAlignment[0])

            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, lastUnpackSwapBytes[0])
            GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, lastUnpackLsbFirst[0])
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, lastUnpackRowLength[0])
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, lastUnpackImageHeight[0])
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, lastUnpackSkipPixels[0])
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, lastUnpackSkipRows[0])
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, lastUnpackSkipImages[0])
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, lastUnpackAlignment[0])

            GL11.glShadeModel(GL11.GL_SMOOTH)
        }
    }
    *///?}
}
