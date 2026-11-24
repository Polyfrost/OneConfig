package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sound.instance.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resource.Identifier;

public final class SimpleSoundInstance extends AbstractSoundInstance {
    private static final double LISTENER_TICK_DELTA = 0.5;

    private static float listenerX;
    private static float listenerY;
    private static float listenerZ;

    private SimpleSoundInstance(Identifier event, float pitch, float volume) {
        super(event);
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.pitch = pitch;
        this.volume = volume;
        float[] listener = listenerPosition();
        this.x = listener[0];
        this.y = listener[1];
        this.z = listener[2];
    }

    public static SimpleSoundInstance forUI(Identifier event, float pitch, float volume) {
        return new SimpleSoundInstance(event, pitch, volume);
    }

    public static float[] listenerPosition() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            listenerX = (float) (player.lastX + (player.x - player.lastX) * LISTENER_TICK_DELTA);
            listenerY = (float) (player.lastY + (player.y - player.lastY) * LISTENER_TICK_DELTA + (double) player.getEyeHeight());
            listenerZ = (float) (player.lastZ + (player.z - player.lastZ) * LISTENER_TICK_DELTA);
        }
        return new float[]{listenerX, listenerY, listenerZ};
    }
}
*///?}
