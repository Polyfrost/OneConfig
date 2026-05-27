package org.polyfrost.oneconfig.api.platform.v1.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;

public interface CommandPlatform {

    public void register(LiteralCommandNode<ClientCommandSource> command);

}
