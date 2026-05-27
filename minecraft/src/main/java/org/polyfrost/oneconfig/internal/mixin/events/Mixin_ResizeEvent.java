package org.polyfrost.oneconfig.internal.mixin.events;

import org.polyfrost.oneconfig.api.platform.v1.Platform;
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
    public abstract com.mojang.blaze3d.platform.Window getWindow();
    @Inject(method = "resizeDisplay", at = @At("TAIL"))
    private void resizeCallback(CallbackInfo ci) {
        int[] w = new int[1];
        int[] h = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetWindowSize(Platform.compatibility().windowHandle(), w, h);
        EventManager.INSTANCE.post(new ResizeEvent(w[0], h[0]));
    }

}
