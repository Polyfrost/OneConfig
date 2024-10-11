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

package org.polyfrost.oneconfig.api.ui.v1.internal

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs.*
import org.polyfrost.oneconfig.api.ui.v1.TinyFD
import org.polyfrost.polyui.utils.mapToArray
import java.nio.file.Path
import java.nio.file.Paths

class TinyFDImpl : TinyFD {
    override fun openSaveSelector(title: String?, defaultFilePath: String?, filterPatterns: Array<String>?, filterDescription: String?) =
        tinyfd_saveFileDialog(title ?: "Save", defaultFilePath, malloc(filterPatterns), filterDescription)?.toPath()

    override fun openFileSelector(title: String?, defaultFilePath: String?, filterPatterns: Array<String>?, filterDescription: String?) =
        tinyfd_openFileDialog(title ?: "Open file", defaultFilePath, malloc(filterPatterns), filterDescription, false)?.toPath()

    override fun openMultiFileSelector(title: String?, defaultFilePath: String?, filterPatterns: Array<String>?, filterDescription: String?): Array<Path>? {
        val out = tinyfd_openFileDialog(title ?: "Open files", defaultFilePath, malloc(filterPatterns), filterDescription, true)
        return out?.split("|")?.dropLastWhile { it.isEmpty() }?.mapToArray { it.toPath() }
    }

    override fun openFolderSelector(title: String?, defaultFolderPath: String?) =
        tinyfd_selectFolderDialog(title ?: "Select folder", defaultFolderPath)?.toPath()

    override fun showMessageBox(title: String, message: String, dialog: String, icon: String, defaultState: Boolean) =
        tinyfd_messageBox(title, message, dialog, icon, defaultState)

    override fun showNotification(title: String, message: String, icon: String) =
        tinyfd_notifyPopup(title, message, icon)

    private fun malloc(strings: Array<String>?): PointerBuffer? {
        if (strings == null) return null
        MemoryStack.stackPush().use { stack ->
            val p = stack.mallocPointer(strings.size)
            for (i in strings.indices) {
                p.put(i, stack.UTF8(strings[i]))
            }
            return p.flip()
        }
    }

    private fun String.toPath() = Paths.get(this)
}