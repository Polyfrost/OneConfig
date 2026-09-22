package org.polyfrost.oneconfig.api.commands.v1.factories

import com.mojang.brigadier.tree.LiteralCommandNode
import org.polyfrost.oneconfig.api.commands.v1.ClientCommandSource

fun interface CommandFactory {
    /**
     * @param obj the object to create the command from
     * @return the command or null if this factory cannot create a command from [obj]
     *
     * Ideally this is fail-fast
     */
    fun create(obj: Any): Array<LiteralCommandNode<ClientCommandSource>?>?
}