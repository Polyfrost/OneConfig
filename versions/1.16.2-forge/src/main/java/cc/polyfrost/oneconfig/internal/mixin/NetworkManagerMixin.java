package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ReceivePacketEvent;
import cc.polyfrost.oneconfig.events.event.SendPacketEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkManager.class, priority = Integer.MAX_VALUE)
public class NetworkManagerMixin {

    @Inject(method = "sendPacket(Lnet/minecraft/network/IPacket;Lio/netty/util/concurrent/GenericFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(IPacket<?> packetIn, GenericFutureListener<? extends Future<? super Void>> genericFutureListener, CallbackInfo ci) {
        onSendPacket(packetIn, ci);
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/IPacket;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(IPacket<?> packetIn, CallbackInfo ci) {
        SendPacketEvent event = new SendPacketEvent(packetIn);
        EventManager.INSTANCE.post(event);
        if (event.isCancelled) {
            ci.cancel();
        }
    }


    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/IPacket;)V", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext channelHandlerContext, IPacket<?> arg, CallbackInfo ci) {
        ReceivePacketEvent event = new ReceivePacketEvent(arg);
        EventManager.INSTANCE.post(event);
        if (event.isCancelled) {
            ci.cancel();
        }
    }
}