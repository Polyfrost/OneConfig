package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

//? moul_compat {

import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.ConfigStructureReader;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import org.polyfrost.oneconfig.internal.compat.MoulConfigCompat;
import org.polyfrost.oneconfig.internal.utils.MoulConfigProcessorAccessor;
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;
import org.polyfrost.oneconfig.relocator.annotations.RelocatedMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@MoulConfig
@RelocatedMixin
@Mixin(value = ConfigProcessorDriver.class, remap = false)
public class Mixin_ConfigProcessorDriver {

    @Shadow
    @Final
    public ConfigStructureReader reader;

    @Inject(at = @At("TAIL"), method = "processConfig", require = 0)
    public void processorEndConfig(CallbackInfo ci) {
        if (reader instanceof MoulConfigProcessor<?> && reader instanceof MoulConfigProcessorAccessor) {
            MoulConfigCompat.parseMoulconfig(
                    (MoulConfigProcessor<?>) reader,
                    ((MoulConfigProcessorAccessor<?>) reader).oneconfig$getConfig());
        }
    }
}
//? }