/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

@file:JvmName("BuilderUtils")
@file:Suppress("unused")

package org.polyfrost.oneconfig.api.commands.factories.builder

import org.polyfrost.oneconfig.api.commands.factories.builder.CommandBuilder.Arg

@JvmOverloads
fun arg(name: String? = null, description: String? = null, arity: Int = 1, type: Class<*>) =
    Arg(name, description, arity, type)

@JvmOverloads
fun stringArg(name: String? = null, description: String? = null, arity: Int = 1) =
    arg(name, description, arity, String::class.java)

@JvmOverloads
fun intArg(name: String? = null, description: String? = null, arity: Int = 1) =
    arg(name, description, arity, Int::class.java)

@JvmOverloads
fun floatArg(name: String? = null, description: String? = null, arity: Int = 1) =
    Arg(name, description, arity, Float::class.java)

@JvmOverloads
fun doubleArg(name: String? = null, description: String? = null, arity: Int = 1) =
    Arg(name, description, arity, Double::class.java)

@JvmOverloads
fun byteArg(name: String? = null, description: String? = null, arity: Int = 1) =
    Arg(name, description, arity, Byte::class.java)

@JvmOverloads
fun shortArg(name: String? = null, description: String? = null, arity: Int = 1) =
    Arg(name, description, arity, Short::class.java)

@JvmOverloads
fun longArg(name: String? = null, description: String? = null, arity: Int = 1) =
    Arg(name, description, arity, Long::class.java)

@JvmOverloads
fun booleanArg(name: String? = null, description: String? = null, arity: Int = 1) =
    Arg(name, description, arity, Boolean::class.java)

@JvmOverloads
fun charArg(name: String? = null, description: String? = null, arity: Int = 1) =
    Arg(name, description, arity, Char::class.java)