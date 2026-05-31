package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Obsolete(since = "1.0.0")
abstract class LegacyHud(id: String, title: String, category: Category) : Hud(id, title, category), LegacyHudMarker {

    abstract val width: Float
    abstract val height: Float

    abstract fun render(mcCtx: GuiGraphicsExtractor)


    @Composable
    override fun Content() {}
}
