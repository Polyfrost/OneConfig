package org.polyfrost.oneconfig.api.commands.v1

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.polyfrost.oneconfig.api.commands.v1.factories.CommandFactory
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.AnnotationCommandFactory
import org.polyfrost.oneconfig.utils.v1.WrappingUtils
import java.util.*


/**
 * Handles the registration of OneConfig commands
 *
 * @see Command
 */
typealias ClientCommandSource = FabricClientCommandSource

object CommandManager {
    private val factories: MutableSet<CommandFactory> = HashSet<CommandFactory>()
    private val argumentTypes: MutableMap<Class<*>, ArgumentType<*>> = IdentityHashMap<Class<*>, ArgumentType<*>>()


    init {
        argumentTypes[Int::class.javaPrimitiveType!!] = IntegerArgumentType.integer()
        argumentTypes[String::class.java] = StringArgumentType.string()
        argumentTypes[Array<String>::class.java] = StringArgumentType.greedyString()
        argumentTypes[Boolean::class.javaPrimitiveType!!] = BoolArgumentType.bool()
        argumentTypes[Float::class.javaPrimitiveType!!] = FloatArgumentType.floatArg()
        argumentTypes[Double::class.javaPrimitiveType!!] = DoubleArgumentType.doubleArg()
        argumentTypes[Long::class.javaPrimitiveType!!] = LongArgumentType.longArg()
        registerFactory(AnnotationCommandFactory())

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, context ->
            commands.forEach { dispatcher.root.addChild(it) }
        }
    }

    fun create(obj: Any): Array<LiteralCommandNode<ClientCommandSource>?>? {
        for (factory in factories) {
            try {
                val nodes = factory.create(obj)
                if (nodes != null) {
                    return nodes
                }
            } catch (e: Exception) {
                LOGGER.error(
                    "Command factory {} failed with an exception trying to create commands for object {}",
                    factory,
                    obj,
                    e
                )
            }
        }

        return null
    }

    /**
     * Register a factory which can be used to create commands from objects in the [.create] method
     */
    fun registerFactory(factory: CommandFactory) {
        factories.add(factory)
    }

    fun <T> registerArgumentType(type: Class<T>, argumentType: ArgumentType<T>) {
        argumentTypes[type] = argumentType
    }

    private val commands = mutableListOf<LiteralCommandNode<ClientCommandSource>>()

    private val LOGGER: Logger = LogManager.getLogger("OneConfig/CommandManger")


    fun register(builder: LiteralArgumentBuilder<ClientCommandSource>) {
        register(builder.build())
    }

    fun register(node: LiteralCommandNode<ClientCommandSource>) {
        commands.add(node)
    }

    @JvmStatic
    fun register(obj: Any): Boolean {
        val nodes: Array<LiteralCommandNode<ClientCommandSource>?> = create(obj) ?: return false

        for (node in nodes) {
            if (node == null) {
                LOGGER.warn("Command node for object {} is null, skipping registration", obj)
                continue
            }

            commands.add(node)
        }

        return true
    }

    @JvmStatic
    fun literal(name: String): LiteralArgumentBuilder<ClientCommandSource> {
        return LiteralArgumentBuilder.literal<ClientCommandSource>(name)
    }

    fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<ClientCommandSource, T> {
        return RequiredArgumentBuilder.argument<ClientCommandSource, T>(name, type)
    }

    fun <T> getArgumentType(type: Class<T>): ArgumentType<T>? {
        @Suppress("UNCHECKED_CAST")
        return argumentTypes[WrappingUtils.getUnwrapped(type)] as ArgumentType<T>?
    }

}