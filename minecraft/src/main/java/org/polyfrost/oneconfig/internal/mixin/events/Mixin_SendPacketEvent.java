package org.polyfrost.oneconfig.internal.mixin.events;
//
//import io.netty.util.concurrent.Future;
//import io.netty.util.concurrent.GenericFutureListener;
//import net.minecraft.network.protocol.Packet;
//import org.polyfrost.oneconfig.api.event.v1.EventManager;
//import org.polyfrost.oneconfig.api.event.v1.events.PacketEvent;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(NetworkManager.class)
//public class Mixin_SendPacketEvent {
//
//    //@formatter:off
//    //#if MC < 1.19
//    //#if MC <= 1.12.2
//    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;[Lio/netty/util/concurrent/GenericFutureListener;)V", at = @At("HEAD"), cancellable = true)
//    private void packetSendCallbackGeneric(Packet<?> packetIn, GenericFutureListener<? extends Future<? super Void>> listener, GenericFutureListener<? extends Future<? super Void>>[] listeners, CallbackInfo ci) {
//    //#else
//    //$$ @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
//    //$$ private void packetCallback(Packet<?> packetIn, GenericFutureListener<? extends Future<? super Void>> genericFutureListener, CallbackInfo ci) {
//    //#endif
//        packetSendCallback(packetIn, ci);
//    }
//    //#endif
//    //@formatter:on
//
//    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
//    private void packetSendCallback(Packet<?> packetIn, CallbackInfo ci) {
//        PacketEvent.Send event = new PacketEvent.Send(packetIn);
//        EventManager.INSTANCE.post(event);
//        if (event.cancelled) {
//            ci.cancel();
//        }
//    }
//
//}
