//#if MC==10809
package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ChatReceiveEvent;
import cc.polyfrost.oneconfig.events.event.SendPacketEvent;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetHandlerPlayClient.class, priority = Integer.MAX_VALUE)
public class NetHandlerPlayClientMixin {

    @Inject(method = "handleChat", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/ForgeEventFactory;onClientChat(BLnet/minecraft/util/IChatComponent;)Lnet/minecraft/util/IChatComponent;", remap = false), cancellable = true, remap = true)
    private void onClientChat(S02PacketChat packetIn, CallbackInfo ci) {
        if (packetIn.getType() == 0) {
            ChatReceiveEvent event = new ChatReceiveEvent(packetIn.getChatComponent());
            EventManager.INSTANCE.post(event);
            if (event.isCancelled) {
                ci.cancel();
            }
        }
    }
}
//#endif