package org.polyfrost.oneconfig.internal.mixin.render;

//? if = 1.8.9 {
/*import net.minecraft.client.render.TextRenderer;
import org.polyfrost.oneconfig.internal.LegacyVanillaHudSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderer.class)
public abstract class Mixin_VanillaHudFootprintLegacyTextRenderer {
    @Inject(method = "draw(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void oneconfig$measureText(String text, float x, float y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (text == null || !LegacyVanillaHudSampler.isMeasuring()) return;
        int width = ((TextRenderer) (Object) this).width(text);
        if (LegacyVanillaHudSampler.record(x, y, x + width, y + 9)) cir.setReturnValue((int) x + width);
    }
}
*///?}
