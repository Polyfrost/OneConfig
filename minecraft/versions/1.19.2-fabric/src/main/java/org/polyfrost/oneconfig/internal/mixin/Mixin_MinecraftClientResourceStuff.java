package org.polyfrost.oneconfig.internal.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceReload;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class Mixin_MinecraftClientResourceStuff {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V"))
    public void meow(CallbackInfo ci, @Local ResourceReload resourceReload) {
        resourceReload.whenComplete().thenRun(() -> EventManager.INSTANCE.post(ResourceFinishedLoading.INSTANCE));
    }

}
