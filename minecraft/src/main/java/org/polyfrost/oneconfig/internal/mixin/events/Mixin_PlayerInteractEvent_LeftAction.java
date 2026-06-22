package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if > 1.8.9
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class Mixin_PlayerInteractEvent_LeftAction {

    @Shadow public HitResult hitResult;
    @Shadow public LocalPlayer player;

    @Unique private PlayerInteractEvent ocfg$lastAttackEvent;

    @Inject(
            //~ if = 1.8.9 'startAttack' -> 'doAttack'
            method = "startAttack",
            at = @At("HEAD"),
            cancellable = true
    )
    //~ if = 1.8.9 'CallbackInfoReturnable<Boolean> cir' -> 'CallbackInfo ci'
    private void onPlayerAttackCallback(CallbackInfoReturnable<Boolean> cir) {
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

        ocfg$lastAttackEvent = new PlayerInteractEvent(this.player, PlayerInteractEvent.Action.LEFT, type);
        EventManager.INSTANCE.post(ocfg$lastAttackEvent);
        if (ocfg$lastAttackEvent.cancelled) {
            //$ if > 1.8.9 'cir.cancel();' else 'ci.cancel();'
            cir.cancel();
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void onBlockClickPacketCallback(boolean leftClick, CallbackInfo ci) {
        if (ocfg$lastAttackEvent != null && ocfg$lastAttackEvent.getAction() == PlayerInteractEvent.Action.LEFT && ocfg$lastAttackEvent.cancelled) {
            ci.cancel();
        }

        if (!leftClick) {
            ocfg$lastAttackEvent = null;
        }
    }

}
