/*
 * This file is part of essential-gradle-toolkit, licensed under the GPL-3.0.
 *
 * Copyright (C) 2025 EssentialGG and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gg.essential.gradle.util

import kotlin.metadata.jvm.JvmMetadataVersion
import java.util.Calendar
import java.util.GregorianCalendar

internal fun compatibleKotlinMetadataVersion(version: IntArray): JvmMetadataVersion {
    // Upgrade versions older than 1.4 to 1.4 in accordance with https://youtrack.jetbrains.com/issue/KT-41011
    if (version.size < 2 || version[0] < 1 || version[0] <= 1 && version[1] < 4) {
        return JvmMetadataVersion(1, 4)
    }
    return JvmMetadataVersion(version[0], version[1], version[2])
}

// A safe constant value for creating consistent zip entries
// From https://github.com/gradle/gradle/blob/d6c7fd470449a59fc57a26b4ebc0ad83c64af50a/subprojects/core/src/main/java/org/gradle/api/internal/file/archive/ZipCopyAction.java#L42-L57
val CONSTANT_TIME_FOR_ZIP_ENTRIES = GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0).timeInMillis