package org.polyfrost.oneconfig.internal.mixin.compat;

//#if FORGE && MC < 1.13
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.deftu.omnicore.client.OmniKeyboard;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "cc.polyfrost.oneconfig.internal.config.core.KeyBindHandler", remap = false)
public class Mixin_OverwriteLegacyKeyBind {

    @Dynamic
    @WrapWithCondition(method = "onKeyPressed", at = @At(value = "INVOKE", target = "Lcc/polyfrost/oneconfig/config/core/OneKeyBind;run()V"), remap = false)
    private static boolean onKeyPressed(OneKeyBind instance) {
        return !instance.getKeyBinds().contains(OmniKeyboard.KEY_RSHIFT);
    }

}
//#endif
