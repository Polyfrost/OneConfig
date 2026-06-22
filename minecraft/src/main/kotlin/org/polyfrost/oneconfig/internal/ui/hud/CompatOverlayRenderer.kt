package org.polyfrost.oneconfig.internal.ui.hud

//? if > 1.8.9
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

object CompatOverlayRenderer {
    private val LOG = LoggerFactory.getLogger("OneConfig/CompatOverlayRenderer")
    //~ if = 1.8.9 '<(GuiGraphicsExtractor)' -> '<()'
    private val hooks = CopyOnWriteArrayList<(GuiGraphicsExtractor) -> Unit>()

    @JvmStatic
    //~ if = 1.8.9 'hook: (GuiGraphicsExtractor)' -> 'hook: ()'
    fun register(hook: (GuiGraphicsExtractor) -> Unit) {
        hooks.add(hook)
    }

    @JvmStatic
    fun hasHooks(): Boolean = hooks.isNotEmpty()

    @JvmStatic
    fun oneConfigScreenOpen(): Boolean = HudManager.isEditing || SkiaCtx.suppressInGameHudRender

    @JvmStatic
    //~ if = 1.8.9 '(ctx: GuiGraphicsExtractor)' -> '()'
    fun render(ctx: GuiGraphicsExtractor) {
        if (hooks.isEmpty()) return
        for (hook in hooks) {
            try {
                //~ if = 1.8.9 'hook(ctx)' -> 'hook()'
                hook(ctx)
            } catch (t: Throwable) {
                LOG.debug("compat overlay hook failed", t)
            }
        }
    }
}
