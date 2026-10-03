package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
//? if = 1.8.9 {
/*import net.minecraft.client.sound.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
*///?}
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundDucking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class Mixin_MusicDucking {
    //~ if = 1.8.9 'calculateVolume(FLnet/minecraft/sounds/SoundSource;)F' -> 'getVolume(Lnet/minecraft/client/sound/instance/SoundInstance;Lnet/minecraft/client/sound/Sound;Lnet/minecraft/client/sound/SoundCategory;)F'
    @Inject(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("RETURN"), cancellable = true)
    //~ if = 1.8.9 '(float volume, SoundSource source' -> '(SoundInstance instance, Sound sound, SoundCategory source'
    private void oneconfig$duckMusic(float volume, SoundSource source, CallbackInfoReturnable<Float> cir) {
        //~ if = 1.8.9 'SoundSource.MUSIC' -> 'SoundCategory.MUSIC'
        if (source != SoundSource.MUSIC) return;
        float multiplier = UiSoundDucking.musicVolumeMultiplier();
        if (multiplier < 1f) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}
