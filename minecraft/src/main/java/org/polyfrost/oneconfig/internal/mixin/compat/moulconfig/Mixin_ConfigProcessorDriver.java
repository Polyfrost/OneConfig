package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

import io.github.notenoughupdates.moulconfig.processor.*;
import org.polyfrost.oneconfig.utils.v1.internal.compat.MoulConfigCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = ConfigProcessorDriver.class, remap = false)
public class Mixin_ConfigProcessorDriver {

    @Shadow
    @Final
    public ConfigStructureReader reader;

    @Inject(at = @At("TAIL"), method = "processConfig")
    public void processorEndConfig(CallbackInfo ci) {
        if (reader instanceof MoulConfigProcessor<?> && reader instanceof Accessor_MoulConfigProcessor) {
            MoulConfigCompat.parseMoulconfig((MoulConfigProcessor<?>) reader, ((Accessor_MoulConfigProcessor<?>) reader).oneconfig$getConfig());
        }
    }
}
