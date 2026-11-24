package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "tektonikal.customblockhighlight.CustomBlockHighlight", remap = false)
public class Mixin_Toggle_BlockHighlightOutline {
    @Unique
    private static final ModToggle oneconfig$toggle = ModToggles.handle("custom-block-highlight");

    @Inject(method = {"lambda$onInitialize$0", "lambda$onInitialize$2"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$vanillaOutline(CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$toggle.isEnabled()) cir.setReturnValue(true);
    }
}
