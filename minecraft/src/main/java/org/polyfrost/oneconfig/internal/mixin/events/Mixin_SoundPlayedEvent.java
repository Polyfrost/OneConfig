package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.options.SoundCategory;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.SoundPlayedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SoundManager.class)
public class Mixin_SoundPlayedEvent {
    @ModifyVariable(
            //#if MC >= 1.21.8
            //$$ method = "play",
            //#else
            method = "play",
            //#endif
            at = @At("HEAD"),
            argsOnly = true
    )
    private SoundInstance onPlaySoundCallback(SoundInstance value) {
        if (value != null && value.getSound() != null) {
            String name = value.getSound().getLocation().toString();
            SoundPlayedEvent event = new SoundPlayedEvent(name, null, value);
            EventManager.INSTANCE.post(event);
        }

        return value;
    }

}
