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

import org.junit.jupiter.api.Test

class DSLTest {
    @Test
    fun main() {
        /*
        val tree = command(ArgumentParser.defaultParsers, "test") {
            command(
                "sup", "hello", paramData = listOf(
                    param(0, "a", "an integer"),
                    param(3, "some float", "a float"),
                )
            ) { a: Int, b: String, c: Float, d: Double, e: Byte, f: Float ->
                println(a)
                println(b)
                println(c + f)
                println(e)
                println(d)
            }
            subcmd("jeff") {
                cmd("chicken") {
                    println("chicken")
                }
                cmd("chicken") { a: Int, b: Int ->
                    return@cmd a + b
                }
                subcmd("bob") {
                    cmd("jeff") { c: Double, d: Double ->
                        println(c + d)
                        return@cmd c + d
                    }
                }
            }
        }.tree

        tree.execute("sup", "1", "hello", "3.5", "4.2", "1", "4.5")
        tree.execute("jeff", "chicken")
        assertEquals(3, tree.execute("jeff", "chicken", "1", "2"))
        assertEquals(3.0, tree.execute("jeff", "bob", "jeff", "1.5", "1.5"))
        */
    }
}