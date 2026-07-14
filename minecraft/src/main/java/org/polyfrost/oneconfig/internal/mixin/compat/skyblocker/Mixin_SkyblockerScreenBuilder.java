package org.polyfrost.oneconfig.internal.mixin.compat.skyblocker;

//? skyblocker_compat {
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.ScreenBuilder;
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.WidgetManager;
//~ gui_graphics
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.polyfrost.oneconfig.internal.compat.SkyblockerWidgetCompat;
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ScreenBuilder.class)
public class Mixin_SkyblockerScreenBuilder {

    /**
     * While a OneConfig screen is open the widgets are re-drawn above the blur by
     * {@link SkyblockerWidgetCompat}, so suppress Skyblocker's own in-game draw to avoid drawing them twice.
     */
    @Inject(method = "run", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$suppressWhileEditing(GuiGraphicsExtractor graphics, int screenW, int screenH, WidgetManager.ScreenLayer screenLayer, CallbackInfo ci) {
        if (CompatOverlayRenderer.oneConfigScreenOpen() && !SkyblockerWidgetCompat.isRedrawing()) {
            ci.cancel();
        }
    }
}
//? }
