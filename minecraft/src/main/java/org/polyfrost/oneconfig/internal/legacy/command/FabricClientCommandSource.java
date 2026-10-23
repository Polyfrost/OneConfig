package org.polyfrost.oneconfig.internal.legacy.command;

//? if = 1.8.9 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public interface FabricClientCommandSource {
    default Minecraft getClient() {
        return Minecraft.getInstance();
    }

    default void sendFeedback(Component message) {
        getClient().gui.getChat().addMessage(message);
    }

    default void sendError(Component message) {
        sendFeedback(Component.empty().append(message).withStyle(ChatFormatting.RED));
    }
}
*///?}
