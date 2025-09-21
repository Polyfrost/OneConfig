package org.polyfrost.oneconfig.internal.mixin.events;

//#if MC <= 1.8.9
import net.minecraft.client.audio.SoundEventAccessorComposite;
import org.polyfrost.oneconfig.internal.mixin.Mixin_SoundHandlerAccessor;
//#endif

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.SoundPlayedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SoundManager.class)
public class Mixin_SoundPlayedEvent {

    //#if MC <= 1.12.2
    @Shadow
    private boolean loaded;
    //#endif

    @ModifyVariable(
            //#if MC >= 1.21.8
            //$$ method = "play",
            //#else
            method = "playSound",
            //#endif
            at = @At("HEAD"),
            argsOnly = true
    )
    private ISound onPlaySoundCallback(ISound value) {
        //#if MC <= 1.12.2
        if (!this.loaded) {
            return value;
        }
        //#endif

        //#if MC <= 1.8.9
        SoundEventAccessorComposite accessor = ((Mixin_SoundHandlerAccessor) this).getSndHandler().getSound(value.getSoundLocation());
        SoundCategory category = (accessor == null ? null : accessor.getSoundCategory());
        //#else
        //$$ SoundCategory category = value.getCategory();
        //#endif

        String name = value.getSoundLocation().getResourcePath();
        SoundPlayedEvent event = new SoundPlayedEvent(name, category, value);
        EventManager.INSTANCE.post(event);

        return event.getSound();
    }

}
