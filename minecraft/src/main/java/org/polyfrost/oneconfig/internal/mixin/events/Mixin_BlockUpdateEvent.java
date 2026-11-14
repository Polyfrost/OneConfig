package org.polyfrost.oneconfig.internal.mixin.events;

import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.BlockUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if > 1.8.9 {
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;

@Mixin(ClientPacketListener.class)
public class Mixin_BlockUpdateEvent {
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void ocfg$onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        if (!EventManager.INSTANCE.hasListeners(BlockUpdateEvent.class)) return;
        EventManager.INSTANCE.post(new BlockUpdateEvent(packet.getPos(), packet.getBlockState()));
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void ocfg$onSectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        if (!EventManager.INSTANCE.hasListeners(BlockUpdateEvent.class)) return;
        packet.runUpdates((pos, state) -> EventManager.INSTANCE.post(new BlockUpdateEvent(pos.immutable(), state)));
    }
}
//?} else {
/*import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlocksUpdateS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public class Mixin_BlockUpdateEvent {
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void ocfg$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        if (!EventManager.INSTANCE.hasListeners(BlockUpdateEvent.class)) return;
        EventManager.INSTANCE.post(new BlockUpdateEvent(packet.getPos(), packet.getBlockState()));
    }

    @Inject(method = "handleBlocksUpdate", at = @At("TAIL"))
    private void ocfg$onSectionBlocksUpdate(BlocksUpdateS2CPacket packet, CallbackInfo ci) {
        if (!EventManager.INSTANCE.hasListeners(BlockUpdateEvent.class)) return;
        for (BlocksUpdateS2CPacket.BlockUpdate update : packet.getUpdates()) {
            EventManager.INSTANCE.post(new BlockUpdateEvent(update.getBlockPos(), update.getBlockState()));
        }
    }
}
*///?}
