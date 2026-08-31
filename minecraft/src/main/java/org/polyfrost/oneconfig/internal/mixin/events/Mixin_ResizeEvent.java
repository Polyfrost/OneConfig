package org.polyfrost.oneconfig.internal.mixin.events;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ResizeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class Mixin_ResizeEvent {

    @Shadow
    public abstract Window getWindow();
    //~ if >= 26.1 'resizeDisplay' -> 'resizeGui'
    @Inject(method = "resizeGui", at = @At("TAIL"))
    private void resizeCallback(CallbackInfo ci) {
        Window window = getWindow();
        EventManager.INSTANCE.post(new ResizeEvent(window.getScreenWidth(), window.getScreenHeight()));
    }

}
