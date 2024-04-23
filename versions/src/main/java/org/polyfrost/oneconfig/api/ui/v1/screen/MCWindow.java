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

package org.polyfrost.oneconfig.api.ui.v1.screen;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.libs.universal.UResolution;
import org.polyfrost.oneconfig.libs.universal.UScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.renderer.Window;
import org.polyfrost.polyui.renderer.data.Cursor;

//#if MC>=11300
//$$ import static org.lwjgl.glfw.GLFW.*;
//#endif

@ApiStatus.Internal
public class MCWindow extends Window {
    private final Minecraft mc;
    //#if MC>=11300
    //$$ private final long handle;
    //#endif

    public MCWindow(Minecraft minecraft) {
        super(UResolution.getViewportWidth(), UResolution.getViewportHeight(), 1f);
        this.mc = minecraft;
        //#if MC>=11300
        //$$ this.handle = mc
                //#if MC>=11700
                //$$ .getWindow()
                //#else
        //$$         .getMainWindow()
                //#endif
        //$$         .getHandle();
        //#endif
    }

    @Override
    public void close() {
        UScreen.displayScreen(null);
    }

    @NotNull
    @Override
    public Window open(@NotNull PolyUI polyUI) {
        throw new UnsupportedOperationException("Cannot be opened this way, see PolyUIScreen");
    }

    @Override
    public boolean supportsRenderPausing() {
        return false;
    }

    @Nullable
    @Override
    public String getClipboard() {
        //#if MC>=11300
        //$$ return glfwGetClipboardString(handle);
        //#else
        try {
            return java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor).toString();
        } catch (Exception ignored) {
            return null;
        }
        //#endif
    }

    @Override
    public void setClipboard(@Nullable String s) {
        //#if MC>=11300
        //$$ glfwSetClipboardString(handle, s);
        //#else
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(s), null);
        //#endif
    }

    @Override
    public void setCursor(@NotNull Cursor cursor) {
        //#if MC>=11300
        //$$ switch (cursor) {
        //$$     case Pointer:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_ARROW_CURSOR));
        //$$         return;
        //$$     case Clicker:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_HAND_CURSOR));
        //$$         return;
        //$$     case Text:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_IBEAM_CURSOR));
        //$$ }
        //#endif
    }

    @NotNull
    @Override
    public String getKeyName(int i) {
        String k =
                //#if MC>=11300
                //$$ glfwGetKeyName(i, 0);
                //#else
                org.lwjgl.input.Keyboard.getKeyName(i);
        //#endif
        return k == null ? "Unknown" : k;
    }
}
