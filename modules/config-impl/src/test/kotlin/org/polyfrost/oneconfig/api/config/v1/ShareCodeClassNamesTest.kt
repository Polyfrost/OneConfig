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

package org.polyfrost.oneconfig.api.config.v1

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer
import java.util.function.Function

/**
 * Share codes while classes go by other names than the ones they are saved under, as those of a mod do that Hotwire
 * runs as a copy
 */
class ShareCodeClassNamesTest {
    private val savedName = ObjectSerializer.savedName
    private val runningClass = ObjectSerializer.runningClass

    @AfterEach
    fun tearDown() {
        ObjectSerializer.savedName = savedName
        ObjectSerializer.runningClass = runningClass
    }

    @Test
    fun valuesAreReadIntoTheClassThatRunsUnderTheirSavedName() {
        ObjectSerializer.savedName = Function { if (it == Month::class.java) "own.Month" else savedName.apply(it) }
        ObjectSerializer.runningClass = Function { if (it == "own.Month") Month::class.java else runningClass.apply(it) }
        val months = Properties.simple("months", "Months", "", arrayListOf(Month.FEB))
        val tree = Tree.tree("share_code_class_names").put(months)
        val code = tree.toShareCode("t:")
        months.setAs(arrayListOf(Month.JAN))

        val result = tree.applyShareCode(code, "t:")

        assertEquals(ShareCodeResult.Success(listOf("months"), emptyList()), result)
        assertEquals(listOf(Month.FEB), months.get())
    }

    // a code is checked before anything is made from it, which only holds for the class the check has seen
    @Test
    fun theClassThatIsCheckedIsTheOneThatIsMade() {
        val values = Properties.simple("values", "Values", "", arrayListOf<Any?>(Unchecked.ENUM))
        val tree = Tree.tree("share_code_class_names").put(values)
        val code = tree.toShareCode("t:")
        values.setAs(ArrayList<Any?>())
        // the name of an enum, which may be made from a code, now stands for a class that may not
        ObjectSerializer.runningClass = Function {
            if (it == Unchecked::class.java.name) Made::class.java else runningClass.apply(it)
        }

        val result = tree.applyShareCode(code, "t:")

        assertEquals(0, Made.count)
        assertEquals(ShareCodeResult.Success(emptyList(), listOf("values")), result)
    }

    private enum class Month { JAN, FEB }

    private enum class Unchecked { ENUM }

    class Made {
        init {
            count++
        }

        companion object {
            var count = 0
        }
    }
}
