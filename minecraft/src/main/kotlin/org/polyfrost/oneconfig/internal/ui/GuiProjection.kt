package org.polyfrost.oneconfig.internal.ui

//? if < 1.21.8 {
/*//? if > 1.8.9 {
import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Matrix4f
//?} else {
/*import com.mojang.blaze3d.platform.GlStateManager
import org.lwjgl.opengl.GL11
*///?}

//? if >= 1.21.4 {
import com.mojang.blaze3d.ProjectionType
//?} elif > 1.8.9
//import com.mojang.blaze3d.vertex.VertexSorting

/** Sets up vanilla's GUI projection for drawing GUI elements outside the GUI pass */
internal inline fun withGuiProjection(guiWidth: Int, guiHeight: Int, render: () -> Unit) {
    //? if > 1.8.9 {
    RenderSystem.backupProjectionMatrix()
    val modelView = RenderSystem.getModelViewStack()
    modelView.pushMatrix()
    try {
        val projection = Matrix4f().setOrtho(
            0f,
            guiWidth.toFloat(),
            guiHeight.toFloat(),
            0f,
            1000f,
            21000f,
        )
        //? if >= 1.21.4 {
        RenderSystem.setProjectionMatrix(projection, ProjectionType.ORTHOGRAPHIC)
        //? } else {
        /*RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z)
        *///? }
        modelView.translation(0f, 0f, -11000f)
        //? if < 1.21.4
        //RenderSystem.applyModelViewMatrix()
        render()
    } finally {
        modelView.popMatrix()
        //? if < 1.21.4
        //RenderSystem.applyModelViewMatrix()
        RenderSystem.restoreProjectionMatrix()
    }
    //?} else {
    /*val previousMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE)
    GlStateManager.matrixMode(GL11.GL_PROJECTION)
    GlStateManager.pushMatrix()
    GlStateManager.loadIdentity()
    GlStateManager.ortho(0.0, guiWidth.toDouble(), guiHeight.toDouble(), 0.0, 1000.0, 3000.0)
    GlStateManager.matrixMode(GL11.GL_MODELVIEW)
    GlStateManager.pushMatrix()
    GlStateManager.loadIdentity()
    GlStateManager.translatef(0f, 0f, -2000f)
    try {
        render()
    } finally {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW)
        GlStateManager.popMatrix()
        GlStateManager.matrixMode(GL11.GL_PROJECTION)
        GlStateManager.popMatrix()
        GlStateManager.matrixMode(previousMode)
    }
    *///?}
}
*///?}
