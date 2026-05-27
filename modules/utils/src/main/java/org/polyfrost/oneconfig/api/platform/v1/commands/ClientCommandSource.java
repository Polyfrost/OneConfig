package org.polyfrost.oneconfig.api.platform.v1.commands;

import com.mojang.brigadier.Command;
import net.kyori.adventure.text.Component;
import org.polyfrost.oneconfig.api.platform.v1.Platform;

public interface ClientCommandSource {

    int sendText(Component message);
    default int sendText(String message) {
        return sendText(Component.text(message));
    }
    void sendError(Component message);

    int replyError(Exception e);

    default int openScreen(Object screen) {
        Platform.screen().display(screen);
        return Command.SINGLE_SUCCESS;
    }
}
