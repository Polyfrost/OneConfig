package org.polyfrost.oneconfig.api.platform.v1.internal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import org.polyfrost.oneconfig.api.platform.v1.commands.ClientCommandSource;
import org.polyfrost.oneconfig.api.platform.v1.commands.CommandPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

        }
    }

    private <Source> void map(CommandNode<ClientCommandSource> original, ArgumentBuilder<Source, ?> mapped, Function<Source, ClientCommandSource> mapper) {

        mapped.executes(context -> {
            return 1;
        });


    }

    private <Source> LiteralCommandNode<Source> map(
            LiteralCommandNode<ClientCommandSource> original,
            Function<Source, ClientCommandSource> mapper
    ) {
        var mapped = LiteralArgumentBuilder.<Source>literal(original.getLiteral());

        return mapped.build();
    }

    private <Source, Argument> ArgumentCommandNode<Source, Argument> map(
            RequiredArgumentBuilder<ClientCommandSource, Argument> original,
            Function<Source, ClientCommandSource> mapper
    ) {
        var mapped = RequiredArgumentBuilder.<Source, Argument>argument(original.getName(), original.getType());


        return mapped.build();
    }

    @Override
    public void register(LiteralCommandNode<ClientCommandSource> command) {
        commands.add(command);
    }
}
