package org.polyfrost.oneconfig.internal.mixin.events;

import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.AttackEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if > 1.8.9 {
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
//?} else {
/*import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
*///?}

//? if > 1.8.9 {
@Mixin(MultiPlayerGameMode.class)
public class Mixin_AttackEntityEvent {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void ocfg$onAttack(Player player, Entity target, CallbackInfo ci) {
        AttackEntityEvent event = new AttackEntityEvent(player, target);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
//?} else {
/*@Mixin(ClientPlayerInteractionManager.class)
public class Mixin_AttackEntityEvent {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void ocfg$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEntityEvent event = new AttackEntityEvent(player, target);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
*///?}
