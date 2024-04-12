package org.polyfrost.oneconfig.internal.commands;

import com.mojang.brigadier.CommandDispatcher;
import org.polyfrost.oneconfig.api.events.event.Event;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandSource;

public class RegisterCommandsEvent implements Event {
    public final CommandDispatcher<ClientCommandSource> dispatcher;

    public RegisterCommandsEvent(CommandDispatcher<ClientCommandSource> dispatcher) {
        this.dispatcher = dispatcher;
    }
}
