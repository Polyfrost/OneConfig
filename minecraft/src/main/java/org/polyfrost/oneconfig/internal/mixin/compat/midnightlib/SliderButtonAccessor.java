package org.polyfrost.oneconfig.internal.mixin.compat.midnightlib;

//? midnightlib_compat {
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSliderButton.class)
public interface SliderButtonAccessor {
    @Accessor("value")
    double oneconfig$getValue();

    @Invoker("setValue")
    void oneconfig$setValue(double value);
}
//? }
