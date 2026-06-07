package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundDucking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class Mixin_MusicDucking {
    @Inject(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("RETURN"), cancellable = true)
    private void oneconfig$duckMusic(float volume, SoundSource source, CallbackInfoReturnable<Float> cir) {
        if (source != SoundSource.MUSIC) return;
        float multiplier = UiSoundDucking.musicVolumeMultiplier();
        if (multiplier < 1f) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}
