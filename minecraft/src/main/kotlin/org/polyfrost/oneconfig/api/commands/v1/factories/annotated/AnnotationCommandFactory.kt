package org.polyfrost.oneconfig.api.commands.v1.factories.annotated

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.polyfrost.oneconfig.api.commands.v1.ClientCommandSource
import org.polyfrost.oneconfig.api.commands.v1.CommandManager
import org.polyfrost.oneconfig.api.commands.v1.CommandManager.argument
import org.polyfrost.oneconfig.api.commands.v1.CommandManager.getArgumentType
import org.polyfrost.oneconfig.api.commands.v1.factories.CommandFactory
import org.polyfrost.oneconfig.utils.v1.MHUtils
import kotlin.math.max


class AnnotationCommandFactory : CommandFactory {
    override fun create(obj: Any): Array<LiteralCommandNode<ClientCommandSource>?>? {
        val cls: Class<*> = obj.javaClass
        val command = cls.getAnnotation(Command::class.java) ?: return null

        val help = StringBuilder()
        val primaryName = if (command.value.isEmpty()) cls.getSimpleName() else command.value[0]
        help.append("Help for /").append(primaryName)
        if (command.value.isNotEmpty()) {
            help.append(" (")
            val aliases: Array<String> = command.value
            for (i in aliases.indices) {
                help.append(aliases[i])
                if (i < aliases.size - 1) {
                    help.append(", ")
                }
            }

            help.append(')')
        }

        val builder: LiteralArgumentBuilder<ClientCommandSource> = CommandManager.literal(primaryName)
        setup(true, builder, obj, help, "  /$primaryName")

        val nodes: Array<LiteralCommandNode<ClientCommandSource>?> = arrayOfNulls(max(1, command.value.size + 1))

        builder.then(
            CommandManager.literal("help").executes { ctx: CommandContext<ClientCommandSource> ->
                ctx.getSource().sendFeedback(Component.literal(help.toString()))
                return@executes 1
            }
        )

        nodes[0] = builder.build()
        for (i in 1..<command.value.size) {
            nodes[i] = CommandManager.literal(command.value[i]).redirect(nodes[0]).build()
        }

        return nodes
    }

    private fun setup(
        isRoot: Boolean,
        tree: LiteralArgumentBuilder<ClientCommandSource>,
        obj: Any,
        help: StringBuilder,
        stem: String?,
    ) {
        // deferred so the help header is only emitted once a subcommand actually exists
        var isRootFormattingSetup = false

        // the root command is the tree itself rather than a subcommand so it can only be defined once
        var isRootCommandSetup = false

        for (clz in obj.javaClass.declaredClasses) {
            val command =
                clz.getAnnotation<Command?>(Command::class.java) ?: continue

            val aliases: Array<String> = command.value
            val primaryName: String = (if (aliases.isEmpty()) clz.getSimpleName() else aliases[0])!!

            val subTree: LiteralArgumentBuilder<ClientCommandSource> = CommandManager.literal(primaryName)
            setup(false, subTree, MHUtils.instantiate(clz, true), help, "$stem $primaryName")
        }

        for (method in obj.javaClass.declaredMethods) {
            val handler = method.getAnnotation<Handler?>(Handler::class.java) ?: continue

            method.setAccessible(true)

            val aliases = handler.value
            val primaryName = if (aliases.isEmpty()) method.name else aliases[0]

            var isSectionApplied = false
            val currentSection = StringBuilder()
            currentSection.append(stem).append(' ')
                .append(primaryName)

            var theMethod: ArgumentBuilder<ClientCommandSource, *>
            if (method.parameterCount > 0) {
                val paramNames = arrayOfNulls<String>(method.parameterCount)
                val paramTypes = method.parameterTypes
                val params = method.parameters
                val name = params[0].name
                paramNames[0] = name

                val argumentType: ArgumentType<*>? = getArgumentType(paramTypes[0])
                if (argumentType == null) {
                    LOGGER.error(
                        "Failed to find argument type for parameter {} of method {} in class {}",
                        paramNames[0], method.getName(), obj.javaClass.getName()
                    )
                    continue
                }

                theMethod = argument(name, argumentType)

                if (!isRootFormattingSetup && isRoot) {
                    help.append(':').append('\n')
                    isRootFormattingSetup = true
                }

                isSectionApplied = true
                help.append(currentSection)
                help.append('<')
                help.append(paramNames[0])

                var annotation = params[0].getAnnotation<Param?>(Param::class.java)
                if (annotation != null) {
                    help.append(": ").append(annotation.value)
                }

                for (i in 1..<paramNames.size) {
                    val paramType: ArgumentType<*>? = getArgumentType(paramTypes[i])
                    if (paramType == null) {
                        LOGGER.error(
                            "Failed to find argument type for parameter {} of method {} in class {}",
                            params[i].getName(), method.getName(), obj.javaClass.getName()
                        )
                        continue
                    }

                    val p = params[i]
                    val name = p.getName()
                    help.append(", ").append(name)
                    annotation = p.getAnnotation<Param?>(Param::class.java)
                    if (annotation != null) {
                        help.append(": ").append(annotation.value)
                    }

                    paramNames[i] = name
                    theMethod = theMethod.then(argument(name, paramType))
                }

                help.append('>')
                theMethod.executes { ctx: CommandContext<ClientCommandSource?>? ->
                    val args = arrayOfNulls<Any>(paramTypes.size)
                    for (i in paramTypes.indices) {
                        args[i] = ctx!!.getArgument(paramNames[i], paramTypes[i])
                    }
                    try {
                        method.invoke(obj, *args)
                        return@executes 1
                    } catch (e: Exception) {
                        LOGGER.error("Failed to execute command!", e)
                        throw RuntimeException(e)
                    }
                }

                tree.then(CommandManager.literal(method.name).then(theMethod))
                for (s in handler.value) {
                    tree.then(CommandManager.literal(s).then(theMethod))
                }
            } else {
                if (!isRootCommandSetup && aliases.isEmpty()) {
                    isRootCommandSetup = true

                    // the root command gets no help entry of its own
                    tree.executes { ctx: CommandContext<ClientCommandSource> ->
                        try {
                            method.invoke(obj)
                            return@executes 1
                        } catch (e: Exception) {
                            LOGGER.error("Failed to execute command!", e)
                            throw RuntimeException(e)
                        }
                    }
                } else {
                    if (!isRootFormattingSetup && isRoot) {
                        help.append(':').append('\n')
                        isRootFormattingSetup = true
                    }

                    isSectionApplied = true
                    help.append(currentSection)

                    theMethod = CommandManager.literal(method.getName())
                    theMethod.executes { ctx: CommandContext<ClientCommandSource> ->
                        try {
                            method.invoke(obj)
                            return@executes 1
                        } catch (e: Exception) {
                            LOGGER.error("Failed to execute command!", e)
                            throw RuntimeException(e)
                        }
                    }

                    tree.then(theMethod)

                    for (s in handler.value) {
                        tree.then(CommandManager.literal(s).then(theMethod))
                    }
                }
            }

            if (isSectionApplied) {
                val description = handler.description
                if (!description.isEmpty()) {
                    help.append(": ").append(description)
                } else {
                    help.append(" (no description provided)")
                }

                help.append('\n')
            }
        }
    }

    companion object {
        private val LOGGER: Logger = LogManager.getLogger("OneConfig/BrigaiderTranslator")
    }
}