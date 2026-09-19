package org.polyfrost.oneconfig.internal.ui.hud

import com.mojang.blaze3d.pipeline.RenderTarget

object GuiTargetRedirect {
    fun interface ScissorTransform {
        fun map(left: Int, top: Int, right: Int, bottom: Int): IntArray
    }

    @Volatile
    @JvmField
    var target: RenderTarget? = null

    @Volatile
    @JvmField
    var scissorTransform: ScissorTransform? = null

    @Volatile
    @JvmField
    var itemRenderSizePx: Int = 0
}
