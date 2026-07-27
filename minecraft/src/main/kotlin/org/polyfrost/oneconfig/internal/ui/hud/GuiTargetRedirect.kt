//? if >= 1.21.5 {
package org.polyfrost.oneconfig.internal.ui.hud

import com.mojang.blaze3d.pipeline.RenderTarget

object GuiTargetRedirect {
    @Volatile
    @JvmField
    var target: RenderTarget? = null
}
//? }
