package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = MoulConfigProcessor.class, remap = false)
public interface Accessor_MoulConfigProcessor<T extends Config> {

    @Accessor("configBaseObject")
    T oneconfig$getConfig();

}
