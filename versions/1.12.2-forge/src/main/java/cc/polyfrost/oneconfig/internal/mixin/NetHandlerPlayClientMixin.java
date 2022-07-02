package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ChatReceiveEvent;
import cc.polyfrost.oneconfig.events.event.SendPacketEvent;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetHandlerPlayClient.class, priority = Integer.MAX_VALUE)
public class NetHandlerPlayClientMixin {

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> p_147297_1_, CallbackInfo ci) {
        SendPacketEvent event = new SendPacketEvent(p_147297_1_);
        EventManager.INSTANCE.post(event);
        if (event.isCancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "handleChat", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/ForgeEventFactory;onClientChat(Lnet/minecraft/util/text/ChatType;Lnet/minecraft/util/text/ITextComponent;)Lnet/minecraft/util/text/ITextComponent;", remap = false), cancellable = true, remap = true)
    private void onClientChat(SPacketChat packetIn, CallbackInfo ci) {
        if (packetIn.getType().getId() == 0) {
            ChatReceiveEvent event = new ChatReceiveEvent(packetIn.getChatComponent());
            EventManager.INSTANCE.post(event);
            if (event.isCancelled) {
                ci.cancel();
            }
        }
    }
}