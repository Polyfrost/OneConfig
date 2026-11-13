@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
//?}

//? if >= 1.21.11 {
import org.polyfrost.oneconfig.internal.ui.compose.UiBlur
//?} else {
/*import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.polyfrost.oneconfig.internal.ui.UiErrors
import org.polyfrost.oneconfig.internal.ui.compose.GlBlur
import org.polyfrost.oneconfig.internal.ui.compose.GlHost
import org.polyfrost.oneconfig.internal.ui.compose.opengl.StoredGLState
*///?}

@ExperimentalOneConfigUiApi
object ScreenBlur {
    //? if < 1.21.11
    //private val gl = StoredGLState(330)

    //? if > 1.8.9 {
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun draw(graphics: GuiGraphicsExtractor, alpha: Float = 1f) {
    //?} else {
    /*@JvmStatic
    @JvmOverloads
    fun draw(alpha: Float = 1f) {
    *///?}
        val strength = alpha.coerceIn(0f, 1f)
        if (strength <= 0f) return
        //? if >= 1.21.11 {
        UiBlur.extract(graphics, strength)
        //?} else {
        /*if (!GlHost.legacyGl || GlHost.vanillaPipeline) return
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return
        val client = Minecraft.getInstance()
        gl.capture()
        try {
            GlBlur.draw(RenderTargetFbo.getFboId(client.mainRenderTarget), w, h, strength, client.isPaused)
        } catch (t: Throwable) {
            UiErrors.warn("ScreenBlur.draw() error", t)
        } finally {
            gl.restore()
        }
        *///?}
    }
}
