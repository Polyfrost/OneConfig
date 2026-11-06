package org.polyfrost.oneconfig.internal.mixin.compat;

//? if > 1.8.9 {
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.compat.ConfigEntrypoints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class Mixin_RegisterAddedConfigs {

    @Inject(method = "createWorldOpenFlows", at = @At("HEAD"))
    private void registerBeforeWorldOpens(CallbackInfoReturnable<?> cir) {
        ConfigEntrypoints.registerAdded();
    }

}
//?}
