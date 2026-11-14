@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import com.mojang.blaze3d.pipeline.RenderTarget
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.internal.ui.FramePresentHooks
import org.polyfrost.oneconfig.internal.ui.OffscreenTarget
import org.polyfrost.oneconfig.internal.ui.TextureRenderer

//? if > 1.8.9 {
//? if >= 1.21.4 {
import com.mojang.blaze3d.ProjectionType
//?} else
//import com.mojang.blaze3d.vertex.VertexSorting
import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Matrix4f
//?} else {
/*import com.mojang.blaze3d.platform.GlStateManager
import org.lwjgl.opengl.GL11
*///?}

//? if >= 26.1 {
import net.minecraft.client.renderer.ProjectionMatrixBuffer
//?} elif >= 1.21.8
//import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer

//? if >= 26.2 {
import net.minecraft.client.renderer.Projection
import org.joml.Vector4f
//?}

//? if > 1.8.9 && < 1.21.8
//import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect

//? if < 1.21.5
//import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo

private const val PROJECTION_NAME = "oneconfig_poly_offscreen"

private const val RELEASE_FRAMES = 2

@ExperimentalOneConfigUiApi
class PolyOffscreen(width: Int, height: Int) : AutoCloseable {
    var width: Int = width.coerceAtLeast(1)
        private set

    var height: Int = height.coerceAtLeast(1)
        private set

    var hasContent: Boolean = false
        private set

    var isClosed: Boolean = false
        private set

    val renderTarget: RenderTarget?
        get() = if (isClosed) null else offscreen.target

    private val offscreen = OffscreenTarget()
    private var pending: (() -> Unit)? = null
    private var presentHook: AutoCloseable? = null

    //? if >= 26.1 {
    private var projection: ProjectionMatrixBuffer? = null
    //?} elif >= 1.21.8
    //private var projection: CachedOrthoProjectionMatrixBuffer? = null

    fun resize(width: Int, height: Int) {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        if (w == this.width && h == this.height) return
        this.width = w
        this.height = h
        hasContent = false
    }

    fun render(draw: () -> Unit) {
        if (isClosed) return
        hasContent = false
        if (!offscreen.resolve(width, height)) return
        val rt = offscreen.target ?: return
        hasContent = drawInto(rt, draw)
    }

    fun renderBeforePresent(draw: () -> Unit) {
        if (isClosed) return
        pending = draw
        if (presentHook == null) presentHook = FramePresentHooks.add(::runPending)
    }

    fun RenderContext.blit(x: Float, y: Float, w: Float, h: Float) {
        if (isClosed || !hasContent) return
        val rt = offscreen.target ?: return
        val textures = renderer as? TextureRenderer ?: return
        textures.texture(rt, x, y, w, h, 0f, 1f, 1f, 0f, 1f)
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        hasContent = false
        pending = null
        presentHook?.close()
        presentHook = null
        val released = offscreen
        //? if >= 1.21.8 {
        val releasedProjection = projection
        projection = null
        //?}
        FramePresentHooks.later(RELEASE_FRAMES) {
            released.dispose()
            //? if >= 1.21.8
            releasedProjection?.close()
        }
    }

    private fun runPending() {
        val draw = pending ?: return
        pending = null
        render(draw)
    }

    //? if >= 26.1 {
    private fun projectionBuffer(): ProjectionMatrixBuffer =
        projection ?: ProjectionMatrixBuffer(PROJECTION_NAME).also { projection = it }
    //?} elif >= 1.21.8 {
    /*private fun projectionBuffer(): CachedOrthoProjectionMatrixBuffer =
        projection ?: CachedOrthoProjectionMatrixBuffer(PROJECTION_NAME, -1000f, 1000f, true).also { projection = it }
    *///?}

    //? if >= 1.21.8 {
    private fun drawInto(rt: RenderTarget, draw: () -> Unit): Boolean {
        //? if >= 1.21.10 {
        val colorTexture = rt.colorTexture ?: return false
        val depthTexture = rt.depthTexture ?: return false
        //?}
        //? if < 26.3 {
        /*val colorView = rt.colorTextureView ?: return false
        val depthView = rt.depthTextureView ?: return false
        *///? }

        val savedLights = RenderSystem.getShaderLights()
        val savedFog = RenderSystem.getShaderFog()

        //? if >= 26.2 {
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(colorTexture, Vector4f(0f, 0f, 0f, 0f), depthTexture, 0.0)
        //?} elif >= 1.21.10 {
        /*RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(colorTexture, 0x00000000, depthTexture, 1.0)
        *///?} else
        //offscreen.clear()

        val w = rt.width.toFloat()
        val h = rt.height.toFloat()
        RenderSystem.backupProjectionMatrix()
        //? if >= 26.2 {
        RenderSystem.setProjectionMatrix(
            projectionBuffer().getBuffer(Projection().apply { setupOrtho(-1000f, 1000f, w, h, true) }),
            ProjectionType.ORTHOGRAPHIC,
        )
        //? } else if >= 26.1 {
        /*RenderSystem.setProjectionMatrix(
            projectionBuffer().getBuffer(Matrix4f().setOrtho(0f, w, h, 0f, -1000f, 1000f)),
            ProjectionType.ORTHOGRAPHIC,
        )
        *///? } else {
        /*RenderSystem.setProjectionMatrix(projectionBuffer().getBuffer(w, h), ProjectionType.ORTHOGRAPHIC)
        *///? }
        //? if < 26.3 {
        /*RenderSystem.outputColorTextureOverride = colorView
        RenderSystem.outputDepthTextureOverride = depthView
        *///? }

        val scissor = RenderSystem.getScissorStateForRenderTypeDraws()
        val hadScissor = scissor.enabled()
        val scissorX = scissor.x()
        val scissorY = scissor.y()
        val scissorW = scissor.width()
        val scissorH = scissor.height()
        RenderSystem.disableScissorForRenderTypeDraws()

        //? if >= 26.2 || < 1.21.10 {
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushMatrix()
        modelView.identity()
        //? }
        try {
            draw()
        } finally {
            //? if >= 26.2 || < 1.21.10 {
            modelView.popMatrix()
            //? }
            //? if < 26.3 {
            /*RenderSystem.outputColorTextureOverride = null
            RenderSystem.outputDepthTextureOverride = null
            *///? }
            RenderSystem.restoreProjectionMatrix()
            savedLights?.let { RenderSystem.setShaderLights(it) }
            savedFog?.let { RenderSystem.setShaderFog(it) }
            if (hadScissor) RenderSystem.enableScissorForRenderTypeDraws(scissorX, scissorY, scissorW, scissorH)
        }
        return true
    }
    //?} else if > 1.8.9 {
    /*private fun drawInto(rt: RenderTarget, draw: () -> Unit): Boolean {
        //? if < 1.21.5
        //val bound = RenderTargetFbo.saveBoundTarget()
        offscreen.clear()

        val previousTarget = GuiTargetRedirect.target
        val ortho = Matrix4f().setOrtho(0f, rt.width.toFloat(), rt.height.toFloat(), 0f, -1000f, 1000f)
        RenderSystem.backupProjectionMatrix()
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushMatrix()
        modelView.identity()
        //? if >= 1.21.4 {
        RenderSystem.setProjectionMatrix(ortho, ProjectionType.ORTHOGRAPHIC)
        //?} else {
        /^RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z)
        RenderSystem.applyModelViewMatrix()
        ^///?}
        GuiTargetRedirect.target = rt
        //? if < 1.21.5
        //rt.bindWrite(true)
        try {
            draw()
        } finally {
            GuiTargetRedirect.target = previousTarget
            modelView.popMatrix()
            //? if < 1.21.4
            //RenderSystem.applyModelViewMatrix()
            RenderSystem.restoreProjectionMatrix()
            //? if < 1.21.5
            //RenderTargetFbo.restoreBoundTarget(bound)
        }
        return true
    }
    *///?} else {
    /*private fun drawInto(rt: RenderTarget, draw: () -> Unit): Boolean {
        val bound = RenderTargetFbo.saveBoundTarget()
        offscreen.clear()

        val depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        rt.bindWrite(true)
        GlStateManager.matrixMode(GL11.GL_PROJECTION)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()
        GlStateManager.ortho(0.0, rt.width.toDouble(), rt.height.toDouble(), 0.0, 1000.0, 3000.0)
        GlStateManager.matrixMode(GL11.GL_MODELVIEW)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()
        GlStateManager.translatef(0f, 0f, -2000f)
        GlStateManager.color4f(1f, 1f, 1f, 1f)
        GlStateManager.enableDepthTest()
        try {
            draw()
        } finally {
            if (!depthWasEnabled) GlStateManager.disableDepthTest()
            GlStateManager.matrixMode(GL11.GL_PROJECTION)
            GlStateManager.popMatrix()
            GlStateManager.matrixMode(GL11.GL_MODELVIEW)
            GlStateManager.popMatrix()
            RenderTargetFbo.restoreBoundTarget(bound)
        }
        return true
    }
    *///?}

    companion object {
        @JvmStatic
        fun onBeforePresent(callback: () -> Unit): AutoCloseable = FramePresentHooks.add(callback)
    }
}
