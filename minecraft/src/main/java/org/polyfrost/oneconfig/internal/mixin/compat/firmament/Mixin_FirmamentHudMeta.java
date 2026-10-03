package org.polyfrost.oneconfig.internal.mixin.compat.firmament;

//? if > 1.8.9 {
import org.polyfrost.oneconfig.internal.compat.FirmamentHudCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "moe.nea.firmament.gui.config.HudMeta", remap = false)
public class Mixin_FirmamentHudMeta {

    @Inject(method = "applyTransformations(Lorg/joml/Matrix3x2f;)V", at = @At("HEAD"), require = 0)
    private void oneconfig$noteRendered(CallbackInfo ci) {
        FirmamentHudCompat.noteHudRendered(this);
    }
}
//?}
