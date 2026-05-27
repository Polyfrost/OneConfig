package org.polyfrost.oneconfig.api.platform.v1.internal;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.Audiences;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.Component;
import org.polyfrost.oneconfig.api.platform.v1.commands.ClientCommandSource;

public record FabricClientCommandSourceWrapper(FabricClientCommandSource original) implements ClientCommandSource {
    @Override
    public int sendText(Component message) {
        ((Audience) original.getPlayer()).sendMessage(message);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public void sendError(Component message) {
        ((Audience) original.getPlayer()).sendMessage(message);
    }

    @Override
    public int replyError(Exception e) {
        ((Audience) original.getPlayer()).sendMessage(Component.text(e.getMessage()));
        return 1;
    }
}
