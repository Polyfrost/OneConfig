package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorSlider;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;
import org.polyfrost.oneconfig.internal.compat.GuiOptionEditorSliderAccessor;
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;
import org.polyfrost.oneconfig.relocator.annotations.RelocatedMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@MoulConfig
//#if MC==1.8.9||MC>=1.21.4
@RelocatedMixin
//#endif
@Mixin(value = GuiOptionEditorSlider.class, remap = false)
public class Mixin_GuiOptionEditorSlider implements GuiOptionEditorSliderAccessor {

    @Unique
    private float oneconfig$minValue;
    @Unique
    private float oneconfig$maxValue;
    @Unique
    private float oneconfig$minStep;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void meow(ProcessedOption option, float minValue, float maxValue, float minStep, CallbackInfo ci) {
        this.oneconfig$minValue = minValue;
        this.oneconfig$maxValue = maxValue;
        this.oneconfig$minStep = minStep;
    }

    @Override
    public float getOneconfig$minValue() {
        return oneconfig$minValue;
    }

    @Override
    public float getOneconfig$maxValue() {
        return oneconfig$maxValue;
    }

    @Override
    public float getOneconfig$minStep() {
        return oneconfig$minStep;
    }
}
