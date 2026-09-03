package org.polyfrost.oneconfig.internal.mixin.compat.skyblocker;

//? skyblocker_hud_v2 {
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.LayerBuilder;
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
@Mixin(LayerBuilder.class)
public class Mixin_SkyblockerLayerBuilder {

    @Inject(method = "extractRenderStates", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$suppressWhileEditing(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, boolean config, CallbackInfo ci) {
        if (CompatOverlayRenderer.oneConfigScreenOpen() && !SkyblockerWidgetCompat.isRedrawing()) {
            ci.cancel();
        }
    }
}
//? }
