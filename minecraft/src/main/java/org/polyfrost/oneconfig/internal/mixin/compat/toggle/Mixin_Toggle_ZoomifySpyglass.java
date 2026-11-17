package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

//? if >= 1.21.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class Mixin_Toggle_ZoomifySpyglass {
    @Unique
    private static final ModToggle oneconfig$toggle = ModToggles.handle("zoomify");

    // Zoomify takes the spyglass's own zoom out from a mixin in here to do the zooming itself, which leaves
    // nothing of the mod to hook, so the value vanilla is about to return is handed back ahead of it
    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$vanillaSpyglassZoom(CallbackInfoReturnable<Float> cir) {
        if (oneconfig$toggle.isEnabled()) return;
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() && ((AbstractClientPlayer) (Object) this).isScoping()) {
            cir.setReturnValue(0.1F);
        }
    }
}
//?}
