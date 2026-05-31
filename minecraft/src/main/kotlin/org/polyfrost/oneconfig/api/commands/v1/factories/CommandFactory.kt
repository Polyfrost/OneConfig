package org.polyfrost.oneconfig.api.commands.v1.factories

import com.mojang.brigadier.tree.LiteralCommandNode
import org.polyfrost.oneconfig.api.commands.v1.ClientCommandSource

fun interface CommandFactory {
    /**
     * Create a command from the given object.
     *
     * @param obj the object to create the command from
     * @return the command, or null if this factory cannot create a command from the given object. Ideally this is fail-fast.
     */
    fun create(obj: Any): Array<LiteralCommandNode<ClientCommandSource>?>?
}