package org.polyfrost.oneconfig.api.platform.v1.internal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.Commands;
import org.polyfrost.oneconfig.api.platform.v1.commands.ClientCommandSource;
import org.polyfrost.oneconfig.api.platform.v1.commands.CommandPlatform;

import java.util.ArrayList;
import java.util.List;

public class CommandPlatformImpl implements CommandPlatform {
    private List<LiteralCommandNode<ClientCommandSource>> commands = new ArrayList<>();

    public CommandPlatformImpl() {
        ClientCommandRegistrationCallback.EVENT.register(this::register);
    }

    private void register(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandBuildContext context
    ) {
        for (var command : commands) {
            // todo
        }
    }

    @Override
    public void register(LiteralCommandNode<ClientCommandSource> command) {
        commands.add(command);
    }
}
