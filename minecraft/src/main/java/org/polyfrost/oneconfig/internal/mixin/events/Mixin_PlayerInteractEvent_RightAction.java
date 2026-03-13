package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_PlayerInteractEvent_RightAction {
    @Shadow public HitResult hitResult;
    @Shadow public LocalPlayer player;

    @Inject(
            method = "startUseItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPlayerInteractCallback(CallbackInfo ci) {
        HitResult rayCastedObject = this.hitResult;
        PlayerInteractEvent.Type type = PlayerInteractEvent.Type.AIR;
        if (rayCastedObject != null) {
            switch (rayCastedObject.getType()) {
                case BLOCK:
                    type = PlayerInteractEvent.Type.BLOCK;
                    break;
                case ENTITY:
                    type = PlayerInteractEvent.Type.ENTITY;
                    break;
                default:
                    break;
            }
        }

        PlayerInteractEvent event = new PlayerInteractEvent(this.player, PlayerInteractEvent.Action.RIGHT, type);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
