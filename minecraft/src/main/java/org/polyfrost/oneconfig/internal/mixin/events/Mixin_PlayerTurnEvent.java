package org.polyfrost.oneconfig.internal.mixin.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.PlayerTurnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if > 1.8.9 {
import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public class Mixin_PlayerTurnEvent {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void ocfg$onTurn(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        if ((yaw != 0.0 || pitch != 0.0) && EventManager.INSTANCE.hasListeners(PlayerTurnEvent.class)) {
            PlayerTurnEvent event = new PlayerTurnEvent(yaw * 0.15, pitch * 0.15);
            EventManager.INSTANCE.post(event);
            if (event.cancelled) return;
        }
        original.call(player, yaw, pitch);
    }
}
//?} else {
/*import net.minecraft.client.render.GameRenderer;

@Mixin(GameRenderer.class)
public class Mixin_PlayerTurnEvent {
    @WrapOperation(method = "render(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;updateLocalPlayerCamera(FF)V"))
    private void ocfg$onTurn(LocalPlayer player, float yaw, float pitch, Operation<Void> original) {
        if ((yaw != 0.0F || pitch != 0.0F) && EventManager.INSTANCE.hasListeners(PlayerTurnEvent.class)) {
            PlayerTurnEvent event = new PlayerTurnEvent(yaw * 0.15, -pitch * 0.15);
            EventManager.INSTANCE.post(event);
            if (event.cancelled) return;
        }
        original.call(player, yaw, pitch);
    }
}
*///?}
