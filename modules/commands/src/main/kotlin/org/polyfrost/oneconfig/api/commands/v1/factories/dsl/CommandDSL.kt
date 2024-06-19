/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */
@file:Suppress("unused", "deprecation_error", "DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE")

package org.polyfrost.oneconfig.api.commands.v1.factories.dsl

import org.polyfrost.oneconfig.api.commands.v1.CommandTree
import org.polyfrost.oneconfig.api.commands.v1.Executable
import org.polyfrost.oneconfig.api.commands.v1.Executable.Param
import org.polyfrost.oneconfig.api.commands.v1.arguments.ArgumentParser
import org.polyfrost.oneconfig.api.commands.v1.factories.dsl.CommandDSL.Companion.param
import org.polyfrost.oneconfig.api.commands.v1.factories.dsl.CommandDSL.ParamData
import org.polyfrost.oneconfig.utils.v1.MHUtils
import java.lang.reflect.Method

/**
 * Command DSL for Kotlin.
 *
 * Uses some 'interesting' hacks to get the method handle of the function passed to the DSL. Unfortunately, at the moment, lambda parameters cannot be annotated,
 * so the [ParamData] class and [param] function are used to provide metadata for the parameters.
 */
@Deprecated("Currently not working due to K2 changing how it makes lambda functions.")
@DeprecatedSinceKotlin(errorSince = "2.0")
class CommandDSL @JvmOverloads constructor(private val parsers: Map<Class<*>, ArgumentParser<*>>, vararg name: String, description: String? = null) {
    internal val tree = CommandTree(name, description)
    var description: String?
        get() = tree.description()
        set(value) {
            tree.setDescription(value)
        }

    fun command(
        vararg aliases: String,
        description: String? = null,
        greedy: Boolean = false,
        paramData: List<ParamData> = listOf(),
        func: Function<*>
    ) {
        // asm: kotlin compiler produces two methods: public synthetic bridge invoke(Object): Object
        // public final invoke(Object...): Object which is what we want
        val method = func.javaClass.declaredMethods[1]
        val m = MHUtils.getMethodHandle(method, func).getOrThrow()
        tree.put(
            Executable(
                aliases,
                description,
                mapParams(method, paramData, parsers),
                greedy
            ) { if (it == null) m.invoke() else m.invokeWithArguments(*it) }
        )
    }

    fun cmd(
        vararg aliases: String,
        description: String? = null,
        greedy: Boolean = false,
        paramData: List<ParamData> = listOf(),
        func: Function<*>
    ) =
        command(*aliases, description = description, greedy = greedy, paramData = paramData, func = func)

    fun subcommand(vararg aliases: String, func: CommandDSL.() -> Unit) {
        tree.put(CommandDSL(parsers, *aliases).apply(func).tree)
    }

    fun subcmd(vararg aliases: String, func: CommandDSL.() -> Unit) = subcommand(*aliases, func = func)

    data class ParamData(val index: Int, val name: String, val description: String? = null, val arity: Int = 1)

    companion object {
        @JvmStatic
        @JvmSynthetic
        fun command(parsers: Map<Class<*>, ArgumentParser<*>>, vararg name: String, description: String? = null, func: CommandDSL.() -> Unit) = CommandDSL(
            parsers, *name, description = description
        ).apply(func)

        @JvmStatic
        fun param(index: Int, name: String, description: String? = null, arity: Int = 1) =
            ParamData(index, name, description, arity)

        private fun mapParams(method: Method, metadata: List<ParamData>, parsers: Map<Class<*>, ArgumentParser<*>>): Array<Param> {
            val params = method.parameters
            return Array(method.parameterCount) {
                val m = metadata.find { data -> data.index == it }
                val type = params[it].type
                Param.create(m?.name ?: type.simpleName, m?.description, type, m?.arity ?: 1, parsers)
            }
        }
    }
}



