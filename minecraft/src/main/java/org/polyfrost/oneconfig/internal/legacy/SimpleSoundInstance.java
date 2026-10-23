package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import net.minecraft.client.sound.instance.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resource.Identifier;

public final class SimpleSoundInstance extends AbstractSoundInstance {
    private SimpleSoundInstance(Identifier event, float pitch, float volume) {
        super(event);
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.pitch = pitch;
        this.volume = volume;
    }

    public static SimpleSoundInstance forUI(Identifier event, float pitch, float volume) {
        return new SimpleSoundInstance(event, pitch, volume);
    }
}
*///?}
