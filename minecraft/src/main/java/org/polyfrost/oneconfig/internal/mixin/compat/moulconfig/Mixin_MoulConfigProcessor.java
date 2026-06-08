package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

//? moul_compat {

/*import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import org.polyfrost.oneconfig.internal.utils.MoulConfigProcessorAccessor;
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;
import org.polyfrost.oneconfig.relocator.annotations.RelocatedMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@MoulConfig
@RelocatedMixin
@Mixin(value = MoulConfigProcessor.class, remap = false)
public class Mixin_MoulConfigProcessor<T extends Config> implements MoulConfigProcessorAccessor<T> {

    @Final
    @Shadow
    private T configBaseObject;

    public T oneconfig$getConfig() {
        return this.configBaseObject;
    }

}
*///? }
