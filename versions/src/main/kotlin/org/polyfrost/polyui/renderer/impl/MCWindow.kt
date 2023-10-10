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

package org.polyfrost.polyui.renderer.impl

import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.libs.universal.UResolution
import org.polyfrost.oneconfig.libs.universal.UScreen
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.renderer.Window
import org.polyfrost.polyui.renderer.data.Cursor

//#if MC>=11300
//$$ import org.lwjgl.glfw.GLFW.*
//#endif

class MCWindow(private val mc: Minecraft) : Window(
    UResolution.viewportWidth, UResolution.viewportHeight
) {

    //#if MC>=11300
    //$$ private val handle =
    //#if MC>=11700
    //$$     mc.window.handle
    //#else
    //$$ mc.mainWindow.handle
    //#endif
    //#endif

    override fun close() {
        UScreen.displayScreen(null)
    }

    override fun createCallbacks() {
    }

    override fun getClipboard(): String? {
        //#if MC>=11300
        //$$ return glfwGetClipboardString(handle)
        //#else
        return java.awt.Toolkit.getDefaultToolkit().systemClipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
        //#endif
    }

    override fun getKeyName(key: Int): String {
        //#if MC>=11300
        //$$ return glfwGetKeyName(key, 0) ?: "Unknown"
        //#else
        return org.lwjgl.input.Keyboard.getKeyName(key) ?: "Unknown"
        //#endif
    }

    override fun open(polyUI: PolyUI): Window {
        throw UnsupportedOperationException("cannot be opened")
    }

    override fun setClipboard(text: String?) {
        //#if MC>=11300
        //$$ glfwSetClipboardString(handle, text ?: "")
        //#else
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
        //#endif
    }

    override fun setCursor(cursor: Cursor) {
        //#if MC>=11300
        //$$ glfwSetCursor(
        //$$     handle,
        //$$     glfwCreateStandardCursor(
        //$$         when (cursor) {
        //$$             Cursor.Pointer -> GLFW_ARROW_CURSOR
        //$$             Cursor.Clicker -> GLFW_HAND_CURSOR
        //$$             Cursor.Text -> GLFW_IBEAM_CURSOR
        //$$         }
        //$$     )
        //$$ )
        //#endif
    }
}