package org.polyfrost.oneconfig.internal.mixin.compat.cloth;

//? clothconfig_compat {
import net.minecraft.client.gui.screens.Screen;
import org.polyfrost.oneconfig.internal.compat.ClothConfigCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.shedaniel.clothconfig2.impl.ConfigBuilderImpl", remap = false)
public class Mixin_ConfigBuilderImpl {

    @Inject(method = "build", at = @At("HEAD"))
    private void oneconfig$onBuild(CallbackInfoReturnable<Screen> cir) {
        try {
            ClothConfigCompat.parseClothBuilder(this);
        } catch (Throwable ignored) {
        }
    }
}
//? }
