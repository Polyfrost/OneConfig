package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ChatReceiveEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = Integer.MAX_VALUE)
public class NetHandlerPlayClientMixin {

    @Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;addChatMessage(Lnet/minecraft/network/MessageType;Lnet/minecraft/text/Text;Ljava/util/UUID;)V"), cancellable = true)
    private void onClientChat(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet.getLocation().getId() == 0) {
            ChatReceiveEvent event = new ChatReceiveEvent(packet.getMessage());
            EventManager.INSTANCE.post(event);
            if (event.isCancelled) {
                ci.cancel();
            }
        }
    }
}