package org.polyfrost.oneconfig.internal.mixin.render;

//? if = 1.8.9 {
/*import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.polyfrost.oneconfig.internal.LegacyVanillaHudSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class Mixin_VanillaHudFootprintLegacyItemRenderer {
    @Inject(method = "renderGuiItemModel", at = @At("HEAD"), cancellable = true)
    private void oneconfig$measureItem(ItemStack item, int x, int y, CallbackInfo ci) {
        if (LegacyVanillaHudSampler.isMeasuring() && LegacyVanillaHudSampler.record(x, y, x + 16, y + 16)) ci.cancel();
    }
}
*///?}
